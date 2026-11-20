package com.cse444.threadtrace.passport.projection;

import java.util.List;
import java.util.UUID;

/**
 * Brand-specific Digital Product Passport projection (FR8).
 * Contains public passport data plus tier supplier facilities for the brand's own lots.
 */
public record BrandPassportView(
        PublicPassportView publicPassport,
        UUID brandId,
        String brandExternalCode,
        UUID rootCartonLotId,
        String rootLotNumber,
        List<BrandSupplierItem> tierSuppliers
) {
    public record BrandSupplierItem(
            UUID partyId,
            String partyName,
            String partyType,
            String facilityName,
            String locationCity,
            String countryCode,
            String supplyChainRole
    ) {}
}
