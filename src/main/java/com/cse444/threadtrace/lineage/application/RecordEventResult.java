package com.cse444.threadtrace.lineage.application;

import com.cse444.threadtrace.lineage.domain.model.EventType;
import com.cse444.threadtrace.lineage.domain.model.LotStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RecordEventResult(
        UUID eventId,
        UUID lotId,
        String lotNumber,
        Long sequenceNo,
        EventType eventType,
        LotStatus lotStatus,
        BigDecimal inputQuantity,
        BigDecimal outputQuantity,
        BigDecimal lossPercentage,
        Boolean massBalanceFlagged,
        String previousEventHash,
        String eventHash,
        Instant occurredAt,
        List<UUID> createdChildLotIds
) {
}
