package com.cse444.threadtrace.lineage.domain.model;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class LotLineageEdgeId implements Serializable {

    private UUID parentLotId;
    private UUID childLotId;

    public LotLineageEdgeId() {
    }

    public LotLineageEdgeId(UUID parentLotId, UUID childLotId) {
        this.parentLotId = parentLotId;
        this.childLotId = childLotId;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LotLineageEdgeId that = (LotLineageEdgeId) o;
        return Objects.equals(parentLotId, that.parentLotId) && Objects.equals(childLotId, that.childLotId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(parentLotId, childLotId);
    }
}
