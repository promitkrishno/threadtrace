package com.cse444.threadtrace.lineage.domain.strategy;

import com.cse444.threadtrace.lineage.domain.model.Unit;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Strategy Pattern: Allocates footprints across split/merged lots by piece count.
 */
@Component("pieceCountAllocationStrategy")
public class PieceCountAllocationStrategy implements AllocationStrategy {

    @Override
    public String getStrategyName() {
        return "PIECE_COUNT";
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

        BigDecimal childPieces = toPieces(childQuantity, childUnit);
        BigDecimal parentPieces = toPieces(parentQuantity, parentUnit);

        if (parentPieces.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        return childPieces.divide(parentPieces, 6, RoundingMode.HALF_UP);
    }

    private BigDecimal toPieces(BigDecimal quantity, Unit unit) {
        if (unit == null) return quantity;
        return switch (unit) {
            case PIECES -> quantity;
            case CARTONS -> quantity.multiply(BigDecimal.valueOf(50.0)); // nominal 50 pieces per carton
            case KG -> quantity.divide(BigDecimal.valueOf(0.20), 4, RoundingMode.HALF_UP); // 200g per piece = 5 pieces per KG
        };
    }
}
