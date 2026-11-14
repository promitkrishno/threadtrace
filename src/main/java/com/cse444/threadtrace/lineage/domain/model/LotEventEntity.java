package com.cse444.threadtrace.lineage.domain.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;

@Entity
@Table(name = "lot_events", uniqueConstraints = {
        @UniqueConstraint(name = "uq_lot_events_lot_seq", columnNames = {"lot_id", "sequence_no"})
})
public class LotEventEntity {

    public static final String GENESIS_HASH = "GENESIS";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "lot_id", nullable = false)
    private UUID lotId;

    @Column(name = "sequence_no", nullable = false)
    private Long sequenceNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private EventType eventType;

    @Column(name = "facility_id", nullable = false)
    private UUID facilityId;

    @Column(name = "input_quantity", nullable = false, precision = 15, scale = 4)
    private BigDecimal inputQuantity;

    @Column(name = "output_quantity", nullable = false, precision = 15, scale = 4)
    private BigDecimal outputQuantity;

    @Column(name = "loss_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal lossPercentage = BigDecimal.ZERO;

    @Column(nullable = false, length = 50)
    private String unit;

    @Column(name = "step_water_liters", nullable = false, precision = 15, scale = 4)
    private BigDecimal stepWaterLiters = BigDecimal.ZERO;

    @Column(name = "step_energy_kwh", nullable = false, precision = 15, scale = 4)
    private BigDecimal stepEnergyKwh = BigDecimal.ZERO;

    @Column(name = "step_co2e_kg", nullable = false, precision = 15, scale = 4)
    private BigDecimal stepCo2eKg = BigDecimal.ZERO;

    @Column(name = "mass_balance_flagged", nullable = false)
    private Boolean massBalanceFlagged = false;

    @Column(name = "previous_event_hash", nullable = false, length = 64)
    private String previousEventHash;

    @Column(name = "event_hash", nullable = false, length = 64)
    private String eventHash;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "recorded_at", nullable = false, updatable = false)
    private Instant recordedAt = Instant.now();

    public LotEventEntity() {
    }

    public static String computeHash(
            String previousHash,
            UUID lotId,
            Long sequenceNo,
            EventType eventType,
            UUID facilityId,
            BigDecimal inputQuantity,
            BigDecimal outputQuantity,
            BigDecimal lossPercentage,
            String unit,
            Instant occurredAt
    ) {
        String inputStr = inputQuantity != null ? inputQuantity.setScale(4, java.math.RoundingMode.HALF_UP).toPlainString() : "0.0000";
        String outputStr = outputQuantity != null ? outputQuantity.setScale(4, java.math.RoundingMode.HALF_UP).toPlainString() : "0.0000";
        String lossStr = lossPercentage != null ? lossPercentage.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString() : "0.00";
        String timeStr = occurredAt != null ? occurredAt.truncatedTo(java.time.temporal.ChronoUnit.MILLIS).toString() : "";

        String canonicalPayload = String.format(
                Locale.US,
                "%s|%s|%d|%s|%s|%s|%s|%s|%s|%s",
                previousHash,
                lotId,
                sequenceNo,
                eventType.name(),
                facilityId,
                inputStr,
                outputStr,
                lossStr,
                unit != null ? unit.trim().toUpperCase() : "",
                timeStr
        );
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(canonicalPayload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    public String calculateExpectedHash() {
        return computeHash(
                this.previousEventHash,
                this.lotId,
                this.sequenceNo,
                this.eventType,
                this.facilityId,
                this.inputQuantity,
                this.outputQuantity,
                this.lossPercentage,
                this.unit,
                this.occurredAt
        );
    }

    // Getters and Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getLotId() {
        return lotId;
    }

    public void setLotId(UUID lotId) {
        this.lotId = lotId;
    }

    public Long getSequenceNo() {
        return sequenceNo;
    }

    public void setSequenceNo(Long sequenceNo) {
        this.sequenceNo = sequenceNo;
    }

    public EventType getEventType() {
        return eventType;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public UUID getFacilityId() {
        return facilityId;
    }

    public void setFacilityId(UUID facilityId) {
        this.facilityId = facilityId;
    }

    public BigDecimal getInputQuantity() {
        return inputQuantity;
    }

    public void setInputQuantity(BigDecimal inputQuantity) {
        this.inputQuantity = inputQuantity;
    }

    public BigDecimal getOutputQuantity() {
        return outputQuantity;
    }

    public void setOutputQuantity(BigDecimal outputQuantity) {
        this.outputQuantity = outputQuantity;
    }

    public BigDecimal getLossPercentage() {
        return lossPercentage;
    }

    public void setLossPercentage(BigDecimal lossPercentage) {
        this.lossPercentage = lossPercentage;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public BigDecimal getStepWaterLiters() {
        return stepWaterLiters;
    }

    public void setStepWaterLiters(BigDecimal stepWaterLiters) {
        this.stepWaterLiters = stepWaterLiters;
    }

    public BigDecimal getStepEnergyKwh() {
        return stepEnergyKwh;
    }

    public void setStepEnergyKwh(BigDecimal stepEnergyKwh) {
        this.stepEnergyKwh = stepEnergyKwh;
    }

    public BigDecimal getStepCo2eKg() {
        return stepCo2eKg;
    }

    public void setStepCo2eKg(BigDecimal stepCo2eKg) {
        this.stepCo2eKg = stepCo2eKg;
    }

    public Boolean getMassBalanceFlagged() {
        return massBalanceFlagged;
    }

    public void setMassBalanceFlagged(Boolean massBalanceFlagged) {
        this.massBalanceFlagged = massBalanceFlagged;
    }

    public String getPreviousEventHash() {
        return previousEventHash;
    }

    public void setPreviousEventHash(String previousEventHash) {
        this.previousEventHash = previousEventHash;
    }

    public String getEventHash() {
        return eventHash;
    }

    public void setEventHash(String eventHash) {
        this.eventHash = eventHash;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt != null ? occurredAt.truncatedTo(java.time.temporal.ChronoUnit.MILLIS) : null;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(Instant recordedAt) {
        this.recordedAt = recordedAt;
    }
}
