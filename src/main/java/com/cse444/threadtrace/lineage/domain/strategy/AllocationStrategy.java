package com.cse444.threadtrace.lineage.domain.strategy;

import com.cse444.threadtrace.lineage.domain.model.Unit;

import java.math.BigDecimal;

public interface AllocationStrategy {

    String getStrategyName();

    BigDecimal calculateRatio(
            BigDecimal childQuantity,
            Unit childUnit,
            BigDecimal parentQuantity,
            Unit parentUnit
    );

    default FootprintMetrics allocate(
            FootprintMetrics parentMetrics,
            BigDecimal childQuantity,
            Unit childUnit,
            BigDecimal parentQuantity,
            Unit parentUnit
    ) {
        BigDecimal ratio = calculateRatio(childQuantity, childUnit, parentQuantity, parentUnit);
        return parentMetrics != null ? parentMetrics.multiply(ratio) : FootprintMetrics.ZERO;
    }
}
