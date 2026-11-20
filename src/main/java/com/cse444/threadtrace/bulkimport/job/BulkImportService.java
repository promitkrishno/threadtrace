package com.cse444.threadtrace.bulkimport.job;

import com.cse444.threadtrace.bulkimport.adapter.LotImportRowAdapter;
import com.cse444.threadtrace.common.error.DomainException;
import com.cse444.threadtrace.lineage.application.RecordEventCommand;
import com.cse444.threadtrace.lineage.application.RecordLotEventUseCase;
import org.apache.poi.ss.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

@Service
public class BulkImportService {

    private static final Logger log = LoggerFactory.getLogger(BulkImportService.class);

    private final List<LotImportRowAdapter> adapters;
    private final RecordLotEventUseCase recordLotEventUseCase;

    public BulkImportService(
            List<LotImportRowAdapter> adapters,
            RecordLotEventUseCase recordLotEventUseCase
    ) {
        this.adapters = adapters;
        this.recordLotEventUseCase = recordLotEventUseCase;
    }

    public ImportExecutionReport executeImport(
            String importType,
            InputStream inputStream,
            String originalFilename,
            UUID defaultFacilityId,
            UUID defaultBrandId
    ) {
        LotImportRowAdapter adapter = adapters.stream()
                .filter(a -> a.supports(importType))
                .findFirst()
                .orElseThrow(() -> new DomainException("UNSUPPORTED_IMPORT_TYPE",
                        "No adapter registered for import type: " + importType));

        boolean isExcel = originalFilename != null &&
                (originalFilename.endsWith(".xlsx") || originalFilename.endsWith(".xls"));

        List<Map<String, String>> rows;
        if (isExcel) {
            rows = parseExcel(inputStream);
        } else {
            rows = parseCsv(inputStream);
        }

        int totalRows = rows.size();
        int successCount = 0;
        List<RejectedRowDetail> rejections = new ArrayList<>();

        for (int i = 0; i < rows.size(); i++) {
            int rowNumber = i + 2; // Row 1 is header, data starts at row 2
            Map<String, String> rowData = rows.get(i);

            try {
                RecordEventCommand command = adapter.adapt(rowNumber, rowData, defaultFacilityId, defaultBrandId);
                recordLotEventUseCase.recordEvent(command);
                successCount++;
            } catch (Exception e) {
                log.warn("Row {} rejected in {} import: {}", rowNumber, importType, e.getMessage());
                rejections.add(new RejectedRowDetail(
                        rowNumber,
                        rowData.toString(),
                        e.getMessage()
                ));
            }
        }

        return new ImportExecutionReport(
                importType,
                originalFilename != null ? originalFilename : "import.data",
                totalRows,
                successCount,
                rejections.size(),
                List.copyOf(rejections),
                Instant.now()
        );
    }

    private List<Map<String, String>> parseCsv(InputStream inputStream) {
        List<Map<String, String>> result = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null || headerLine.isBlank()) {
                return result;
            }

            String delimiter = headerLine.contains(";") ? ";" : ",";
            String[] headers = parseCsvLine(headerLine, delimiter);

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] values = parseCsvLine(line, delimiter);
                Map<String, String> row = new LinkedHashMap<>();
                for (int i = 0; i < headers.length; i++) {
                    String h = headers[i].trim();
                    String v = i < values.length ? values[i].trim() : "";
                    row.put(h, v);
                }
                result.add(row);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to read CSV input: " + e.getMessage(), e);
        }
        return result;
    }

    private static String[] parseCsvLine(String line, String delimiter) {
        List<String> tokens = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '\"') {
                inQuotes = !inQuotes;
            } else if (!inQuotes && String.valueOf(c).equals(delimiter)) {
                tokens.add(sb.toString().trim());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        tokens.add(sb.toString().trim());
        return tokens.toArray(new String[0]);
    }

    private List<Map<String, String>> parseExcel(InputStream inputStream) {
        List<Map<String, String>> result = new ArrayList<>();
        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) return result;

            List<String> headers = new ArrayList<>();
            for (Cell cell : headerRow) {
                headers.add(getCellValueAsString(cell).trim());
            }

            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                Map<String, String> rowMap = new LinkedHashMap<>();
                boolean empty = true;
                for (int c = 0; c < headers.size(); c++) {
                    Cell cell = row.getCell(c, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    String val = getCellValueAsString(cell).trim();
                    if (!val.isEmpty()) empty = false;
                    rowMap.put(headers.get(c), val);
                }
                if (!empty) {
                    result.add(rowMap);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to read Excel input: " + e.getMessage(), e);
        }
        return result;
    }

    private static String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getLocalDateTimeCellValue().toLocalDate().toString();
                }
                double d = cell.getNumericCellValue();
                if (d == (long) d) {
                    yield String.valueOf((long) d);
                } else {
                    yield String.valueOf(d);
                }
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try {
                    yield String.valueOf(cell.getNumericCellValue());
                } catch (Exception ex) {
                    yield cell.getStringCellValue();
                }
            }
            default -> "";
        };
    }
}
