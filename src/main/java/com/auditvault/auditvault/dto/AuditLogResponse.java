package com.auditvault.auditvault.dto;

import tools.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * Response DTO for audit log entries
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogResponse {

    private Long id;
    private String eventType;
    private String actorId;
    private String resourceType;
    private String resourceId;
    private JsonNode payload;
    private LocalDateTime eventTimestamp;
    private String contentHash;
    private String previousHash;
    private LocalDateTime createdAt;
    private Boolean isArchived;
    private String status;
}
