package com.cse444.threadtrace.bulkimport.job;

import java.time.Instant;
import java.util.List;

public record ImportExecutionReport(
        String importType,
        String fileName,
        int totalRows,
        int successfulRows,
        int rejectedRows,
        List<RejectedRowDetail> rejections,
        Instant completedAt
) {}
