package com.cse444.threadtrace.lineage.application;

import java.math.BigDecimal;
import java.util.UUID;

public record ParentLotInput(
        UUID parentLotId,
        BigDecimal inputQuantityUsed,
        String inputUnit,
        BigDecimal allocationRatio
) {
}
