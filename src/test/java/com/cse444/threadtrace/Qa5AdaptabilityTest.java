package com.cse444.threadtrace;

import com.cse444.threadtrace.common.config.TraceabilityProperties;
import com.cse444.threadtrace.passport.domain.builder.DigitalPassportBuilder;
import com.cse444.threadtrace.passport.domain.builder.DigitalPassportEntity;
import com.cse444.threadtrace.passport.domain.builder.DigitalPassportRepository;
import com.cse444.threadtrace.passport.projection.PublicPassportView;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Quality Attribute QA5: Modifiability & Adaptability Verification.
 * 
 * Verifies that:
 * 1. Adding a new certificate type (e.g. CRADLE_TO_CRADLE), new process step (e.g. QUILT),
 *    and custom emission factors works purely via configuration without code changes.
 * 2. Adding arbitrary custom fields to Digital Product Passports operates via
 *    DigitalPassportBuilder and JSONB without modifying any lineage domain/application classes.
 * 3. Lineage classes remain completely decoupled from passport custom fields.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "threadtrace.valid-certificate-types[0]=CRADLE_TO_CRADLE",
        "threadtrace.valid-certificate-types[1]=GOTS",
        "threadtrace.valid-certificate-types[2]=GRS",
        "threadtrace.process-steps.QUILT.label=Quilting",
        "threadtrace.process-steps.QUILT.max-loss-percentage=8.5",
        "threadtrace.emission-factors.water-liters-per-unit=35.5",
        "threadtrace.emission-factors.energy-kwh-per-unit=5.2",
        "threadtrace.emission-factors.co2e-kg-per-unit=1.9"
})
class Qa5AdaptabilityTest {

    @Autowired
    private TraceabilityProperties traceabilityProperties;

    @Autowired
    private DigitalPassportRepository passportRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("QA5 Scenario 1: New certificate type is registered purely via configuration")
    void testNewCertificateTypeViaConfiguration() {
        List<String> validCerts = traceabilityProperties.validCertificateTypes();
        assertThat(validCerts)
                .as("Valid certificate types must contain CRADLE_TO_CRADLE loaded from test properties")
                .contains("CRADLE_TO_CRADLE");
    }

    @Test
    @DisplayName("QA5 Scenario 2: New supply-chain process step and loss limits configured without code changes")
    void testNewProcessStepViaConfiguration() {
        Map<String, TraceabilityProperties.StepConfig> steps = traceabilityProperties.processSteps();
        assertThat(steps).containsKey("QUILT");

        TraceabilityProperties.StepConfig quiltStep = steps.get("QUILT");
        assertThat(quiltStep.label()).isEqualTo("Quilting");
        assertThat(quiltStep.maxLossPercentage()).isEqualTo(8.5);
    }

    @Test
    @DisplayName("QA5 Scenario 3: Custom emission factors loaded dynamically via configuration properties")
    void testCustomEmissionFactorsViaConfiguration() {
        TraceabilityProperties.EmissionFactors emissionFactors = traceabilityProperties.emissionFactors();
        assertThat(emissionFactors.waterLitersPerUnit()).isEqualTo(35.5);
        assertThat(emissionFactors.energyKwhPerUnit()).isEqualTo(5.2);
        assertThat(emissionFactors.co2eKgPerUnit()).isEqualTo(1.9);
    }

    @Test
    @DisplayName("QA5 Scenario 4: Dynamic custom fields added to DPP via Builder without altering lineage classes")
    void testDynamicCustomFieldsInPassport() {
        UUID brandId = UUID.randomUUID();
        UUID rootCartonLotId = UUID.randomUUID();

        var buildResult = DigitalPassportBuilder.builder()
                .withStyleAndShipment("STYLE-ADAPT-001", "SHIP-ADAPT-2026")
                .withBrand(brandId, "Patagonia Global", "BRAND-PAT-001")
                .withRootCartonLot(rootCartonLotId, "LOT-CARTON-ADAPT-01")
                .withCustomField("circularDesignScore", 96.5)
                .withCustomField("takeBackUrl", "https://patagonia.com/wornwear")
                .withCustomField("repairWarrantyYears", 10)
                .withCustomField("microfiberSheddingMg", 0.85)
                .buildAndSave(passportRepository, objectMapper);

        DigitalPassportEntity passport = buildResult.entity();
        assertThat(passport.getPublicPayload()).isNotNull();
        assertThat(passport.getPublicPayload()).contains("circularDesignScore");
        assertThat(passport.getPublicPayload()).contains("takeBackUrl");

        // Verify PublicPassportView projection preserves dynamic custom fields
        PublicPassportView view = buildResult.publicView();
        assertThat(view.customFields()).containsEntry("circularDesignScore", 96.5);
        assertThat(view.customFields()).containsEntry("takeBackUrl", "https://patagonia.com/wornwear");
        assertThat(view.customFields()).containsEntry("repairWarrantyYears", 10);
        assertThat(view.customFields()).containsEntry("microfiberSheddingMg", 0.85);
    }

    @Test
    @DisplayName("QA5 Scenario 5: Architectural independence - lineage package contains zero passport references")
    void testLineagePackageIsIndependentOfPassportCustomFields() {
        JavaClasses lineageClasses = new ClassFileImporter()
                .importPackages("com.cse444.threadtrace.lineage..");

        // Ensure no class in lineage references passport builder or passport projections
        boolean hasPassportCoupling = lineageClasses.stream()
                .anyMatch(javaClass -> javaClass.getDirectDependenciesFromSelf().stream()
                        .anyMatch(dep -> dep.getTargetClass().getPackageName().contains("passport")));

        assertThat(hasPassportCoupling)
                .as("Lineage domain/application must have ZERO dependencies on passport classes")
                .isFalse();
    }
}
