package com.cse444.threadtrace.lineage.infrastructure;

import com.cse444.threadtrace.lineage.domain.model.LotEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LotRepository extends JpaRepository<LotEntity, UUID> {
    Optional<LotEntity> findByLotNumber(String lotNumber);
    boolean existsByLotNumber(String lotNumber);
    Page<LotEntity> findByBrandId(UUID brandId, Pageable pageable);
}
