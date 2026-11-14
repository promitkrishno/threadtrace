package com.cse444.threadtrace.registry;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Registry", description = "Party & Facility Registry management (FR1)")
public class RegistryController {

    private final RegistryService registryService;

    public RegistryController(RegistryService registryService) {
        this.registryService = registryService;
    }

    @PostMapping("/parties")
    @Operation(summary = "Register a supply-chain party (brand, mill, factory, supplier)")
    public ResponseEntity<PartyResponse> registerParty(@Valid @RequestBody CreatePartyRequest request) {
        PartyResponse response = registryService.registerParty(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/parties")
    @Operation(summary = "List registered parties with pagination and optional type filter")
    public ResponseEntity<Page<PartyResponse>> listParties(
            @RequestParam(required = false) PartyType partyType,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(registryService.listParties(partyType, pageable));
    }

    @GetMapping("/parties/{id}")
    @Operation(summary = "Get party details by ID")
    public ResponseEntity<PartyResponse> getParty(@PathVariable UUID id) {
        return ResponseEntity.ok(registryService.getParty(id));
    }

    @PostMapping("/facilities")
    @Operation(summary = "Register a facility or production line")
    public ResponseEntity<FacilityResponse> registerFacility(@Valid @RequestBody CreateFacilityRequest request) {
        FacilityResponse response = registryService.registerFacility(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/facilities")
    @Operation(summary = "List registered facilities with pagination and optional party filter")
    public ResponseEntity<Page<FacilityResponse>> listFacilities(
            @RequestParam(required = false) UUID partyId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(registryService.listFacilities(partyId, pageable));
    }

    @GetMapping("/facilities/{id}")
    @Operation(summary = "Get facility details by ID")
    public ResponseEntity<FacilityResponse> getFacility(@PathVariable UUID id) {
        return ResponseEntity.ok(registryService.getFacility(id));
    }
}
