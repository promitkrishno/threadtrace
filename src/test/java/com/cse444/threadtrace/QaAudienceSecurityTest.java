package com.cse444.threadtrace;

import com.cse444.threadtrace.lineage.application.ChildLotOutput;
import com.cse444.threadtrace.lineage.application.RecordEventCommand;
import com.cse444.threadtrace.lineage.application.RecordLotEventUseCase;
import com.cse444.threadtrace.lineage.domain.model.EventType;
import com.cse444.threadtrace.lineage.domain.model.Unit;
import com.cse444.threadtrace.passport.DigitalPassportService;
import com.cse444.threadtrace.passport.projection.PublicPassportView;
import com.cse444.threadtrace.registry.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * QA3 & QA5 Verification Test Suite:
 * 1. Zero confidential field leaks on public endpoints (JSON and Thymeleaf HTML).
 * 2. Per-brand tenant isolation (cross-brand access blocked with HTTP 403).
 * 3. Role-restricted Auditor views (non-auditors blocked with HTTP 403).
 * 4. Extensible custom fields without modifying lineage classes (QA5).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class QaAudienceSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RegistryService registryService;

    @Autowired
    private RecordLotEventUseCase recordLotEventUseCase;

    @Autowired
    private DigitalPassportService passportService;

    @Autowired
    private PartyRepository partyRepository;

    @Autowired
    private FacilityRepository facilityRepository;

    private UUID brandAId;
    private UUID brandBId;
    private UUID cartonLotId;
    private String qrCodeToken;

    @BeforeEach
    void setUp() {
        // Register Brand A idempotently
        brandAId = partyRepository.findByExternalCode("BRAND-PAT-001")
                .map(PartyEntity::getId)
                .orElseGet(() -> registryService.registerParty(new CreatePartyRequest(
                        PartyType.BRAND, "Patagonia Security QA", "BRAND-PAT-001", "US", "259 W Santa Clara St, Ventura", 450, BigDecimal.valueOf(1800.0)
                )).id());

        // Register Brand B idempotently
        brandBId = partyRepository.findByExternalCode("BRAND-OTHER-002")
                .map(PartyEntity::getId)
                .orElseGet(() -> registryService.registerParty(new CreatePartyRequest(
                        PartyType.BRAND, "Nike Competitor QA", "BRAND-OTHER-002", "US", "One Bowerman Dr, Beaverton", 5000, BigDecimal.valueOf(3200.0)
                )).id());

        // Register Mill & Facility idempotently
        UUID millId = partyRepository.findByExternalCode("MILL-BEX-001")
                .map(PartyEntity::getId)
                .orElseGet(() -> registryService.registerParty(new CreatePartyRequest(
                        PartyType.FABRIC_MILL, "Beximco Textiles", "MILL-BEX-001", "BD", "Confidential Mill Address 123", 2200, BigDecimal.valueOf(420.0)
                )).id());

        var facPage = facilityRepository.findByPartyId(millId, org.springframework.data.domain.PageRequest.of(0, 1));
        UUID millFacId = facPage.hasContent()
                ? facPage.getContent().get(0).getId()
                : registryService.registerFacility(new CreateFacilityRequest(
                        millId, FacilityType.MILL, null, "Beximco Spinning Unit", "Gazipur"
                )).id();

        // Create Genesis Yarn Lot
        var genesis = recordLotEventUseCase.recordEvent(new RecordEventCommand(
                null,
                "LOT-YARN-SEC-" + UUID.randomUUID().toString().substring(0, 8),
                brandAId,
                "STYLE-SEC-01",
                "SHIP-SEC-01",
                Unit.KG,
                EventType.RECEIVE,
                millFacId,
                BigDecimal.valueOf(500.0),
                BigDecimal.valueOf(500.0),
                "KG",
                null,
                null,
                null
        ));

        // Create Carton Lot (Finished Product)
        String cartonLotNum = "LOT-CARTON-SEC-" + UUID.randomUUID().toString().substring(0, 8);
        var packEvent = recordLotEventUseCase.recordEvent(new RecordEventCommand(
                genesis.lotId(),
                null,
                null,
                null,
                null,
                Unit.CARTONS,
                EventType.PACK,
                millFacId,
                BigDecimal.valueOf(500.0),
                BigDecimal.valueOf(25.0),
                "KG",
                null,
                List.of(new ChildLotOutput(cartonLotNum, "STYLE-SEC-01", "SHIP-SEC-01", BigDecimal.valueOf(25.0), Unit.CARTONS, BigDecimal.ONE)),
                null
        ));
        cartonLotId = packEvent.createdChildLotIds().get(0);

        // Generate Passport with dynamic custom fields (QA5)
        var buildResult = passportService.generatePassport(new DigitalPassportService.GeneratePassportCommand(
                "STYLE-SEC-01",
                "SHIP-SEC-01",
                brandAId,
                cartonLotId,
                Map.of(
                        "circularity_score", "A+",
                        "carbon_offset_partner", "South Pole Climate Group",
                        "repair_dropoff_available", true
                )
        ));
        qrCodeToken = buildResult.entity().getQrCodeToken();
    }

    @Test
    @DisplayName("QA3: Public JSON endpoint strictly excludes all confidential commercial fields")
    void publicJsonEndpointExcludesConfidentialFields() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/passports/" + qrCodeToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();

        // 1. Verify public data is present
        assertThat(responseBody).contains("STYLE-SEC-01");
        assertThat(responseBody).contains("Patagonia Security QA");

        // 2. Verify QA5 custom dynamic fields are present
        assertThat(responseBody).contains("circularity_score");
        assertThat(responseBody).contains("South Pole Climate Group");

        // 3. Verify ZERO confidential fields appear in JSON
        assertThat(responseBody).doesNotContain("exact_address");
        assertThat(responseBody).doesNotContain("exactAddress");
        assertThat(responseBody).doesNotContain("unit_price_bdt");
        assertThat(responseBody).doesNotContain("unitPriceBdt");
        assertThat(responseBody).doesNotContain("worker_count");
        assertThat(responseBody).doesNotContain("workerCount");
        assertThat(responseBody).doesNotContain("Confidential Mill Address 123");
        assertThat(responseBody).doesNotContain("1800.0");
        assertThat(responseBody).doesNotContain("2200");
    }

    @Test
    @DisplayName("QA3: Public Thymeleaf HTML page strictly excludes all confidential commercial fields")
    void publicThymeleafHtmlExcludesConfidentialFields() throws Exception {
        MvcResult result = mockMvc.perform(get("/passports/" + qrCodeToken)
                        .accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk())
                .andReturn();

        String htmlContent = result.getResponse().getContentAsString();

        // 1. Verify HTML template renders properly
        assertThat(htmlContent).contains("Patagonia Security QA");
        assertThat(htmlContent).contains("Environmental Footprint");
        assertThat(htmlContent).contains("Water Footprint");

        // 2. Verify ZERO confidential commercial fields appear in HTML
        assertThat(htmlContent).doesNotContain("Confidential Mill Address 123");
        assertThat(htmlContent).doesNotContain("unit_price_bdt");
        assertThat(htmlContent).doesNotContain("worker_count");
        assertThat(htmlContent).doesNotContain("420.0");
        assertThat(htmlContent).doesNotContain("2200");
    }

    @Test
    @DisplayName("QA3: Brand tenant isolation allows Brand A but blocks Brand B with HTTP 403")
    void brandTenantIsolationEnforced() throws Exception {
        // Brand A accessing its own passport -> HTTP 200
        mockMvc.perform(get("/api/v1/passports/" + qrCodeToken + "/brand")
                        .with(httpBasic("BRAND-PAT-001", "password")))
                .andExpect(status().isOk());

        // Brand B (Competitor) attempting cross-brand access -> HTTP 403 Forbidden!
        mockMvc.perform(get("/api/v1/passports/" + qrCodeToken + "/brand")
                        .with(httpBasic("BRAND-OTHER-002", "password")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("QA3: Auditor view granted to ROLE_AUDITOR and blocked with HTTP 403 for Brand users")
    void auditorRoleAccessControl() throws Exception {
        // Auditor accessing auditor view -> HTTP 200 with full confidential data
        MvcResult auditorResult = mockMvc.perform(get("/api/v1/passports/" + qrCodeToken + "/auditor")
                        .with(httpBasic("auditor-inspector", "password")))
                .andExpect(status().isOk())
                .andReturn();

        String auditorBody = auditorResult.getResponse().getContentAsString();
        assertThat(auditorBody).contains("confidentialSuppliers");
        assertThat(auditorBody).contains("auditViolations");

        // Brand A user attempting to view Auditor endpoint -> HTTP 403 Forbidden!
        mockMvc.perform(get("/api/v1/passports/" + qrCodeToken + "/auditor")
                        .with(httpBasic("BRAND-PAT-001", "password")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("QA5: Regeneration increments version_no and keeps old versions retrievable")
    void regenerationIncrementsVersionNumber() {
        // Regenerate passport for the same (styleCode, shipmentCode)
        var buildResultV2 = passportService.generatePassport(new DigitalPassportService.GeneratePassportCommand(
                "STYLE-SEC-01",
                "SHIP-SEC-01",
                brandAId,
                cartonLotId,
                Map.of("regenerated", true, "version", 2)
        ));

        assertThat(buildResultV2.entity().getVersionNo()).isEqualTo(2);
        assertThat(buildResultV2.entity().isLatest()).isTrue();

        // Old version is still retrievable
        PublicPassportView v1 = passportService.getPublicPassport(qrCodeToken, 1);
        assertThat(v1.versionNo()).isEqualTo(1);
    }
}
