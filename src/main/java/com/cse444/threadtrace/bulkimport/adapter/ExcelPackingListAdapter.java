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
 * Adapter Pattern: Converts Excel packing list rows into standardized PACK event commands (FR9).
 */
@Component
public class ExcelPackingListAdapter implements LotImportRowAdapter {

    private final LotRepository lotRepository;

    public ExcelPackingListAdapter(LotRepository lotRepository) {
        this.lotRepository = lotRepository;
    }

    @Override
    public boolean supports(String importType) {
        if (importType == null) return false;
        String t = importType.trim().toLowerCase();
        return t.equals("packing-list") || t.equals("packing") || t.equals("pack-list") || t.equals("pack");
    }

    @Override
    public RecordEventCommand adapt(
            int rowNumber,
            Map<String, String> rowFields,
            UUID defaultFacilityId,
            UUID defaultBrandId
    ) {
        String garmentLotNumber = findField(rowFields, "garment_lot_number", "parent_lot_number", "lot_number", "sewn_lot");
        if (garmentLotNumber == null || garmentLotNumber.isBlank()) {
            throw new ImportRowValidationException(rowNumber, "Missing parent garment lot number (expected 'garment_lot_number')");
        }

        String cartonNumber = findField(rowFields, "carton_number", "carton_lot_number", "child_lot_number", "carton_id");
        if (cartonNumber == null || cartonNumber.isBlank()) {
            throw new ImportRowValidationException(rowNumber, "Missing carton lot number (expected 'carton_number')");
        }

        BigDecimal piecesCount = parseDecimal(rowNumber, rowFields, "pieces_count", "input_quantity", "pieces", "quantity");
        String cartonsCountStr = findField(rowFields, "carton_count", "output_quantity", "cartons");
        BigDecimal cartonQty = (cartonsCountStr != null && !cartonsCountStr.isBlank())
                ? new BigDecimal(cartonsCountStr.trim())
                : BigDecimal.ONE;

        if (piecesCount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ImportRowValidationException(rowNumber, "Pieces count must be positive, found: " + piecesCount);
        }

        LotEntity parentLot = lotRepository.findByLotNumber(garmentLotNumber)
                .orElseThrow(() -> new ImportRowValidationException(rowNumber, "Parent garment lot not found: '" + garmentLotNumber + "'"));

        UUID facilityId = defaultFacilityId != null ? defaultFacilityId : parentLot.getCurrentFacilityId();
        String styleCode = findField(rowFields, "style_code", "style");
        String shipmentCode = findField(rowFields, "shipment_code", "shipment");

        ChildLotOutput childCarton = new ChildLotOutput(
                cartonNumber,
                styleCode != null ? styleCode : parentLot.getStyleCode(),
                shipmentCode != null ? shipmentCode : parentLot.getShipmentCode(),
                cartonQty,
                Unit.CARTONS,
                BigDecimal.ONE
        );

        return new RecordEventCommand(
                parentLot.getId(),
                null,
                null,
                null,
                null,
                Unit.CARTONS,
                EventType.PACK,
                facilityId,
                piecesCount,
                cartonQty,
                "PIECES",
                null,
                List.of(childCarton),
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
