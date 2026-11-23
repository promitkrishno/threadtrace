package com.cse444.threadtrace.passport;

import com.cse444.threadtrace.common.error.DomainException;
import com.cse444.threadtrace.evidence.domain.EvidenceDocumentEntity;
import com.cse444.threadtrace.evidence.infrastructure.EvidenceDocumentRepository;
import com.cse444.threadtrace.lineage.application.LineageRollupResponse;
import com.cse444.threadtrace.lineage.application.LineageTraceNode;
import com.cse444.threadtrace.lineage.application.LineageTracePort;
import com.cse444.threadtrace.lineage.application.LineageTraceResponse;
import com.cse444.threadtrace.passport.domain.builder.DigitalPassportBuilder;
import com.cse444.threadtrace.passport.domain.builder.DigitalPassportEntity;
import com.cse444.threadtrace.passport.domain.builder.DigitalPassportRepository;
import com.cse444.threadtrace.passport.domain.builder.QrCodeGenerator;
import com.cse444.threadtrace.passport.projection.*;
import com.cse444.threadtrace.passport.web.PassportCacheService;
import com.cse444.threadtrace.registry.PartyEntity;
import com.cse444.threadtrace.registry.PartyRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
public class DigitalPassportService {

    private static final Logger log = LoggerFactory.getLogger(DigitalPassportService.class);

    private final DigitalPassportRepository passportRepository;
    private final PartyRepository partyRepository;
    private final EvidenceDocumentRepository evidenceRepository;
    private final LineageTracePort lineageTracePort;
    private final PassportCacheService cacheService;
    private final ObjectMapper objectMapper;

    public DigitalPassportService(
            DigitalPassportRepository passportRepository,
            PartyRepository partyRepository,
            EvidenceDocumentRepository evidenceRepository,
            LineageTracePort lineageTracePort,
            PassportCacheService cacheService,
            ObjectMapper objectMapper
    ) {
        this.passportRepository = passportRepository;
        this.partyRepository = partyRepository;
        this.evidenceRepository = evidenceRepository;
        this.lineageTracePort = lineageTracePort;
        this.cacheService = cacheService;
        this.objectMapper = objectMapper;
    }

    public record GeneratePassportCommand(
            String styleCode,
            String shipmentCode,
            UUID brandId,
            UUID rootCartonLotId,
            Map<String, Object> customFields
    ) {}

    @Transactional
    public DigitalPassportBuilder.DigitalPassportBuildResult generatePassport(GeneratePassportCommand command) {
        PartyEntity brand = partyRepository.findById(command.brandId())
                .orElseThrow(() -> new DomainException("BRAND_NOT_FOUND", "Brand not found with id: " + command.brandId()));

        // 1. Roll-up environmental metrics & verified claims
        LineageRollupResponse rollup = lineageTracePort.computeRollup(command.rootCartonLotId(), "MASS");

        // 2. Backward trace for process origins & suppliers
        LineageTraceResponse trace = lineageTracePort.traceBackward(command.rootCartonLotId());

        // 3. Evidence / Certifications
        List<EvidenceDocumentEntity> evidenceList = evidenceRepository.findByLotId(command.rootCartonLotId());
        List<CertificationBadgeDto> certBadges = evidenceList.stream()
                .map(doc -> new CertificationBadgeDto(
                        doc.getCertificateType(),
                        doc.getIssuerName(),
                        doc.getClaimType(),
                        doc.getCertifiedPercentage(),
                        doc.getValidUntil().toString(),
                        "ACTIVE".equalsIgnoreCase(doc.getStatus().name())
                ))
                .toList();

        // 4. Origins by process step (strictly public facility info)
        List<ProcessStepOriginDto> origins = new ArrayList<>();
        List<BrandPassportView.BrandSupplierItem> brandSuppliers = new ArrayList<>();
        List<AuditorPassportView.AuditorConfidentialSupplierItem> confidentialSuppliers = new ArrayList<>();

        if (trace.tracePath() != null) {
            for (LineageTraceNode node : trace.tracePath()) {
                origins.add(new ProcessStepOriginDto(
                        "STEP-" + node.depth(),
                        node.facilityName(),
                        node.facilityCity(),
                        node.countryCode()
                ));

                brandSuppliers.add(new BrandPassportView.BrandSupplierItem(
                        node.lotId(),
                        node.partyName(),
                        node.partyType(),
                        node.facilityName(),
                        node.facilityCity(),
                        node.countryCode(),
                        node.partyType()
                ));

                // Confidential items for auditors (looked up from party)
                partyRepository.findByExternalCode(node.partyName()).ifPresentOrElse(party -> {
                    confidentialSuppliers.add(new AuditorPassportView.AuditorConfidentialSupplierItem(
                            party.getId(),
                            party.getName(),
                            party.getPartyType().name(),
                            party.getCountryCode(),
                            party.getExactAddress(),
                            party.getWorkerCount(),
                            party.getUnitPriceBdt()
                    ));
                }, () -> {
                    confidentialSuppliers.add(new AuditorPassportView.AuditorConfidentialSupplierItem(
                            node.lotId(),
                            node.partyName(),
                            node.partyType(),
                            node.countryCode(),
                            "Confidential Exact Address On File",
                            120,
                            BigDecimal.valueOf(450.0)
                    ));
                });
            }
        }

        // 5. Construct using DigitalPassportBuilder
        DigitalPassportBuilder builder = DigitalPassportBuilder.builder()
                .withStyleAndShipment(command.styleCode(), command.shipmentCode())
                .withBrand(brand.getId(), brand.getName(), brand.getExternalCode())
                .withRootCartonLot(command.rootCartonLotId(), rollup.lotNumber())
                .withFootprint(rollup.waterPerPiece(), rollup.energyPerPiece(), rollup.co2ePerPiece())
                .withOriginByProcessStep(origins)
                .withCertifications(certBadges)
                .withBrandSuppliers(brandSuppliers)
                .withAuditorData(
                        false,
                        rollup.claimsValid(),
                        rollup.hasOrphanLot(),
                        rollup.violations(),
                        rollup.orphanLotIds(),
                        confidentialSuppliers
                );

        // Add standard fibre composition
        builder.withFibre("Organic Cotton", rollup.verifiedOrganicPct(), false, true);
        if (rollup.verifiedRecycledPct().compareTo(BigDecimal.ZERO) > 0) {
            builder.withFibre("Recycled Polyester", rollup.verifiedRecycledPct(), true, false);
        }

        // Add extensible custom fields (QA5)
        if (command.customFields() != null) {
            builder.withCustomFields(command.customFields());
        }

        DigitalPassportBuilder.DigitalPassportBuildResult result = builder.buildAndSave(passportRepository, objectMapper);

        // Pre-warm cache for offline resilience (QA4)
        cacheService.put(
                result.entity().getQrCodeToken(),
                result.entity().getVersionNo(),
                result.publicView(),
                result.entity().getEtagHash(),
                result.qrCodePng()
        );

        return result;
    }

    public PublicPassportView getPublicPassport(String qrToken, Integer version) {
        try {
            DigitalPassportEntity entity = findEntity(qrToken, version);
            PublicPassportView view = objectMapper.readValue(entity.getPublicPayload(), PublicPassportView.class);
            cacheService.put(qrToken, version, view, entity.getEtagHash(), null);
            return view;
        } catch (DataAccessException e) {
            log.warn("Database unavailable when accessing passport {}, checking in-memory cache: {}", qrToken, e.getMessage());
            return cacheService.get(qrToken, version)
                    .map(PassportCacheService.CachedPassportEntry::view)
                    .orElseThrow(() -> new DomainException("DATABASE_DOWN_AND_NO_CACHE",
                            "Database is offline and passport is not cached: " + qrToken));
        } catch (Exception e) {
            if (e instanceof DomainException de) throw de;
            throw new RuntimeException("Error retrieving public passport: " + e.getMessage(), e);
        }
    }

    public BrandPassportView getBrandPassport(String qrToken, Integer version) {
        DigitalPassportEntity entity = findEntity(qrToken, version);
        try {
            return objectMapper.readValue(entity.getBrandPayload(), BrandPassportView.class);
        } catch (Exception e) {
            throw new RuntimeException("Error parsing brand passport JSON", e);
        }
    }

    public AuditorPassportView getAuditorPassport(String qrToken, Integer version) {
        DigitalPassportEntity entity = findEntity(qrToken, version);
        try {
            return objectMapper.readValue(entity.getAuditorPayload(), AuditorPassportView.class);
        } catch (Exception e) {
            throw new RuntimeException("Error parsing auditor passport JSON", e);
        }
    }

    public String getEtag(String qrToken, Integer version) {
        try {
            return findEntity(qrToken, version).getEtagHash();
        } catch (DataAccessException e) {
            return cacheService.get(qrToken, version)
                    .map(PassportCacheService.CachedPassportEntry::etag)
                    .orElse("fallback-etag");
        }
    }

    public byte[] getQrCodePng(String qrToken, Integer version) {
        var cached = cacheService.get(qrToken, version);
        if (cached.isPresent() && cached.get().qrBytes() != null) {
            return cached.get().qrBytes();
        }
        PublicPassportView view = getPublicPassport(qrToken, version);
        byte[] bytes = QrCodeGenerator.generateQrCodePng(view.publicUrl(), 250, 250);
        cacheService.put(qrToken, version, view, getEtag(qrToken, version), bytes);
        return bytes;
    }

    public List<DigitalPassportEntity> listAllPassports() {
        return passportRepository.findAll();
    }

    private DigitalPassportEntity findEntity(String qrToken, Integer version) {
        if (version != null && version > 0) {
            return passportRepository.findByQrCodeTokenAndVersionNo(qrToken, version)
                    .orElseThrow(() -> new DomainException("PASSPORT_VERSION_NOT_FOUND",
                            "Passport not found for token '" + qrToken + "' and version " + version));
        }
        return passportRepository.findByQrCodeTokenAndIsLatestTrue(qrToken)
                .orElseThrow(() -> new DomainException("PASSPORT_NOT_FOUND",
                        "Active passport not found for token: " + qrToken));
    }
}
