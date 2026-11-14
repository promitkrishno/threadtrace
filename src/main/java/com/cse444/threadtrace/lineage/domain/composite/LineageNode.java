package com.cse444.threadtrace.lineage.domain.composite;

import com.cse444.threadtrace.lineage.domain.model.Unit;
import com.cse444.threadtrace.lineage.domain.visitor.LineageVisitor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Composite Pattern: Component interface representing a node in the lot lineage DAG.
 */
public interface LineageNode {
    UUID getLotId();
    String getLotNumber();
    BigDecimal getQuantity();
    String getUnit();
    Unit getLotUnit();
    boolean isLeaf();
    List<LineageNode> getParents();

    BigDecimal getStepWaterL();
    BigDecimal getStepEnergyKwh();
    BigDecimal getStepCo2eKg();

    BigDecimal getAccumulatedWaterL();
    BigDecimal getAccumulatedEnergyKwh();
    BigDecimal getAccumulatedCo2eKg();

    BigDecimal getCertifiedRecycledPct();
    BigDecimal getCertifiedOrganicPct();
    BigDecimal getClaimedRecycledPct();
    BigDecimal getClaimedOrganicPct();

    boolean hasCertificate();
    boolean isOrphan();

    void accept(LineageVisitor visitor);
}
