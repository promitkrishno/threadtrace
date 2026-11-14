package com.cse444.threadtrace.lineage.domain.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "lot_lineage_edges")
@IdClass(LotLineageEdgeId.class)
public class LotLineageEdgeEntity {

    @Id
    @Column(name = "parent_lot_id", nullable = false)
    private UUID parentLotId;

    @Id
    @Column(name = "child_lot_id", nullable = false)
    private UUID childLotId;

    @Column(name = "causing_event_id", nullable = false)
    private UUID causingEventId;

    @Column(name = "input_quantity_used", nullable = false, precision = 15, scale = 4)
    private BigDecimal inputQuantityUsed;

    @Column(name = "input_unit", nullable = false, length = 50)
    private String inputUnit;

    @Column(name = "allocation_ratio", nullable = false, precision = 10, scale = 6)
    private BigDecimal allocationRatio = BigDecimal.ONE;

    public LotLineageEdgeEntity() {
    }

    public LotLineageEdgeEntity(
            UUID parentLotId,
            UUID childLotId,
            UUID causingEventId,
            BigDecimal inputQuantityUsed,
            String inputUnit,
            BigDecimal allocationRatio
    ) {
        this.parentLotId = parentLotId;
        this.childLotId = childLotId;
        this.causingEventId = causingEventId;
        this.inputQuantityUsed = inputQuantityUsed;
        this.inputUnit = inputUnit;
        this.allocationRatio = allocationRatio != null ? allocationRatio : BigDecimal.ONE;
    }

    public UUID getParentLotId() {
        return parentLotId;
    }

    public void setParentLotId(UUID parentLotId) {
        this.parentLotId = parentLotId;
    }

    public UUID getChildLotId() {
        return childLotId;
    }

    public void setChildLotId(UUID childLotId) {
        this.childLotId = childLotId;
    }

    public UUID getCausingEventId() {
        return causingEventId;
    }

    public void setCausingEventId(UUID causingEventId) {
        this.causingEventId = causingEventId;
    }

    public BigDecimal getInputQuantityUsed() {
        return inputQuantityUsed;
    }

    public void setInputQuantityUsed(BigDecimal inputQuantityUsed) {
        this.inputQuantityUsed = inputQuantityUsed;
    }

    public String getInputUnit() {
        return inputUnit;
    }

    public void setInputUnit(String inputUnit) {
        this.inputUnit = inputUnit;
    }

    public BigDecimal getAllocationRatio() {
        return allocationRatio;
    }

    public void setAllocationRatio(BigDecimal allocationRatio) {
        this.allocationRatio = allocationRatio;
    }
}
