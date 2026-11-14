package com.cse444.threadtrace.registry;

import java.time.Instant;
import java.util.UUID;

public record FacilityResponse(
        UUID id,
        UUID partyId,
        String partyName,
        FacilityType facilityType,
        UUID parentId,
        String name,
        String locationCity,
        Instant createdAt
) {
    public static FacilityResponse fromEntity(FacilityEntity entity) {
        return new FacilityResponse(
                entity.getId(),
                entity.getParty().getId(),
                entity.getParty().getName(),
                entity.getFacilityType(),
                entity.getParent() != null ? entity.getParent().getId() : null,
                entity.getName(),
                entity.getLocationCity(),
                entity.getCreatedAt()
        );
    }
}
