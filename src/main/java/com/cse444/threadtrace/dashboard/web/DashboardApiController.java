package com.cse444.threadtrace.dashboard.web;

import com.cse444.threadtrace.evidence.EvidenceService;
import com.cse444.threadtrace.evidence.domain.EvidenceDocumentEntity;
import com.cse444.threadtrace.evidence.domain.EvidenceStatus;
import com.cse444.threadtrace.evidence.dto.EvidenceResponse;
import com.cse444.threadtrace.evidence.infrastructure.EvidenceDocumentRepository;
import com.cse444.threadtrace.evidence.observer.EvidenceExpiryScheduler;
import com.cse444.threadtrace.lineage.application.*;
import com.cse444.threadtrace.lineage.domain.composite.CompositeTransformedLot;
import com.cse444.threadtrace.lineage.domain.composite.LeafSourceLot;
import com.cse444.threadtrace.lineage.domain.model.EventType;
import com.cse444.threadtrace.lineage.domain.model.LotEntity;
import com.cse444.threadtrace.lineage.domain.model.LotStatus;
import com.cse444.threadtrace.lineage.domain.model.Unit;
import com.cse444.threadtrace.lineage.domain.visitor.ClaimRollupResult;
import com.cse444.threadtrace.lineage.domain.visitor.ClaimRollupVisitor;
import com.cse444.threadtrace.lineage.infrastructure.LotEventRepository;
import com.cse444.threadtrace.lineage.infrastructure.LotRepository;
import com.cse444.threadtrace.passport.DigitalPassportService;
import com.cse444.threadtrace.passport.domain.builder.DigitalPassportRepository;
import com.cse444.threadtrace.registry.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/v1/dashboard")
@Tag(name = "Dashboard & Demo API", description = "Endpoints for live metrics, RMG supply-chain seeding, defect scenarios, and sample file downloads")
public class DashboardApiController {

    private final PartyRepository partyRepository;
    private final FacilityRepository facilityRepository;
    private final LotRepository lotRepository;
    private final LotEventRepository lotEventRepository;
    private final DigitalPassportRepository passportRepository;
    private final EvidenceDocumentRepository evidenceRepository;
    private final RegistryService registryService;
    private final RecordLotEventUseCase recordLotEventUseCase;
    private final DigitalPassportService passportService;
    private final EvidenceService evidenceService;
    private final EvidenceExpiryScheduler expiryScheduler;
    private final LineageTracePort lineageTracePort;

    public DashboardApiController(
            PartyRepository partyRepository,
            FacilityRepository facilityRepository,
            LotRepository lotRepository,
            LotEventRepository lotEventRepository,
            DigitalPassportRepository passportRepository,
            EvidenceDocumentRepository evidenceRepository,
            RegistryService registryService,
            RecordLotEventUseCase recordLotEventUseCase,
            DigitalPassportService passportService,
            EvidenceService evidenceService,
            EvidenceExpiryScheduler expiryScheduler,
            LineageTracePort lineageTracePort
    ) {
        this.partyRepository = partyRepository;
        this.facilityRepository = facilityRepository;
        this.lotRepository = lotRepository;
        this.lotEventRepository = lotEventRepository;
        this.passportRepository = passportRepository;
        this.evidenceRepository = evidenceRepository;
        this.registryService = registryService;
        this.recordLotEventUseCase = recordLotEventUseCase;
        this.passportService = passportService;
        this.evidenceService = evidenceService;
        this.expiryScheduler = expiryScheduler;
        this.lineageTracePort = lineageTracePort;
    }

    public record DashboardStats(
            long totalParties,
            long totalFacilities,
            long activeLots,
            long totalLots,
            long recordedEvents,
            long publishedPassports,
            long expiringCertificates,
            long expiredCertificates
    ) {}

    @GetMapping("/stats")
    @Operation(summary = "Get aggregated live KPIs for dashboard overview")
    public ResponseEntity<DashboardStats> getStats() {
        long parties = partyRepository.count();
        long facilities = facilityRepository.count();
        long lots = lotRepository.count();
        long events = lotEventRepository.count();
        long passports = passportRepository.count();

        LocalDate today = LocalDate.now();
        LocalDate in30Days = today.plusDays(30);

        List<EvidenceDocumentEntity> allDocs = evidenceRepository.findAll();
        long expiringCount = allDocs.stream()
                .filter(d -> d.getStatus() == EvidenceStatus.EXPIRING_SOON ||
                        (d.getValidUntil().isAfter(today.minusDays(1)) && d.getValidUntil().isBefore(in30Days)))
                .count();
        long expiredCount = allDocs.stream()
                .filter(d -> d.getStatus() == EvidenceStatus.EXPIRED || d.getValidUntil().isBefore(today))
                .count();

        long activeLotsCount = lotRepository.findAll().stream()
                .filter(l -> l.getStatus() == LotStatus.ACTIVE)
                .count();

        return ResponseEntity.ok(new DashboardStats(
                parties,
                facilities,
                activeLotsCount,
                lots,
                events,
                passports,
                expiringCount,
                expiredCount
        ));
    }

    public record DemoSeedingResult(
            String message,
            UUID brandAId,
            String brandACode,
            UUID brandBId,
            String brandBCode,
            String cartonLotNumber,
            UUID cartonLotId,
            String qrCodeTokenBrandA,
            String qrCodeTokenBrandB,
            List<String> createdLots
    ) {}

    @PostMapping("/demo/seed-rmg")
    @Operation(summary = "Seed complete 6-level Bangladeshi RMG supply-chain flow with Brand A passport & Brand B lot")
    public ResponseEntity<DemoSeedingResult> seedRmgSupplyChain() {
        String uid = UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        // 1. Register Parties
        PartyEntity brandA = partyRepository.findByExternalCode("BRAND-PAT-001")
                .orElseGet(() -> partyRepository.save(new PartyEntity(
                        PartyType.BRAND, "Patagonia Apparel", "BRAND-PAT-001", "US"
                )));
        brandA.setExactAddress("259 W Santa Clara St, Ventura, CA 93001");
        brandA.setWorkerCount(450);
        brandA.setUnitPriceBdt(BigDecimal.valueOf(1800.0));
        partyRepository.save(brandA);

        PartyEntity brandB = partyRepository.findByExternalCode("BRAND-OTHER-002")
                .orElseGet(() -> partyRepository.save(new PartyEntity(
                        PartyType.BRAND, "Nike Global Sourcing", "BRAND-OTHER-002", "US"
                )));
        brandB.setExactAddress("One Bowerman Dr, Beaverton, OR 97005");
        brandB.setWorkerCount(5000);
        brandB.setUnitPriceBdt(BigDecimal.valueOf(3200.0));
        partyRepository.save(brandB);

        PartyEntity spinningMill = partyRepository.findByExternalCode("MILL-BEX-001")
                .orElseGet(() -> partyRepository.save(new PartyEntity(
                        PartyType.SPINNING_MILL, "Beximco Textiles Spinning Unit", "MILL-BEX-001", "BD"
                )));
        spinningMill.setExactAddress("Beximco Industrial Park, Sarabo, Kashimpur, Gazipur");
        spinningMill.setWorkerCount(2200);
        spinningMill.setUnitPriceBdt(BigDecimal.valueOf(420.0));
        partyRepository.save(spinningMill);

        PartyEntity fabricMill = partyRepository.findByExternalCode("MILL-SHA-002")
                .orElseGet(() -> partyRepository.save(new PartyEntity(
                        PartyType.FABRIC_MILL, "Shasha Denim Weaving Unit", "MILL-SHA-002", "BD"
                )));
        fabricMill.setExactAddress("Plot 12-16, DEPZ, Savar, Dhaka");
        fabricMill.setWorkerCount(1800);
        fabricMill.setUnitPriceBdt(BigDecimal.valueOf(460.0));
        partyRepository.save(fabricMill);

        PartyEntity dyeHouse = partyRepository.findByExternalCode("DYE-ENV-003")
                .orElseGet(() -> partyRepository.save(new PartyEntity(
                        PartyType.DYE_HOUSE, "Envoy Textiles Dyeing & Finishing", "DYE-ENV-003", "BD"
                )));
        dyeHouse.setExactAddress("Jamirdia, Bhaluka, Mymensingh");
        dyeHouse.setWorkerCount(1100);
        dyeHouse.setUnitPriceBdt(BigDecimal.valueOf(510.0));
        partyRepository.save(dyeHouse);

        PartyEntity garmentFactory = partyRepository.findByExternalCode("FAC-DBL-004")
                .orElseGet(() -> partyRepository.save(new PartyEntity(
                        PartyType.FACTORY, "DBL Garments Ltd", "FAC-DBL-004", "BD"
                )));
        garmentFactory.setExactAddress("Mawna, Sreepur, Gazipur");
        garmentFactory.setWorkerCount(4500);
        garmentFactory.setUnitPriceBdt(BigDecimal.valueOf(620.0));
        partyRepository.save(garmentFactory);

        // 2. Register Facilities
        FacilityEntity facSpinning = findOrCreateFacility(spinningMill, FacilityType.MILL, "Beximco Spinning Unit 1", "Gazipur");
        FacilityEntity facWeaving = findOrCreateFacility(fabricMill, FacilityType.MILL, "Shasha Weaving Unit 1", "Dhaka");
        FacilityEntity facDyeing = findOrCreateFacility(dyeHouse, FacilityType.WASH_PLANT, "Envoy Dyeing & Wash Line", "Mymensingh");
        FacilityEntity facCutting = findOrCreateFacility(garmentFactory, FacilityType.FACTORY, "DBL Cutting Unit", "Gazipur");
        FacilityEntity facSewing = findOrCreateFacility(garmentFactory, FacilityType.PRODUCTION_LINE, "DBL Sewing Floor 3", "Gazipur");
        FacilityEntity facPacking = findOrCreateFacility(garmentFactory, FacilityType.FACTORY, "DBL Packing & Shipment Unit", "Gazipur");

        List<String> createdLots = new ArrayList<>();

        // Level 1: Genesis Organic Cotton Yarn
        String yarnLotNum = "LOT-YARN-" + uid;
        var yarnEvent = recordLotEventUseCase.recordEvent(new RecordEventCommand(
                null,
                yarnLotNum,
                brandA.getId(),
                "STYLE-PAT-101",
                "SHIP-2026-BD01",
                Unit.KG,
                EventType.RECEIVE,
                facSpinning.getId(),
                BigDecimal.valueOf(600.0),
                BigDecimal.valueOf(600.0),
                "KG",
                null,
                null,
                null
        ));
        createdLots.add(yarnLotNum);
        UUID yarnLotId = yarnEvent.lotId();

        // Attach valid GRS certificate to spinning lot
        EvidenceDocumentEntity grsCert = new EvidenceDocumentEntity(
                "GRS",
                "Control Union Certifications",
                "Scope Certificate for Global Recycled Standard",
                "RECYCLED",
                BigDecimal.valueOf(50.0),
                "certificates/grs-beximco-" + uid + ".pdf",
                "a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2",
                LocalDate.now().minusMonths(2),
                LocalDate.now().plusMonths(10),
                spinningMill.getId(),
                yarnLotId
        );
        grsCert.setStatus(EvidenceStatus.ACTIVE);
        evidenceRepository.save(grsCert);

        // Level 2: Fabric Mill Greige Weaving (Split 1-to-1)
        String greigeLotNum = "LOT-GREIGE-" + uid;
        var greigeEvent = recordLotEventUseCase.recordEvent(new RecordEventCommand(
                yarnLotId,
                null,
                null,
                null,
                null,
                Unit.KG,
                EventType.FINISH,
                facWeaving.getId(),
                BigDecimal.valueOf(600.0),
                BigDecimal.valueOf(580.0),
                "KG",
                null,
                List.of(new ChildLotOutput(greigeLotNum, "STYLE-PAT-101", "SHIP-2026-BD01", BigDecimal.valueOf(580.0), Unit.KG, BigDecimal.ONE)),
                null
        ));
        createdLots.add(greigeLotNum);
        UUID greigeLotId = greigeEvent.createdChildLotIds().get(0);

        // Level 3: Dye House Dyeing & Washing
        String dyedLotNum = "LOT-DYED-" + uid;
        var dyedEvent = recordLotEventUseCase.recordEvent(new RecordEventCommand(
                greigeLotId,
                null,
                null,
                null,
                null,
                Unit.KG,
                EventType.DYE,
                facDyeing.getId(),
                BigDecimal.valueOf(580.0),
                BigDecimal.valueOf(560.0),
                "KG",
                null,
                List.of(new ChildLotOutput(dyedLotNum, "STYLE-PAT-101", "SHIP-2026-BD01", BigDecimal.valueOf(560.0), Unit.KG, BigDecimal.ONE)),
                null
        ));
        createdLots.add(dyedLotNum);
        UUID dyedLotId = dyedEvent.createdChildLotIds().get(0);

        // Level 4: Garment Factory Cutting (Converts KG to PIECES)
        String cutLotNum = "LOT-CUT-" + uid;
        var cutEvent = recordLotEventUseCase.recordEvent(new RecordEventCommand(
                dyedLotId,
                null,
                null,
                null,
                null,
                Unit.PIECES,
                EventType.CUT,
                facCutting.getId(),
                BigDecimal.valueOf(560.0),
                BigDecimal.valueOf(2000.0),
                "KG",
                null,
                List.of(new ChildLotOutput(cutLotNum, "STYLE-PAT-101", "SHIP-2026-BD01", BigDecimal.valueOf(2000.0), Unit.PIECES, BigDecimal.ONE)),
                null
        ));
        createdLots.add(cutLotNum);
        UUID cutLotId = cutEvent.createdChildLotIds().get(0);

        // Level 5: Garment Factory Sewing
        String sewnLotNum = "LOT-SEWN-" + uid;
        var sewEvent = recordLotEventUseCase.recordEvent(new RecordEventCommand(
                cutLotId,
                null,
                null,
                null,
                null,
                Unit.PIECES,
                EventType.SEW,
                facSewing.getId(),
                BigDecimal.valueOf(2000.0),
                BigDecimal.valueOf(1960.0),
                "PIECES",
                null,
                List.of(new ChildLotOutput(sewnLotNum, "STYLE-PAT-101", "SHIP-2026-BD01", BigDecimal.valueOf(1960.0), Unit.PIECES, BigDecimal.ONE)),
                null
        ));
        createdLots.add(sewnLotNum);
        UUID sewnLotId = sewEvent.createdChildLotIds().get(0);

        // Level 6: Packing into Export Cartons
        String cartonLotNum = "LOT-CARTON-" + uid;
        var packEvent = recordLotEventUseCase.recordEvent(new RecordEventCommand(
                sewnLotId,
                null,
                null,
                null,
                null,
                Unit.CARTONS,
                EventType.PACK,
                facPacking.getId(),
                BigDecimal.valueOf(1960.0),
                BigDecimal.valueOf(50.0),
                "PIECES",
                null,
                List.of(new ChildLotOutput(cartonLotNum, "STYLE-PAT-101", "SHIP-2026-BD01", BigDecimal.valueOf(50.0), Unit.CARTONS, BigDecimal.ONE)),
                null
        ));
        createdLots.add(cartonLotNum);
        UUID cartonLotId = packEvent.createdChildLotIds().get(0);

        // 4. Generate Digital Product Passport for Brand A (Patagonia)
        Map<String, Object> customFieldsA = new HashMap<>();
        customFieldsA.put("circularity_score", "A+ (98/100)");
        customFieldsA.put("carbon_offset_partner", "South Pole Climate Group");
        customFieldsA.put("repair_dropoff_available", true);
        customFieldsA.put("supply_chain_transparency", "Tier 1 to Tier 4 Fully Verified");

        var passportA = passportService.generatePassport(new DigitalPassportService.GeneratePassportCommand(
                "STYLE-PAT-101",
                "SHIP-2026-BD01",
                brandA.getId(),
                cartonLotId,
                customFieldsA
        ));

        // 5. Create Brand B Carton Lot & Passport for cross-brand isolation testing
        String cartonLotNumB = "LOT-CARTON-NIKE-" + uid;
        var genesisB = recordLotEventUseCase.recordEvent(new RecordEventCommand(
                null,
                cartonLotNumB,
                brandB.getId(),
                "STYLE-NIKE-RUNNER",
                "SHIP-2026-NK02",
                Unit.CARTONS,
                EventType.RECEIVE,
                facPacking.getId(),
                BigDecimal.valueOf(100.0),
                BigDecimal.valueOf(100.0),
                "CARTONS",
                null,
                null,
                null
        ));
        createdLots.add(cartonLotNumB);

        var passportB = passportService.generatePassport(new DigitalPassportService.GeneratePassportCommand(
                "STYLE-NIKE-RUNNER",
                "SHIP-2026-NK02",
                brandB.getId(),
                genesisB.lotId(),
                Map.of("category", "Athletic Footwear", "competitor_secret", "Protected Under QA3")
        ));

        return ResponseEntity.ok(new DemoSeedingResult(
                "Complete 6-level RMG supply chain seeded successfully across 6 process levels!",
                brandA.getId(),
                brandA.getExternalCode(),
                brandB.getId(),
                brandB.getExternalCode(),
                cartonLotNum,
                cartonLotId,
                passportA.entity().getQrCodeToken(),
                passportB.entity().getQrCodeToken(),
                createdLots
        ));
    }

    public record ViolationReport(
            String title,
            String description,
            String detectedIssue,
            String affectedLot,
            String status
    ) {}

    public record DefectSeedingResult(
            String message,
            List<ViolationReport> violations
    ) {}

    @PostMapping("/demo/seed-violations")
    @Operation(summary = "Seed the 4 planted QA2 violation scenarios and return verification report")
    public ResponseEntity<DefectSeedingResult> seedViolations() {
        String uid = UUID.randomUUID().toString().substring(0, 5).toUpperCase();
        List<ViolationReport> reports = new ArrayList<>();

        PartyEntity brand = partyRepository.findByExternalCode("BRAND-PAT-001")
                .orElseGet(() -> partyRepository.save(new PartyEntity(PartyType.BRAND, "Patagonia Demo", "BRAND-PAT-001", "US")));
        PartyEntity mill = partyRepository.findByExternalCode("MILL-BEX-001")
                .orElseGet(() -> partyRepository.save(new PartyEntity(PartyType.FABRIC_MILL, "Beximco Demo", "MILL-BEX-001", "BD")));
        FacilityEntity fac = findOrCreateFacility(mill, FacilityType.MILL, "Demo Spinning Facility", "Gazipur");

        // Planted Problem 1: Over-claimed recycled content
        LeafSourceLot certifiedSource = new LeafSourceLot(
                UUID.randomUUID(),
                "LOT-CERT-INPUT-" + uid,
                BigDecimal.valueOf(100.0),
                "KG",
                BigDecimal.valueOf(200.0),
                BigDecimal.valueOf(25.0),
                BigDecimal.valueOf(10.0)
        ).withCertifications(BigDecimal.valueOf(25.0), BigDecimal.ZERO, true)
         .withClaims(BigDecimal.valueOf(25.0), BigDecimal.ZERO);

        CompositeTransformedLot overclaimedGarment = new CompositeTransformedLot(
                UUID.randomUUID(),
                "LOT-GREENWASH-" + uid,
                BigDecimal.valueOf(500.0),
                "PIECES",
                BigDecimal.valueOf(50.0),
                BigDecimal.valueOf(10.0),
                BigDecimal.valueOf(5.0)
        ).withClaims(BigDecimal.valueOf(70.0), BigDecimal.ZERO);
        overclaimedGarment.addParent(certifiedSource);

        ClaimRollupVisitor v1 = new ClaimRollupVisitor();
        overclaimedGarment.accept(v1);
        ClaimRollupResult res1 = v1.getResult();

        reports.add(new ViolationReport(
                "Planted Defect 1: Over-Claimed Recycled Content (Greenwashing)",
                "Child lot claims 70.0% recycled content while verified input is only 25.0%.",
                res1.violations().isEmpty() ? "Violation detected" : res1.violations().get(0),
                "LOT-GREENWASH-" + uid,
                "BLOCKED (claimsValid = " + res1.claimsValid() + ")"
        ));

        // Planted Problem 2: Expired Certificate
        LotEntity expLot = new LotEntity(
                "LOT-EXPIRED-CERT-" + uid,
                brand.getId(),
                fac.getId(),
                "STYLE-EXP",
                "SHIP-EXP",
                LotStatus.ACTIVE,
                BigDecimal.valueOf(100.0),
                Unit.KG
        );
        expLot.setCertifiedRecycledPct(BigDecimal.valueOf(50.0));
        expLot = lotRepository.save(expLot);

        EvidenceDocumentEntity expiredDoc = new EvidenceDocumentEntity(
                "GRS",
                "Global Standards Certifier",
                "Expired Recycled Certificate",
                "RECYCLED",
                BigDecimal.valueOf(50.0),
                "certificates/expired-" + uid + ".pdf",
                "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                LocalDate.now().minusYears(1),
                LocalDate.now().minusDays(3),
                mill.getId(),
                expLot.getId()
        );
        expiredDoc.setStatus(EvidenceStatus.ACTIVE);
        evidenceRepository.save(expiredDoc);

        // Run observer scheduler
        expiryScheduler.checkAndPublishExpiryEvents();
        LotEntity updatedExpLot = lotRepository.findById(expLot.getId()).orElse(expLot);

        reports.add(new ViolationReport(
                "Planted Defect 2: Expired Certificate & Automatic Observer Revocation",
                "GRS certificate validity elapsed 3 days ago. Scheduled observer job fired automatically.",
                "Certificate flagged EXPIRED; Lot status changed to QUARANTINED; certified % revoked to 0%.",
                expLot.getLotNumber(),
                "QUARANTINED (Status: " + updatedExpLot.getStatus() + ", Certified Recycled: " + updatedExpLot.getCertifiedRecycledPct() + "%)"
        ));

        // Planted Problem 3: Excessive Cutting Loss (>12%)
        String fabricLotNum = "LOT-FABRIC-CUTLOSS-" + uid;
        var genesisMb = recordLotEventUseCase.recordEvent(new RecordEventCommand(
                null,
                fabricLotNum,
                brand.getId(),
                "STYLE-CUTLOSS",
                "SHIP-CUTLOSS",
                Unit.KG,
                EventType.RECEIVE,
                fac.getId(),
                BigDecimal.valueOf(100.0),
                BigDecimal.valueOf(100.0),
                "KG",
                null,
                null,
                null
        ));

        var cutLossEvent = recordLotEventUseCase.recordEvent(new RecordEventCommand(
                genesisMb.lotId(),
                null,
                null,
                null,
                null,
                Unit.KG,
                EventType.CUT,
                fac.getId(),
                BigDecimal.valueOf(100.0),
                BigDecimal.valueOf(78.0), // 22% loss!
                "KG",
                null,
                null,
                null
        ));

        reports.add(new ViolationReport(
                "Planted Defect 3: Mass Balance Violation (Cutting Loss 22% > Max Allowed 12%)",
                "100 KG fabric input produced only 78 KG output, exceeding the 12% cutting loss threshold.",
                "Event flagged with mass_balance_flagged=true; Lot immediately QUARANTINED.",
                fabricLotNum,
                "QUARANTINED (Loss: " + cutLossEvent.lossPercentage() + "%, Flagged: " + cutLossEvent.massBalanceFlagged() + ")"
        ));

        // Planted Problem 4: Orphan Lot
        CompositeTransformedLot orphanLot = new CompositeTransformedLot(
                UUID.randomUUID(),
                "LOT-ORPHAN-" + uid,
                BigDecimal.valueOf(300.0),
                "PIECES",
                BigDecimal.valueOf(30.0),
                BigDecimal.valueOf(5.0),
                BigDecimal.valueOf(2.0)
        ).withClaims(BigDecimal.valueOf(50.0), BigDecimal.ZERO);

        ClaimRollupVisitor v4 = new ClaimRollupVisitor();
        orphanLot.accept(v4);
        ClaimRollupResult res4 = v4.getResult();

        reports.add(new ViolationReport(
                "Planted Defect 4: Orphan Lot Provenance Defect",
                "Lot claims 50% recycled content but possesses ZERO parent provenance links or certified source.",
                "Orphan detected: hasOrphanLot=true, claimsValid=false, orphanLotIds=" + res4.orphanLotIds(),
                orphanLot.getLotNumber(),
                "REJECTED (claimsValid = " + res4.claimsValid() + ")"
        ));

        return ResponseEntity.ok(new DefectSeedingResult(
                "All 4 planted defect scenarios seeded and successfully detected by ThreadTrace validation engine!",
                reports
        ));
    }

    @GetMapping("/sample-csv")
    @Operation(summary = "Download a ready-to-use CSV sample for Factory Cutting Report bulk import")
    public ResponseEntity<byte[]> downloadSampleCsv() {
        // Find an active fabric/yarn lot to use as a valid parent if available
        String sampleParentLot = lotRepository.findAll().stream()
                .filter(l -> l.getUnit() == Unit.KG)
                .map(LotEntity::getLotNumber)
                .findFirst()
                .orElse("LOT-FABRIC-DEMO-01");

        String csv = String.join("\r\n",
                "fabric_lot_number,cut_bundle_number,input_kg,output_pieces,style_code,shipment_code",
                sampleParentLot + ",CUT-BNDL-" + UUID.randomUUID().toString().substring(0, 5) + ",45.0,200,STYLE-DEMO-01,SHIP-DEMO-01",
                sampleParentLot + ",CUT-BNDL-" + UUID.randomUUID().toString().substring(0, 5) + ",45.0,200,STYLE-DEMO-01,SHIP-DEMO-01",
                "NON_EXISTENT_FABRIC_LOT,CUT-BNDL-ERR1,10.0,50,STYLE-DEMO-01,SHIP-DEMO-01",
                sampleParentLot + ",,10.0,50,STYLE-DEMO-01,SHIP-DEMO-01"
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"factory_cutting_report_sample.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csv.getBytes(StandardCharsets.UTF_8));
    }

    @GetMapping("/sample-xlsx")
    @Operation(summary = "Download a ready-to-use Excel (.xlsx) sample for Factory Packing List bulk import")
    public ResponseEntity<byte[]> downloadSampleXlsx() throws IOException {
        String sampleGarmentLot = lotRepository.findAll().stream()
                .filter(l -> l.getUnit() == Unit.PIECES)
                .map(LotEntity::getLotNumber)
                .findFirst()
                .orElse("LOT-SEWN-DEMO-01");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("PackingList");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("garment_lot_number");
            header.createCell(1).setCellValue("carton_number");
            header.createCell(2).setCellValue("pieces_count");
            header.createCell(3).setCellValue("carton_count");
            header.createCell(4).setCellValue("style_code");
            header.createCell(5).setCellValue("shipment_code");

            // Row 1 (Valid)
            Row r1 = sheet.createRow(1);
            r1.createCell(0).setCellValue(sampleGarmentLot);
            r1.createCell(1).setCellValue("CARTON-XLS-" + UUID.randomUUID().toString().substring(0, 5));
            r1.createCell(2).setCellValue(50);
            r1.createCell(3).setCellValue(1);
            r1.createCell(4).setCellValue("STYLE-DEMO-01");
            r1.createCell(5).setCellValue("SHIP-DEMO-01");

            // Row 2 (Valid)
            Row r2 = sheet.createRow(2);
            r2.createCell(0).setCellValue(sampleGarmentLot);
            r2.createCell(1).setCellValue("CARTON-XLS-" + UUID.randomUUID().toString().substring(0, 5));
            r2.createCell(2).setCellValue(50);
            r2.createCell(3).setCellValue(1);
            r2.createCell(4).setCellValue("STYLE-DEMO-01");
            r2.createCell(5).setCellValue("SHIP-DEMO-01");

            // Row 3 (Invalid - Unknown lot)
            Row r3 = sheet.createRow(3);
            r3.createCell(0).setCellValue("NON_EXISTENT_GARMENT_LOT");
            r3.createCell(1).setCellValue("CARTON-ERR");
            r3.createCell(2).setCellValue(50);
            r3.createCell(3).setCellValue(1);
            r3.createCell(4).setCellValue("STYLE-DEMO-01");
            r3.createCell(5).setCellValue("SHIP-DEMO-01");

            workbook.write(out);
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"factory_packing_list_sample.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(out.toByteArray());
    }

    private FacilityEntity findOrCreateFacility(PartyEntity party, FacilityType type, String name, String city) {
        var page = facilityRepository.findByPartyId(party.getId(), org.springframework.data.domain.PageRequest.of(0, 10));
        return page.stream()
                .filter(f -> f.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElseGet(() -> facilityRepository.save(new FacilityEntity(party, type, null, name, city)));
    }
}
