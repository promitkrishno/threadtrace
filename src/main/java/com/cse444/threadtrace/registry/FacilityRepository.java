package com.cse444.threadtrace.registry;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FacilityRepository extends JpaRepository<FacilityEntity, UUID> {
    Page<FacilityEntity> findByPartyId(UUID partyId, Pageable pageable);
    List<FacilityEntity> findByParentId(UUID parentId);
}
