package com.cse444.threadtrace.registry;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "parties")
public class PartyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "party_type", nullable = false, length = 50)
    private PartyType partyType;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "external_code", nullable = false, unique = true, length = 100)
    private String externalCode;

    @Column(name = "country_code", nullable = false, length = 10)
    private String countryCode;

    @Column(name = "exact_address", length = 500)
    private String exactAddress;

    @Column(name = "worker_count")
    private Integer workerCount;

    @Column(name = "unit_price_bdt", precision = 15, scale = 4)
    private BigDecimal unitPriceBdt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public PartyEntity() {
    }

    public PartyEntity(PartyType partyType, String name, String externalCode, String countryCode) {
        this.partyType = partyType;
        this.name = name;
        this.externalCode = externalCode;
        this.countryCode = countryCode;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public PartyType getPartyType() {
        return partyType;
    }

    public void setPartyType(PartyType partyType) {
        this.partyType = partyType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getExternalCode() {
        return externalCode;
    }

    public void setExternalCode(String externalCode) {
        this.externalCode = externalCode;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getExactAddress() {
        return exactAddress;
    }

    public void setExactAddress(String exactAddress) {
        this.exactAddress = exactAddress;
    }

    public Integer getWorkerCount() {
        return workerCount;
    }

    public void setWorkerCount(Integer workerCount) {
        this.workerCount = workerCount;
    }

    public BigDecimal getUnitPriceBdt() {
        return unitPriceBdt;
    }

    public void setUnitPriceBdt(BigDecimal unitPriceBdt) {
        this.unitPriceBdt = unitPriceBdt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
