package com.cse444.threadtrace.evidence.dto;

import com.cse444.threadtrace.evidence.domain.EvidenceDocumentEntity;
import com.cse444.threadtrace.evidence.domain.EvidenceStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record EvidenceResponse(
        UUID id,
        String certificateType,
        String issuerName,
        String scopeDescription,
        String claimType,
        BigDecimal certifiedPercentage,
        String storageObjectKey,
        String fileSha256,
        LocalDate validFrom,
        LocalDate validUntil,
        EvidenceStatus status,
        UUID partyId,
        UUID lotId,
        Instant createdAt
) {
    public static EvidenceResponse from(EvidenceDocumentEntity doc) {
        return new EvidenceResponse(
                doc.getId(),
                doc.getCertificateType(),
                doc.getIssuerName(),
                doc.getScopeDescription(),
                doc.getClaimType(),
                doc.getCertifiedPercentage(),
                doc.getStorageObjectKey(),
                doc.getFileSha256(),
                doc.getValidFrom(),
                doc.getValidUntil(),
                doc.getStatus(),
                doc.getPartyId(),
                doc.getLotId(),
                doc.getCreatedAt()
        );
    }
}
