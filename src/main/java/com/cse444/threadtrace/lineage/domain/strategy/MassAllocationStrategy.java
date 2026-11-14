package com.cse444.threadtrace.lineage.domain.strategy;

import com.cse444.threadtrace.lineage.domain.model.Unit;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Strategy Pattern: Allocates footprints across split/merged lots by mass (KG).
 */
@Component("massAllocationStrategy")
public class MassAllocationStrategy implements AllocationStrategy {

    @Override
    public String getStrategyName() {
        return "MASS";
    }

    @Override
    public BigDecimal calculateRatio(
            BigDecimal childQuantity,
            Unit childUnit,
            BigDecimal parentQuantity,
            Unit parentUnit
    ) {
        if (childQuantity == null || parentQuantity == null || parentQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        // Mass-based allocation: normalized to KG
        // If child and parent are KG, direct ratio
        BigDecimal childMassKg = toKg(childQuantity, childUnit);
        BigDecimal parentMassKg = toKg(parentQuantity, parentUnit);

        if (parentMassKg.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        return childMassKg.divide(parentMassKg, 6, RoundingMode.HALF_UP);
    }

    private BigDecimal toKg(BigDecimal quantity, Unit unit) {
        if (unit == null) return quantity;
        return switch (unit) {
            case KG -> quantity;
            case PIECES -> quantity.multiply(BigDecimal.valueOf(0.20)); // nominal 200g per garment piece
            case CARTONS -> quantity.multiply(BigDecimal.valueOf(10.0)); // nominal 10kg per carton
        };
    }
}
