package com.cse444.threadtrace.lineage.domain.state;

import com.cse444.threadtrace.common.error.DomainException;
import com.cse444.threadtrace.lineage.domain.model.EventType;
import com.cse444.threadtrace.lineage.domain.model.LotStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LotStatePatternTest {

    @Test
    @DisplayName("ActiveLotState permits all standard processing events and transitions appropriately")
    void activeLotStateTransitions() {
        LotState state = LotStateFactory.from(LotStatus.ACTIVE);
        assertThat(state.getStatus()).isEqualTo(LotStatus.ACTIVE);

        // Can record valid events
        state.validateCanRecordEvent(EventType.CUT);
        state.validateCanRecordEvent(EventType.SEW);
        state.validateCanRecordEvent(EventType.SHIP);

        // SHIP transitions to SHIPPED
        assertThat(state.determineNextStatus(EventType.SHIP, false)).isEqualTo(LotStatus.SHIPPED);

        // Fully consumed transitions to CONSUMED
        assertThat(state.determineNextStatus(EventType.CUT, true)).isEqualTo(LotStatus.CONSUMED);

        // Partially consumed remains ACTIVE
        assertThat(state.determineNextStatus(EventType.CUT, false)).isEqualTo(LotStatus.ACTIVE);
    }

    @Test
    @DisplayName("ConsumedLotState rejects further processing events")
    void consumedLotStateRejectsProcessing() {
        LotState state = LotStateFactory.from(LotStatus.CONSUMED);
        assertThat(state.getStatus()).isEqualTo(LotStatus.CONSUMED);

        assertThatThrownBy(() -> state.validateCanRecordEvent(EventType.CUT))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Cannot record event 'CUT' on a CONSUMED lot");

        assertThatThrownBy(() -> state.validateCanRecordEvent(EventType.SEW))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("CONSUMED");

        // CORRECTION event is allowed
        state.validateCanRecordEvent(EventType.CORRECTION);
    }

    @Test
    @DisplayName("QuarantinedLotState blocks operations until correction")
    void quarantinedLotStateBlocksOperations() {
        LotState state = LotStateFactory.from(LotStatus.QUARANTINED);
        assertThat(state.getStatus()).isEqualTo(LotStatus.QUARANTINED);

        assertThatThrownBy(() -> state.validateCanRecordEvent(EventType.DYE))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Cannot process lot in QUARANTINED state");

        // CORRECTION un-quarantines back to ACTIVE
        state.validateCanRecordEvent(EventType.CORRECTION);
        assertThat(state.determineNextStatus(EventType.CORRECTION, false)).isEqualTo(LotStatus.ACTIVE);
    }

    @Test
    @DisplayName("ShippedLotState strictly prohibits any subsequent events")
    void shippedLotStateProhibitsEvents() {
        LotState state = LotStateFactory.from(LotStatus.SHIPPED);
        assertThat(state.getStatus()).isEqualTo(LotStatus.SHIPPED);

        assertThatThrownBy(() -> state.validateCanRecordEvent(EventType.WASH))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Cannot record event 'WASH' on a SHIPPED lot");
    }
}
