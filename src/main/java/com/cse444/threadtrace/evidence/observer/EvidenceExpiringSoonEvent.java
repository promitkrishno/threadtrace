package com.cse444.threadtrace.evidence.observer;

import java.time.LocalDate;
import java.util.UUID;

public record EvidenceExpiringSoonEvent(
        UUID evidenceId,
        String certificateType,
        String issuerName,
        LocalDate validUntil,
        UUID lotId,
        UUID partyId
) {}
