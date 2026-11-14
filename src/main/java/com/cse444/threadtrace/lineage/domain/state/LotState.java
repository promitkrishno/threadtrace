package com.cse444.threadtrace.lineage.domain.state;

import com.cse444.threadtrace.lineage.domain.model.EventType;
import com.cse444.threadtrace.lineage.domain.model.LotStatus;

public interface LotState {
    LotStatus getStatus();
    void validateCanRecordEvent(EventType eventType);
    LotStatus determineNextStatus(EventType eventType, boolean isFullyConsumed);
}
