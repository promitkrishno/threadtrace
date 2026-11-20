package com.cse444.threadtrace.bulkimport.adapter;

import com.cse444.threadtrace.lineage.application.ChildLotOutput;
import com.cse444.threadtrace.lineage.application.RecordEventCommand;
import com.cse444.threadtrace.lineage.domain.model.EventType;
import com.cse444.threadtrace.lineage.domain.model.LotEntity;
import com.cse444.threadtrace.lineage.domain.model.Unit;
import com.cse444.threadtrace.lineage.infrastructure.LotRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Adapter Pattern: Converts CSV cutting report rows into standardized CUT event commands (FR9).
 */
@Component
public class CsvCuttingReportAdapter implements LotImportRowAdapter {

    private final LotRepository lotRepository;

    public CsvCuttingReportAdapter(LotRepository lotRepository) {
        this.lotRepository = lotRepository;
    }

    @Override
    public boolean supports(String importType) {
        if (importType == null) return false;
        String t = importType.trim().toLowerCase();
        return t.equals("cutting-report") || t.equals("cutting") || t.equals("cut-report");
    }

    @Override
    public RecordEventCommand adapt(
            int rowNumber,
            Map<String, String> rowFields,
            UUID defaultFacilityId,
            UUID defaultBrandId
    ) {
        String parentLotNumber = findField(rowFields, "fabric_lot_number", "parent_lot_number", "lot_number", "fabric_lot");
        if (parentLotNumber == null || parentLotNumber.isBlank()) {
            throw new ImportRowValidationException(rowNumber, "Missing parent fabric lot number (expected 'fabric_lot_number')");
        }

        String cutBundleNumber = findField(rowFields, "cut_bundle_number", "child_lot_number", "cut_lot_number", "bundle_number");
        if (cutBundleNumber == null || cutBundleNumber.isBlank()) {
            throw new ImportRowValidationException(rowNumber, "Missing cut bundle lot number (expected 'cut_bundle_number')");
        }

        BigDecimal inputKg = parseDecimal(rowNumber, rowFields, "input_kg", "input_quantity", "input_qty");
        BigDecimal outputPieces = parseDecimal(rowNumber, rowFields, "output_pieces", "output_quantity", "pieces");

        if (inputKg.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ImportRowValidationException(rowNumber, "Input quantity must be positive, found: " + inputKg);
        }
        if (outputPieces.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ImportRowValidationException(rowNumber, "Output quantity must be positive, found: " + outputPieces);
        }

        LotEntity parentLot = lotRepository.findByLotNumber(parentLotNumber)
                .orElseThrow(() -> new ImportRowValidationException(rowNumber, "Parent fabric lot not found: '" + parentLotNumber + "'"));

        UUID facilityId = defaultFacilityId != null ? defaultFacilityId : parentLot.getCurrentFacilityId();
        String styleCode = findField(rowFields, "style_code", "style");
        String shipmentCode = findField(rowFields, "shipment_code", "shipment");

        ChildLotOutput childLot = new ChildLotOutput(
                cutBundleNumber,
                styleCode != null ? styleCode : parentLot.getStyleCode(),
                shipmentCode != null ? shipmentCode : parentLot.getShipmentCode(),
                outputPieces,
                Unit.PIECES,
                BigDecimal.ONE
        );

        return new RecordEventCommand(
                parentLot.getId(),
                null,
                null,
                null,
                null,
                Unit.PIECES,
                EventType.CUT,
                facilityId,
                inputKg,
                outputPieces,
                "KG",
                null,
                List.of(childLot),
                null
        );
    }

    private static String findField(Map<String, String> fields, String... candidates) {
        for (String c : candidates) {
            for (Map.Entry<String, String> entry : fields.entrySet()) {
                String cleanKey = entry.getKey().trim().toLowerCase().replace("-", "_").replace(" ", "_");
                if (cleanKey.equals(c) && entry.getValue() != null && !entry.getValue().isBlank()) {
                    return entry.getValue().trim();
                }
            }
        }
        return null;
    }

    private static BigDecimal parseDecimal(int rowNumber, Map<String, String> fields, String... candidates) {
        String val = findField(fields, candidates);
        if (val == null) {
            throw new ImportRowValidationException(rowNumber, "Missing required numeric quantity field: " + String.join(" / ", candidates));
        }
        try {
            return new BigDecimal(val.trim());
        } catch (NumberFormatException e) {
            throw new ImportRowValidationException(rowNumber, "Invalid numeric value '" + val + "' for field: " + candidates[0]);
        }
    }
}
