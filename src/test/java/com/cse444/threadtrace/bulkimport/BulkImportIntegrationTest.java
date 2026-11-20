package com.cse444.threadtrace.bulkimport;

import com.cse444.threadtrace.bulkimport.job.BulkImportService;
import com.cse444.threadtrace.bulkimport.job.ImportExecutionReport;
import com.cse444.threadtrace.lineage.application.RecordEventCommand;
import com.cse444.threadtrace.lineage.application.RecordLotEventUseCase;
import com.cse444.threadtrace.lineage.domain.model.EventType;
import com.cse444.threadtrace.lineage.domain.model.Unit;
import com.cse444.threadtrace.lineage.infrastructure.LotRepository;
import com.cse444.threadtrace.registry.*;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Bulk Import Integration Test (FR9):
 * Validates CSV cutting reports and Excel packing lists with structured rejection reporting.
 */
@SpringBootTest
@ActiveProfiles("test")
class BulkImportIntegrationTest {

    @Autowired
    private RegistryService registryService;

    @Autowired
    private RecordLotEventUseCase recordLotEventUseCase;

    @Autowired
    private BulkImportService bulkImportService;

    @Autowired
    private LotRepository lotRepository;

    private UUID brandId;
    private UUID facilityId;

    @BeforeEach
    void setUp() {
        PartyResponse brand = registryService.registerParty(new CreatePartyRequest(
                PartyType.BRAND, "H&M Bulk QA", "BRAND-HM-" + UUID.randomUUID().toString().substring(0, 8),
                "SE", "Stockholm", 200, BigDecimal.valueOf(1200.0)
        ));
        brandId = brand.id();

        PartyResponse factory = registryService.registerParty(new CreatePartyRequest(
                PartyType.FACTORY, "Epyllion Group QA", "FAC-EPY-" + UUID.randomUUID().toString().substring(0, 8),
                "BD", "Mirpur Dhaka", 3500, BigDecimal.valueOf(480.0)
        ));
        FacilityResponse fac = registryService.registerFacility(new CreateFacilityRequest(
                factory.id(), FacilityType.FACTORY, null, "Epyllion Cutting & Packing Unit", "Dhaka"
        ));
        facilityId = fac.id();
    }

    private String createFabricLot() {
        String lotNum = "LOT-FABRIC-BULK-" + UUID.randomUUID().toString().substring(0, 8);
        recordLotEventUseCase.recordEvent(new RecordEventCommand(
                null,
                lotNum,
                brandId,
                "STYLE-BULK-01",
                "SHIP-BULK-01",
                Unit.KG,
                EventType.RECEIVE,
                facilityId,
                BigDecimal.valueOf(500.0),
                BigDecimal.valueOf(500.0),
                "KG",
                null,
                null,
                null
        ));
        return lotNum;
    }

    private String createGarmentLot() {
        String lotNum = "LOT-SEWN-BULK-" + UUID.randomUUID().toString().substring(0, 8);
        recordLotEventUseCase.recordEvent(new RecordEventCommand(
                null,
                lotNum,
                brandId,
                "STYLE-BULK-01",
                "SHIP-BULK-01",
                Unit.PIECES,
                EventType.RECEIVE,
                facilityId,
                BigDecimal.valueOf(1000.0),
                BigDecimal.valueOf(1000.0),
                "PIECES",
                null,
                null,
                null
        ));
        return lotNum;
    }

    @Test
    @DisplayName("FR9: CSV Cutting Report imports valid rows and outputs structured rejection report for invalid rows")
    void csvCuttingReportImportWithRejections() {
        String fabricLot1 = createFabricLot();
        String fabricLot2 = createFabricLot();

        String cutLot1 = "CUT-BNDL-" + UUID.randomUUID().toString().substring(0, 6);
        String cutLot2 = "CUT-BNDL-" + UUID.randomUUID().toString().substring(0, 6);

        String csvData = String.join("\n",
                "fabric_lot_number,cut_bundle_number,input_kg,output_pieces,style_code,shipment_code",
                fabricLot1 + "," + cutLot1 + ",45.0,200,STYLE-BULK-01,SHIP-BULK-01",
                fabricLot2 + "," + cutLot2 + ",45.0,200,STYLE-BULK-01,SHIP-BULK-01",
                "NON_EXISTENT_LOT,CUT-BNDL-ERR1,10.0,50,STYLE-BULK-01,SHIP-BULK-01", // Missing parent lot
                fabricLot1 + ",,10.0,50,STYLE-BULK-01,SHIP-BULK-01"                  // Missing cut bundle number
        );

        ByteArrayInputStream stream = new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8));

        ImportExecutionReport report = bulkImportService.executeImport(
                "cutting-report",
                stream,
                "cutting_factory_sheet.csv",
                facilityId,
                brandId
        );

        assertThat(report.totalRows()).isEqualTo(4);
        assertThat(report.successfulRows()).isEqualTo(2);
        assertThat(report.rejectedRows()).isEqualTo(2);
        assertThat(report.rejections()).hasSize(2);

        // Verify rejection report details
        assertThat(report.rejections().get(0).rowNumber()).isEqualTo(4);
        assertThat(report.rejections().get(0).reason()).contains("Parent fabric lot not found");

        assertThat(report.rejections().get(1).rowNumber()).isEqualTo(5);
        assertThat(report.rejections().get(1).reason()).contains("Missing cut bundle lot number");

        // Verify valid lots were created in database
        assertThat(lotRepository.existsByLotNumber(cutLot1)).isTrue();
        assertThat(lotRepository.existsByLotNumber(cutLot2)).isTrue();
    }

    @Test
    @DisplayName("FR9: Excel Packing List imports valid cartons and rejects missing garment parent lot")
    void excelPackingListImportWithRejections() throws Exception {
        String garmentLot1 = createGarmentLot();
        String garmentLot2 = createGarmentLot();

        String carton1 = "CARTON-XLS-" + UUID.randomUUID().toString().substring(0, 6);
        String carton2 = "CARTON-XLS-" + UUID.randomUUID().toString().substring(0, 6);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("PackingList");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("garment_lot_number");
            header.createCell(1).setCellValue("carton_number");
            header.createCell(2).setCellValue("pieces_count");
            header.createCell(3).setCellValue("carton_count");
            header.createCell(4).setCellValue("style_code");
            header.createCell(5).setCellValue("shipment_code");

            // Row 1 (Valid)
            Row r1 = sheet.createRow(1);
            r1.createCell(0).setCellValue(garmentLot1);
            r1.createCell(1).setCellValue(carton1);
            r1.createCell(2).setCellValue(50);
            r1.createCell(3).setCellValue(1);
            r1.createCell(4).setCellValue("STYLE-BULK-01");
            r1.createCell(5).setCellValue("SHIP-BULK-01");

            // Row 2 (Valid)
            Row r2 = sheet.createRow(2);
            r2.createCell(0).setCellValue(garmentLot2);
            r2.createCell(1).setCellValue(carton2);
            r2.createCell(2).setCellValue(50);
            r2.createCell(3).setCellValue(1);
            r2.createCell(4).setCellValue("STYLE-BULK-01");
            r2.createCell(5).setCellValue("SHIP-BULK-01");

            // Row 3 (Invalid - Unknown lot)
            Row r3 = sheet.createRow(3);
            r3.createCell(0).setCellValue("NON_EXISTENT_GARMENT_LOT");
            r3.createCell(1).setCellValue("CARTON-ERR");
            r3.createCell(2).setCellValue(50);
            r3.createCell(3).setCellValue(1);
            r3.createCell(4).setCellValue("STYLE-BULK-01");
            r3.createCell(5).setCellValue("SHIP-BULK-01");

            workbook.write(out);
        }

        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());

        ImportExecutionReport report = bulkImportService.executeImport(
                "packing-list",
                in,
                "factory_packing_list.xlsx",
                facilityId,
                brandId
        );

        assertThat(report.totalRows()).isEqualTo(3);
        assertThat(report.successfulRows()).isEqualTo(2);
        assertThat(report.rejectedRows()).isEqualTo(1);
        assertThat(report.rejections()).hasSize(1);
        assertThat(report.rejections().get(0).rowNumber()).isEqualTo(4); // Excel row 4 (1-indexed)
        assertThat(report.rejections().get(0).reason()).contains("Parent garment lot not found");

        assertThat(lotRepository.existsByLotNumber(carton1)).isTrue();
        assertThat(lotRepository.existsByLotNumber(carton2)).isTrue();
    }
}
