package com.cse444.threadtrace.evidence;

import com.cse444.threadtrace.common.config.TraceabilityProperties;
import com.cse444.threadtrace.common.error.DomainException;
import com.cse444.threadtrace.common.storage.StoragePort;
import com.cse444.threadtrace.evidence.domain.EvidenceDocumentEntity;
import com.cse444.threadtrace.evidence.domain.EvidenceStatus;
import com.cse444.threadtrace.evidence.dto.EvidenceResponse;
import com.cse444.threadtrace.evidence.dto.UploadEvidenceRequest;
import com.cse444.threadtrace.evidence.infrastructure.EvidenceDocumentRepository;
import com.cse444.threadtrace.lineage.domain.model.LotEntity;
import com.cse444.threadtrace.lineage.infrastructure.LotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class EvidenceService {

    private final EvidenceDocumentRepository evidenceRepository;
    private final LotRepository lotRepository;
    private final StoragePort storagePort;
    private final TraceabilityProperties properties;

    public EvidenceService(
            EvidenceDocumentRepository evidenceRepository,
            LotRepository lotRepository,
            StoragePort storagePort,
            TraceabilityProperties properties
    ) {
        this.evidenceRepository = evidenceRepository;
        this.lotRepository = lotRepository;
        this.storagePort = storagePort;
        this.properties = properties;
    }

    public EvidenceResponse uploadAndAttach(
            UploadEvidenceRequest request,
            byte[] fileBytes,
            String originalFilename,
            String contentType
    ) {
        // 1. Validate Certificate Type
        List<String> validTypes = properties.validCertificateTypes();
        if (validTypes != null && !validTypes.isEmpty()) {
            boolean matches = validTypes.stream().anyMatch(t -> t.equalsIgnoreCase(request.certificateType()));
            if (!matches) {
                throw new DomainException("INVALID_CERTIFICATE_TYPE",
                        "Certificate type '" + request.certificateType() + "' is not recognized. Valid types: " + validTypes);
            }
        }

        // 2. Validate dates
        if (request.validFrom().isAfter(request.validUntil())) {
            throw new DomainException("INVALID_DATE_RANGE", "validFrom must not be after validUntil");
        }

        // 3. Compute SHA-256 and store file
        String sha256 = storagePort.computeSha256(fileBytes);
        String objectKey = "certificates/" + UUID.randomUUID() + "-" + (originalFilename != null ? originalFilename : "doc.pdf");
        storagePort.store(objectKey, fileBytes, contentType != null ? contentType : "application/pdf");

        // 4. Determine initial status
        LocalDate today = LocalDate.now();
        EvidenceStatus status = EvidenceStatus.ACTIVE;
        if (request.validUntil().isBefore(today)) {
            status = EvidenceStatus.EXPIRED;
        } else if (request.validUntil().isBefore(today.plusDays(30))) {
            status = EvidenceStatus.EXPIRING_SOON;
        }

        // 5. Persist Evidence Entity
        EvidenceDocumentEntity doc = new EvidenceDocumentEntity(
                request.certificateType().toUpperCase(),
                request.issuerName(),
                request.scopeDescription(),
                request.claimType(),
                request.certifiedPercentage(),
                objectKey,
                sha256,
                request.validFrom(),
                request.validUntil(),
                request.partyId(),
                request.lotId()
        );
        doc.setStatus(status);
        EvidenceDocumentEntity saved = evidenceRepository.save(doc);

        // 6. Update Lot certified percentage if attached and active
        if (request.lotId() != null && status == EvidenceStatus.ACTIVE) {
            lotRepository.findById(request.lotId()).ifPresent(lot -> {
                BigDecimal pct = request.certifiedPercentage() != null ? request.certifiedPercentage() : BigDecimal.ZERO;
                if ("RECYCLED".equalsIgnoreCase(request.claimType())) {
                    lot.setCertifiedRecycledPct(pct);
                } else if ("ORGANIC".equalsIgnoreCase(request.claimType())) {
                    lot.setCertifiedOrganicPct(pct);
                }
                lotRepository.save(lot);
            });
        }

        return EvidenceResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public EvidenceResponse getEvidence(UUID id) {
        return evidenceRepository.findById(id)
                .map(EvidenceResponse::from)
                .orElseThrow(() -> new DomainException("EVIDENCE_NOT_FOUND", "Evidence document not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public byte[] downloadEvidenceFile(UUID id) {
        EvidenceDocumentEntity doc = evidenceRepository.findById(id)
                .orElseThrow(() -> new DomainException("EVIDENCE_NOT_FOUND", "Evidence document not found with id: " + id));
        return storagePort.retrieve(doc.getStorageObjectKey());
    }

    @Transactional(readOnly = true)
    public List<EvidenceResponse> getEvidenceForLot(UUID lotId) {
        return evidenceRepository.findByLotId(lotId).stream()
                .map(EvidenceResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EvidenceResponse> getEvidenceForParty(UUID partyId) {
        return evidenceRepository.findByPartyId(partyId).stream()
                .map(EvidenceResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EvidenceResponse> listAllEvidence() {
        return evidenceRepository.findAll().stream()
                .map(EvidenceResponse::from)
                .toList();
    }
}
