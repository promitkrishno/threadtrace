package com.cse444.threadtrace.common.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import java.util.List;
import java.util.Map;

@ConfigurationProperties(prefix = "threadtrace")
@Validated
public record TraceabilityProperties(
        @NotEmpty Map<String, @Valid StepConfig> processSteps,
        @NotEmpty List<String> validCertificateTypes,
        @Valid @NotNull EmissionFactors emissionFactors,
        @Valid @NotNull StorageConfig storage
) {
    public record StepConfig(
            @NotBlank String label,
            @DecimalMin("0.0") @DecimalMax("100.0") double maxLossPercentage
    ) {}

    public record EmissionFactors(
            @Positive double waterLitersPerUnit,
            @Positive double energyKwhPerUnit,
            @Positive double co2eKgPerUnit
    ) {}

    public record StorageConfig(
            @NotBlank String endpoint,
            @NotBlank String accessKey,
            @NotBlank String secretKey,
            @NotBlank String bucket
    ) {}
}
