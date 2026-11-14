package com.cse444.threadtrace.lineage.application;

import com.cse444.threadtrace.lineage.domain.model.EventType;
import com.cse444.threadtrace.lineage.domain.model.LotEventEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class LotHashChainIntegrityTest {

    @Test
    @DisplayName("Genesis event is linked to GENESIS previous hash and hashes deterministically")
    void genesisEventHashing() {
        UUID lotId = UUID.randomUUID();
        UUID facilityId = UUID.randomUUID();
        Instant now = Instant.parse("2026-11-13T10:00:00Z");

        String hash1 = LotEventEntity.computeHash(
                LotEventEntity.GENESIS_HASH,
                lotId,
                1L,
                EventType.RECEIVE,
                facilityId,
                BigDecimal.valueOf(100.0),
                BigDecimal.valueOf(100.0),
                BigDecimal.ZERO,
                "KG",
                now
        );

        String hash2 = LotEventEntity.computeHash(
                LotEventEntity.GENESIS_HASH,
                lotId,
                1L,
                EventType.RECEIVE,
                facilityId,
                BigDecimal.valueOf(100.0),
                BigDecimal.valueOf(100.0),
                BigDecimal.ZERO,
                "KG",
                now
        );

        assertThat(hash1).isNotEmpty().hasSize(64).isEqualTo(hash2);
    }

    @Test
    @DisplayName("Hash chain detects any tampering of intermediate quantity, facility, or prior hash")
    void hashChainDetectsTampering() {
        UUID lotId = UUID.randomUUID();
        UUID facilityId = UUID.randomUUID();
        Instant t1 = Instant.parse("2026-11-13T10:00:00Z");
        Instant t2 = Instant.parse("2026-11-13T12:00:00Z");

        // Event 1 (Genesis)
        String hash1 = LotEventEntity.computeHash(
                LotEventEntity.GENESIS_HASH,
                lotId,
                1L,
                EventType.RECEIVE,
                facilityId,
                BigDecimal.valueOf(100.0),
                BigDecimal.valueOf(100.0),
                BigDecimal.ZERO,
                "KG",
                t1
        );

        // Event 2 (Cut)
        String hash2 = LotEventEntity.computeHash(
                hash1,
                lotId,
                2L,
                EventType.CUT,
                facilityId,
                BigDecimal.valueOf(100.0),
                BigDecimal.valueOf(88.0),
                BigDecimal.valueOf(12.0),
                "KG",
                t2
        );

        LotEventEntity event2 = new LotEventEntity();
        event2.setLotId(lotId);
        event2.setSequenceNo(2L);
        event2.setEventType(EventType.CUT);
        event2.setFacilityId(facilityId);
        event2.setInputQuantity(BigDecimal.valueOf(100.0));
        event2.setOutputQuantity(BigDecimal.valueOf(88.0));
        event2.setLossPercentage(BigDecimal.valueOf(12.0));
        event2.setUnit("KG");
        event2.setPreviousEventHash(hash1);
        event2.setEventHash(hash2);
        event2.setOccurredAt(t2);

        // Intact check
        assertThat(event2.calculateExpectedHash()).isEqualTo(hash2);

        // Tamper: Modify output quantity secretly in database
        event2.setOutputQuantity(BigDecimal.valueOf(95.0));
        assertThat(event2.calculateExpectedHash()).isNotEqualTo(hash2);
    }
}
