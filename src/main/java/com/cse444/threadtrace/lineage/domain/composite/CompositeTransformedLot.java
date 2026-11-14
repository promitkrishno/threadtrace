package com.cse444.threadtrace.lineage.domain.composite;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Composite Pattern: Composite node representing a transformed or merged lot
 * composed of one or more parent input lots.
 */
public class CompositeTransformedLot implements LineageNode {

    private final UUID lotId;
    private final String lotNumber;
    private final BigDecimal quantity;
    private final String unit;
    private final BigDecimal stepWaterL;
    private final BigDecimal stepEnergyKwh;
    private final BigDecimal stepCo2eKg;
    private final List<LineageNode> parents = new ArrayList<>();

    public CompositeTransformedLot(
            UUID lotId,
            String lotNumber,
            BigDecimal quantity,
            String unit,
            BigDecimal stepWaterL,
            BigDecimal stepEnergyKwh,
            BigDecimal stepCo2eKg
    ) {
        this.lotId = lotId;
        this.lotNumber = lotNumber;
        this.quantity = quantity;
        this.unit = unit;
        this.stepWaterL = stepWaterL != null ? stepWaterL : BigDecimal.ZERO;
        this.stepEnergyKwh = stepEnergyKwh != null ? stepEnergyKwh : BigDecimal.ZERO;
        this.stepCo2eKg = stepCo2eKg != null ? stepCo2eKg : BigDecimal.ZERO;
    }

    public void addParent(LineageNode parent) {
        if (parent != null) {
            this.parents.add(parent);
        }
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
        return false;
    }

    @Override
    public List<LineageNode> getParents() {
        return Collections.unmodifiableList(parents);
    }

    @Override
    public BigDecimal getAccumulatedWaterL() {
        BigDecimal total = stepWaterL;
        for (LineageNode p : parents) {
            total = total.add(p.getAccumulatedWaterL());
        }
        return total;
    }

    @Override
    public BigDecimal getAccumulatedEnergyKwh() {
        BigDecimal total = stepEnergyKwh;
        for (LineageNode p : parents) {
            total = total.add(p.getAccumulatedEnergyKwh());
        }
        return total;
    }

    @Override
    public BigDecimal getAccumulatedCo2eKg() {
        BigDecimal total = stepCo2eKg;
        for (LineageNode p : parents) {
            total = total.add(p.getAccumulatedCo2eKg());
        }
        return total;
    }
}
