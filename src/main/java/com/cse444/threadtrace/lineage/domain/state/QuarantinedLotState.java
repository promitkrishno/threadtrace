package com.cse444.threadtrace.lineage.domain.state;

import com.cse444.threadtrace.common.error.DomainException;
import com.cse444.threadtrace.lineage.domain.model.EventType;
import com.cse444.threadtrace.lineage.domain.model.LotStatus;

public class QuarantinedLotState implements LotState {

    @Override
    public LotStatus getStatus() {
        return LotStatus.QUARANTINED;
    }

    @Override
    public void validateCanRecordEvent(EventType eventType) {
        if (eventType != EventType.CORRECTION) {
            throw new DomainException(
                    "LOT_QUARANTINED",
                    "Cannot process lot in QUARANTINED state with event '" + eventType + "'"
            );
        }
    }

    @Override
    public LotStatus determineNextStatus(EventType eventType, boolean isFullyConsumed) {
        if (eventType == EventType.CORRECTION) {
            return LotStatus.ACTIVE;
        }
        return LotStatus.QUARANTINED;
    }
}
