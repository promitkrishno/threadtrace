package com.cse444.threadtrace.lineage.application;

import com.cse444.threadtrace.common.error.DomainException;
import com.cse444.threadtrace.lineage.domain.model.LotEntity;
import com.cse444.threadtrace.lineage.domain.model.LotEventEntity;
import com.cse444.threadtrace.lineage.infrastructure.LotEventRepository;
import com.cse444.threadtrace.lineage.infrastructure.LotRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/lots")
@Tag(name = "Lot Lineage & Events", description = "Immutable event sourcing, lot splits/merges, and tamper verification (FR2, FR10)")
public class LotController {

    private final RecordLotEventUseCase recordLotEventUseCase;
    private final LotRepository lotRepository;
    private final LotEventRepository lotEventRepository;

    public LotController(
            RecordLotEventUseCase recordLotEventUseCase,
            LotRepository lotRepository,
            LotEventRepository lotEventRepository
    ) {
        this.recordLotEventUseCase = recordLotEventUseCase;
        this.lotRepository = lotRepository;
        this.lotEventRepository = lotEventRepository;
    }

    @PostMapping("/events")
    @Operation(summary = "Record a lot transformation event (RECEIVE, CUT, SEW, WASH, DYE, FINISH, PACK, SHIP, CORRECTION)")
    public ResponseEntity<RecordEventResult> recordEvent(@Valid @RequestBody RecordEventCommand command) {
        RecordEventResult result = recordLotEventUseCase.recordEvent(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping
    @Operation(summary = "List all lots with pagination")
    public ResponseEntity<Page<LotEntity>> listLots(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(lotRepository.findAll(pageable));
    }

    @GetMapping("/{lotId}")
    @Operation(summary = "Get lot details by ID")
    public ResponseEntity<LotEntity> getLot(@PathVariable UUID lotId) {
        LotEntity lot = lotRepository.findById(lotId)
                .orElseThrow(() -> new DomainException("LOT_NOT_FOUND", "Lot not found with id: " + lotId));
        return ResponseEntity.ok(lot);
    }

    @GetMapping("/{lotId}/events")
    @Operation(summary = "Get chronological immutable event log for a lot")
    public ResponseEntity<List<LotEventEntity>> getLotEvents(@PathVariable UUID lotId) {
        return ResponseEntity.ok(lotEventRepository.findByLotIdOrderBySequenceNoAsc(lotId));
    }

    @GetMapping("/{lotId}/verify-integrity")
    @Operation(summary = "Verify cryptographic SHA-256 hash chain integrity of a lot (FR10)")
    public ResponseEntity<IntegrityVerificationResult> verifyIntegrity(@PathVariable UUID lotId) {
        IntegrityVerificationResult result = recordLotEventUseCase.verifyLotIntegrity(lotId);
        return ResponseEntity.ok(result);
    }
}
