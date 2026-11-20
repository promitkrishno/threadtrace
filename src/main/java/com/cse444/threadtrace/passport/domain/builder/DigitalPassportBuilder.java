package com.cse444.threadtrace.passport.domain.builder;

import com.cse444.threadtrace.passport.projection.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;

/**
 * Builder Pattern: Constructs versioned Digital Product Passports (FR7, QA5).
 * Supports extensible custom dynamic fields without modifying lineage or mass balance classes.
 */
public class DigitalPassportBuilder {

    private String styleCode;
    private String shipmentCode;
    private UUID brandId;
    private String brandName;
    private String brandExternalCode;
    private UUID rootCartonLotId;
    private String rootLotNumber;

    private final List<FibreCompositionDto> fibreComposition = new ArrayList<>();
    private final List<ProcessStepOriginDto> originByProcessStep = new ArrayList<>();
    private final List<CertificationBadgeDto> certifications = new ArrayList<>();

    private String careAndRepairGuidance = "Wash cool at 30°C. Line dry to save energy. Repair seams and buttons.";
    private String endOfLifeGuidance = "100% recyclable mono-material. Return to store or textile recycling drop-off.";

    private BigDecimal perPieceWaterLiters = BigDecimal.ZERO;
    private BigDecimal perPieceEnergyKwh = BigDecimal.ZERO;
    private BigDecimal perPieceCo2eKg = BigDecimal.ZERO;

    private final Map<String, Object> customFields = new HashMap<>();

    private final List<BrandPassportView.BrandSupplierItem> brandSuppliers = new ArrayList<>();
    private final List<AuditorPassportView.AuditorConfidentialSupplierItem> confidentialSuppliers = new ArrayList<>();

    private boolean massBalanceFlagged = false;
    private boolean claimsValid = true;
    private boolean hasOrphanLot = false;
    private final List<String> auditViolations = new ArrayList<>();
    private final List<UUID> orphanLotIds = new ArrayList<>();

    private String baseUrl = "http://localhost:8080";

    public static DigitalPassportBuilder builder() {
        return new DigitalPassportBuilder();
    }

    public DigitalPassportBuilder withStyleAndShipment(String styleCode, String shipmentCode) {
        this.styleCode = styleCode;
        this.shipmentCode = shipmentCode;
        return this;
    }

    public DigitalPassportBuilder withBrand(UUID brandId, String brandName, String brandExternalCode) {
        this.brandId = brandId;
        this.brandName = brandName;
        this.brandExternalCode = brandExternalCode;
        return this;
    }

    public DigitalPassportBuilder withRootCartonLot(UUID rootCartonLotId, String rootLotNumber) {
        this.rootCartonLotId = rootCartonLotId;
        this.rootLotNumber = rootLotNumber;
        return this;
    }

    public DigitalPassportBuilder withFibre(String materialName, BigDecimal percentage, boolean isRecycled, boolean isOrganic) {
        this.fibreComposition.add(new FibreCompositionDto(materialName, percentage, isRecycled, isOrganic));
        return this;
    }

    public DigitalPassportBuilder withFibreComposition(List<FibreCompositionDto> fibres) {
        if (fibres != null) {
            this.fibreComposition.addAll(fibres);
        }
        return this;
    }

    public DigitalPassportBuilder withOriginStep(String step, String facility, String city, String country) {
        this.originByProcessStep.add(new ProcessStepOriginDto(step, facility, city, country));
        return this;
    }

    public DigitalPassportBuilder withOriginByProcessStep(List<ProcessStepOriginDto> origins) {
        if (origins != null) {
            this.originByProcessStep.addAll(origins);
        }
        return this;
    }

    public DigitalPassportBuilder withCertifications(List<CertificationBadgeDto> certs) {
        if (certs != null) {
            this.certifications.addAll(certs);
        }
        return this;
    }

    public DigitalPassportBuilder withCareAndRepair(String guidance) {
        this.careAndRepairGuidance = guidance;
        return this;
    }

    public DigitalPassportBuilder withEndOfLife(String guidance) {
        this.endOfLifeGuidance = guidance;
        return this;
    }

    public DigitalPassportBuilder withFootprint(BigDecimal waterL, BigDecimal energyKwh, BigDecimal co2eKg) {
        this.perPieceWaterLiters = waterL != null ? waterL : BigDecimal.ZERO;
        this.perPieceEnergyKwh = energyKwh != null ? energyKwh : BigDecimal.ZERO;
        this.perPieceCo2eKg = co2eKg != null ? co2eKg : BigDecimal.ZERO;
        return this;
    }

    /**
     * QA5 Dynamic extensibility: add custom fields to DPP without modifying lineage classes.
     */
    public DigitalPassportBuilder withCustomField(String key, Object value) {
        if (key != null && value != null) {
            this.customFields.put(key, value);
        }
        return this;
    }

    public DigitalPassportBuilder withCustomFields(Map<String, Object> fields) {
        if (fields != null) {
            this.customFields.putAll(fields);
        }
        return this;
    }

    public DigitalPassportBuilder withBrandSuppliers(List<BrandPassportView.BrandSupplierItem> suppliers) {
        if (suppliers != null) {
            this.brandSuppliers.addAll(suppliers);
        }
        return this;
    }

    public DigitalPassportBuilder withAuditorData(
            boolean massBalanceFlagged,
            boolean claimsValid,
            boolean hasOrphanLot,
            List<String> violations,
            List<UUID> orphanLots,
            List<AuditorPassportView.AuditorConfidentialSupplierItem> confidentialSuppliers
    ) {
        this.massBalanceFlagged = massBalanceFlagged;
        this.claimsValid = claimsValid;
        this.hasOrphanLot = hasOrphanLot;
        if (violations != null) this.auditViolations.addAll(violations);
        if (orphanLots != null) this.orphanLotIds.addAll(orphanLots);
        if (confidentialSuppliers != null) this.confidentialSuppliers.addAll(confidentialSuppliers);
        return this;
    }

    public DigitalPassportBuilder withBaseUrl(String baseUrl) {
        if (baseUrl != null && !baseUrl.isBlank()) {
            this.baseUrl = baseUrl.replaceAll("/+$", "");
        }
        return this;
    }

    public DigitalPassportBuildResult buildAndSave(
            DigitalPassportRepository repository,
            ObjectMapper objectMapper
    ) {
        Objects.requireNonNull(styleCode, "styleCode is required");
        Objects.requireNonNull(shipmentCode, "shipmentCode is required");
        Objects.requireNonNull(brandId, "brandId is required");
        Objects.requireNonNull(rootCartonLotId, "rootCartonLotId is required");

        // 1. Determine Versioning: if regenerating, increment version_no and mark old versions as not latest
        int nextVersion = 1;
        Optional<DigitalPassportEntity> latestOpt = repository.findTopByStyleCodeAndShipmentCodeOrderByVersionNoDesc(styleCode, shipmentCode);
        if (latestOpt.isPresent()) {
            nextVersion = latestOpt.get().getVersionNo() + 1;
            List<DigitalPassportEntity> existingVersions = repository.findByStyleCodeAndShipmentCodeOrderByVersionNoAsc(styleCode, shipmentCode);
            for (DigitalPassportEntity oldVersion : existingVersions) {
                if (oldVersion.isLatest()) {
                    oldVersion.setLatest(false);
                    repository.save(oldVersion);
                }
            }
        }

        // 2. Generate QR Code Token & Public URL
        String tokenSuffix = UUID.randomUUID().toString().substring(0, 8);
        String qrCodeToken = String.format("DPP-%s-%s-V%d-%s", styleCode, shipmentCode, nextVersion, tokenSuffix);
        String publicUrl = String.format("%s/passports/%s", baseUrl, qrCodeToken);

        // Generate ZXing QR Code PNG
        byte[] qrCodePng = QrCodeGenerator.generateQrCodePng(publicUrl, 250, 250);

        // 3. Construct Views
        Instant now = Instant.now();
        PublicPassportView publicView = new PublicPassportView(
                styleCode,
                shipmentCode,
                brandName != null ? brandName : "Brand",
                nextVersion,
                qrCodeToken,
                publicUrl,
                now,
                List.copyOf(fibreComposition),
                List.copyOf(originByProcessStep),
                List.copyOf(certifications),
                careAndRepairGuidance,
                endOfLifeGuidance,
                perPieceWaterLiters,
                perPieceEnergyKwh,
                perPieceCo2eKg,
                Map.copyOf(customFields)
        );

        BrandPassportView brandView = new BrandPassportView(
                publicView,
                brandId,
                brandExternalCode != null ? brandExternalCode : "BRAND",
                rootCartonLotId,
                rootLotNumber != null ? rootLotNumber : "LOT-ROOT",
                List.copyOf(brandSuppliers)
        );

        AuditorPassportView auditorView = new AuditorPassportView(
                brandView,
                massBalanceFlagged,
                claimsValid,
                hasOrphanLot,
                List.copyOf(auditViolations),
                List.copyOf(orphanLotIds),
                List.copyOf(confidentialSuppliers)
        );

        // 4. Serialize Payloads
        try {
            String publicJson = objectMapper.writeValueAsString(publicView);
            String brandJson = objectMapper.writeValueAsString(brandView);
            String auditorJson = objectMapper.writeValueAsString(auditorView);

            String etag = computeSha256Hex(publicJson);

            DigitalPassportEntity entity = new DigitalPassportEntity(
                    styleCode,
                    shipmentCode,
                    brandId,
                    rootCartonLotId,
                    nextVersion,
                    true,
                    qrCodeToken,
                    etag,
                    publicJson,
                    brandJson,
                    auditorJson
            );

            DigitalPassportEntity saved = repository.save(entity);
            return new DigitalPassportBuildResult(saved, publicView, brandView, auditorView, qrCodePng);

        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize passport JSON payloads: " + e.getMessage(), e);
        }
    }

    private static String computeSha256Hex(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public record DigitalPassportBuildResult(
            DigitalPassportEntity entity,
            PublicPassportView publicView,
            BrandPassportView brandView,
            AuditorPassportView auditorView,
            byte[] qrCodePng
    ) {}
}
