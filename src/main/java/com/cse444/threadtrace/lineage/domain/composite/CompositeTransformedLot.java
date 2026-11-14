package com.cse444.threadtrace.lineage.domain.composite;

import com.cse444.threadtrace.lineage.domain.model.Unit;
import com.cse444.threadtrace.lineage.domain.visitor.LineageVisitor;

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
    private final Unit lotUnit;
    private final BigDecimal stepWaterL;
    private final BigDecimal stepEnergyKwh;
    private final BigDecimal stepCo2eKg;
    private final List<LineageNode> parents = new ArrayList<>();

    private BigDecimal certifiedRecycledPct = BigDecimal.ZERO;
    private BigDecimal certifiedOrganicPct = BigDecimal.ZERO;
    private BigDecimal claimedRecycledPct = BigDecimal.ZERO;
    private BigDecimal claimedOrganicPct = BigDecimal.ZERO;
    private boolean certificateAttached = false;
    private boolean orphan = false;

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
        this.lotUnit = parseUnit(unit);
        this.stepWaterL = stepWaterL != null ? stepWaterL : BigDecimal.ZERO;
        this.stepEnergyKwh = stepEnergyKwh != null ? stepEnergyKwh : BigDecimal.ZERO;
        this.stepCo2eKg = stepCo2eKg != null ? stepCo2eKg : BigDecimal.ZERO;
    }

    private static Unit parseUnit(String u) {
        if (u == null) return Unit.KG;
        try {
            return Unit.valueOf(u.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return Unit.KG;
        }
    }

    public void addParent(LineageNode parent) {
        if (parent != null) {
            this.parents.add(parent);
        }
    }

    public CompositeTransformedLot withCertifications(
            BigDecimal certifiedRecycledPct,
            BigDecimal certifiedOrganicPct,
            boolean hasCertificate
    ) {
        this.certifiedRecycledPct = certifiedRecycledPct != null ? certifiedRecycledPct : BigDecimal.ZERO;
        this.certifiedOrganicPct = certifiedOrganicPct != null ? certifiedOrganicPct : BigDecimal.ZERO;
        this.certificateAttached = hasCertificate;
        return this;
    }

    public CompositeTransformedLot withClaims(
            BigDecimal claimedRecycledPct,
            BigDecimal claimedOrganicPct
    ) {
        this.claimedRecycledPct = claimedRecycledPct != null ? claimedRecycledPct : BigDecimal.ZERO;
        this.claimedOrganicPct = claimedOrganicPct != null ? claimedOrganicPct : BigDecimal.ZERO;
        return this;
    }

    public CompositeTransformedLot setOrphan(boolean orphan) {
        this.orphan = orphan;
        return this;
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
    public Unit getLotUnit() {
        return lotUnit;
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
    public BigDecimal getStepWaterL() {
        return stepWaterL;
    }

    @Override
    public BigDecimal getStepEnergyKwh() {
        return stepEnergyKwh;
    }

    @Override
    public BigDecimal getStepCo2eKg() {
        return stepCo2eKg;
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

    @Override
    public BigDecimal getCertifiedRecycledPct() {
        return certifiedRecycledPct;
    }

    @Override
    public BigDecimal getCertifiedOrganicPct() {
        return certifiedOrganicPct;
    }

    @Override
    public BigDecimal getClaimedRecycledPct() {
        return claimedRecycledPct;
    }

    @Override
    public BigDecimal getClaimedOrganicPct() {
        return claimedOrganicPct;
    }

    @Override
    public boolean hasCertificate() {
        return certificateAttached;
    }

    @Override
    public boolean isOrphan() {
        return orphan || parents.isEmpty();
    }

    @Override
    public void accept(LineageVisitor visitor) {
        visitor.visitComposite(this);
    }
}
