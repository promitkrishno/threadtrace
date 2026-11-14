package com.cse444.threadtrace.lineage.domain.composite;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Composite Pattern: Leaf node representing an un-transformed source material lot (e.g. raw cotton, yarn).
 */
public class LeafSourceLot implements LineageNode {

    private final UUID lotId;
    private final String lotNumber;
    private final BigDecimal quantity;
    private final String unit;
    private final BigDecimal accumulatedWaterL;
    private final BigDecimal accumulatedEnergyKwh;
    private final BigDecimal accumulatedCo2eKg;

    public LeafSourceLot(
            UUID lotId,
            String lotNumber,
            BigDecimal quantity,
            String unit,
            BigDecimal accumulatedWaterL,
            BigDecimal accumulatedEnergyKwh,
            BigDecimal accumulatedCo2eKg
    ) {
        this.lotId = lotId;
        this.lotNumber = lotNumber;
        this.quantity = quantity;
        this.unit = unit;
        this.accumulatedWaterL = accumulatedWaterL != null ? accumulatedWaterL : BigDecimal.ZERO;
        this.accumulatedEnergyKwh = accumulatedEnergyKwh != null ? accumulatedEnergyKwh : BigDecimal.ZERO;
        this.accumulatedCo2eKg = accumulatedCo2eKg != null ? accumulatedCo2eKg : BigDecimal.ZERO;
    }

    @Override
    public UUID getLotId() {
        return lotId;
    }

    @Override
    public String getLotNumber() {
        return lotNumber;
    }

    @Override
    public BigDecimal getQuantity() {
        return quantity;
    }

    @Override
    public String getUnit() {
        return unit;
    }

    @Override
    public boolean isLeaf() {
        return true;
    }

    @Override
    public List<LineageNode> getParents() {
        return Collections.emptyList();
    }

    @Override
    public BigDecimal getAccumulatedWaterL() {
        return accumulatedWaterL;
    }

    @Override
    public BigDecimal getAccumulatedEnergyKwh() {
        return accumulatedEnergyKwh;
    }

    @Override
    public BigDecimal getAccumulatedCo2eKg() {
        return accumulatedCo2eKg;
    }
}
