package com.cse444.threadtrace;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DashboardWebIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET / serves the interactive Thymeleaf Web Dashboard publicly")
    void testDashboardWebPageServedPublicly() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("ThreadTrace")))
                .andExpect(content().string(containsString("Garment Supply-Chain Traceability Dashboard")))
                .andExpect(content().string(containsString("Overview & Demo")));
    }

    @Test
    @DisplayName("Static assets (/css/dashboard.css, /js/dashboard.js) are accessible publicly")
    void testStaticAssetsAccessible() throws Exception {
        mockMvc.perform(get("/css/dashboard.css"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("--brand-primary")));

        mockMvc.perform(get("/js/dashboard.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("AUTH_PROFILES")));
    }

    @Test
    @DisplayName("Dashboard stats API returns live counts")
    void testDashboardStatsApi() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalParties").isNumber())
                .andExpect(jsonPath("$.totalFacilities").isNumber())
                .andExpect(jsonPath("$.totalLots").isNumber());
    }

    @Test
    @DisplayName("RMG 6-Level Supply-Chain demo seeding endpoint functions correctly")
    void testSeedRmgDemo() throws Exception {
        mockMvc.perform(post("/api/v1/dashboard/demo/seed-rmg"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cartonLotNumber").isString())
                .andExpect(jsonPath("$.qrCodeTokenBrandA").isString())
                .andExpect(jsonPath("$.qrCodeTokenBrandB").isString());
    }

    @Test
    @DisplayName("QA2 Defect Scenarios demo seeding endpoint returns 4 planted violations")
    void testSeedViolationsDemo() throws Exception {
        mockMvc.perform(post("/api/v1/dashboard/demo/seed-violations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.violations").isArray())
                .andExpect(jsonPath("$.violations.length()").value(4));
    }

    @Test
    @DisplayName("Sample CSV and XLSX downloads are served correctly")
    void testSampleDownloads() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/sample-csv"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("factory_cutting_report_sample.csv")))
                .andExpect(content().string(containsString("fabric_lot_number,cut_bundle_number")));

        mockMvc.perform(get("/api/v1/dashboard/sample-xlsx"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("factory_packing_list_sample.xlsx")));
    }

    @Test
    @DisplayName("List lots, passports, and evidence endpoints return 200 OK")
    void testListEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/lots"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        mockMvc.perform(get("/api/v1/passports"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/evidence"))
                .andExpect(status().isOk());
    }
}
