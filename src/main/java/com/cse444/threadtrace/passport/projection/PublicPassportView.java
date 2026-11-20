package com.cse444.threadtrace.passport.projection;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Strictly public Digital Product Passport projection (FR7, FR8, QA3).
 * Guaranteed to NEVER contain confidential commercial or worker data
 * (unit_price_bdt, exact_address, worker_count).
 */
public record PublicPassportView(
        String styleCode,
        String shipmentCode,
        String brandName,
        int versionNo,
        String qrCodeToken,
        String publicUrl,
        Instant publishedAt,
        List<FibreCompositionDto> fibreComposition,
        List<ProcessStepOriginDto> originByProcessStep,
        List<CertificationBadgeDto> certifications,
        String careAndRepairGuidance,
        String endOfLifeGuidance,
        BigDecimal perPieceWaterLiters,
        BigDecimal perPieceEnergyKwh,
        BigDecimal perPieceCo2eKg,
        Map<String, Object> customFields
) {}
