package com.cse444.threadtrace.lineage.application;

import com.cse444.threadtrace.lineage.domain.model.Unit;

import java.math.BigDecimal;
import java.util.UUID;

public record ChildLotOutput(
        String lotNumber,
        String styleCode,
        String shipmentCode,
        BigDecimal quantity,
        Unit unit,
        BigDecimal allocationRatio
) {
}
