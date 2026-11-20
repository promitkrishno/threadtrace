package com.cse444.threadtrace.passport.web;

import com.cse444.threadtrace.passport.DigitalPassportService;
import com.cse444.threadtrace.passport.projection.AuditorPassportView;
import com.cse444.threadtrace.passport.projection.BrandPassportView;
import com.cse444.threadtrace.passport.projection.PublicPassportView;
import com.cse444.threadtrace.passport.proxy.PassportAccessPort;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/passports")
@Tag(name = "Digital Product Passports", description = "CQRS Digital Product Passports with Audience Security Views (FR7, FR8)")
public class PassportApiController {

    private final PassportAccessPort passportAccessPort;
    private final DigitalPassportService passportService;

    public PassportApiController(PassportAccessPort passportAccessPort, DigitalPassportService passportService) {
        this.passportAccessPort = passportAccessPort;
        this.passportService = passportService;
    }

    public record GeneratePassportRequest(
            String styleCode,
            String shipmentCode,
            UUID brandId,
            UUID rootCartonLotId,
            Map<String, Object> customFields
    ) {}

    @PostMapping("/generate")
    @Operation(summary = "Generate a new or updated version of a Digital Product Passport")
    public ResponseEntity<PublicPassportView> generatePassport(@RequestBody GeneratePassportRequest request) {
        var result = passportService.generatePassport(new DigitalPassportService.GeneratePassportCommand(
                request.styleCode(),
                request.shipmentCode(),
                request.brandId(),
                request.rootCartonLotId(),
                request.customFields()
        ));
        return ResponseEntity.status(HttpStatus.CREATED).body(result.publicView());
    }

    @GetMapping("/{qrToken}")
    @Operation(summary = "Get public Digital Product Passport with ETag and Cache-Control headers (FR7, QA4)")
    public ResponseEntity<PublicPassportView> getPublicPassport(
            @PathVariable String qrToken,
            @RequestParam(required = false) Integer version,
            @RequestHeader(value = HttpHeaders.IF_NONE_MATCH, required = false) String ifNoneMatch
    ) {
        String etag = passportService.getEtag(qrToken, version);
        String quotedEtag = "\"" + etag + "\"";

        if (ifNoneMatch != null && (ifNoneMatch.equals(quotedEtag) || ifNoneMatch.equals(etag))) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                    .eTag(etag)
                    .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
                    .build();
        }

        PublicPassportView view = passportAccessPort.getPublicPassport(qrToken, version);
        return ResponseEntity.ok()
                .eTag(etag)
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
                .body(view);
    }

    @GetMapping("/{qrToken}/qr")
    @Operation(summary = "Get ZXing-rendered QR Code image (PNG) for public passport URL")
    public ResponseEntity<byte[]> getQrCode(
            @PathVariable String qrToken,
            @RequestParam(required = false) Integer version
    ) {
        byte[] qrBytes = passportService.getQrCodePng(qrToken, version);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                .body(qrBytes);
    }

    @GetMapping("/{qrToken}/brand")
    @Operation(summary = "Get Brand audience passport view (requires ROLE_BRAND and tenant match)")
    public ResponseEntity<BrandPassportView> getBrandPassport(
            @PathVariable String qrToken,
            @RequestParam(required = false) Integer version,
            Principal principal
    ) {
        String callerIdentity = principal != null ? principal.getName() : null;
        return ResponseEntity.ok(passportAccessPort.getBrandPassport(qrToken, version, callerIdentity));
    }

    @GetMapping("/{qrToken}/auditor")
    @Operation(summary = "Get Auditor audience passport view with confidential data and mass-balance checks (requires ROLE_AUDITOR)")
    public ResponseEntity<AuditorPassportView> getAuditorPassport(
            @PathVariable String qrToken,
            @RequestParam(required = false) Integer version
    ) {
        return ResponseEntity.ok(passportAccessPort.getAuditorPassport(qrToken, version));
    }
}
