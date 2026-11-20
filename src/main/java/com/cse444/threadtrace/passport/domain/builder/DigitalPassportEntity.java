package com.cse444.threadtrace.passport.domain.builder;

import jakarta.persistence.*;
import org.hibernate.type.SqlTypes;
import org.hibernate.annotations.JdbcTypeCode;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "digital_passports")
public class DigitalPassportEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "style_code", nullable = false, length = 100)
    private String styleCode;

    @Column(name = "shipment_code", nullable = false, length = 100)
    private String shipmentCode;

    @Column(name = "brand_id", nullable = false)
    private UUID brandId;

    @Column(name = "root_carton_lot_id", nullable = false)
    private UUID rootCartonLotId;

    @Column(name = "version_no", nullable = false)
    private int versionNo = 1;

    @Column(name = "is_latest", nullable = false)
    private boolean isLatest = true;

    @Column(name = "qr_code_token", nullable = false, unique = true, length = 255)
    private String qrCodeToken;

    @Column(name = "etag_hash", nullable = false, length = 64)
    private String etagHash;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "public_payload", nullable = false)
    private String publicPayload;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "brand_payload")
    private String brandPayload;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "auditor_payload")
    private String auditorPayload;

    @Column(name = "published_at", nullable = false)
    private Instant publishedAt = Instant.now();

    public DigitalPassportEntity() {}

    public DigitalPassportEntity(
            String styleCode,
            String shipmentCode,
            UUID brandId,
            UUID rootCartonLotId,
            int versionNo,
            boolean isLatest,
            String qrCodeToken,
            String etagHash,
            String publicPayload,
            String brandPayload,
            String auditorPayload
    ) {
        this.styleCode = styleCode;
        this.shipmentCode = shipmentCode;
        this.brandId = brandId;
        this.rootCartonLotId = rootCartonLotId;
        this.versionNo = versionNo;
        this.isLatest = isLatest;
        this.qrCodeToken = qrCodeToken;
        this.etagHash = etagHash;
        this.publicPayload = publicPayload;
        this.brandPayload = brandPayload;
        this.auditorPayload = auditorPayload;
        this.publishedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getStyleCode() {
        return styleCode;
    }

    public String getShipmentCode() {
        return shipmentCode;
    }

    public UUID getBrandId() {
        return brandId;
    }

    public UUID getRootCartonLotId() {
        return rootCartonLotId;
    }

    public int getVersionNo() {
        return versionNo;
    }

    public void setVersionNo(int versionNo) {
        this.versionNo = versionNo;
    }

    public boolean isLatest() {
        return isLatest;
    }

    public void setLatest(boolean latest) {
        isLatest = latest;
    }

    public String getQrCodeToken() {
        return qrCodeToken;
    }

    public String getEtagHash() {
        return etagHash;
    }

    public void setEtagHash(String etagHash) {
        this.etagHash = etagHash;
    }

    public String getPublicPayload() {
        return publicPayload;
    }

    public void setPublicPayload(String publicPayload) {
        this.publicPayload = publicPayload;
    }

    public String getBrandPayload() {
        return brandPayload;
    }

    public void setBrandPayload(String brandPayload) {
        this.brandPayload = brandPayload;
    }

    public String getAuditorPayload() {
        return auditorPayload;
    }

    public void setAuditorPayload(String auditorPayload) {
        this.auditorPayload = auditorPayload;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(Instant publishedAt) {
        this.publishedAt = publishedAt;
    }
}
