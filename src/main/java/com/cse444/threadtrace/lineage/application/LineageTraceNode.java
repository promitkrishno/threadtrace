package com.cse444.threadtrace.lineage.application;

import java.math.BigDecimal;
import java.util.UUID;

public record LineageTraceNode(
        UUID lotId,
        String lotNumber,
        String status,
        BigDecimal quantity,
        String unit,
        String styleCode,
        String shipmentCode,
        String facilityName,
        String facilityCity,
        String partyName,
        String partyType,
        String countryCode,
        int depth,
        String path
) {
}
