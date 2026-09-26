package com.auditvault.auditvault.controller;

import com.auditvault.auditvault.dto.AuditLogQueryFilter;
import com.auditvault.auditvault.dto.AuditLogResponse;
import com.auditvault.auditvault.dto.CreateAuditLogRequest;
import com.auditvault.auditvault.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * REST Controller for Audit Log API
 *
 * Endpoints:
 * - POST /audit/create - Create new audit log entry (Write API)
 * - GET /audit/query - Query audit logs with filtering (Query API)
 * - GET /audit/{id} - Retrieve single audit log
 * - GET /audit/count - Get total record count
 */
@RestController
@RequestMapping("/audit")
@CrossOrigin(origins = "*")
@Slf4j
@Tag(name = "Audit Log Management", description = "APIs for creating, querying, and managing tamper-evident audit logs")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    /**
     * Write API: Create a new audit log entry
     *
     * @param request The audit log creation request
     * @return Created audit log response
     */
    @PostMapping("/create")
    @Operation(summary = "Create audit log", description = "Creates a new tamper-evident audit log entry with hash chain linking")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Audit log created successfully",
                    content = @Content(schema = @Schema(implementation = AuditLogResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<AuditLogResponse> createAuditLog(
            @Parameter(description = "Audit log creation request", required = true, schema = @Schema(implementation = CreateAuditLogRequest.class))
            @Valid @RequestBody CreateAuditLogRequest request) {

        log.info("POST /audit/create - Creating audit log for event: {}", request.getEventType());

        AuditLogResponse response = auditLogService.createAuditLog(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Query API: Retrieve audit logs with filtering and pagination
     *
     * Supports filtering by:
     * - actorId
     * - resourceType & resourceId
     * - eventType
     * - Time range (fromTimestamp, toTimestamp)
     * - Pagination (pageNumber, pageSize)
     *
     * @param actorId Optional filter by actor ID
     * @param resourceType Optional filter by resource type
     * @param resourceId Optional filter by resource ID
     * @param eventType Optional filter by event type
     * @param fromTimestamp Optional start of time range
     * @param toTimestamp Optional end of time range
     * @param pageNumber Page number (0-based)
     * @param pageSize Number of records per page
     * @return Paginated list of audit logs
     */
    @GetMapping("/query")
    @Operation(summary = "Query audit logs", description = "Retrieve audit logs with optional filtering by actor, resource, event type, and time range. Supports pagination.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Audit logs retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid query parameters"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Page<AuditLogResponse>> queryAuditLogs(
            @Parameter(description = "Filter by actor ID")
            @RequestParam(required = false) String actorId,
            @Parameter(description = "Filter by resource type")
            @RequestParam(required = false) String resourceType,
            @Parameter(description = "Filter by resource ID")
            @RequestParam(required = false) String resourceId,
            @Parameter(description = "Filter by event type")
            @RequestParam(required = false) String eventType,
            @Parameter(description = "Start of time range (ISO format)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime fromTimestamp,
            @Parameter(description = "End of time range (ISO format)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime toTimestamp,
            @Parameter(description = "Page number (0-based, default: 0)")
            @RequestParam(defaultValue = "0") Integer pageNumber,
            @Parameter(description = "Page size (default: 50)")
            @RequestParam(defaultValue = "50") Integer pageSize) {

        log.info("GET /audit/query - Querying audit logs with actorId={}, resourceType={}, resourceId={}",
                actorId, resourceType, resourceId);

        AuditLogQueryFilter filter = AuditLogQueryFilter.builder()
                .actorId(actorId)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .eventType(eventType)
                .fromTimestamp(fromTimestamp)
                .toTimestamp(toTimestamp)
                .pageNumber(pageNumber)
                .pageSize(pageSize)
                .build();

        Page<AuditLogResponse> result = auditLogService.queryAuditLogs(filter);

        return ResponseEntity.ok(result);
    }

    /**
     * Retrieve a single audit log by ID
     *
     * @param id The audit log ID
     * @return The audit log if found, 404 otherwise
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get audit log by ID", description = "Retrieve a single audit log entry by its unique identifier")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Audit log retrieved successfully",
                    content = @Content(schema = @Schema(implementation = AuditLogResponse.class))),
            @ApiResponse(responseCode = "404", description = "Audit log not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<AuditLogResponse> getAuditLogById(
            @Parameter(description = "Audit log ID", required = true, example = "1")
            @PathVariable Long id) {
        log.info("GET /audit/{} - Retrieving audit log", id);

        Optional<AuditLogResponse> response = auditLogService.getAuditLogById(id);

        return response
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Get total count of audit log records
     *
     * @return Total record count
     */
    @GetMapping("/count")
    @Operation(summary = "Get audit log count", description = "Retrieve the total count of audit log records in the system")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Count retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Long> getRecordCount() {
        log.info("GET /audit/count - Getting total record count");

        long count = auditLogService.getRecordCount();

        return ResponseEntity.ok(count);
    }
}
