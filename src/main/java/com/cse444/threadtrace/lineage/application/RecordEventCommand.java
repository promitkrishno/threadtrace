package com.cse444.threadtrace.lineage.application;

import com.cse444.threadtrace.lineage.domain.model.EventType;
import com.cse444.threadtrace.lineage.domain.model.Unit;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RecordEventCommand(
        // For processing an existing lot
        UUID lotId,

        // For RECEIVE (genesis) creation of a new lot:
        String newLotNumber,
        UUID brandId,
        String styleCode,
        String shipmentCode,
        Unit lotUnit,

        @NotNull(message = "Event type is required")
        EventType eventType,

        @NotNull(message = "Facility ID is required")
        UUID facilityId,

        @NotNull(message = "Input quantity is required")
        @Positive
        BigDecimal inputQuantity,

        @NotNull(message = "Output quantity is required")
        BigDecimal outputQuantity,

        @NotNull(message = "Unit is required")
        String unit,

        Instant occurredAt,

        // For 1-to-N splits:
        List<ChildLotOutput> splitChildren,

        // For N-to-1 merges:
        List<ParentLotInput> mergeParents
) {
}
