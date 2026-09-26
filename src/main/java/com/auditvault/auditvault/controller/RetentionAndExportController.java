package com.auditvault.auditvault.controller;

import com.auditvault.auditvault.service.RetentionService;
import com.auditvault.auditvault.service.RedactionService;
import com.auditvault.auditvault.service.BulkExportService;
import tools.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for Scenario B Features: Retention, Redaction, and Bulk Export
 * 
 * Endpoints:
 * - POST /audit/retention/archive - Archive old records
 * - POST /audit/retention/restore/{id} - Restore archived record
 * - GET /audit/retention/status - Get retention statistics
 * - POST /audit/redact/{id} - Redact sensitive fields
 * - GET /audit/redaction/{id} - Get redaction metadata
 * - GET /audit/export/actor/{actorId} - Export by actor
 * - GET /audit/export/resource/{resourceId} - Export by resource
 */
@RestController
@RequestMapping("/audit")
@CrossOrigin(origins = "*")
@Slf4j
@Tag(name = "Retention, Redaction & Export", description = "APIs for data retention, field redaction, and bulk export operations")
public class RetentionAndExportController {

    private final RetentionService retentionService;
    private final RedactionService redactionService;
    private final BulkExportService bulkExportService;

    public RetentionAndExportController(
            RetentionService retentionService,
            RedactionService redactionService,
            BulkExportService bulkExportService) {
        this.retentionService = retentionService;
        this.redactionService = redactionService;
        this.bulkExportService = bulkExportService;
    }

    /**
     * Scenario B: Retention Policy
     * Archive records older than configured retention window
     * 
     * @return Number of records archived
     */
    @PostMapping("/retention/archive")
    @Operation(summary = "Archive old records", description = "Archives audit log records older than the configured retention window")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Records archived successfully"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Map<String, Object>> archiveOldRecords() {
        log.info("POST /audit/retention/archive - Archiving old records");

        long archived = retentionService.archiveOldRecords();

        return ResponseEntity.ok(Map.of(
            "archivedCount", archived,
            "message", "Successfully archived " + archived + " records"
        ));
    }

    /**
     * Restore an archived record
     * 
     * @param recordId The ID of the record to restore
     * @return Success status
     */
    @PostMapping("/retention/restore/{recordId}")
    @Operation(summary = "Restore archived record", description = "Restores a previously archived audit log record to active status")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Record restored successfully"),
        @ApiResponse(responseCode = "404", description = "Record not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Map<String, Object>> restoreArchivedRecord(
            @Parameter(description = "Record ID to restore", required = true, example = "1")
            @PathVariable Long recordId) {
        log.info("POST /audit/retention/restore/{} - Restoring archived record", recordId);

        boolean restored = retentionService.restoreArchivedRecord(recordId);

        return ResponseEntity.ok(Map.of(
            "recordId", recordId,
            "restored", restored,
            "message", restored ? "Record restored successfully" : "Record not found"
        ));
    }

    /**
     * Get retention policy status and statistics
     * 
     * @return Retention configuration and record counts
     */
    @GetMapping("/retention/status")
    @Operation(summary = "Get retention status", description = "Retrieves retention policy configuration and current record statistics")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Retention status retrieved successfully"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Map<String, Object>> getRetentionStatus() {
        log.info("GET /audit/retention/status - Getting retention status");

        return ResponseEntity.ok(Map.of(
            "retentionWindowDays", retentionService.getRetentionWindow(),
            "activeRecords", retentionService.getActiveRecordCount(),
            "archivedRecords", retentionService.getArchivedRecordCount(),
            "totalRecords", retentionService.getActiveRecordCount() + retentionService.getArchivedRecordCount()
        ));
    }

    /**
     * Scenario B: Structured Redaction
     * Redact sensitive fields from a record without breaking hash chain
     * 
     * @param recordId The ID of the record to redact
     * @param request Request containing list of fields to redact
     * @return Success status
     */
    @PostMapping("/redact/{recordId}")
    @Operation(summary = "Redact sensitive fields", description = "Redacts specified sensitive fields from an audit log record while maintaining hash chain integrity")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Fields redacted successfully"),
        @ApiResponse(responseCode = "404", description = "Record not found"),
        @ApiResponse(responseCode = "400", description = "Invalid redaction request"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Map<String, Object>> redactSensitiveFields(
            @Parameter(description = "Record ID to redact", required = true, example = "1")
            @PathVariable Long recordId,
            @Parameter(description = "Redaction request with field paths", required = true, schema = @Schema(implementation = RedactRequest.class))
            @RequestBody RedactRequest request) {

        log.info("POST /audit/redact/{} - Redacting {} fields", recordId, request.getFieldsToRedact().size());

        boolean redacted = redactionService.redactSensitiveFields(recordId, request.getFieldsToRedact());

        return ResponseEntity.ok(Map.of(
            "recordId", recordId,
            "redacted", redacted,
            "fieldsRedacted", request.getFieldsToRedact().size(),
            "message", redacted ? "Fields redacted successfully" : "Record not found"
        ));
    }

    /**
     * Get redaction metadata for a record
     * 
     * @param recordId The ID of the record
     * @return Redaction metadata (without revealing original values)
     */
    @GetMapping("/redaction/{recordId}")
    @Operation(summary = "Get redaction metadata", description = "Retrieves redaction metadata for a record without revealing the original redacted values")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Redaction metadata retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Record not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Map<String, Object>> getRedactionMetadata(
            @Parameter(description = "Record ID", required = true, example = "1")
            @PathVariable Long recordId) {
        log.info("GET /audit/redaction/{} - Getting redaction metadata", recordId);

        JsonNode metadata = redactionService.getRedactionMetadata(recordId);

        return ResponseEntity.ok(Map.of(
            "recordId", recordId,
            "redactionMetadata", metadata != null ? metadata : "No redaction data"
        ));
    }

    /**
     * Scenario B: Bulk Export
     * Export all records for a given actorId as a verifiable bundle
     * 
     * @param actorId The actor ID to export
     * @return Verifiable export bundle
     */
    @GetMapping("/export/actor/{actorId}")
    @Operation(summary = "Export by actor", description = "Exports all audit log records for a specific actor as a verifiable bundle with metadata and verification instructions")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Export bundle generated successfully",
            content = @Content(schema = @Schema(type = "object", example = "{\"metadata\":{},\"records\":[]}"))),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<JsonNode> exportByActorId(
            @Parameter(description = "Actor ID to export", required = true, example = "user123")
            @PathVariable String actorId) {
        log.info("GET /audit/export/actor/{} - Exporting records by actor", actorId);

        JsonNode bundle = bulkExportService.exportByActorId(actorId);

        return ResponseEntity.ok(bundle);
    }

    /**
     * Export all records for a given resourceId as a verifiable bundle
     * 
     * @param resourceId The resource ID to export
     * @return Verifiable export bundle
     */
    @GetMapping("/export/resource/{resourceId}")
    @Operation(summary = "Export by resource", description = "Exports all audit log records for a specific resource as a verifiable bundle with metadata and verification instructions")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Export bundle generated successfully",
            content = @Content(schema = @Schema(type = "object", example = "{\"metadata\":{},\"records\":[]}"))),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<JsonNode> exportByResourceId(
            @Parameter(description = "Resource ID to export", required = true, example = "ACC001")
            @PathVariable String resourceId) {
        log.info("GET /audit/export/resource/{} - Exporting records by resource", resourceId);

        JsonNode bundle = bulkExportService.exportByResourceId(resourceId);

        return ResponseEntity.ok(bundle);
    }

    /**
     * DTO for redaction request
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @Schema(description = "Request body for redacting sensitive fields from an audit log record")
    public static class RedactRequest {
        @Schema(description = "List of field paths to redact (e.g., 'payload.ssn', 'payload.email')", required = true, example = "[\"payload.ssn\", \"payload.email\"]")
        private List<String> fieldsToRedact;
    }
}
