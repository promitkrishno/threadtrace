package com.cse444.threadtrace.passport.projection;

import java.math.BigDecimal;

public record CertificationBadgeDto(
        String certificateType,
        String issuerName,
        String claimType,
        BigDecimal certifiedPercentage,
        String validUntil,
        boolean valid
) {}
