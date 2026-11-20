package com.cse444.threadtrace.passport.projection;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Auditor Digital Product Passport projection (FR8, QA3).
 * Contains full supply chain transparency including confidential supplier details,
 * worker counts, pricing, mass-balance flags, and lineage audit verification.
 */
public record AuditorPassportView(
        BrandPassportView brandPassport,
        boolean massBalanceFlagged,
        boolean claimsValid,
        boolean hasOrphanLot,
        List<String> auditViolations,
        List<UUID> orphanLotIds,
        List<AuditorConfidentialSupplierItem> confidentialSuppliers
) {
    public record AuditorConfidentialSupplierItem(
            UUID partyId,
            String partyName,
            String partyType,
            String countryCode,
            String exactAddress,
            Integer workerCount,
            BigDecimal unitPriceBdt
    ) {}
}
