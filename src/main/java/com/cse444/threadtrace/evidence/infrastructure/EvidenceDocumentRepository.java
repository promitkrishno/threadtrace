package com.cse444.threadtrace.evidence.infrastructure;

import com.cse444.threadtrace.evidence.domain.EvidenceDocumentEntity;
import com.cse444.threadtrace.evidence.domain.EvidenceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface EvidenceDocumentRepository extends JpaRepository<EvidenceDocumentEntity, UUID> {

    List<EvidenceDocumentEntity> findByLotId(UUID lotId);

    List<EvidenceDocumentEntity> findByPartyId(UUID partyId);

    List<EvidenceDocumentEntity> findByValidUntilLessThanEqualAndStatusIn(
            LocalDate date,
            Collection<EvidenceStatus> statuses
    );

    List<EvidenceDocumentEntity> findByValidUntilBetweenAndStatusIn(
            LocalDate start,
            LocalDate end,
            Collection<EvidenceStatus> statuses
    );
}
