package com.cse444.threadtrace.evidence.observer;

import com.cse444.threadtrace.evidence.domain.EvidenceDocumentEntity;
import com.cse444.threadtrace.evidence.domain.EvidenceStatus;
import com.cse444.threadtrace.evidence.infrastructure.EvidenceDocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Observer Pattern Publisher: Periodically scans for expiring and expired evidence documents
 * and dispatches domain events to alert downstream subscribers.
 */
@Component
public class EvidenceExpiryScheduler {

    private static final Logger log = LoggerFactory.getLogger(EvidenceExpiryScheduler.class);

    private final EvidenceDocumentRepository repository;
    private final ApplicationEventPublisher eventPublisher;

    public EvidenceExpiryScheduler(
            EvidenceDocumentRepository repository,
            ApplicationEventPublisher eventPublisher
    ) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Checks certificates and publishes expiry events.
     * Can be invoked by Spring scheduled task or triggered on-demand.
     */
    @Transactional
    public ExpiryCheckResult checkAndPublishExpiryEvents() {
        LocalDate today = LocalDate.now();
        LocalDate warningThreshold = today.plusDays(30);

        // 1. Find certificates that have expired (validUntil <= today) and are still ACTIVE or EXPIRING_SOON
        List<EvidenceDocumentEntity> expiredDocs = repository.findByValidUntilLessThanEqualAndStatusIn(
                today,
                List.of(EvidenceStatus.ACTIVE, EvidenceStatus.EXPIRING_SOON)
        );

        for (EvidenceDocumentEntity doc : expiredDocs) {
            doc.setStatus(EvidenceStatus.EXPIRED);
            repository.save(doc);
            eventPublisher.publishEvent(new EvidenceExpiredEvent(
                    doc.getId(),
                    doc.getCertificateType(),
                    doc.getIssuerName(),
                    doc.getValidUntil(),
                    doc.getLotId(),
                    doc.getPartyId()
            ));
            log.warn("Published EvidenceExpiredEvent for certificate {} id {}", doc.getCertificateType(), doc.getId());
        }

        // 2. Find certificates expiring within 30 days that are currently ACTIVE
        List<EvidenceDocumentEntity> expiringSoonDocs = repository.findByValidUntilBetweenAndStatusIn(
                today,
                warningThreshold,
                List.of(EvidenceStatus.ACTIVE)
        );

        for (EvidenceDocumentEntity doc : expiringSoonDocs) {
            doc.setStatus(EvidenceStatus.EXPIRING_SOON);
            repository.save(doc);
            eventPublisher.publishEvent(new EvidenceExpiringSoonEvent(
                    doc.getId(),
                    doc.getCertificateType(),
                    doc.getIssuerName(),
                    doc.getValidUntil(),
                    doc.getLotId(),
                    doc.getPartyId()
            ));
            log.info("Published EvidenceExpiringSoonEvent for certificate {} id {}", doc.getCertificateType(), doc.getId());
        }

        return new ExpiryCheckResult(expiredDocs.size(), expiringSoonDocs.size());
    }

    public record ExpiryCheckResult(int expiredCount, int expiringSoonCount) {}
}
