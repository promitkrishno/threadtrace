package com.cse444.threadtrace.lineage.domain.composite;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class LotCompositePatternTest {

    @Test
    @DisplayName("LeafSourceLot acts as terminal genesis node with no parents")
    void leafSourceLotBehavior() {
        UUID lotId = UUID.randomUUID();
        LeafSourceLot leaf = new LeafSourceLot(
                lotId,
                "LOT-RAW-001",
                BigDecimal.valueOf(100.0),
                "KG",
                BigDecimal.valueOf(500.0),
                BigDecimal.valueOf(50.0),
                BigDecimal.valueOf(25.0)
        );

        assertThat(leaf.isLeaf()).isTrue();
        assertThat(leaf.getParents()).isEmpty();
        assertThat(leaf.getLotId()).isEqualTo(lotId);
        assertThat(leaf.getLotNumber()).isEqualTo("LOT-RAW-001");
        assertThat(leaf.getAccumulatedWaterL()).isEqualByComparingTo("500.0");
        assertThat(leaf.getAccumulatedEnergyKwh()).isEqualByComparingTo("50.0");
        assertThat(leaf.getAccumulatedCo2eKg()).isEqualByComparingTo("25.0");
    }

    @Test
    @DisplayName("CompositeTransformedLot aggregates metrics across multi-parent DAG tree")
    void compositeTransformedLotAggregatesMetrics() {
        // Parent 1: Fabric roll A
        LeafSourceLot fabricA = new LeafSourceLot(
                UUID.randomUUID(),
                "LOT-FABRIC-A",
                BigDecimal.valueOf(50.0),
                "KG",
                BigDecimal.valueOf(200.0),
                BigDecimal.valueOf(20.0),
                BigDecimal.valueOf(10.0)
        );

        // Parent 2: Fabric roll B (contrast panel)
        LeafSourceLot fabricB = new LeafSourceLot(
                UUID.randomUUID(),
                "LOT-FABRIC-B",
                BigDecimal.valueOf(10.0),
                "KG",
                BigDecimal.valueOf(50.0),
                BigDecimal.valueOf(5.0),
                BigDecimal.valueOf(2.5)
        );

        // Intermediate Composite: Cut bundle
        CompositeTransformedLot cutBundle = new CompositeTransformedLot(
                UUID.randomUUID(),
                "LOT-CUT-BUNDLE-01",
                BigDecimal.valueOf(45.0),
                "KG",
                BigDecimal.valueOf(10.0), // cutting step water
                BigDecimal.valueOf(2.0),  // cutting step energy
                BigDecimal.valueOf(1.0)   // cutting step co2e
        );
        cutBundle.addParent(fabricA);

        // Final Composite: Sewn Garment assembling cut bundle + contrast fabric B
        CompositeTransformedLot sewnGarment = new CompositeTransformedLot(
                UUID.randomUUID(),
                "LOT-SEWN-01",
                BigDecimal.valueOf(50.0),
                "PIECES",
                BigDecimal.valueOf(30.0), // sewing step water
                BigDecimal.valueOf(8.0),  // sewing step energy
                BigDecimal.valueOf(4.0)   // sewing step co2e
        );
        sewnGarment.addParent(cutBundle);
        sewnGarment.addParent(fabricB);

        assertThat(sewnGarment.isLeaf()).isFalse();
        assertThat(sewnGarment.getParents()).hasSize(2);

        // Total Water: 30 (sewn) + 10 (cut) + 200 (fabricA) + 50 (fabricB) = 290.0
        assertThat(sewnGarment.getAccumulatedWaterL()).isEqualByComparingTo("290.0");

        // Total Energy: 8 (sewn) + 2 (cut) + 20 (fabricA) + 5 (fabricB) = 35.0
        assertThat(sewnGarment.getAccumulatedEnergyKwh()).isEqualByComparingTo("35.0");

        // Total CO2e: 4 (sewn) + 1 (cut) + 10 (fabricA) + 2.5 (fabricB) = 17.5
        assertThat(sewnGarment.getAccumulatedCo2eKg()).isEqualByComparingTo("17.5");
    }
}
