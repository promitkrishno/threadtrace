package com.cse444.threadtrace.lineage.domain.state;

import com.cse444.threadtrace.common.error.DomainException;
import com.cse444.threadtrace.lineage.domain.model.EventType;
import com.cse444.threadtrace.lineage.domain.model.LotStatus;

public class ActiveLotState implements LotState {

    @Override
    public LotStatus getStatus() {
        return LotStatus.ACTIVE;
    }

    @Override
    public void validateCanRecordEvent(EventType eventType) {
        if (eventType == null) {
            throw new DomainException("INVALID_EVENT", "Event type cannot be null");
        }
    }

    @Override
    public LotStatus determineNextStatus(EventType eventType, boolean isFullyConsumed) {
        if (eventType == EventType.SHIP) {
            return LotStatus.SHIPPED;
        }
        if (isFullyConsumed) {
            return LotStatus.CONSUMED;
        }
        return LotStatus.ACTIVE;
    }
}
