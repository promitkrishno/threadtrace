package com.cse444.threadtrace.lineage.application;

import com.cse444.threadtrace.common.config.TraceabilityProperties;
import com.cse444.threadtrace.common.error.DomainException;
import com.cse444.threadtrace.lineage.domain.model.EventType;
import com.cse444.threadtrace.lineage.domain.model.LotEntity;
import com.cse444.threadtrace.lineage.domain.model.LotStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Mass Balance Validation (FR4, QA2):
 * Verifies that transformation event losses stay within configured thresholds
 * (e.g. max 12% cutting loss). Blocks or quarantines violations according to configuration.
 */
@Component
public class MassBalanceValidator {

    private final TraceabilityProperties properties;

    public MassBalanceValidator(TraceabilityProperties properties) {
        this.properties = properties;
    }

    public record ValidationResult(
            boolean valid,
            boolean flagged,
            BigDecimal lossPercentage,
            Double allowedMaxLossPct,
            String message
    ) {}

    public ValidationResult validate(
            EventType eventType,
            BigDecimal inputQty,
            BigDecimal outputQty,
            LotEntity lot
    ) {
        if (inputQty == null || outputQty == null) {
            return new ValidationResult(true, false, BigDecimal.ZERO, null, null);
        }

        if (eventType == EventType.RECEIVE || inputQty.compareTo(BigDecimal.ZERO) <= 0) {
            return new ValidationResult(true, false, BigDecimal.ZERO, null, null);
        }

        BigDecimal lossPct = BigDecimal.ZERO;
        if (inputQty.compareTo(outputQty) > 0) {
            lossPct = inputQty.subtract(outputQty)
                    .divide(inputQty, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        TraceabilityProperties.StepConfig stepConfig = properties.processSteps() != null
                ? properties.processSteps().get(eventType.name())
                : null;

        double allowedLoss = stepConfig != null ? stepConfig.maxLossPercentage() : 100.0;

        if (lossPct.doubleValue() > allowedLoss) {
            TraceabilityProperties.MassBalanceAction action = properties.resolveMassBalanceAction();
            String msg = String.format(
                    "Mass balance violation for step %s: observed loss %.2f%% exceeds configured limit of %.2f%%",
                    eventType.name(), lossPct.doubleValue(), allowedLoss
            );

            if (action == TraceabilityProperties.MassBalanceAction.BLOCK) {
                throw new DomainException("MASS_BALANCE_VIOLATION", msg);
            }

            if (lot != null) {
                lot.setStatus(LotStatus.QUARANTINED);
            }
            return new ValidationResult(false, true, lossPct, allowedLoss, msg);
        }

        return new ValidationResult(true, false, lossPct, allowedLoss, null);
    }
}
