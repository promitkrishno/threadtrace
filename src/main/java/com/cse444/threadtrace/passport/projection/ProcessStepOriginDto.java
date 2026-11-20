package com.cse444.threadtrace.passport.projection;

public record ProcessStepOriginDto(
        String processStep,
        String facilityName,
        String locationCity,
        String countryCode
) {}
