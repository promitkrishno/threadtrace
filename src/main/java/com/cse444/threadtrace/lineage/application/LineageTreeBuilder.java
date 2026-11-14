package com.cse444.threadtrace.lineage.application;

import com.cse444.threadtrace.common.error.DomainException;
import com.cse444.threadtrace.lineage.domain.composite.CompositeTransformedLot;
import com.cse444.threadtrace.lineage.domain.composite.LeafSourceLot;
import com.cse444.threadtrace.lineage.domain.composite.LineageNode;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
public class LineageTreeBuilder {

    private final JdbcClient jdbcClient;

    public LineageTreeBuilder(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public LineageNode buildTree(UUID lotId) {
        return buildNodeRecursive(lotId, new HashSet<>());
    }

    private LineageNode buildNodeRecursive(UUID lotId, Set<UUID> visited) {
        if (visited.contains(lotId)) {
            return null;
        }
        visited.add(lotId);

        String lotSql = """
                SELECT l.id, l.lot_number, l.current_quantity, l.unit,
                       l.certified_recycled_pct, l.certified_organic_pct,
                       l.accumulated_water_l, l.accumulated_energy_kwh, l.accumulated_co2e_kg,
                       f.party_id
                FROM lots l
                JOIN facilities f ON l.current_facility_id = f.id
                WHERE l.id = :lotId
                """;

        var lotOpt = jdbcClient.sql(lotSql)
                .param("lotId", lotId)
                .query((rs, rowNum) -> new LotRow(
                        UUID.fromString(rs.getString("id")),
                        rs.getString("lot_number"),
                        rs.getBigDecimal("current_quantity"),
                        rs.getString("unit"),
                        rs.getBigDecimal("certified_recycled_pct"),
                        rs.getBigDecimal("certified_organic_pct"),
                        rs.getBigDecimal("accumulated_water_l"),
                        rs.getBigDecimal("accumulated_energy_kwh"),
                        rs.getBigDecimal("accumulated_co2e_kg"),
                        UUID.fromString(rs.getString("party_id"))
                ))
                .optional();

        if (lotOpt.isEmpty()) {
            throw new DomainException("LOT_NOT_FOUND", "Lot not found with id: " + lotId);
        }
        LotRow lot = lotOpt.get();

        String certSql = """
                SELECT COUNT(*) FROM evidence_documents
                WHERE (lot_id = :lotId OR party_id = :partyId)
                  AND status = 'ACTIVE'
                  AND valid_until >= CURRENT_DATE
                """;
        Integer certCount = jdbcClient.sql(certSql)
                .param("lotId", lotId)
                .param("partyId", lot.partyId())
                .query(Integer.class)
                .single();
        boolean hasCertificate = certCount != null && certCount > 0;

        String edgeSql = """
                SELECT parent_lot_id, input_quantity_used, input_unit, allocation_ratio
                FROM lot_lineage_edges
                WHERE child_lot_id = :lotId
                """;
        List<UUID> parentIds = jdbcClient.sql(edgeSql)
                .param("lotId", lotId)
                .query((rs, rowNum) -> UUID.fromString(rs.getString("parent_lot_id")))
                .list();

        if (parentIds.isEmpty()) {
            return new LeafSourceLot(
                    lot.id(),
                    lot.lotNumber(),
                    lot.currentQuantity(),
                    lot.unit(),
                    lot.accumulatedWaterL(),
                    lot.accumulatedEnergyKwh(),
                    lot.accumulatedCo2eKg()
            ).withCertifications(
                    lot.certifiedRecycledPct(),
                    lot.certifiedOrganicPct(),
                    hasCertificate
            ).withClaims(
                    lot.certifiedRecycledPct(),
                    lot.certifiedOrganicPct()
            );
        } else {
            CompositeTransformedLot composite = new CompositeTransformedLot(
                    lot.id(),
                    lot.lotNumber(),
                    lot.currentQuantity(),
                    lot.unit(),
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO
            ).withCertifications(
                    lot.certifiedRecycledPct(),
                    lot.certifiedOrganicPct(),
                    hasCertificate
            ).withClaims(
                    lot.certifiedRecycledPct(),
                    lot.certifiedOrganicPct()
            );

            for (UUID parentId : parentIds) {
                LineageNode parentNode = buildNodeRecursive(parentId, new HashSet<>(visited));
                if (parentNode != null) {
                    composite.addParent(parentNode);
                }
            }
            return composite;
        }
    }

    private record LotRow(
            UUID id,
            String lotNumber,
            BigDecimal currentQuantity,
            String unit,
            BigDecimal certifiedRecycledPct,
            BigDecimal certifiedOrganicPct,
            BigDecimal accumulatedWaterL,
            BigDecimal accumulatedEnergyKwh,
            BigDecimal accumulatedCo2eKg,
            UUID partyId
    ) {}
}
