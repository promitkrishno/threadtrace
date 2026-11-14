package com.cse444.threadtrace.lineage.domain.state;

import com.cse444.threadtrace.lineage.domain.model.LotStatus;

public final class LotStateFactory {

    private static final LotState ACTIVE = new ActiveLotState();
    private static final LotState CONSUMED = new ConsumedLotState();
    private static final LotState QUARANTINED = new QuarantinedLotState();
    private static final LotState SHIPPED = new ShippedLotState();

    private LotStateFactory() {
    }

    public static LotState from(LotStatus status) {
        if (status == null) {
            return ACTIVE;
        }
        return switch (status) {
            case ACTIVE -> ACTIVE;
            case CONSUMED -> CONSUMED;
            case QUARANTINED -> QUARANTINED;
            case SHIPPED -> SHIPPED;
        };
    }
}
