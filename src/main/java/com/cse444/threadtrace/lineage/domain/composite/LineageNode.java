package com.cse444.threadtrace.lineage.domain.composite;

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
    boolean isLeaf();
    List<LineageNode> getParents();
    BigDecimal getAccumulatedWaterL();
    BigDecimal getAccumulatedEnergyKwh();
    BigDecimal getAccumulatedCo2eKg();
}
