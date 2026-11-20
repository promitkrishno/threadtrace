package com.cse444.threadtrace.bulkimport.job;

public record RejectedRowDetail(
        int rowNumber,
        String rawContent,
        String reason
) {}
