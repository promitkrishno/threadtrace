package com.cse444.threadtrace.passport.proxy;

import com.cse444.threadtrace.passport.DigitalPassportService;
import com.cse444.threadtrace.passport.projection.AuditorPassportView;
import com.cse444.threadtrace.passport.projection.BrandPassportView;
import com.cse444.threadtrace.passport.projection.PublicPassportView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Proxy Pattern: Audience Security Proxy for Digital Product Passports (FR8, QA3).
 * Intercepts requests, verifies caller credentials, enforces per-brand tenant isolation
 * (refusing cross-brand access with HTTP 403), and ensures zero confidential fields leak.
 */
@Component
public class AudiencePassportProxy implements PassportAccessPort {

    private static final Logger log = LoggerFactory.getLogger(AudiencePassportProxy.class);

    private final DigitalPassportService passportService;

    public AudiencePassportProxy(DigitalPassportService passportService) {
        this.passportService = passportService;
    }

    @Override
    public PublicPassportView getPublicPassport(String qrToken, Integer version) {
        // Public endpoint: open to all, strictly public projection (no confidential fields)
        return passportService.getPublicPassport(qrToken, version);
    }

    @Override
    public BrandPassportView getBrandPassport(String qrToken, Integer version, String authenticatedBrandIdentity) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String caller = (authenticatedBrandIdentity != null && !authenticatedBrandIdentity.isBlank())
                ? authenticatedBrandIdentity
                : (auth != null ? auth.getName() : "anonymous");

        boolean isAuditor = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_AUDITOR".equals(a.getAuthority()));

        BrandPassportView brandPassport = passportService.getBrandPassport(qrToken, version);

        // Per-Brand Tenant Isolation Check
        if (!isAuditor) {
            boolean matchesCode = brandPassport.brandExternalCode().equalsIgnoreCase(caller);
            boolean matchesName = brandPassport.publicPassport().brandName().equalsIgnoreCase(caller);
            boolean matchesId = brandPassport.brandId().toString().equalsIgnoreCase(caller);

            if (!matchesCode && !matchesName && !matchesId) {
                log.warn("Access Denied: Caller '{}' attempted cross-brand access to passport for brand '{}' ({})",
                        caller, brandPassport.publicPassport().brandName(), brandPassport.brandExternalCode());
                throw new AccessDeniedException("Cross-brand passport access is forbidden. Tenant isolation violation.");
            }
        }

        return brandPassport;
    }

    @Override
    public AuditorPassportView getAuditorPassport(String qrToken, Integer version) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuditor = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_AUDITOR".equals(a.getAuthority()));

        if (!isAuditor) {
            log.warn("Access Denied: Caller '{}' attempted to view auditor confidential passport data without ROLE_AUDITOR",
                    auth != null ? auth.getName() : "anonymous");
            throw new AccessDeniedException("Access Denied: ROLE_AUDITOR authority is required to view confidential auditor passport data.");
        }

        return passportService.getAuditorPassport(qrToken, version);
    }
}
