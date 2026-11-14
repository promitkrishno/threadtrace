package com.cse444.threadtrace.lineage.domain.state;

import com.cse444.threadtrace.common.error.DomainException;
import com.cse444.threadtrace.lineage.domain.model.EventType;
import com.cse444.threadtrace.lineage.domain.model.LotStatus;

public class ShippedLotState implements LotState {

    @Override
    public LotStatus getStatus() {
        return LotStatus.SHIPPED;
    }

    @Override
    public void validateCanRecordEvent(EventType eventType) {
        throw new DomainException(
                "LOT_ALREADY_SHIPPED",
                "Cannot record event '" + eventType + "' on a SHIPPED lot"
        );
    }

    @Override
    public LotStatus determineNextStatus(EventType eventType, boolean isFullyConsumed) {
        return LotStatus.SHIPPED;
    }
}
