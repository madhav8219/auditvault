package com.auditvault.auditvault.controller;

import com.auditvault.auditvault.dto.VerificationResult;
import com.auditvault.auditvault.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for Hash Chain Verification
 * 
 * Endpoints:
 * - GET /audit/verify - Verify the entire hash chain integrity
 */
@RestController
@RequestMapping("/audit")
@CrossOrigin(origins = "*")
@Slf4j
@Tag(name = "Chain Verification", description = "APIs for verifying hash chain integrity and detecting tampering")
public class ChainVerificationController {

    private final AuditLogService auditLogService;

    public ChainVerificationController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    /**
     * Chain Verification Endpoint
     * 
     * Walks the entire hash chain and reports:
     * - Whether the chain is intact
     * - If broken: which record is the first inconsistency
     * - The type of violation detected (PREVIOUS_HASH_MISMATCH or CONTENT_HASH_INVALID)
     * 
     * @return VerificationResult with chain status
     */
    @GetMapping("/verify")
    @Operation(summary = "Verify hash chain", description = "Verifies the integrity of the entire hash chain by walking through all audit logs and checking hash links")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Chain verification completed",
            content = @Content(schema = @Schema(implementation = VerificationResult.class))),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<VerificationResult> verifyChain() {
        log.info("GET /audit/verify - Starting hash chain verification");

        VerificationResult result = auditLogService.verifyChain();

        log.info("Chain verification complete: isIntact={}, message={}", 
            result.getIsIntact(), result.getMessage());

        return ResponseEntity.ok(result);
    }

    /**
     * Detect tampering by verifying the chain
     * Same as verify but used in context of detecting modifications
     * 
     * @return VerificationResult showing any tampering detected
     */
    @GetMapping("/detect-tampering")
    @Operation(summary = "Detect tampering", description = "Detects any tampering in the audit log hash chain by verifying cryptographic integrity")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Tampering detection completed",
            content = @Content(schema = @Schema(implementation = VerificationResult.class))),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<VerificationResult> detectTampering() {
        log.info("GET /audit/detect-tampering - Detecting chain tampering");

        VerificationResult result = auditLogService.detectTampering();

        if (!result.getIsIntact()) {
            log.warn("Tampering detected: violationType={}, recordId={}",
                result.getViolationType(), result.getFirstInconsistencyId());
        }

        return ResponseEntity.ok(result);
    }
}
