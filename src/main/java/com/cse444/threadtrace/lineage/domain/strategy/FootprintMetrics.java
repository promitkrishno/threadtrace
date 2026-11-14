package com.cse444.threadtrace.lineage.domain.strategy;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record FootprintMetrics(
        BigDecimal waterLiters,
        BigDecimal energyKwh,
        BigDecimal co2eKg
) {
    public static final FootprintMetrics ZERO = new FootprintMetrics(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

    public FootprintMetrics {
        waterLiters = waterLiters != null ? waterLiters : BigDecimal.ZERO;
        energyKwh = energyKwh != null ? energyKwh : BigDecimal.ZERO;
        co2eKg = co2eKg != null ? co2eKg : BigDecimal.ZERO;
    }

    public FootprintMetrics add(FootprintMetrics other) {
        if (other == null) return this;
        return new FootprintMetrics(
                this.waterLiters.add(other.waterLiters),
                this.energyKwh.add(other.energyKwh),
                this.co2eKg.add(other.co2eKg)
        );
    }

    public FootprintMetrics multiply(BigDecimal ratio) {
        if (ratio == null) ratio = BigDecimal.ONE;
        return new FootprintMetrics(
                this.waterLiters.multiply(ratio).setScale(4, RoundingMode.HALF_UP),
                this.energyKwh.multiply(ratio).setScale(4, RoundingMode.HALF_UP),
                this.co2eKg.multiply(ratio).setScale(4, RoundingMode.HALF_UP)
        );
    }

    public FootprintMetrics divide(BigDecimal divisor) {
        if (divisor == null || divisor.compareTo(BigDecimal.ZERO) == 0) {
            return ZERO;
        }
        return new FootprintMetrics(
                this.waterLiters.divide(divisor, 4, RoundingMode.HALF_UP),
                this.energyKwh.divide(divisor, 4, RoundingMode.HALF_UP),
                this.co2eKg.divide(divisor, 4, RoundingMode.HALF_UP)
        );
    }
}
