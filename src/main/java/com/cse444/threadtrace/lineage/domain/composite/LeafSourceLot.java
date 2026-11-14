package com.cse444.threadtrace.lineage.domain.composite;

import com.cse444.threadtrace.lineage.domain.model.Unit;
import com.cse444.threadtrace.lineage.domain.visitor.LineageVisitor;

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
    private final Unit lotUnit;
    private final BigDecimal accumulatedWaterL;
    private final BigDecimal accumulatedEnergyKwh;
    private final BigDecimal accumulatedCo2eKg;

    private BigDecimal certifiedRecycledPct = BigDecimal.ZERO;
    private BigDecimal certifiedOrganicPct = BigDecimal.ZERO;
    private BigDecimal claimedRecycledPct = BigDecimal.ZERO;
    private BigDecimal claimedOrganicPct = BigDecimal.ZERO;
    private boolean certificateAttached = false;
    private boolean orphan = false;

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
        this.lotUnit = parseUnit(unit);
        this.accumulatedWaterL = accumulatedWaterL != null ? accumulatedWaterL : BigDecimal.ZERO;
        this.accumulatedEnergyKwh = accumulatedEnergyKwh != null ? accumulatedEnergyKwh : BigDecimal.ZERO;
        this.accumulatedCo2eKg = accumulatedCo2eKg != null ? accumulatedCo2eKg : BigDecimal.ZERO;
    }

    private static Unit parseUnit(String u) {
        if (u == null) return Unit.KG;
        try {
            return Unit.valueOf(u.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return Unit.KG;
        }
    }

    public LeafSourceLot withCertifications(
            BigDecimal certifiedRecycledPct,
            BigDecimal certifiedOrganicPct,
            boolean hasCertificate
    ) {
        this.certifiedRecycledPct = certifiedRecycledPct != null ? certifiedRecycledPct : BigDecimal.ZERO;
        this.certifiedOrganicPct = certifiedOrganicPct != null ? certifiedOrganicPct : BigDecimal.ZERO;
        this.certificateAttached = hasCertificate;
        return this;
    }

    public LeafSourceLot withClaims(
            BigDecimal claimedRecycledPct,
            BigDecimal claimedOrganicPct
    ) {
        this.claimedRecycledPct = claimedRecycledPct != null ? claimedRecycledPct : BigDecimal.ZERO;
        this.claimedOrganicPct = claimedOrganicPct != null ? claimedOrganicPct : BigDecimal.ZERO;
        return this;
    }

    public LeafSourceLot setOrphan(boolean orphan) {
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
        return true;
    }

    @Override
    public List<LineageNode> getParents() {
        return Collections.emptyList();
    }

    @Override
    public BigDecimal getStepWaterL() {
        return accumulatedWaterL;
    }

    @Override
    public BigDecimal getStepEnergyKwh() {
        return accumulatedEnergyKwh;
    }

    @Override
    public BigDecimal getStepCo2eKg() {
        return accumulatedCo2eKg;
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
        return orphan;
    }

    @Override
    public void accept(LineageVisitor visitor) {
        visitor.visitLeaf(this);
    }
}
