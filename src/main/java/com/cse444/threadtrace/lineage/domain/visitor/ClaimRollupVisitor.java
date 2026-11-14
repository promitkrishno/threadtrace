package com.cse444.threadtrace.lineage.domain.visitor;

import com.cse444.threadtrace.lineage.domain.composite.CompositeTransformedLot;
import com.cse444.threadtrace.lineage.domain.composite.LeafSourceLot;
import com.cse444.threadtrace.lineage.domain.composite.LineageNode;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Visitor Pattern: Computes verified recycled and organic content percentages
 * across a lineage DAG, flagging orphan lots and invalid claims.
 */
public class ClaimRollupVisitor implements LineageVisitor {

    private final Map<UUID, BigDecimal> nodeVerifiedRecycled = new HashMap<>();
    private final Map<UUID, BigDecimal> nodeVerifiedOrganic = new HashMap<>();
    private final List<UUID> orphanLotIds = new ArrayList<>();
    private final List<String> violations = new ArrayList<>();

    private UUID rootLotId;
    private BigDecimal rootClaimedRecycled = BigDecimal.ZERO;
    private BigDecimal rootClaimedOrganic = BigDecimal.ZERO;

    @Override
    public void visitLeaf(LeafSourceLot leaf) {
        if (leaf == null) return;
        if (rootLotId == null) {
            rootLotId = leaf.getLotId();
            rootClaimedRecycled = leaf.getClaimedRecycledPct();
            rootClaimedOrganic = leaf.getClaimedOrganicPct();
        }

        boolean isOrphan = leaf.isOrphan() || !leaf.hasCertificate();
        BigDecimal verifiedRecycled;
        BigDecimal verifiedOrganic;

        if (isOrphan) {
            orphanLotIds.add(leaf.getLotId());
            violations.add("Source lot " + leaf.getLotNumber() + " is an orphan or missing valid certificate.");
            verifiedRecycled = BigDecimal.ZERO;
            verifiedOrganic = BigDecimal.ZERO;
        } else {
            verifiedRecycled = leaf.getCertifiedRecycledPct();
            verifiedOrganic = leaf.getCertifiedOrganicPct();
        }

        // Validate claims on leaf
        if (leaf.getClaimedRecycledPct().compareTo(verifiedRecycled) > 0) {
            violations.add("Lot " + leaf.getLotNumber() + " claims " + leaf.getClaimedRecycledPct()
                    + "% recycled content, exceeding verified certified input of " + verifiedRecycled + "%.");
        }
        if (leaf.getClaimedOrganicPct().compareTo(verifiedOrganic) > 0) {
            violations.add("Lot " + leaf.getLotNumber() + " claims " + leaf.getClaimedOrganicPct()
                    + "% organic content, exceeding verified certified input of " + verifiedOrganic + "%.");
        }

        nodeVerifiedRecycled.put(leaf.getLotId(), verifiedRecycled);
        nodeVerifiedOrganic.put(leaf.getLotId(), verifiedOrganic);
    }

    @Override
    public void visitComposite(CompositeTransformedLot composite) {
        if (composite == null) return;
        if (rootLotId == null) {
            rootLotId = composite.getLotId();
            rootClaimedRecycled = composite.getClaimedRecycledPct();
            rootClaimedOrganic = composite.getClaimedOrganicPct();
        }

        if (nodeVerifiedRecycled.containsKey(composite.getLotId())) {
            return;
        }

        boolean isOrphan = composite.isOrphan() || composite.getParents().isEmpty();
        if (isOrphan) {
            orphanLotIds.add(composite.getLotId());
            violations.add("Lot " + composite.getLotNumber() + " is an orphan lot lacking parent lineage.");
        }

        BigDecimal totalParentQty = BigDecimal.ZERO;
        BigDecimal weightedRecycled = BigDecimal.ZERO;
        BigDecimal weightedOrganic = BigDecimal.ZERO;

        for (LineageNode parent : composite.getParents()) {
            if (!nodeVerifiedRecycled.containsKey(parent.getLotId())) {
                parent.accept(this);
            }

            BigDecimal pRecycled = nodeVerifiedRecycled.getOrDefault(parent.getLotId(), BigDecimal.ZERO);
            BigDecimal pOrganic = nodeVerifiedOrganic.getOrDefault(parent.getLotId(), BigDecimal.ZERO);
            BigDecimal pQty = parent.getQuantity() != null ? parent.getQuantity() : BigDecimal.ONE;

            totalParentQty = totalParentQty.add(pQty);
            weightedRecycled = weightedRecycled.add(pQty.multiply(pRecycled));
            weightedOrganic = weightedOrganic.add(pQty.multiply(pOrganic));
        }

        BigDecimal verifiedRecycled = BigDecimal.ZERO;
        BigDecimal verifiedOrganic = BigDecimal.ZERO;

        if (totalParentQty.compareTo(BigDecimal.ZERO) > 0) {
            verifiedRecycled = weightedRecycled.divide(totalParentQty, 2, RoundingMode.HALF_UP);
            verifiedOrganic = weightedOrganic.divide(totalParentQty, 2, RoundingMode.HALF_UP);
        }

        // If composite itself has direct certifications that are lower or bound it
        if (composite.getCertifiedRecycledPct().compareTo(BigDecimal.ZERO) > 0) {
            verifiedRecycled = verifiedRecycled.min(composite.getCertifiedRecycledPct());
        }
        if (composite.getCertifiedOrganicPct().compareTo(BigDecimal.ZERO) > 0) {
            verifiedOrganic = verifiedOrganic.min(composite.getCertifiedOrganicPct());
        }

        if (composite.getClaimedRecycledPct().compareTo(verifiedRecycled) > 0) {
            violations.add("Lot " + composite.getLotNumber() + " claims " + composite.getClaimedRecycledPct()
                    + "% recycled content, exceeding verified certified input of " + verifiedRecycled + "%.");
        }
        if (composite.getClaimedOrganicPct().compareTo(verifiedOrganic) > 0) {
            violations.add("Lot " + composite.getLotNumber() + " claims " + composite.getClaimedOrganicPct()
                    + "% organic content, exceeding verified certified input of " + verifiedOrganic + "%.");
        }

        nodeVerifiedRecycled.put(composite.getLotId(), verifiedRecycled);
        nodeVerifiedOrganic.put(composite.getLotId(), verifiedOrganic);
    }

    public ClaimRollupResult getResult() {
        BigDecimal finalVerifiedRecycled = rootLotId != null
                ? nodeVerifiedRecycled.getOrDefault(rootLotId, BigDecimal.ZERO)
                : BigDecimal.ZERO;
        BigDecimal finalVerifiedOrganic = rootLotId != null
                ? nodeVerifiedOrganic.getOrDefault(rootLotId, BigDecimal.ZERO)
                : BigDecimal.ZERO;

        boolean hasOrphan = !orphanLotIds.isEmpty();
        boolean claimsValid = violations.isEmpty() && !hasOrphan;

        return new ClaimRollupResult(
                finalVerifiedRecycled,
                finalVerifiedOrganic,
                rootClaimedRecycled,
                rootClaimedOrganic,
                hasOrphan,
                claimsValid,
                violations,
                orphanLotIds
        );
    }
}
