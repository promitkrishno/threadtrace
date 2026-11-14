package com.cse444.threadtrace.lineage.domain.state;

import com.cse444.threadtrace.common.error.DomainException;
import com.cse444.threadtrace.lineage.domain.model.EventType;
import com.cse444.threadtrace.lineage.domain.model.LotStatus;

public class ConsumedLotState implements LotState {

    @Override
    public LotStatus getStatus() {
        return LotStatus.CONSUMED;
    }

    @Override
    public void validateCanRecordEvent(EventType eventType) {
        if (eventType != EventType.CORRECTION) {
            throw new DomainException(
                    "LOT_ALREADY_CONSUMED",
                    "Cannot record event '" + eventType + "' on a CONSUMED lot"
            );
        }
    }

    @Override
    public LotStatus determineNextStatus(EventType eventType, boolean isFullyConsumed) {
        return LotStatus.CONSUMED;
    }
}
