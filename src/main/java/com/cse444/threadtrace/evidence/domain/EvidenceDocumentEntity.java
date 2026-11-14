package com.cse444.threadtrace.evidence.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "evidence_documents")
public class EvidenceDocumentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "certificate_type", nullable = false, length = 50)
    private String certificateType;

    @Column(name = "issuer_name", nullable = false)
    private String issuerName;

    @Column(name = "scope_description", columnDefinition = "TEXT")
    private String scopeDescription;

    @Column(name = "claim_type", length = 100)
    private String claimType;

    @Column(name = "certified_percentage", precision = 5, scale = 2)
    private BigDecimal certifiedPercentage = BigDecimal.ZERO;

    @Column(name = "storage_object_key", nullable = false, length = 500)
    private String storageObjectKey;

    @Column(name = "file_sha256", nullable = false, length = 64)
    private String fileSha256;

    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    @Column(name = "valid_until", nullable = false)
    private LocalDate validUntil;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private EvidenceStatus status = EvidenceStatus.ACTIVE;

    @Column(name = "party_id")
    private UUID partyId;

    @Column(name = "lot_id")
    private UUID lotId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public EvidenceDocumentEntity() {}

    public EvidenceDocumentEntity(
            String certificateType,
            String issuerName,
            String scopeDescription,
            String claimType,
            BigDecimal certifiedPercentage,
            String storageObjectKey,
            String fileSha256,
            LocalDate validFrom,
            LocalDate validUntil,
            UUID partyId,
            UUID lotId
    ) {
        this.certificateType = certificateType;
        this.issuerName = issuerName;
        this.scopeDescription = scopeDescription;
        this.claimType = claimType;
        this.certifiedPercentage = certifiedPercentage != null ? certifiedPercentage : BigDecimal.ZERO;
        this.storageObjectKey = storageObjectKey;
        this.fileSha256 = fileSha256;
        this.validFrom = validFrom;
        this.validUntil = validUntil;
        this.partyId = partyId;
        this.lotId = lotId;
        this.status = EvidenceStatus.ACTIVE;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getCertificateType() {
        return certificateType;
    }

    public void setCertificateType(String certificateType) {
        this.certificateType = certificateType;
    }

    public String getIssuerName() {
        return issuerName;
    }

    public void setIssuerName(String issuerName) {
        this.issuerName = issuerName;
    }

    public String getScopeDescription() {
        return scopeDescription;
    }

    public void setScopeDescription(String scopeDescription) {
        this.scopeDescription = scopeDescription;
    }

    public String getClaimType() {
        return claimType;
    }

    public void setClaimType(String claimType) {
        this.claimType = claimType;
    }

    public BigDecimal getCertifiedPercentage() {
        return certifiedPercentage;
    }

    public void setCertifiedPercentage(BigDecimal certifiedPercentage) {
        this.certifiedPercentage = certifiedPercentage;
    }

    public String getStorageObjectKey() {
        return storageObjectKey;
    }

    public void setStorageObjectKey(String storageObjectKey) {
        this.storageObjectKey = storageObjectKey;
    }

    public String getFileSha256() {
        return fileSha256;
    }

    public void setFileSha256(String fileSha256) {
        this.fileSha256 = fileSha256;
    }

    public LocalDate getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(LocalDate validFrom) {
        this.validFrom = validFrom;
    }

    public LocalDate getValidUntil() {
        return validUntil;
    }

    public void setValidUntil(LocalDate validUntil) {
        this.validUntil = validUntil;
    }

    public EvidenceStatus getStatus() {
        return status;
    }

    public void setStatus(EvidenceStatus status) {
        this.status = status;
    }

    public UUID getPartyId() {
        return partyId;
    }

    public void setPartyId(UUID partyId) {
        this.partyId = partyId;
    }

    public UUID getLotId() {
        return lotId;
    }

    public void setLotId(UUID lotId) {
        this.lotId = lotId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
