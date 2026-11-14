package com.cse444.threadtrace.lineage.domain.visitor;

import com.cse444.threadtrace.lineage.domain.composite.CompositeTransformedLot;
import com.cse444.threadtrace.lineage.domain.composite.LeafSourceLot;
import com.cse444.threadtrace.lineage.domain.composite.LineageNode;
import com.cse444.threadtrace.lineage.domain.strategy.AllocationStrategy;
import com.cse444.threadtrace.lineage.domain.strategy.FootprintMetrics;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Visitor Pattern: Traverses a LineageNode tree/DAG and computes total water (L),
 * energy (kWh), and CO2e (kg) per lot and per piece using an AllocationStrategy.
 */
public class FootprintRollupVisitor implements LineageVisitor {

    private final AllocationStrategy allocationStrategy;
    private final Map<UUID, FootprintMetrics> nodeFootprints = new HashMap<>();
    private UUID rootLotId;

    public FootprintRollupVisitor() {
        this(null);
    }

    public FootprintRollupVisitor(AllocationStrategy allocationStrategy) {
        this.allocationStrategy = allocationStrategy;
    }

    @Override
    public void visitLeaf(LeafSourceLot leaf) {
        if (leaf == null) return;
        if (rootLotId == null) {
            rootLotId = leaf.getLotId();
        }

        FootprintMetrics metrics = new FootprintMetrics(
                leaf.getAccumulatedWaterL(),
                leaf.getAccumulatedEnergyKwh(),
                leaf.getAccumulatedCo2eKg()
        );
        nodeFootprints.put(leaf.getLotId(), metrics);
    }

    @Override
    public void visitComposite(CompositeTransformedLot composite) {
        if (composite == null) return;
        if (rootLotId == null) {
            rootLotId = composite.getLotId();
        }

        if (nodeFootprints.containsKey(composite.getLotId())) {
            return;
        }

        FootprintMetrics parentsAllocatedTotal = FootprintMetrics.ZERO;

        for (LineageNode parent : composite.getParents()) {
            if (!nodeFootprints.containsKey(parent.getLotId())) {
                parent.accept(this);
            }

            FootprintMetrics parentMetric = nodeFootprints.getOrDefault(
                    parent.getLotId(),
                    new FootprintMetrics(
                            parent.getAccumulatedWaterL(),
                            parent.getAccumulatedEnergyKwh(),
                            parent.getAccumulatedCo2eKg()
                    )
            );

            if (allocationStrategy != null) {
                FootprintMetrics allocated = allocationStrategy.allocate(
                        parentMetric,
                        composite.getQuantity(),
                        composite.getLotUnit(),
                        parent.getQuantity(),
                        parent.getLotUnit()
                );
                parentsAllocatedTotal = parentsAllocatedTotal.add(allocated);
            } else {
                parentsAllocatedTotal = parentsAllocatedTotal.add(parentMetric);
            }
        }

        FootprintMetrics stepMetric = new FootprintMetrics(
                composite.getStepWaterL(),
                composite.getStepEnergyKwh(),
                composite.getStepCo2eKg()
        );

        FootprintMetrics totalComposite = stepMetric.add(parentsAllocatedTotal);
        nodeFootprints.put(composite.getLotId(), totalComposite);
    }

    public FootprintMetrics getRootMetrics() {
        if (rootLotId != null && nodeFootprints.containsKey(rootLotId)) {
            return nodeFootprints.get(rootLotId);
        }
        return FootprintMetrics.ZERO;
    }

    public BigDecimal getTotalWaterL() {
        return getRootMetrics().waterLiters();
    }

    public BigDecimal getTotalEnergyKwh() {
        return getRootMetrics().energyKwh();
    }

    public BigDecimal getTotalCo2eKg() {
        return getRootMetrics().co2eKg();
    }

    public FootprintMetrics getFootprintPerPiece(BigDecimal pieceCount) {
        return getRootMetrics().divide(pieceCount);
    }

    public FootprintMetrics getMetricsForLot(UUID lotId) {
        return nodeFootprints.getOrDefault(lotId, FootprintMetrics.ZERO);
    }
}
