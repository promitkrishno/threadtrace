package com.cse444.threadtrace.evidence;

import com.cse444.threadtrace.evidence.dto.EvidenceResponse;
import com.cse444.threadtrace.evidence.dto.UploadEvidenceRequest;
import com.cse444.threadtrace.evidence.observer.EvidenceExpiryScheduler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/evidence")
@Tag(name = "Evidence Management", description = "Upload and verify compliance certificates and audit reports")
public class EvidenceController {

    private final EvidenceService evidenceService;
    private final EvidenceExpiryScheduler expiryScheduler;

    public EvidenceController(
            EvidenceService evidenceService,
            EvidenceExpiryScheduler expiryScheduler
    ) {
        this.evidenceService = evidenceService;
        this.expiryScheduler = expiryScheduler;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload and attach certificate evidence document (PDF/image)")
    public ResponseEntity<EvidenceResponse> uploadEvidence(
            @RequestPart("file") MultipartFile file,
            @RequestPart("request") @Valid UploadEvidenceRequest request
    ) throws IOException {
        byte[] bytes = file.getBytes();
        EvidenceResponse response = evidenceService.uploadAndAttach(
                request,
                bytes,
                file.getOriginalFilename(),
                file.getContentType()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get evidence document metadata")
    public ResponseEntity<EvidenceResponse> getEvidence(@PathVariable UUID id) {
        return ResponseEntity.ok(evidenceService.getEvidence(id));
    }

    @GetMapping("/{id}/download")
    @Operation(summary = "Download raw evidence document file")
    public ResponseEntity<byte[]> downloadEvidence(@PathVariable UUID id) {
        EvidenceResponse meta = evidenceService.getEvidence(id);
        byte[] fileBytes = evidenceService.downloadEvidenceFile(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + id + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(fileBytes);
    }

    @GetMapping("/lot/{lotId}")
    @Operation(summary = "List all evidence documents attached to a lot")
    public ResponseEntity<List<EvidenceResponse>> getEvidenceForLot(@PathVariable UUID lotId) {
        return ResponseEntity.ok(evidenceService.getEvidenceForLot(lotId));
    }

    @GetMapping("/party/{partyId}")
    @Operation(summary = "List all evidence documents attached to a supplier/facility")
    public ResponseEntity<List<EvidenceResponse>> getEvidenceForParty(@PathVariable UUID partyId) {
        return ResponseEntity.ok(evidenceService.getEvidenceForParty(partyId));
    }

    @PostMapping("/check-expiry")
    @Operation(summary = "Trigger on-demand check for expiring and expired certificates")
    public ResponseEntity<EvidenceExpiryScheduler.ExpiryCheckResult> triggerExpiryCheck() {
        return ResponseEntity.ok(expiryScheduler.checkAndPublishExpiryEvents());
    }
}
