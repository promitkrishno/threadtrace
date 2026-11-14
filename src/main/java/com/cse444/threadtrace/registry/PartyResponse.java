package com.cse444.threadtrace.registry;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PartyResponse(
        UUID id,
        PartyType partyType,
        String name,
        String externalCode,
        String countryCode,
        String exactAddress,
        Integer workerCount,
        BigDecimal unitPriceBdt,
        Instant createdAt
) {
    public static PartyResponse fromEntity(PartyEntity entity) {
        return new PartyResponse(
                entity.getId(),
                entity.getPartyType(),
                entity.getName(),
                entity.getExternalCode(),
                entity.getCountryCode(),
                entity.getExactAddress(),
                entity.getWorkerCount(),
                entity.getUnitPriceBdt(),
                entity.getCreatedAt()
        );
    }
}
