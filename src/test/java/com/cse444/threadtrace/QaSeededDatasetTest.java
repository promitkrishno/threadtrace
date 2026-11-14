package com.cse444.threadtrace;

import com.cse444.threadtrace.common.config.TraceabilityProperties;
import com.cse444.threadtrace.common.error.DomainException;
import com.cse444.threadtrace.evidence.EvidenceService;
import com.cse444.threadtrace.evidence.domain.EvidenceDocumentEntity;
import com.cse444.threadtrace.evidence.domain.EvidenceStatus;
import com.cse444.threadtrace.evidence.dto.UploadEvidenceRequest;
import com.cse444.threadtrace.evidence.infrastructure.EvidenceDocumentRepository;
import com.cse444.threadtrace.evidence.observer.EvidenceEventListener;
import com.cse444.threadtrace.evidence.observer.EvidenceExpiryScheduler;
import com.cse444.threadtrace.lineage.application.*;
import com.cse444.threadtrace.lineage.domain.composite.CompositeTransformedLot;
import com.cse444.threadtrace.lineage.domain.composite.LeafSourceLot;
import com.cse444.threadtrace.lineage.domain.model.EventType;
import com.cse444.threadtrace.lineage.domain.model.LotEntity;
import com.cse444.threadtrace.lineage.domain.model.LotStatus;
import com.cse444.threadtrace.lineage.domain.model.Unit;
import com.cse444.threadtrace.lineage.domain.strategy.AllocationStrategyRegistry;
import com.cse444.threadtrace.lineage.domain.visitor.ClaimRollupResult;
import com.cse444.threadtrace.lineage.domain.visitor.ClaimRollupVisitor;
import com.cse444.threadtrace.lineage.domain.visitor.FootprintRollupVisitor;
import com.cse444.threadtrace.lineage.infrastructure.LotRepository;
import com.cse444.threadtrace.registry.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Milestone 2 QA Test Suite:
 * Validates the detection and prevention of all 4 planted supply chain defects:
 * 1. Over-claimed recycled content (e.g. claim 60% with only 30% certified input).
 * 2. Expired certificate (valid_until in past -> quarantined & claims revoked).
 * 3. Mass balance violation (e.g. 20% cutting loss > 12% configured limit).
 * 4. Orphan lot (lots lacking provenance or parent links -> flagged).
 * Ensures ZERO unsupported claims are approved.
 */
@SpringBootTest
@ActiveProfiles("test")
class QaSeededDatasetTest {

    private static final Logger log = LoggerFactory.getLogger(QaSeededDatasetTest.class);

    @Autowired
    private RegistryService registryService;

    @Autowired
    private RecordLotEventUseCase recordLotEventUseCase;

    @Autowired
    private MassBalanceValidator massBalanceValidator;

    @Autowired
    private EvidenceService evidenceService;

    @Autowired
    private EvidenceExpiryScheduler expiryScheduler;

    @Autowired
    private EvidenceEventListener evidenceEventListener;

    @Autowired
    private EvidenceDocumentRepository evidenceRepository;

    @Autowired
    private LotRepository lotRepository;

    @Autowired
    private LineageTracePort lineageTracePort;

    @Autowired
    private AllocationStrategyRegistry strategyRegistry;

    @Autowired
    private TraceabilityProperties traceabilityProperties;

    private UUID brandId;
    private UUID millFacilityId;
    private UUID factoryFacilityId;

    @BeforeEach
    void setUp() {
        evidenceEventListener.clearHistory();

        PartyResponse brand = registryService.registerParty(new CreatePartyRequest(
                PartyType.BRAND, "Patagonia QA Brand", "BRAND-QA-" + UUID.randomUUID().toString().substring(0, 8),
                "US", "Ventura, CA", 100, BigDecimal.valueOf(2500.0)
        ));
        brandId = brand.id();

        PartyResponse mill = registryService.registerParty(new CreatePartyRequest(
                PartyType.FABRIC_MILL, "Shasha Denim QA", "MILL-QA-" + UUID.randomUUID().toString().substring(0, 8),
                "BD", "DEPZ Dhaka", 800, BigDecimal.valueOf(350.0)
        ));
        FacilityResponse millFac = registryService.registerFacility(new CreateFacilityRequest(
                mill.id(), FacilityType.MILL, null, "Shasha Spinning Facility 1", "Dhaka"
        ));
        millFacilityId = millFac.id();

        PartyResponse factory = registryService.registerParty(new CreatePartyRequest(
                PartyType.FACTORY, "DBL Garments QA", "FAC-QA-" + UUID.randomUUID().toString().substring(0, 8),
                "BD", "Gazipur", 2500, BigDecimal.valueOf(500.0)
        ));
        FacilityResponse fac = registryService.registerFacility(new CreateFacilityRequest(
                factory.id(), FacilityType.FACTORY, null, "DBL Sewing Unit 1", "Gazipur"
        ));
        factoryFacilityId = fac.id();
    }

    @Test
    @DisplayName("Planted Problem 1: Over-claimed recycled content is detected and rejected")
    void plantedProblem1_overClaimedRecycledContent() {
        log.info("Testing Planted Problem 1: Over-claimed recycled content");

        // 1. Genesis source lot with 30.0% certified recycled content
        LeafSourceLot certifiedSource = new LeafSourceLot(
                UUID.randomUUID(),
                "LOT-CERT-COTTON-30",
                BigDecimal.valueOf(100.0),
                "KG",
                BigDecimal.valueOf(200.0),
                BigDecimal.valueOf(25.0),
                BigDecimal.valueOf(10.0)
        ).withCertifications(BigDecimal.valueOf(30.0), BigDecimal.ZERO, true)
         .withClaims(BigDecimal.valueOf(30.0), BigDecimal.ZERO);

        // 2. Child garment claims 60.0% recycled content (unsupported greenwashing!)
        CompositeTransformedLot greenwashedGarment = new CompositeTransformedLot(
                UUID.randomUUID(),
                "LOT-GARMENT-CLAIM-60",
                BigDecimal.valueOf(500.0),
                "PIECES",
                BigDecimal.valueOf(50.0),
                BigDecimal.valueOf(10.0),
                BigDecimal.valueOf(5.0)
        ).withClaims(BigDecimal.valueOf(60.0), BigDecimal.ZERO); // Claims 60%!

        greenwashedGarment.addParent(certifiedSource);

        // 3. Execute Visitor roll-up
        ClaimRollupVisitor visitor = new ClaimRollupVisitor();
        greenwashedGarment.accept(visitor);
        ClaimRollupResult result = visitor.getResult();

        // 4. Verification: Claim MUST be rejected, verified stays at 30%
        assertThat(result.claimsValid()).isFalse();
        assertThat(result.verifiedRecycledPct()).isEqualByComparingTo("30.0");
        assertThat(result.claimedRecycledPct()).isEqualByComparingTo("60.0");
        assertThat(result.violations()).isNotEmpty();
        assertThat(result.violations().get(0)).contains("claims 60.0% recycled content, exceeding verified certified input of 30.00%");

        log.info("Problem 1 caught successfully: {}", result.violations().get(0));
    }

    @Test
    @DisplayName("Planted Problem 2: Expired certificate triggers automatic quarantine and revocation")
    void plantedProblem2_expiredCertificateRevocation() {
        log.info("Testing Planted Problem 2: Expired certificate revocation");

        // 1. Create a lot
        LotEntity lot = new LotEntity(
                "LOT-EXP-TEST-01",
                brandId,
                millFacilityId,
                "STYLE-EXP",
                "SHIP-EXP",
                LotStatus.ACTIVE,
                BigDecimal.valueOf(100.0),
                Unit.KG
        );
        lot.setCertifiedRecycledPct(BigDecimal.valueOf(50.0));
        lot = lotRepository.save(lot);

        // 2. Attach an existing ACTIVE certificate whose validUntil has elapsed (expired yesterday)
        EvidenceDocumentEntity cert = new EvidenceDocumentEntity(
                "GRS",
                "Control Union",
                "Global Recycled Standard Certificate",
                "RECYCLED",
                BigDecimal.valueOf(50.0),
                "certificates/grs_expired.pdf",
                "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                LocalDate.now().minusYears(1),
                LocalDate.now().minusDays(1), // EXPIRED yesterday
                null,
                lot.getId()
        );
        cert.setStatus(EvidenceStatus.ACTIVE); // Was active, now past expiration date
        evidenceRepository.save(cert);

        // 3. Run scheduled expiry observer job
        var summary = expiryScheduler.checkAndPublishExpiryEvents();
        assertThat(summary.expiredCount()).isGreaterThanOrEqualTo(1);

        // 4. Assert Observer Pattern received the event
        List<?> expiredEvents = evidenceEventListener.getReceivedExpiredEvents();
        assertThat(expiredEvents).isNotEmpty();

        // 5. Assert lot was automatically QUARANTINED and claims revoked
        LotEntity updatedLot = lotRepository.findById(lot.getId()).orElseThrow();
        assertThat(updatedLot.getStatus()).isEqualTo(LotStatus.QUARANTINED);
        assertThat(updatedLot.getCertifiedRecycledPct()).isEqualByComparingTo(BigDecimal.ZERO);

        log.info("Problem 2 caught successfully: Lot quarantined and certificate revoked");
    }

    @Test
    @DisplayName("Planted Problem 3: Excessive cutting loss (>12%) is flagged and quarantined")
    void plantedProblem3_massBalanceExcessiveLoss() {
        log.info("Testing Planted Problem 3: Mass balance excessive loss");

        // 1. Create fabric lot with 100 KG
        RecordEventResult genesis = recordLotEventUseCase.recordEvent(new RecordEventCommand(
                null,
                "LOT-FABRIC-MB-01",
                brandId,
                "STYLE-MB",
                "SHIP-MB",
                Unit.KG,
                EventType.RECEIVE,
                factoryFacilityId,
                BigDecimal.valueOf(100.0),
                BigDecimal.valueOf(100.0),
                "KG",
                null,
                null,
                null
        ));
        UUID fabricLotId = genesis.lotId();

        // 2. Perform CUT with 20% loss (input 100 KG -> output 80 KG). Max allowed cutting loss is 12%.
        RecordEventResult cutResult = recordLotEventUseCase.recordEvent(new RecordEventCommand(
                fabricLotId,
                null,
                null,
                null,
                null,
                Unit.KG,
                EventType.CUT,
                factoryFacilityId,
                BigDecimal.valueOf(100.0),
                BigDecimal.valueOf(80.0), // 20% loss
                "KG",
                null,
                null,
                null
        ));

        // 3. Assert mass balance violation was flagged and lot QUARANTINED
        assertThat(cutResult.lossPercentage()).isEqualByComparingTo("20.00");
        assertThat(cutResult.lotStatus()).isEqualTo(LotStatus.QUARANTINED);

        LotEntity quarantinedLot = lotRepository.findById(fabricLotId).orElseThrow();
        assertThat(quarantinedLot.getStatus()).isEqualTo(LotStatus.QUARANTINED);

        // Also test direct BLOCK configuration with MassBalanceValidator
        TraceabilityProperties blockProps = new TraceabilityProperties(
                traceabilityProperties.processSteps(),
                traceabilityProperties.validCertificateTypes(),
                traceabilityProperties.emissionFactors(),
                traceabilityProperties.storage(),
                TraceabilityProperties.MassBalanceAction.BLOCK,
                "MASS"
        );
        MassBalanceValidator blockingValidator = new MassBalanceValidator(blockProps);

        assertThatThrownBy(() -> blockingValidator.validate(
                EventType.CUT,
                BigDecimal.valueOf(100.0),
                BigDecimal.valueOf(80.0),
                quarantinedLot
        )).isInstanceOf(DomainException.class)
          .hasMessageContaining("exceeds configured limit of 12.00%");

        log.info("Problem 3 caught successfully: 20% loss quarantined and blocked");
    }

    @Test
    @DisplayName("Planted Problem 4: Orphan lot with no parent provenance is flagged and rejected")
    void plantedProblem4_orphanLotDetection() {
        log.info("Testing Planted Problem 4: Orphan lot detection");

        // 1. Orphan composite lot with no parents
        CompositeTransformedLot orphanGarment = new CompositeTransformedLot(
                UUID.randomUUID(),
                "LOT-ORPHAN-MYSTERY-01",
                BigDecimal.valueOf(200.0),
                "PIECES",
                BigDecimal.valueOf(20.0),
                BigDecimal.valueOf(5.0),
                BigDecimal.valueOf(2.0)
        ).withClaims(BigDecimal.valueOf(50.0), BigDecimal.ZERO);
        // NO parents added!

        // 2. Leaf lot without valid certificate is also an orphan
        LeafSourceLot uncertifiedLeaf = new LeafSourceLot(
                UUID.randomUUID(),
                "LOT-UNCERT-LEAF-01",
                BigDecimal.valueOf(50.0),
                "KG",
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO
        ).withCertifications(BigDecimal.valueOf(50.0), BigDecimal.ZERO, false); // hasCertificate = false!

        // 3. Test orphan composite
        ClaimRollupVisitor v1 = new ClaimRollupVisitor();
        orphanGarment.accept(v1);
        ClaimRollupResult res1 = v1.getResult();

        assertThat(res1.hasOrphanLot()).isTrue();
        assertThat(res1.claimsValid()).isFalse();
        assertThat(res1.orphanLotIds()).contains(orphanGarment.getLotId());

        // 4. Test uncertified leaf
        ClaimRollupVisitor v2 = new ClaimRollupVisitor();
        uncertifiedLeaf.accept(v2);
        ClaimRollupResult res2 = v2.getResult();

        assertThat(res2.hasOrphanLot()).isTrue();
        assertThat(res2.claimsValid()).isFalse();
        assertThat(res2.orphanLotIds()).contains(uncertifiedLeaf.getLotId());

        log.info("Problem 4 caught successfully: Both orphan scenarios detected and rejected");
    }

    @Test
    @DisplayName("Footprint Rollup with Allocation Strategies: Mass vs Piece Count")
    void footprintRollupAllocationStrategies() {
        // Parent: Fabric roll 100 KG with 1000 L water, 100 kWh energy, 50 kg CO2e
        LeafSourceLot fabric = new LeafSourceLot(
                UUID.randomUUID(),
                "LOT-FABRIC-ROLL-100KG",
                BigDecimal.valueOf(100.0),
                "KG",
                BigDecimal.valueOf(1000.0),
                BigDecimal.valueOf(100.0),
                BigDecimal.valueOf(50.0)
        ).withCertifications(BigDecimal.valueOf(100.0), BigDecimal.valueOf(100.0), true);

        // Child: 200 PIECES sewn (each piece nominal 200g = 40 KG used from 100 KG parent roll)
        // Child step adds 200 L water, 20 kWh energy, 10 kg CO2e
        CompositeTransformedLot garment = new CompositeTransformedLot(
                UUID.randomUUID(),
                "LOT-GARMENT-200PCS",
                BigDecimal.valueOf(200.0),
                "PIECES",
                BigDecimal.valueOf(200.0),
                BigDecimal.valueOf(20.0),
                BigDecimal.valueOf(10.0)
        );
        garment.addParent(fabric);

        // 1. Mass Allocation Strategy:
        // 200 PIECES = 40 KG. Ratio = 40 / 100 = 0.40.
        // Allocated parent footprint: 1000 * 0.4 = 400 L water, 100 * 0.4 = 40 kWh, 50 * 0.4 = 20 kg CO2e
        // Total footprint: 200 + 400 = 600 L water, 20 + 40 = 60 kWh, 10 + 20 = 30 kg CO2e
        FootprintRollupVisitor massVisitor = new FootprintRollupVisitor(strategyRegistry.getStrategy("MASS"));
        garment.accept(massVisitor);

        assertThat(massVisitor.getTotalWaterL()).isEqualByComparingTo("600.0");
        assertThat(massVisitor.getTotalEnergyKwh()).isEqualByComparingTo("60.0");
        assertThat(massVisitor.getTotalCo2eKg()).isEqualByComparingTo("30.0");

        // Per piece footprint (divided by 200 pieces):
        // 600 / 200 = 3.0 L/piece, 60 / 200 = 0.30 kWh/piece, 30 / 200 = 0.15 kg CO2e/piece
        var perPiece = massVisitor.getFootprintPerPiece(BigDecimal.valueOf(200.0));
        assertThat(perPiece.waterLiters()).isEqualByComparingTo("3.0000");
        assertThat(perPiece.energyKwh()).isEqualByComparingTo("0.3000");
        assertThat(perPiece.co2eKg()).isEqualByComparingTo("0.1500");

        // 2. Direct sum visitor (no allocation strategy)
        FootprintRollupVisitor rawSumVisitor = new FootprintRollupVisitor(null);
        garment.accept(rawSumVisitor);
        assertThat(rawSumVisitor.getTotalWaterL()).isEqualByComparingTo("1200.0"); // 1000 + 200
    }
}
