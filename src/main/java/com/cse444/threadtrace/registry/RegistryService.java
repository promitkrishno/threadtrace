package com.cse444.threadtrace.registry;

import com.cse444.threadtrace.common.error.DomainException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class RegistryService {

    private final PartyRepository partyRepository;
    private final FacilityRepository facilityRepository;

    public RegistryService(PartyRepository partyRepository, FacilityRepository facilityRepository) {
        this.partyRepository = partyRepository;
        this.facilityRepository = facilityRepository;
    }

    @Transactional
    public PartyResponse registerParty(CreatePartyRequest request) {
        if (partyRepository.existsByExternalCode(request.externalCode())) {
            throw new DomainException("PARTY_EXISTS", "Party with external code '" + request.externalCode() + "' already exists");
        }
        PartyEntity entity = new PartyEntity(
                request.partyType(),
                request.name(),
                request.externalCode(),
                request.countryCode()
        );
        entity.setExactAddress(request.exactAddress());
        entity.setWorkerCount(request.workerCount());
        entity.setUnitPriceBdt(request.unitPriceBdt());

        PartyEntity saved = partyRepository.save(entity);
        return PartyResponse.fromEntity(saved);
    }

    public Page<PartyResponse> listParties(PartyType partyType, Pageable pageable) {
        if (partyType != null) {
            return partyRepository.findByPartyType(partyType, pageable).map(PartyResponse::fromEntity);
        }
        return partyRepository.findAll(pageable).map(PartyResponse::fromEntity);
    }

    public PartyResponse getParty(UUID id) {
        PartyEntity entity = partyRepository.findById(id)
                .orElseThrow(() -> new DomainException("PARTY_NOT_FOUND", "Party not found with id: " + id));
        return PartyResponse.fromEntity(entity);
    }

    @Transactional
    public FacilityResponse registerFacility(CreateFacilityRequest request) {
        PartyEntity party = partyRepository.findById(request.partyId())
                .orElseThrow(() -> new DomainException("PARTY_NOT_FOUND", "Party not found with id: " + request.partyId()));

        FacilityEntity parent = null;
        if (request.parentId() != null) {
            parent = facilityRepository.findById(request.parentId())
                    .orElseThrow(() -> new DomainException("PARENT_FACILITY_NOT_FOUND", "Parent facility not found with id: " + request.parentId()));
        }

        FacilityEntity facility = new FacilityEntity(
                party,
                request.facilityType(),
                parent,
                request.name(),
                request.locationCity()
        );

        FacilityEntity saved = facilityRepository.save(facility);
        return FacilityResponse.fromEntity(saved);
    }

    public Page<FacilityResponse> listFacilities(UUID partyId, Pageable pageable) {
        if (partyId != null) {
            return facilityRepository.findByPartyId(partyId, pageable).map(FacilityResponse::fromEntity);
        }
        return facilityRepository.findAll(pageable).map(FacilityResponse::fromEntity);
    }

    public FacilityResponse getFacility(UUID id) {
        FacilityEntity entity = facilityRepository.findById(id)
                .orElseThrow(() -> new DomainException("FACILITY_NOT_FOUND", "Facility not found with id: " + id));
        return FacilityResponse.fromEntity(entity);
    }
}
