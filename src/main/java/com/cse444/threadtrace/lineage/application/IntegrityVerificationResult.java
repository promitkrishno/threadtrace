package com.cse444.threadtrace.lineage.application;

import java.time.Instant;
import java.util.UUID;

public record IntegrityVerificationResult(
        UUID lotId,
        String lotNumber,
        int totalEvents,
        boolean verified,
        UUID compromisedEventId,
        Long compromisedSequenceNo,
        String message,
        Instant checkedAt
) {
    public static IntegrityVerificationResult valid(UUID lotId, String lotNumber, int totalEvents) {
        return new IntegrityVerificationResult(
                lotId,
                lotNumber,
                totalEvents,
                true,
                null,
                null,
                "All " + totalEvents + " events verified successfully against SHA-256 hash chain.",
                Instant.now()
        );
    }

    public static IntegrityVerificationResult tampered(
            UUID lotId,
            String lotNumber,
            int totalEvents,
            UUID compromisedEventId,
            Long sequenceNo,
            String reason
    ) {
        return new IntegrityVerificationResult(
                lotId,
                lotNumber,
                totalEvents,
                false,
                compromisedEventId,
                sequenceNo,
                "Tampering detected at event sequence " + sequenceNo + ": " + reason,
                Instant.now()
        );
    }
}
