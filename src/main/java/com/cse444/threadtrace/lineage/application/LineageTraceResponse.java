package com.cse444.threadtrace.lineage.application;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record LineageTraceResponse(
        UUID originLotId,
        String originLotNumber,
        String direction,
        int totalNodes,
        int maxDepth,
        List<LineageTraceNode> tracePath,
        Instant tracedAt
) {
}
