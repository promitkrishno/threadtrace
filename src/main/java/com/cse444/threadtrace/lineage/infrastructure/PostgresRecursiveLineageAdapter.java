package com.cse444.threadtrace.lineage.infrastructure;

import com.cse444.threadtrace.common.error.DomainException;
import com.cse444.threadtrace.lineage.application.LineageTraceNode;
import com.cse444.threadtrace.lineage.application.LineageTracePort;
import com.cse444.threadtrace.lineage.application.LineageTraceResponse;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
public class PostgresRecursiveLineageAdapter implements LineageTracePort {

    private final JdbcClient jdbcClient;

    public PostgresRecursiveLineageAdapter(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public LineageTraceResponse traceBackward(UUID lotId) {
        String checkSql = "SELECT lot_number FROM lots WHERE id = :lotId";
        String originLotNumber = jdbcClient.sql(checkSql)
                .param("lotId", lotId)
                .query(String.class)
                .optional()
                .orElseThrow(() -> new DomainException("LOT_NOT_FOUND", "Lot not found with id: " + lotId));

        String sql = """
                WITH RECURSIVE backward_trace(
                    lot_id,
                    lot_number,
                    status,
                    current_quantity,
                    unit,
                    style_code,
                    shipment_code,
                    facility_name,
                    facility_city,
                    party_name,
                    party_type,
                    country_code,
                    depth,
                    path_ids,
                    path_string
                ) AS (
                    SELECT
                        l.id,
                        l.lot_number,
                        CAST(l.status AS VARCHAR(50)),
                        l.current_quantity,
                        CAST(l.unit AS VARCHAR(50)),
                        l.style_code,
                        l.shipment_code,
                        f.name,
                        f.location_city,
                        p.name,
                        CAST(p.party_type AS VARCHAR(50)),
                        p.country_code,
                        0,
                        '/' || CAST(l.id AS VARCHAR(100)) || '/',
                        CAST(l.lot_number AS VARCHAR(2000))
                    FROM lots l
                    JOIN facilities f ON l.current_facility_id = f.id
                    JOIN parties p ON f.party_id = p.id
                    WHERE l.id = :lotId

                    UNION ALL

                    SELECT
                        parent_lot.id,
                        parent_lot.lot_number,
                        CAST(parent_lot.status AS VARCHAR(50)),
                        parent_lot.current_quantity,
                        CAST(parent_lot.unit AS VARCHAR(50)),
                        parent_lot.style_code,
                        parent_lot.shipment_code,
                        f.name,
                        f.location_city,
                        p.name,
                        CAST(p.party_type AS VARCHAR(50)),
                        p.country_code,
                        bt.depth + 1,
                        bt.path_ids || CAST(parent_lot.id AS VARCHAR(100)) || '/',
                        CAST(bt.path_string || ' <- ' || parent_lot.lot_number AS VARCHAR(2000))
                    FROM backward_trace bt
                    JOIN lot_lineage_edges e ON bt.lot_id = e.child_lot_id
                    JOIN lots parent_lot ON e.parent_lot_id = parent_lot.id
                    JOIN facilities f ON parent_lot.current_facility_id = f.id
                    JOIN parties p ON f.party_id = p.id
                    WHERE bt.depth < 50
                    AND bt.path_ids NOT LIKE '%/' || CAST(parent_lot.id AS VARCHAR(100)) || '/%'
                )
                SELECT
                    lot_id,
                    lot_number,
                    status,
                    current_quantity,
                    unit,
                    style_code,
                    shipment_code,
                    facility_name,
                    facility_city,
                    party_name,
                    party_type,
                    country_code,
                    depth,
                    path_string
                FROM backward_trace
                ORDER BY depth ASC
                """;

        List<LineageTraceNode> nodes = jdbcClient.sql(sql)
                .param("lotId", lotId)
                .query((rs, rowNum) -> new LineageTraceNode(
                        UUID.fromString(rs.getString("lot_id")),
                        rs.getString("lot_number"),
                        rs.getString("status"),
                        rs.getBigDecimal("current_quantity"),
                        rs.getString("unit"),
                        rs.getString("style_code"),
                        rs.getString("shipment_code"),
                        rs.getString("facility_name"),
                        rs.getString("facility_city"),
                        rs.getString("party_name"),
                        rs.getString("party_type"),
                        rs.getString("country_code"),
                        rs.getInt("depth"),
                        rs.getString("path_string")
                ))
                .list();

        int maxDepth = nodes.stream().mapToInt(LineageTraceNode::depth).max().orElse(0);

        return new LineageTraceResponse(
                lotId,
                originLotNumber,
                "BACKWARD",
                nodes.size(),
                maxDepth,
                nodes,
                Instant.now()
        );
    }

    @Override
    public LineageTraceResponse traceForward(UUID lotId) {
        String checkSql = "SELECT lot_number FROM lots WHERE id = :lotId";
        String originLotNumber = jdbcClient.sql(checkSql)
                .param("lotId", lotId)
                .query(String.class)
                .optional()
                .orElseThrow(() -> new DomainException("LOT_NOT_FOUND", "Lot not found with id: " + lotId));

        String sql = """
                WITH RECURSIVE forward_trace(
                    lot_id,
                    lot_number,
                    status,
                    current_quantity,
                    unit,
                    style_code,
                    shipment_code,
                    facility_name,
                    facility_city,
                    party_name,
                    party_type,
                    country_code,
                    depth,
                    path_ids,
                    path_string
                ) AS (
                    SELECT
                        l.id,
                        l.lot_number,
                        CAST(l.status AS VARCHAR(50)),
                        l.current_quantity,
                        CAST(l.unit AS VARCHAR(50)),
                        l.style_code,
                        l.shipment_code,
                        f.name,
                        f.location_city,
                        p.name,
                        CAST(p.party_type AS VARCHAR(50)),
                        p.country_code,
                        0,
                        '/' || CAST(l.id AS VARCHAR(100)) || '/',
                        CAST(l.lot_number AS VARCHAR(2000))
                    FROM lots l
                    JOIN facilities f ON l.current_facility_id = f.id
                    JOIN parties p ON f.party_id = p.id
                    WHERE l.id = :lotId

                    UNION ALL

                    SELECT
                        child_lot.id,
                        child_lot.lot_number,
                        CAST(child_lot.status AS VARCHAR(50)),
                        child_lot.current_quantity,
                        CAST(child_lot.unit AS VARCHAR(50)),
                        child_lot.style_code,
                        child_lot.shipment_code,
                        f.name,
                        f.location_city,
                        p.name,
                        CAST(p.party_type AS VARCHAR(50)),
                        p.country_code,
                        ft.depth + 1,
                        ft.path_ids || CAST(child_lot.id AS VARCHAR(100)) || '/',
                        CAST(ft.path_string || ' -> ' || child_lot.lot_number AS VARCHAR(2000))
                    FROM forward_trace ft
                    JOIN lot_lineage_edges e ON ft.lot_id = e.parent_lot_id
                    JOIN lots child_lot ON e.child_lot_id = child_lot.id
                    JOIN facilities f ON child_lot.current_facility_id = f.id
                    JOIN parties p ON f.party_id = p.id
                    WHERE ft.depth < 50
                    AND ft.path_ids NOT LIKE '%/' || CAST(child_lot.id AS VARCHAR(100)) || '/%'
                )
                SELECT
                    lot_id,
                    lot_number,
                    status,
                    current_quantity,
                    unit,
                    style_code,
                    shipment_code,
                    facility_name,
                    facility_city,
                    party_name,
                    party_type,
                    country_code,
                    depth,
                    path_string
                FROM forward_trace
                ORDER BY depth ASC
                """;

        List<LineageTraceNode> nodes = jdbcClient.sql(sql)
                .param("lotId", lotId)
                .query((rs, rowNum) -> new LineageTraceNode(
                        UUID.fromString(rs.getString("lot_id")),
                        rs.getString("lot_number"),
                        rs.getString("status"),
                        rs.getBigDecimal("current_quantity"),
                        rs.getString("unit"),
                        rs.getString("style_code"),
                        rs.getString("shipment_code"),
                        rs.getString("facility_name"),
                        rs.getString("facility_city"),
                        rs.getString("party_name"),
                        rs.getString("party_type"),
                        rs.getString("country_code"),
                        rs.getInt("depth"),
                        rs.getString("path_string")
                ))
                .list();

        int maxDepth = nodes.stream().mapToInt(LineageTraceNode::depth).max().orElse(0);

        return new LineageTraceResponse(
                lotId,
                originLotNumber,
                "FORWARD",
                nodes.size(),
                maxDepth,
                nodes,
                Instant.now()
        );
    }
}
