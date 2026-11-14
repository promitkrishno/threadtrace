package com.cse444.threadtrace.evidence.observer;

import com.cse444.threadtrace.lineage.domain.model.LotStatus;
import com.cse444.threadtrace.lineage.infrastructure.LotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Observer Pattern Subscriber: Listens to certificate expiry events and
 * automatically invalidates certified claims or quarantines affected lots.
 */
@Component
public class EvidenceEventListener {

    private static final Logger log = LoggerFactory.getLogger(EvidenceEventListener.class);

    private final LotRepository lotRepository;
    private final List<EvidenceExpiredEvent> receivedExpiredEvents = new ArrayList<>();
    private final List<EvidenceExpiringSoonEvent> receivedExpiringSoonEvents = new ArrayList<>();

    public EvidenceEventListener(LotRepository lotRepository) {
        this.lotRepository = lotRepository;
    }

    @EventListener
    @Transactional
    public void onEvidenceExpired(EvidenceExpiredEvent event) {
        log.warn("Handling expired certificate: id={}, type={}, lotId={}, partyId={}",
                event.evidenceId(), event.certificateType(), event.lotId(), event.partyId());
        receivedExpiredEvents.add(event);

        if (event.lotId() != null) {
            lotRepository.findById(event.lotId()).ifPresent(lot -> {
                log.warn("Quarantining lot {} due to expired certificate {}", lot.getLotNumber(), event.certificateType());
                lot.setStatus(LotStatus.QUARANTINED);
                // Invalidate certified percentages
                lot.setCertifiedRecycledPct(BigDecimal.ZERO);
                lot.setCertifiedOrganicPct(BigDecimal.ZERO);
                lotRepository.save(lot);
            });
        }
    }

    @EventListener
    public void onEvidenceExpiringSoon(EvidenceExpiringSoonEvent event) {
        log.info("Handling expiring soon certificate: id={}, type={}, validUntil={}",
                event.evidenceId(), event.certificateType(), event.validUntil());
        receivedExpiringSoonEvents.add(event);
    }

    public List<EvidenceExpiredEvent> getReceivedExpiredEvents() {
        return List.copyOf(receivedExpiredEvents);
    }

    public List<EvidenceExpiringSoonEvent> getReceivedExpiringSoonEvents() {
        return List.copyOf(receivedExpiringSoonEvents);
    }

    public void clearHistory() {
        receivedExpiredEvents.clear();
        receivedExpiringSoonEvents.clear();
    }
}
