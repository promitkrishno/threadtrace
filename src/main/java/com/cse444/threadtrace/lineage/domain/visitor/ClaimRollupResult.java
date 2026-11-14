package com.cse444.threadtrace.lineage.domain.visitor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ClaimRollupResult(
        BigDecimal verifiedRecycledPct,
        BigDecimal verifiedOrganicPct,
        BigDecimal claimedRecycledPct,
        BigDecimal claimedOrganicPct,
        boolean hasOrphanLot,
        boolean claimsValid,
        List<String> violations,
        List<UUID> orphanLotIds
) {
    public ClaimRollupResult {
        verifiedRecycledPct = verifiedRecycledPct != null ? verifiedRecycledPct : BigDecimal.ZERO;
        verifiedOrganicPct = verifiedOrganicPct != null ? verifiedOrganicPct : BigDecimal.ZERO;
        claimedRecycledPct = claimedRecycledPct != null ? claimedRecycledPct : BigDecimal.ZERO;
        claimedOrganicPct = claimedOrganicPct != null ? claimedOrganicPct : BigDecimal.ZERO;
        violations = violations != null ? List.copyOf(violations) : List.of();
        orphanLotIds = orphanLotIds != null ? List.copyOf(orphanLotIds) : List.of();
    }
}
