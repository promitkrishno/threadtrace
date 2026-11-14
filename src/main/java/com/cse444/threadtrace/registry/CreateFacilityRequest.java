package com.cse444.threadtrace.registry;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateFacilityRequest(
        @NotNull(message = "Party ID is required")
        UUID partyId,

        @NotNull(message = "Facility type is required")
        FacilityType facilityType,

        UUID parentId,

        @NotBlank(message = "Facility name is required")
        @Size(max = 255)
        String name,

        @NotBlank(message = "Location city is required")
        @Size(max = 100)
        String locationCity
) {
}
