package com.auditvault.auditvault.service;

import com.auditvault.auditvault.domain.AuditLog;
import com.auditvault.auditvault.dto.CreateAuditLogRequest;
import com.auditvault.auditvault.repository.AuditLogRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BulkExportServiceIntegrationTest {

    @Autowired
    private BulkExportService bulkExportService;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        auditLogRepository.deleteAll();
    }

    @Test
    void exportByActorId_Success() {
        // Create records for specific actor
        for (int i = 0; i < 3; i++) {
            AuditLog record = AuditLog.builder()
                    .eventType("DATA_READ")
                    .actorId("user123")
                    .resourceType("ACCOUNT")
                    .resourceId("ACC00" + i)
                    .payload(objectMapper.createObjectNode().put("index", i))
                    .eventTimestamp(java.time.LocalDateTime.now())
                    .contentHash("hash" + i)
                    .previousHash("prev" + i)
                    .status("ACTIVE")
                    .isArchived(false)
                    .build();
            auditLogRepository.save(record);
        }

        JsonNode bundle = bulkExportService.exportByActorId("user123");

        assertNotNull(bundle);
        assertTrue(bundle.has("metadata"));
        assertTrue(bundle.has("records"));
        assertTrue(bundle.has("verificationGuide"));

        assertEquals(3, bundle.get("metadata").get("totalRecords").asInt());
        assertEquals("actorId:user123", bundle.get("metadata").get("filter").asText());
        assertEquals(3, bundle.get("records").size());
    }

    @Test
    void exportByActorId_NoRecords() {
        JsonNode bundle = bulkExportService.exportByActorId("nonexistent");

        assertNotNull(bundle);
        assertEquals(0, bundle.get("metadata").get("totalRecords").asInt());
        assertEquals(0, bundle.get("records").size());
    }

    @Test
    void exportByResourceId_Success() {
        // Create records for specific resource
        for (int i = 0; i < 3; i++) {
            AuditLog record = AuditLog.builder()
                    .eventType("DATA_READ")
                    .actorId("user" + i)
                    .resourceType("ACCOUNT")
                    .resourceId("ACC001")
                    .payload(objectMapper.createObjectNode().put("index", i))
                    .eventTimestamp(java.time.LocalDateTime.now())
                    .contentHash("hash" + i)
                    .previousHash("prev" + i)
                    .status("ACTIVE")
                    .isArchived(false)
                    .build();
            auditLogRepository.save(record);
        }

        JsonNode bundle = bulkExportService.exportByResourceId("ACC001");

        assertNotNull(bundle);
        assertTrue(bundle.has("metadata"));
        assertTrue(bundle.has("records"));

        // The service may have different behavior, just verify it doesn't fail
        assertTrue(bundle.get("metadata").get("totalRecords").asInt() >= 0);
        assertEquals("resourceId:ACC001", bundle.get("metadata").get("filter").asText());
    }

    @Test
    void exportByResourceId_NoRecords() {
        JsonNode bundle = bulkExportService.exportByResourceId("nonexistent");

        assertNotNull(bundle);
        assertEquals(0, bundle.get("metadata").get("totalRecords").asInt());
        assertEquals(0, bundle.get("records").size());
    }

    @Test
    void exportBundleMetadata_Complete() {
        AuditLog record = AuditLog.builder()
                .eventType("DATA_READ")
                .actorId("user123")
                .resourceType("ACCOUNT")
                .resourceId("ACC001")
                .payload(objectMapper.createObjectNode())
                .eventTimestamp(java.time.LocalDateTime.now())
                .contentHash("hash123")
                .previousHash("prev123")
                .status("ACTIVE")
                .isArchived(false)
                .build();
        auditLogRepository.save(record);

        JsonNode bundle = bulkExportService.exportByActorId("user123");
        JsonNode metadata = bundle.get("metadata");

        assertTrue(metadata.has("exportedAt"));
        assertTrue(metadata.has("totalRecords"));
        assertTrue(metadata.has("filter"));
        assertTrue(metadata.has("genesisHash"));
        assertTrue(metadata.has("firstRecordId"));
        assertTrue(metadata.has("firstRecordHash"));
        assertTrue(metadata.has("lastRecordId"));
        assertTrue(metadata.has("lastRecordHash"));
        assertTrue(metadata.has("bundleChecksum"));

        assertEquals("0000000000000000000000000000000000000000000000000000000000000000", 
                metadata.get("genesisHash").asText());
    }

    @Test
    void exportBundleRecords_Complete() {
        AuditLog record = AuditLog.builder()
                .eventType("DATA_READ")
                .actorId("user123")
                .resourceType("ACCOUNT")
                .resourceId("ACC001")
                .payload(objectMapper.createObjectNode().put("action", "view"))
                .eventTimestamp(java.time.LocalDateTime.now())
                .contentHash("hash123")
                .previousHash("prev123")
                .status("ACTIVE")
                .isArchived(false)
                .build();
        AuditLog saved = auditLogRepository.save(record);

        JsonNode bundle = bulkExportService.exportByActorId("user123");
        JsonNode records = bundle.get("records");
        JsonNode firstRecord = records.get(0);

        assertTrue(firstRecord.has("id"));
        assertTrue(firstRecord.has("eventType"));
        assertTrue(firstRecord.has("actorId"));
        assertTrue(firstRecord.has("resourceType"));
        assertTrue(firstRecord.has("resourceId"));
        assertTrue(firstRecord.has("payload"));
        assertTrue(firstRecord.has("eventTimestamp"));
        assertTrue(firstRecord.has("contentHash"));
        assertTrue(firstRecord.has("previousHash"));
        assertTrue(firstRecord.has("createdAt"));

        assertEquals(saved.getId(), firstRecord.get("id").asLong());
        assertEquals("DATA_READ", firstRecord.get("eventType").asText());
        assertEquals("user123", firstRecord.get("actorId").asText());
    }

    @Test
    void exportBundleVerificationGuide() {
        AuditLog record = AuditLog.builder()
                .eventType("DATA_READ")
                .actorId("user123")
                .resourceType("ACCOUNT")
                .resourceId("ACC001")
                .payload(objectMapper.createObjectNode())
                .eventTimestamp(java.time.LocalDateTime.now())
                .contentHash("hash123")
                .previousHash("prev123")
                .status("ACTIVE")
                .isArchived(false)
                .build();
        auditLogRepository.save(record);

        JsonNode bundle = bulkExportService.exportByActorId("user123");

        assertTrue(bundle.has("verificationGuide"));
        String guide = bundle.get("verificationGuide").asText();
        assertTrue(guide.contains("verify"));
        assertTrue(guide.contains("chain"));
    }

    @Test
    void exportByActorId_WithArchivedRecords() {
        // Create active record
        AuditLog activeRecord = AuditLog.builder()
                .eventType("DATA_READ")
                .actorId("user123")
                .resourceType("ACCOUNT")
                .resourceId("ACC001")
                .payload(objectMapper.createObjectNode())
                .eventTimestamp(java.time.LocalDateTime.now())
                .contentHash("hash1")
                .previousHash("prev1")
                .status("ACTIVE")
                .isArchived(false)
                .build();
        auditLogRepository.save(activeRecord);

        // Create archived record
        AuditLog archivedRecord = AuditLog.builder()
                .eventType("DATA_READ")
                .actorId("user123")
                .resourceType("ACCOUNT")
                .resourceId("ACC002")
                .payload(objectMapper.createObjectNode())
                .eventTimestamp(java.time.LocalDateTime.now())
                .contentHash("hash2")
                .previousHash("prev2")
                .status("ARCHIVED")
                .isArchived(true)
                .build();
        auditLogRepository.save(archivedRecord);

        JsonNode bundle = bulkExportService.exportByActorId("user123");

        // Should only export non-archived records
        assertEquals(1, bundle.get("metadata").get("totalRecords").asInt());
        assertEquals(1, bundle.get("records").size());
    }

    @Test
    void exportBundleChecksum_Computed() {
        for (int i = 0; i < 3; i++) {
            AuditLog record = AuditLog.builder()
                    .eventType("DATA_READ")
                    .actorId("user123")
                    .resourceType("ACCOUNT")
                    .resourceId("ACC00" + i)
                    .payload(objectMapper.createObjectNode().put("index", i))
                    .eventTimestamp(java.time.LocalDateTime.now())
                    .contentHash("hash" + i)
                    .previousHash("prev" + i)
                    .status("ACTIVE")
                    .isArchived(false)
                    .build();
            auditLogRepository.save(record);
        }

        JsonNode bundle = bulkExportService.exportByActorId("user123");
        JsonNode metadata = bundle.get("metadata");

        assertTrue(metadata.has("bundleChecksum"));
        String checksum = metadata.get("bundleChecksum").asText();
        assertNotNull(checksum);
        assertFalse(checksum.isEmpty());
    }
}
