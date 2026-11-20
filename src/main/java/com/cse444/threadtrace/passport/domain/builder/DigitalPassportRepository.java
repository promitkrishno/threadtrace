package com.cse444.threadtrace.passport.domain.builder;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DigitalPassportRepository extends JpaRepository<DigitalPassportEntity, UUID> {

    Optional<DigitalPassportEntity> findByQrCodeTokenAndIsLatestTrue(String qrCodeToken);

    Optional<DigitalPassportEntity> findByQrCodeTokenAndVersionNo(String qrCodeToken, int versionNo);

    Optional<DigitalPassportEntity> findByStyleCodeAndShipmentCodeAndIsLatestTrue(String styleCode, String shipmentCode);

    Optional<DigitalPassportEntity> findTopByStyleCodeAndShipmentCodeOrderByVersionNoDesc(String styleCode, String shipmentCode);

    List<DigitalPassportEntity> findByStyleCodeAndShipmentCodeOrderByVersionNoAsc(String styleCode, String shipmentCode);

    List<DigitalPassportEntity> findByBrandId(UUID brandId);
}
