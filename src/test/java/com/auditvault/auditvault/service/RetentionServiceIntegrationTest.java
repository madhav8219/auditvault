package com.auditvault.auditvault.service;

import com.auditvault.auditvault.dto.CreateAuditLogRequest;
import com.auditvault.auditvault.repository.AuditLogRepository;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RetentionServiceIntegrationTest {

    @Autowired
    private RetentionService retentionService;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private ObjectMapper objectMapper;

   @BeforeEach
    void setUp() {
        auditLogRepository.deleteAll();
    }

    @Test
    void archiveOldRecords_EmptyDatabase() {
        long archived = retentionService.archiveOldRecords();
        assertEquals(0, archived);
    }

    @Test
    void archiveOldRecords_WithOldRecords() {
        // Create old records (older than 365 days)
        for (int i = 0; i < 3; i++) {
            com.auditvault.auditvault.domain.AuditLog record = com.auditvault.auditvault.domain.AuditLog.builder()
                    .eventType("DATA_READ")
                    .actorId("user" + i)
                    .resourceType("ACCOUNT")
                    .resourceId("ACC00" + i)
                    .eventTimestamp(LocalDateTime.now().minusDays(400))
                    .contentHash("hash" + i)
                    .previousHash("prev" + i)
                    .payload(objectMapper.createObjectNode())
                    .status("ACTIVE")
                    .isArchived(false)
                    .build();
            auditLogRepository.save(record);
        }

        long archived = retentionService.archiveOldRecords();
        assertEquals(3, archived);

        // Verify records are archived
        long archivedCount = auditLogRepository.findAll().stream()
                .filter(com.auditvault.auditvault.domain.AuditLog::getIsArchived)
                .count();
        assertEquals(3, archivedCount);
    }

    @Test
    void archiveOldRecords_WithRecentRecords() {
        // Create recent records (within 365 days)
        for (int i = 0; i < 3; i++) {
            com.auditvault.auditvault.domain.AuditLog record = com.auditvault.auditvault.domain.AuditLog.builder()
                    .eventType("DATA_READ")
                    .actorId("user" + i)
                    .resourceType("ACCOUNT")
                    .resourceId("ACC00" + i)
                    .eventTimestamp(LocalDateTime.now().minusDays(10))
                    .contentHash("hash" + i)
                    .previousHash("prev" + i)
                    .payload(objectMapper.createObjectNode())
                    .status("ACTIVE")
                    .isArchived(false)
                    .build();
            auditLogRepository.save(record);
        }

        long archived = retentionService.archiveOldRecords();
        assertEquals(0, archived);
    }

    @Test
    void restoreArchivedRecord_Success() {
        // Create and archive a record
        com.auditvault.auditvault.domain.AuditLog record = com.auditvault.auditvault.domain.AuditLog.builder()
                .eventType("DATA_READ")
                .actorId("user123")
                .resourceType("ACCOUNT")
                .resourceId("ACC001")
                .eventTimestamp(LocalDateTime.now().minusDays(400))
                .contentHash("hash123")
                .previousHash("prev123")
                .payload(objectMapper.createObjectNode())
                .status("ARCHIVED")
                .isArchived(true)
                .build();
        auditLogRepository.save(record);

        boolean restored = retentionService.restoreArchivedRecord(record.getId());

        assertTrue(restored);

        // Verify record is restored
        com.auditvault.auditvault.domain.AuditLog restoredRecord = auditLogRepository.findById(record.getId()).orElse(null);
        assertNotNull(restoredRecord);
        assertFalse(restoredRecord.getIsArchived());
        assertEquals("RESTORED", restoredRecord.getStatus());
    }

    @Test
    void restoreArchivedRecord_NotFound() {
        boolean restored = retentionService.restoreArchivedRecord(99999L);
        assertFalse(restored);
    }

    @Test
    void getRetentionWindow() {
        Integer window = retentionService.getRetentionWindow();
        assertNotNull(window);
        assertTrue(window > 0);
    }

    @Test
    void setRetentionWindow_Valid() {
        retentionService.setRetentionWindow(180);
        assertEquals(180, retentionService.getRetentionWindow());
    }

    @Test
    void setRetentionWindow_Invalid() {
        assertThrows(IllegalArgumentException.class, () -> {
            retentionService.setRetentionWindow(-1);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            retentionService.setRetentionWindow(0);
        });
    }

    @Test
    void getArchivedRecordCount() {
        // Create archived records
        for (int i = 0; i < 3; i++) {
            com.auditvault.auditvault.domain.AuditLog record = com.auditvault.auditvault.domain.AuditLog.builder()
                    .eventType("DATA_READ")
                    .actorId("user" + i)
                    .resourceType("ACCOUNT")
                    .resourceId("ACC00" + i)
                    .eventTimestamp(LocalDateTime.now())
                    .contentHash("hash" + i)
                    .previousHash("prev" + i)
                    .payload(objectMapper.createObjectNode())
                    .status("ARCHIVED")
                    .isArchived(true)
                    .build();
            auditLogRepository.save(record);
        }

        // Create active records
        for (int i = 0; i < 2; i++) {
            com.auditvault.auditvault.domain.AuditLog record = com.auditvault.auditvault.domain.AuditLog.builder()
                    .eventType("DATA_READ")
                    .actorId("user" + (i + 3))
                    .resourceType("ACCOUNT")
                    .resourceId("ACC00" + (i + 3))
                    .eventTimestamp(LocalDateTime.now())
                    .contentHash("hash" + (i + 3))
                    .previousHash("prev" + (i + 3))
                    .payload(objectMapper.createObjectNode())
                    .status("ACTIVE")
                    .isArchived(false)
                    .build();
            auditLogRepository.save(record);
        }

        long archivedCount = retentionService.getArchivedRecordCount();
        assertEquals(3, archivedCount);
    }

    @Test
    void getActiveRecordCount() {
        // Create active records
        for (int i = 0; i < 3; i++) {
            com.auditvault.auditvault.domain.AuditLog record = com.auditvault.auditvault.domain.AuditLog.builder()
                    .eventType("DATA_READ")
                    .actorId("user" + i)
                    .resourceType("ACCOUNT")
                    .resourceId("ACC00" + i)
                    .eventTimestamp(LocalDateTime.now())
                    .contentHash("hash" + i)
                    .previousHash("prev" + i)
                    .payload(objectMapper.createObjectNode())
                    .status("ACTIVE")
                    .isArchived(false)
                    .build();
            auditLogRepository.save(record);
        }

        // Create archived records
        for (int i = 0; i < 2; i++) {
            com.auditvault.auditvault.domain.AuditLog record = com.auditvault.auditvault.domain.AuditLog.builder()
                    .eventType("DATA_READ")
                    .actorId("user" + (i + 3))
                    .resourceType("ACCOUNT")
                    .resourceId("ACC00" + (i + 3))
                    .eventTimestamp(LocalDateTime.now())
                    .contentHash("hash" + (i + 3))
                    .previousHash("prev" + (i + 3))
                    .payload(objectMapper.createObjectNode())
                    .status("ARCHIVED")
                    .isArchived(true)
                    .build();
            auditLogRepository.save(record);
        }

        long activeCount = retentionService.getActiveRecordCount();
        assertEquals(3, activeCount);
    }

    @Test
    void archiveOldRecords_StatusUpdate() {
        com.auditvault.auditvault.domain.AuditLog record = com.auditvault.auditvault.domain.AuditLog.builder()
                .eventType("DATA_READ")
                .actorId("user123")
                .resourceType("ACCOUNT")
                .resourceId("ACC001")
                .eventTimestamp(LocalDateTime.now().minusDays(400))
                .contentHash("hash123")
                .previousHash("prev123")
                .payload(objectMapper.createObjectNode())
                .status("ACTIVE")
                .isArchived(false)
                .build();
        auditLogRepository.save(record);

        retentionService.archiveOldRecords();

        com.auditvault.auditvault.domain.AuditLog archivedRecord = auditLogRepository.findById(record.getId()).orElse(null);
        assertNotNull(archivedRecord);
        assertTrue(archivedRecord.getIsArchived());
        assertEquals("ARCHIVED", archivedRecord.getStatus());
    }
}
