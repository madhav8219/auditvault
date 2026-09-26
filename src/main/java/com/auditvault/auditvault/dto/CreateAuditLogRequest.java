package com.auditvault.auditvault.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import tools.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * Request DTO for creating an audit log entry
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateAuditLogRequest {

    @NotBlank(message = "eventType is required")
    @Schema(description = "Audit event type. Example: CREATE, UPDATE, DELETE, LOGIN, LOGOUT", example = "CREATE")
    private String eventType;

    @NotBlank(message = "actorId is required")
    @Schema(description = "Actor performing the action", example = "admin")
    private String actorId;

    @NotBlank(message = "resourceType is required")
    @Schema(description = "Type of resource being modified", example = "invoice")
    private String resourceType;

    @NotBlank(message = "resourceId is required")
    @Schema(description = "Identifier of the resource being modified", example = "INV-1001")
    private String resourceId;

    @NotNull(message = "payload is required")
    @Schema(
        description = "JSON object payload for the audit event. Example: {\"orderId\":\"A-42\",\"amount\":125.5}",
        type = "object",
        example = "{\"orderId\":\"A-42\",\"amount\":125.5}"
    )
    private JsonNode payload;

    /**
     * Optional timestamp supplied by caller.
     * If not provided, server will assign current time.
     */
    @Schema(description = "Optional event timestamp in ISO-8601 format. If omitted, server uses the current time.", example = "2026-08-19T12:00:00")
    private LocalDateTime timestamp;
}
