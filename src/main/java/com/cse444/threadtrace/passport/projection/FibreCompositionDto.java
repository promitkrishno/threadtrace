package com.cse444.threadtrace.passport.projection;

import java.math.BigDecimal;

public record FibreCompositionDto(
        String materialName,
        BigDecimal percentage,
        boolean isRecycled,
        boolean isOrganic
) {}
