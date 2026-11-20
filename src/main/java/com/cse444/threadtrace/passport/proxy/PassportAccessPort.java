package com.cse444.threadtrace.passport.proxy;

import com.cse444.threadtrace.passport.projection.AuditorPassportView;
import com.cse444.threadtrace.passport.projection.BrandPassportView;
import com.cse444.threadtrace.passport.projection.PublicPassportView;

public interface PassportAccessPort {
    PublicPassportView getPublicPassport(String qrToken, Integer version);
    BrandPassportView getBrandPassport(String qrToken, Integer version, String authenticatedBrandIdentity);
    AuditorPassportView getAuditorPassport(String qrToken, Integer version);
}
