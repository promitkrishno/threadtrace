package com.cse444.threadtrace;

import com.cse444.threadtrace.lineage.application.*;
import com.cse444.threadtrace.lineage.domain.model.EventType;
import com.cse444.threadtrace.lineage.domain.model.LotEntity;
import com.cse444.threadtrace.lineage.domain.model.LotEventEntity;
import com.cse444.threadtrace.lineage.domain.model.Unit;
import com.cse444.threadtrace.lineage.infrastructure.LotEventRepository;
import com.cse444.threadtrace.lineage.infrastructure.LotRepository;
import com.cse444.threadtrace.registry.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class LineageIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(LineageIntegrationTest.class);

    @Autowired
    private RegistryService registryService;

    @Autowired
    private RecordLotEventUseCase recordLotEventUseCase;

    @Autowired
    private LineageTracePort lineageTracePort;

    @Autowired
    private LotRepository lotRepository;

    @Autowired
    private LotEventRepository lotEventRepository;

    @Test
    @DisplayName("Complete supply-chain lifecycle: Registry -> Split -> Merge -> Tamper Detection -> Recursive Trace")
    void fullSupplyChainLineageLifecycle() {
        log.info("===> STEP 1: Parties");
        // 1. Setup Parties (Brand, Mill, Garment Factory)
        PartyResponse brand = registryService.registerParty(new CreatePartyRequest(
                PartyType.BRAND, "EcoWear Global", "BRAND-ECO-001", "US", "100 Fashion Ave, NY", 50, BigDecimal.valueOf(1500.0)
        ));

        PartyResponse mill = registryService.registerParty(new CreatePartyRequest(
                PartyType.FABRIC_MILL, "Apex Spinning & Weaving", "MILL-APX-001", "BD", "Ashulia, Dhaka", 1200, BigDecimal.valueOf(450.0)
        ));

        PartyResponse factory = registryService.registerParty(new CreatePartyRequest(
                PartyType.FACTORY, "Green Garments Ltd", "FAC-GG-001", "BD", "Gazipur, Dhaka", 2500, BigDecimal.valueOf(650.0)
        ));

        log.info("===> STEP 2: Facilities");
        // 2. Setup Facilities
        FacilityResponse millFacility = registryService.registerFacility(new CreateFacilityRequest(
                mill.id(), FacilityType.MILL, null, "Apex Mill Plant 1", "Dhaka"
        ));

        FacilityResponse cuttingFacility = registryService.registerFacility(new CreateFacilityRequest(
                factory.id(), FacilityType.PRODUCTION_LINE, null, "Cutting Line A", "Gazipur"
        ));

        FacilityResponse sewingFacility = registryService.registerFacility(new CreateFacilityRequest(
                factory.id(), FacilityType.PRODUCTION_LINE, null, "Sewing Line 1", "Gazipur"
        ));

        log.info("===> STEP 3: Genesis Event");
        // 3. Genesis Event: Receive raw fabric roll (1000 KG)
        RecordEventResult genesisResult = recordLotEventUseCase.recordEvent(new RecordEventCommand(
                null,
                "LOT-FABRIC-ROLL-100",
                brand.id(),
                "STYLE-TEE-01",
                "SHIP-2026-Q4",
                Unit.KG,
                EventType.RECEIVE,
                millFacility.id(),
                BigDecimal.valueOf(1000.0),
                BigDecimal.valueOf(1000.0),
                "KG",
                Instant.now(),
                null,
                null
        ));

        UUID fabricLotId = genesisResult.lotId();
        assertThat(fabricLotId).isNotNull();
        assertThat(genesisResult.sequenceNo()).isEqualTo(1L);
        assertThat(genesisResult.previousEventHash()).isEqualTo(LotEventEntity.GENESIS_HASH);

        log.info("===> STEP 3b: Verify initial integrity");
        // Verify initial integrity
        IntegrityVerificationResult fabricIntegrity = recordLotEventUseCase.verifyLotIntegrity(fabricLotId);
        assertThat(fabricIntegrity.verified()).isTrue();
        assertThat(fabricIntegrity.totalEvents()).isEqualTo(1);

        log.info("===> STEP 4: Cut Split");
        // 4. 1-to-N Split: CUT fabric into 2 bundles (Body Bundle: 400 KG, Sleeves Bundle: 480 KG, 12% loss)
        RecordEventResult cutResult = recordLotEventUseCase.recordEvent(new RecordEventCommand(
                fabricLotId,
                null,
                null,
                null,
                null,
                null,
                EventType.CUT,
                cuttingFacility.id(),
                BigDecimal.valueOf(1000.0),
                BigDecimal.valueOf(880.0),
                "KG",
                Instant.now(),
                List.of(
                        new ChildLotOutput("LOT-CUT-BODY-101", "STYLE-TEE-01", "SHIP-2026-Q4", BigDecimal.valueOf(400.0), Unit.KG, BigDecimal.valueOf(0.45)),
                        new ChildLotOutput("LOT-CUT-SLEEVE-102", "STYLE-TEE-01", "SHIP-2026-Q4", BigDecimal.valueOf(480.0), Unit.KG, BigDecimal.valueOf(0.55))
                ),
                null
        ));

        assertThat(cutResult.createdChildLotIds()).hasSize(2);
        UUID bodyLotId = cutResult.createdChildLotIds().get(0);
        UUID sleeveLotId = cutResult.createdChildLotIds().get(1);

        log.info("===> STEP 5: Sew Merge");
        // 5. N-to-1 Merge: SEW body bundle + sleeve bundle into final sewn garment carton (1500 Pieces)
        RecordEventResult sewResult = recordLotEventUseCase.recordEvent(new RecordEventCommand(
                null,
                "LOT-SEWN-GARMENT-200",
                brand.id(),
                "STYLE-TEE-01",
                "SHIP-2026-Q4",
                Unit.PIECES,
                EventType.SEW,
                sewingFacility.id(),
                BigDecimal.valueOf(880.0),
                BigDecimal.valueOf(1500.0),
                "PIECES",
                Instant.now(),
                null,
                List.of(
                        new ParentLotInput(bodyLotId, BigDecimal.valueOf(400.0), "KG", BigDecimal.valueOf(0.45)),
                        new ParentLotInput(sleeveLotId, BigDecimal.valueOf(480.0), "KG", BigDecimal.valueOf(0.55))
                )
        ));

        UUID garmentLotId = sewResult.lotId();
        assertThat(garmentLotId).isNotNull();

        log.info("===> STEP 6: Verify Garment Integrity");
        // 6. Verify Cryptographic Integrity of Final Garment Lot
        IntegrityVerificationResult garmentIntegrity = recordLotEventUseCase.verifyLotIntegrity(garmentLotId);
        assertThat(garmentIntegrity.verified()).isTrue();

        log.info("===> STEP 7: Backward Trace");
        // 7. Backward Trace Query: From Sewn Garment back to Cotton/Fabric Roll
        LineageTraceResponse backwardTrace = lineageTracePort.traceBackward(garmentLotId);
        assertThat(backwardTrace.direction()).isEqualTo("BACKWARD");
        assertThat(backwardTrace.totalNodes()).isGreaterThanOrEqualTo(3);
        assertThat(backwardTrace.maxDepth()).isGreaterThanOrEqualTo(2);

        log.info("===> STEP 8: Forward Trace");
        // 8. Forward Trace Query: From Fabric Roll forward to Sewn Garments
        LineageTraceResponse forwardTrace = lineageTracePort.traceForward(fabricLotId);
        assertThat(forwardTrace.direction()).isEqualTo("FORWARD");
        assertThat(forwardTrace.totalNodes()).isGreaterThanOrEqualTo(3);
        assertThat(forwardTrace.maxDepth()).isGreaterThanOrEqualTo(2);

        log.info("===> STEP 9: Tamper Detection");
        // 9. Tamper Detection Test (FR10):
        LotEventEntity firstEvent = lotEventRepository.findByLotIdAndSequenceNo(fabricLotId, 1L).orElseThrow();
        firstEvent.setOutputQuantity(BigDecimal.valueOf(9999.0)); // Tampered!
        lotEventRepository.save(firstEvent);

        IntegrityVerificationResult tamperedResult = recordLotEventUseCase.verifyLotIntegrity(fabricLotId);
        assertThat(tamperedResult.verified()).isFalse();
        assertThat(tamperedResult.compromisedSequenceNo()).isEqualTo(1L);
        assertThat(tamperedResult.message()).contains("Hash mismatch");
        log.info("===> ALL STEPS COMPLETED SUCCESSFULLY!");
    }
}
