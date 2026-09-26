package com.auditvault.auditvault.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.JsonNode;
import com.auditvault.auditvault.config.JsonNodeAttributeConverter;
import java.time.LocalDateTime;

/**
 * AuditLog Entity - Represents an immutable audit log record with tamper-evidence
 * through hash chaining.
 *
 * Each record contains:
 * - contentHash: SHA-256 hash of this record's content
 * - previousHash: SHA-256 hash of the previous record (genesis for first record)
 *
 * This design ensures any modification to a past record is detectable.
 */
@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_actor_id", columnList = "actor_id"),
    @Index(name = "idx_resource_type_id", columnList = "resource_type, resource_id"),
    @Index(name = "idx_event_type", columnList = "event_type"),
    @Index(name = "idx_timestamp", columnList = "event_timestamp"),
    @Index(name = "idx_is_archived", columnList = "is_archived")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String eventType;

    @Column(nullable = false, length = 100)
    private String actorId;

    @Column(nullable = false, length = 100)
    private String resourceType;

    @Column(nullable = false, length = 255)
    private String resourceId;

    @Column(nullable = false, columnDefinition = "TEXT")
    @Convert(converter = JsonNodeAttributeConverter.class)
    private JsonNode payload;

    @Column(nullable = false)
    private LocalDateTime eventTimestamp;

    /**
     * SHA-256 hash of this record's content (eventType, actorId, resourceType,
     * resourceId, payload, eventTimestamp, previousHash)
     */
    @Column(nullable = false, length = 64, unique = true)
    private String contentHash;

    /**
     * SHA-256 hash of the previous record's contentHash.
     * For the first record, this is a defined GENESIS_HASH.
     */
    @Column(nullable = false, length = 64)
    private String previousHash;

    /**
     * Record creation timestamp (system-assigned)
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Soft-delete flag for retention policy (Scenario B)
     * When true, record is archived but still part of chain for verification
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean isArchived = false;

    /**
     * JSON metadata for redacted fields (Scenario B)
     * Format: {"fieldName": "redaction_hash", ...}
     * This allows verification without exposing sensitive data
     */
    @Column(columnDefinition = "TEXT")
    @Convert(converter = JsonNodeAttributeConverter.class)
    private JsonNode redactionMetadata;

    /**
     * Status of the record for audit purposes
     */
    @Column(length = 50)
    @Builder.Default
    private String status = "ACTIVE";

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (eventTimestamp == null) {
            eventTimestamp = LocalDateTime.now();
        }
    }
}
