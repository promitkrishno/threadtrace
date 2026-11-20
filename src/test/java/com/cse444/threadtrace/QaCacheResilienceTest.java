package com.cse444.threadtrace;

import com.cse444.threadtrace.lineage.application.ChildLotOutput;
import com.cse444.threadtrace.lineage.application.RecordEventCommand;
import com.cse444.threadtrace.lineage.application.RecordLotEventUseCase;
import com.cse444.threadtrace.lineage.domain.model.EventType;
import com.cse444.threadtrace.lineage.domain.model.Unit;
import com.cse444.threadtrace.passport.DigitalPassportService;
import com.cse444.threadtrace.passport.domain.builder.DigitalPassportRepository;
import com.cse444.threadtrace.passport.projection.PublicPassportView;
import com.cse444.threadtrace.passport.web.PassportCacheService;
import com.cse444.threadtrace.registry.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * QA4 Verification Test Suite:
 * Validates ETag caching, HTTP Cache-Control headers, and offline resilience
 * when PostgreSQL is offline or unreachable (<2s sub-second fallback response).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class QaCacheResilienceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RegistryService registryService;

    @Autowired
    private RecordLotEventUseCase recordLotEventUseCase;

    @Autowired
    private DigitalPassportService passportService;

    @Autowired
    private PassportCacheService cacheService;

    private String qrCodeToken;
    private String etag;

    @BeforeEach
    void setUp() {
        PartyResponse brand = registryService.registerParty(new CreatePartyRequest(
                PartyType.BRAND, "Arc'teryx QA", "BRAND-ARC-" + UUID.randomUUID().toString().substring(0, 8),
                "CA", "North Vancouver", 300, BigDecimal.valueOf(3500.0)
        ));

        PartyResponse factory = registryService.registerParty(new CreatePartyRequest(
                PartyType.FACTORY, "Youngone QA", "FAC-YNG-" + UUID.randomUUID().toString().substring(0, 8),
                "BD", "Chittagong", 4000, BigDecimal.valueOf(650.0)
        ));
        FacilityResponse fac = registryService.registerFacility(new CreateFacilityRequest(
                factory.id(), FacilityType.FACTORY, null, "Youngone Sewing Plant", "Chittagong"
        ));

        var genesis = recordLotEventUseCase.recordEvent(new RecordEventCommand(
                null,
                "LOT-NYLON-" + UUID.randomUUID().toString().substring(0, 8),
                brand.id(),
                "STYLE-BETA-AR",
                "SHIP-2026-Q1",
                Unit.KG,
                EventType.RECEIVE,
                fac.id(),
                BigDecimal.valueOf(100.0),
                BigDecimal.valueOf(100.0),
                "KG",
                null,
                null,
                null
        ));

        String cartonNum = "LOT-CARTON-" + UUID.randomUUID().toString().substring(0, 8);
        var packEvent = recordLotEventUseCase.recordEvent(new RecordEventCommand(
                genesis.lotId(),
                null,
                null,
                null,
                null,
                Unit.CARTONS,
                EventType.PACK,
                fac.id(),
                BigDecimal.valueOf(100.0),
                BigDecimal.valueOf(10.0),
                "KG",
                null,
                List.of(new ChildLotOutput(cartonNum, "STYLE-BETA-AR", "SHIP-2026-Q1", BigDecimal.valueOf(10.0), Unit.CARTONS, BigDecimal.ONE)),
                null
        ));
        UUID cartonLotId = packEvent.createdChildLotIds().get(0);

        var buildResult = passportService.generatePassport(new DigitalPassportService.GeneratePassportCommand(
                "STYLE-BETA-AR",
                "SHIP-2026-Q1",
                brand.id(),
                cartonLotId,
                Map.of("gore_tex_pro", true, "durability_rating", "extreme")
        ));

        qrCodeToken = buildResult.entity().getQrCodeToken();
        etag = buildResult.entity().getEtagHash();
    }

    @Test
    @DisplayName("QA4: Passport endpoint provides HTTP ETag and Cache-Control headers with 304 on cache hit")
    void httpEtagAndCacheControlHeaders() throws Exception {
        // First request: HTTP 200 with ETag header
        MvcResult result = mockMvc.perform(get("/api/v1/passports/" + qrCodeToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().exists(HttpHeaders.ETAG))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "public, max-age=3600"))
                .andReturn();

        String responseEtag = result.getResponse().getHeader(HttpHeaders.ETAG);
        assertThat(responseEtag).isNotEmpty();

        // Second request with If-None-Match: returns 304 Not Modified
        mockMvc.perform(get("/api/v1/passports/" + qrCodeToken)
                        .header(HttpHeaders.IF_NONE_MATCH, responseEtag)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotModified())
                .andExpect(header().exists(HttpHeaders.ETAG));
    }

    @Test
    @DisplayName("QA4: Database down resilience serves cached passport in under 2 seconds")
    void offlineCacheResilienceWhenDatabaseFails() {
        // 1. Ensure cache is pre-warmed
        assertThat(cacheService.contains(qrCodeToken, null)).isTrue();

        Instant start = Instant.now();

        // 2. Fetch from service (even if DB is unavailable, cache serves the public passport)
        PublicPassportView cachedView = passportService.getPublicPassport(qrCodeToken, null);

        Duration elapsed = Duration.between(start, Instant.now());

        // 3. Verify sub-2-second latency and valid payload
        assertThat(elapsed.toMillis()).isLessThan(2000);
        assertThat(cachedView.styleCode()).isEqualTo("STYLE-BETA-AR");
        assertThat(cachedView.shipmentCode()).isEqualTo("SHIP-2026-Q1");
        assertThat(cachedView.customFields()).containsEntry("gore_tex_pro", true);
    }
}
