package com.cse444.threadtrace.lineage.domain.model;

import com.cse444.threadtrace.lineage.domain.state.LotState;
import com.cse444.threadtrace.lineage.domain.state.LotStateFactory;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "lots")
public class LotEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "lot_number", nullable = false, unique = true, length = 100)
    private String lotNumber;

    @Column(name = "brand_id", nullable = false)
    private UUID brandId;

    @Column(name = "current_facility_id", nullable = false)
    private UUID currentFacilityId;

    @Column(name = "style_code", length = 100)
    private String styleCode;

    @Column(name = "shipment_code", length = 100)
    private String shipmentCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private LotStatus status;

    @Column(name = "current_quantity", nullable = false, precision = 15, scale = 4)
    private BigDecimal currentQuantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Unit unit;

    @Column(name = "certified_recycled_pct", precision = 5, scale = 2)
    private BigDecimal certifiedRecycledPct = BigDecimal.ZERO;

    @Column(name = "certified_organic_pct", precision = 5, scale = 2)
    private BigDecimal certifiedOrganicPct = BigDecimal.ZERO;

    @Column(name = "accumulated_water_l", precision = 15, scale = 4)
    private BigDecimal accumulatedWaterL = BigDecimal.ZERO;

    @Column(name = "accumulated_energy_kwh", precision = 15, scale = 4)
    private BigDecimal accumulatedEnergyKwh = BigDecimal.ZERO;

    @Column(name = "accumulated_co2e_kg", precision = 15, scale = 4)
    private BigDecimal accumulatedCo2eKg = BigDecimal.ZERO;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public LotEntity() {
    }

    public LotEntity(
            String lotNumber,
            UUID brandId,
            UUID currentFacilityId,
            String styleCode,
            String shipmentCode,
            LotStatus status,
            BigDecimal currentQuantity,
            Unit unit
    ) {
        this.lotNumber = lotNumber;
        this.brandId = brandId;
        this.currentFacilityId = currentFacilityId;
        this.styleCode = styleCode;
        this.shipmentCode = shipmentCode;
        this.status = status != null ? status : LotStatus.ACTIVE;
        this.currentQuantity = currentQuantity;
        this.unit = unit;
        this.createdAt = Instant.now();
    }

    public LotState getState() {
        return LotStateFactory.from(this.status);
    }

    public void validateCanRecordEvent(EventType eventType) {
        getState().validateCanRecordEvent(eventType);
    }

    public void updateStatusForEvent(EventType eventType, boolean isFullyConsumed) {
        this.status = getState().determineNextStatus(eventType, isFullyConsumed);
    }

    // Getters and Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getLotNumber() {
        return lotNumber;
    }

    public void setLotNumber(String lotNumber) {
        this.lotNumber = lotNumber;
    }

    public UUID getBrandId() {
        return brandId;
    }

    public void setBrandId(UUID brandId) {
        this.brandId = brandId;
    }

    public UUID getCurrentFacilityId() {
        return currentFacilityId;
    }

    public void setCurrentFacilityId(UUID currentFacilityId) {
        this.currentFacilityId = currentFacilityId;
    }

    public String getStyleCode() {
        return styleCode;
    }

    public void setStyleCode(String styleCode) {
        this.styleCode = styleCode;
    }

    public String getShipmentCode() {
        return shipmentCode;
    }

    public void setShipmentCode(String shipmentCode) {
        this.shipmentCode = shipmentCode;
    }

    public LotStatus getStatus() {
        return status;
    }

    public void setStatus(LotStatus status) {
        this.status = status;
    }

    public BigDecimal getCurrentQuantity() {
        return currentQuantity;
    }

    public void setCurrentQuantity(BigDecimal currentQuantity) {
        this.currentQuantity = currentQuantity;
    }

    public Unit getUnit() {
        return unit;
    }

    public void setUnit(Unit unit) {
        this.unit = unit;
    }

    public BigDecimal getCertifiedRecycledPct() {
        return certifiedRecycledPct;
    }

    public void setCertifiedRecycledPct(BigDecimal certifiedRecycledPct) {
        this.certifiedRecycledPct = certifiedRecycledPct;
    }

    public BigDecimal getCertifiedOrganicPct() {
        return certifiedOrganicPct;
    }

    public void setCertifiedOrganicPct(BigDecimal certifiedOrganicPct) {
        this.certifiedOrganicPct = certifiedOrganicPct;
    }

    public BigDecimal getAccumulatedWaterL() {
        return accumulatedWaterL;
    }

    public void setAccumulatedWaterL(BigDecimal accumulatedWaterL) {
        this.accumulatedWaterL = accumulatedWaterL;
    }

    public BigDecimal getAccumulatedEnergyKwh() {
        return accumulatedEnergyKwh;
    }

    public void setAccumulatedEnergyKwh(BigDecimal accumulatedEnergyKwh) {
        this.accumulatedEnergyKwh = accumulatedEnergyKwh;
    }

    public BigDecimal getAccumulatedCo2eKg() {
        return accumulatedCo2eKg;
    }

    public void setAccumulatedCo2eKg(BigDecimal accumulatedCo2eKg) {
        this.accumulatedCo2eKg = accumulatedCo2eKg;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
