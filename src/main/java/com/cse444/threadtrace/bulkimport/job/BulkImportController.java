package com.cse444.threadtrace.bulkimport.job;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/imports")
@Tag(name = "Bulk Import", description = "Factory CSV Cutting Reports and Excel Packing Lists bulk import (FR9)")
public class BulkImportController {

    private final BulkImportService bulkImportService;

    public BulkImportController(BulkImportService bulkImportService) {
        this.bulkImportService = bulkImportService;
    }

    @PostMapping(value = "/{type}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Import factory cutting report or packing list with structured rejection report")
    public ResponseEntity<ImportExecutionReport> importFile(
            @PathVariable String type,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) UUID facilityId,
            @RequestParam(required = false) UUID brandId
    ) throws IOException {
        ImportExecutionReport report = bulkImportService.executeImport(
                type,
                file.getInputStream(),
                file.getOriginalFilename(),
                facilityId,
                brandId
        );

        HttpStatus status = report.rejectedRows() > 0 && report.successfulRows() > 0
                ? HttpStatus.MULTI_STATUS
                : HttpStatus.OK;

        return ResponseEntity.status(status).body(report);
    }
}
