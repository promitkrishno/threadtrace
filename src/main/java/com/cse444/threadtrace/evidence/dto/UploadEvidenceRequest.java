package com.cse444.threadtrace.evidence.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record UploadEvidenceRequest(
        @NotBlank String certificateType,
        @NotBlank String issuerName,
        String scopeDescription,
        String claimType,
        BigDecimal certifiedPercentage,
        @NotNull LocalDate validFrom,
        @NotNull LocalDate validUntil,
        UUID partyId,
        UUID lotId
) {}
