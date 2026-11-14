package com.cse444.threadtrace.registry;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PartyRepository extends JpaRepository<PartyEntity, UUID> {
    Optional<PartyEntity> findByExternalCode(String externalCode);
    boolean existsByExternalCode(String externalCode);
    Page<PartyEntity> findByPartyType(PartyType partyType, Pageable pageable);
}
