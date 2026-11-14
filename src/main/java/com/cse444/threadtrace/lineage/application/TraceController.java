package com.cse444.threadtrace.lineage.application;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/trace")
@Tag(name = "Recursive Lineage Trace", description = "Backward & forward recursive DAG supply-chain trace (FR3, QA1)")
public class TraceController {

    private final LineageTracePort lineageTracePort;

    public TraceController(LineageTracePort lineageTracePort) {
        this.lineageTracePort = lineageTracePort;
    }

    @GetMapping("/backward/{lotId}")
    @Operation(summary = "Trace backward from a carton/style to all source lots and tier suppliers with full DAG path (FR3)")
    public ResponseEntity<LineageTraceResponse> traceBackward(@PathVariable UUID lotId) {
        return ResponseEntity.ok(lineageTracePort.traceBackward(lotId));
    }

    @GetMapping("/forward/{lotId}")
    @Operation(summary = "Trace forward from a raw/fabric lot to all descendant cartons, shipments, and buyers (FR3)")
    public ResponseEntity<LineageTraceResponse> traceForward(@PathVariable UUID lotId) {
        return ResponseEntity.ok(lineageTracePort.traceForward(lotId));
    }

    @GetMapping("/rollup/{lotId}")
    @Operation(summary = "Execute visitor roll-up for footprint and claim verification on a lot's lineage DAG (FR4, FR6)")
    public ResponseEntity<LineageRollupResponse> rollup(
            @PathVariable UUID lotId,
            @RequestParam(required = false, defaultValue = "MASS") String strategy
    ) {
        return ResponseEntity.ok(lineageTracePort.computeRollup(lotId, strategy));
    }
}
