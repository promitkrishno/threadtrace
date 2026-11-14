package com.cse444.threadtrace.lineage.infrastructure;

import com.cse444.threadtrace.lineage.domain.model.LotEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LotEventRepository extends JpaRepository<LotEventEntity, UUID> {
    List<LotEventEntity> findByLotIdOrderBySequenceNoAsc(UUID lotId);
    Optional<LotEventEntity> findByLotIdAndSequenceNo(UUID lotId, Long sequenceNo);

    @Query("SELECT MAX(e.sequenceNo) FROM LotEventEntity e WHERE e.lotId = :lotId")
    Optional<Long> findMaxSequenceNoByLotId(@Param("lotId") UUID lotId);

    @Query("SELECT e FROM LotEventEntity e WHERE e.lotId = :lotId ORDER BY e.sequenceNo DESC LIMIT 1")
    Optional<LotEventEntity> findLatestEventByLotId(@Param("lotId") UUID lotId);
}
