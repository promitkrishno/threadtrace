package com.cse444.threadtrace.lineage.application;

import java.util.UUID;

public interface LineageTracePort {
    LineageTraceResponse traceBackward(UUID lotId);
    LineageTraceResponse traceForward(UUID lotId);
    LineageRollupResponse computeRollup(UUID lotId, String strategyName);
}
