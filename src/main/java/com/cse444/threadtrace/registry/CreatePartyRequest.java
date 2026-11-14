package com.cse444.threadtrace.registry;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreatePartyRequest(
        @NotNull(message = "Party type is required")
        PartyType partyType,

        @NotBlank(message = "Party name is required")
        @Size(max = 255)
        String name,

        @NotBlank(message = "External code is required")
        @Size(max = 100)
        String externalCode,

        @NotBlank(message = "Country code is required (e.g. BD, VN, US)")
        @Size(min = 2, max = 10)
        String countryCode,

        @Size(max = 500)
        String exactAddress,

        @PositiveOrZero
        Integer workerCount,

        @PositiveOrZero
        BigDecimal unitPriceBdt
) {
}
