package com.cse444.threadtrace.lineage.application;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record LineageRollupResponse(
        UUID lotId,
        String lotNumber,
        String allocationStrategy,
        BigDecimal totalWaterLiters,
        BigDecimal totalEnergyKwh,
        BigDecimal totalCo2eKg,
        BigDecimal waterPerPiece,
        BigDecimal energyPerPiece,
        BigDecimal co2ePerPiece,
        BigDecimal verifiedRecycledPct,
        BigDecimal verifiedOrganicPct,
        BigDecimal claimedRecycledPct,
        BigDecimal claimedOrganicPct,
        boolean claimsValid,
        boolean hasOrphanLot,
        List<String> violations,
        List<UUID> orphanLotIds
) {}
