package com.auditvault.auditvault.service;

import com.auditvault.auditvault.domain.AuditLog;
import com.auditvault.auditvault.dto.AuditLogResponse;
import com.auditvault.auditvault.repository.AuditLogRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service for bulk export of audit logs as a self-contained, verifiable bundle
 * 
 * Scenario B: Bulk Export
 * Provide an endpoint to export all records for a given resourceId or actorId
 * as a self-contained, verifiable bundle.
 * 
 * Bundle contains:
 * - All matching records
 * - Chain metadata (first/last hash, genesis hash)
 * - Verification instructions
 * - Export timestamp and checksum
 */
@Service
@Transactional(readOnly = true)
@Slf4j
public class BulkExportService {

    private final AuditLogRepository repository;
    private final ObjectMapper objectMapper;

    public BulkExportService(
            AuditLogRepository repository,
            ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    /**
     * Exports all records for a given resourceId as a verifiable bundle
     * 
     * @param resourceId The resource ID to export
     * @return Bundle containing records and chain metadata
     */
    public JsonNode exportByResourceId(String resourceId) {
        log.info("Exporting records for resourceId: {}", resourceId);

        List<AuditLog> records = repository.findByResourceTypeAndResourceIdAndIsArchivedFalse(
            null,  // Could be made flexible
            resourceId,
            org.springframework.data.domain.Pageable.unpaged()
        ).getContent();

        return buildExportBundle(records, "resourceId:" + resourceId);
    }

    /**
     * Exports all records for a given actorId as a verifiable bundle
     * 
     * @param actorId The actor ID to export
     * @return Bundle containing records and chain metadata
     */
    public JsonNode exportByActorId(String actorId) {
        log.info("Exporting records for actorId: {}", actorId);

        List<AuditLog> records = repository.findByActorIdAndIsArchivedFalse(
            actorId,
            org.springframework.data.domain.Pageable.unpaged()
        ).getContent();

        return buildExportBundle(records, "actorId:" + actorId);
    }

    /**
     * Builds a self-contained verifiable export bundle
     * 
     * Bundle structure:
     * {
     *   "metadata": {
     *     "exportedAt": "2024-01-15T10:30:00",
     *     "totalRecords": 10,
     *     "filter": "resourceId:RES123",
     *     "genesisHash": "0000...",
     *     "firstRecordHash": "abc...",
     *     "lastRecordHash": "def...",
     *     "bundleChecksum": "xyz..."
     *   },
     *   "records": [
     *     { record1 },
     *     { record2 },
     *     ...
     *   ],
     *   "verificationGuide": "..."
     * }
     */
    private JsonNode buildExportBundle(List<AuditLog> records, String filter) {
        ObjectNode bundle = objectMapper.createObjectNode();

        // Metadata
        ObjectNode metadata = objectMapper.createObjectNode();
        metadata.put("exportedAt", LocalDateTime.now().toString());
        metadata.put("totalRecords", records.size());
        metadata.put("filter", filter);
        metadata.put("genesisHash", "0000000000000000000000000000000000000000000000000000000000000000");

        if (!records.isEmpty()) {
            metadata.put("firstRecordId", records.get(0).getId());
            metadata.put("firstRecordHash", records.get(0).getContentHash());
            metadata.put("lastRecordId", records.get(records.size() - 1).getId());
            metadata.put("lastRecordHash", records.get(records.size() - 1).getContentHash());
        }

        // Build bundle checksum
        String bundleChecksum = computeBundleChecksum(records);
        metadata.put("bundleChecksum", bundleChecksum);

        bundle.set("metadata", metadata);

        // Records
        ArrayNode recordsArray = objectMapper.createArrayNode();
        for (AuditLog record : records) {
            ObjectNode recordNode = objectMapper.createObjectNode();
            recordNode.put("id", record.getId());
            recordNode.put("eventType", record.getEventType());
            recordNode.put("actorId", record.getActorId());
            recordNode.put("resourceType", record.getResourceType());
            recordNode.put("resourceId", record.getResourceId());
            recordNode.set("payload", record.getPayload());
            recordNode.put("eventTimestamp", record.getEventTimestamp().toString());
            recordNode.put("contentHash", record.getContentHash());
            recordNode.put("previousHash", record.getPreviousHash());
            recordNode.put("createdAt", record.getCreatedAt().toString());

            recordsArray.add(recordNode);
        }
        bundle.set("records", recordsArray);

        // Verification guide
        bundle.put("verificationGuide",
            "To verify this bundle: 1) Verify each record's contentHash against its content " +
            "2) Verify previousHash chain links 3) Compare bundleChecksum " +
            "4) Verify against original system using /audit/verify endpoint"
        );

        log.info("Export bundle created: {} records, checksum: {}", records.size(), bundleChecksum);
        return bundle;
    }

    /**
     * Computes a checksum for the bundle for integrity verification
     * Checksum is computed from all record hashes in order
     */
    private String computeBundleChecksum(List<AuditLog> records) {
        StringBuilder content = new StringBuilder();
        for (AuditLog record : records) {
            content.append(record.getContentHash()).append("|");
        }

        // Simple checksum - in production would use more robust method
        return String.valueOf(content.toString().hashCode());
    }
}
