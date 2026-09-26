package com.auditvault.auditvault.service;

import com.auditvault.auditvault.domain.AuditLog;
import com.auditvault.auditvault.repository.AuditLogRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RedactionServiceIntegrationTest {

    @Autowired
    private RedactionService redactionService;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        auditLogRepository.deleteAll();
    }

    private AuditLog createAuditLogWithPayload(ObjectNode payload) {
        AuditLog record = AuditLog.builder()
                .eventType("DATA_READ")
                .actorId("user123")
                .resourceType("ACCOUNT")
                .resourceId("ACC001")
                .payload(payload)
                .eventTimestamp(java.time.LocalDateTime.now())
                .contentHash("originalHash")
                .previousHash("prevHash")
                .status("ACTIVE")
                .isArchived(false)
                .build();
        return auditLogRepository.save(record);
    }

    @Test
    void redactSensitiveFields_Success() {
        // Create a record with sensitive data
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("accountNumber", "1234567890");
        payload.put("ssn", "987654321");

        AuditLog saved = createAuditLogWithPayload(payload);

        boolean redacted = redactionService.redactSensitiveFields(saved.getId(), 
                List.of("payload.accountNumber", "payload.ssn"));

        assertTrue(redacted);

        // Verify redaction
        AuditLog updated = auditLogRepository.findById(saved.getId()).orElse(null);
        assertNotNull(updated);
        assertEquals("[REDACTED]", updated.getPayload().get("accountNumber").asText());
        assertEquals("[REDACTED]", updated.getPayload().get("ssn").asText());
        assertNotNull(updated.getRedactionMetadata());
        assertEquals("REDACTED", updated.getStatus());
    }

    @Test
    void redactSensitiveFields_RecordNotFound() {
        boolean redacted = redactionService.redactSensitiveFields(99999L,
                List.of("payload.accountNumber"));
        assertFalse(redacted);
    }

    @Test
    void redactSensitiveFields_EmptyFieldList_ShouldKeepOriginalPayload() {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("accountNumber", "1234567890");

        AuditLog saved = createAuditLogWithPayload(payload);

        boolean redacted = redactionService.redactSensitiveFields(saved.getId(), List.of());

        assertTrue(redacted);

        AuditLog updated = auditLogRepository.findById(saved.getId()).orElse(null);
        assertNotNull(updated);
        assertEquals("1234567890", updated.getPayload().get("accountNumber").asText());
        assertNotNull(updated.getRedactionMetadata());
    }

    @Test
    void redactSensitiveFields_NullFieldList_ThrowsNullPointerException() {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("accountNumber", "1234567890");

        AuditLog saved = createAuditLogWithPayload(payload);

        assertThrows(NullPointerException.class,
                () -> redactionService.redactSensitiveFields(saved.getId(), null));
    }

    @Test
    void redactSensitiveFields_FieldNotFound() {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("accountNumber", "1234567890");

        AuditLog record = AuditLog.builder()
                .eventType("DATA_READ")
                .actorId("user123")
                .resourceType("ACCOUNT")
                .resourceId("ACC001")
                .payload(payload)
                .eventTimestamp(java.time.LocalDateTime.now())
                .contentHash("originalHash")
                .previousHash("prevHash")
                .status("ACTIVE")
                .isArchived(false)
                .build();

        AuditLog saved = auditLogRepository.save(record);

        boolean redacted = redactionService.redactSensitiveFields(saved.getId(), 
                List.of("payload.nonExistentField"));

        assertTrue(redacted); // Still returns true, just skips non-existent field
    }

    @Test
    void redactSensitiveFields_InvalidFieldPath() {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("accountNumber", "1234567890");

        AuditLog record = AuditLog.builder()
                .eventType("DATA_READ")
                .actorId("user123")
                .resourceType("ACCOUNT")
                .resourceId("ACC001")
                .payload(payload)
                .eventTimestamp(java.time.LocalDateTime.now())
                .contentHash("originalHash")
                .previousHash("prevHash")
                .status("ACTIVE")
                .isArchived(false)
                .build();

        AuditLog saved = auditLogRepository.save(record);

        assertThrows(com.auditvault.auditvault.exception.AuditLogException.class, () -> {
            redactionService.redactSensitiveFields(saved.getId(), 
                    List.of("invalidPath.accountNumber"));
        });
    }

    @Test
    void verifyRedactionIntegrity_Valid() {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("accountNumber", "1234567890");

        AuditLog record = AuditLog.builder()
                .eventType("DATA_READ")
                .actorId("user123")
                .resourceType("ACCOUNT")
                .resourceId("ACC001")
                .payload(payload)
                .eventTimestamp(java.time.LocalDateTime.now())
                .contentHash("originalHash")
                .previousHash("prevHash")
                .status("ACTIVE")
                .isArchived(false)
                .build();

        AuditLog saved = auditLogRepository.save(record);

        redactionService.redactSensitiveFields(saved.getId(), List.of("payload.accountNumber"));

        boolean isValid = redactionService.verifyRedactionIntegrity(saved.getId(), "accountNumber", "1234567890");
        assertTrue(isValid);
    }

    @Test
    void verifyRedactionIntegrity_Invalid() {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("accountNumber", "1234567890");

        AuditLog record = AuditLog.builder()
                .eventType("DATA_READ")
                .actorId("user123")
                .resourceType("ACCOUNT")
                .resourceId("ACC001")
                .payload(payload)
                .eventTimestamp(java.time.LocalDateTime.now())
                .contentHash("originalHash")
                .previousHash("prevHash")
                .status("ACTIVE")
                .isArchived(false)
                .build();

        AuditLog saved = auditLogRepository.save(record);

        redactionService.redactSensitiveFields(saved.getId(), List.of("payload.accountNumber"));

        boolean isValid = redactionService.verifyRedactionIntegrity(saved.getId(), "accountNumber", "wrongvalue");
        assertFalse(isValid);
    }

    @Test
    void verifyRedactionIntegrity_RecordNotFound() {
        boolean isValid = redactionService.verifyRedactionIntegrity(99999L, "accountNumber", "1234567890");
        assertFalse(isValid);
    }

    @Test
    void verifyRedactionIntegrity_WrongFieldName_ReturnsFalse() {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("accountNumber", "1234567890");

        AuditLog saved = createAuditLogWithPayload(payload);
        redactionService.redactSensitiveFields(saved.getId(), List.of("payload.accountNumber"));

        boolean isValid = redactionService.verifyRedactionIntegrity(saved.getId(), "ssn", "1234567890");
        assertFalse(isValid);
    }

    @Test
    void verifyRedactionIntegrity_FieldNotRedacted() {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("accountNumber", "1234567890");

        AuditLog record = AuditLog.builder()
                .eventType("DATA_READ")
                .actorId("user123")
                .resourceType("ACCOUNT")
                .resourceId("ACC001")
                .payload(payload)
                .eventTimestamp(java.time.LocalDateTime.now())
                .contentHash("originalHash")
                .previousHash("prevHash")
                .status("ACTIVE")
                .isArchived(false)
                .build();

        AuditLog saved = auditLogRepository.save(record);

        boolean isValid = redactionService.verifyRedactionIntegrity(saved.getId(), "accountNumber", "1234567890");
        assertFalse(isValid);
    }

    @Test
    void getRedactionMetadata_WithRedaction() {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("accountNumber", "1234567890");

        AuditLog record = AuditLog.builder()
                .eventType("DATA_READ")
                .actorId("user123")
                .resourceType("ACCOUNT")
                .resourceId("ACC001")
                .payload(payload)
                .eventTimestamp(java.time.LocalDateTime.now())
                .contentHash("originalHash")
                .previousHash("prevHash")
                .status("ACTIVE")
                .isArchived(false)
                .build();

        AuditLog saved = auditLogRepository.save(record);

        redactionService.redactSensitiveFields(saved.getId(), List.of("payload.accountNumber"));

        JsonNode metadata = redactionService.getRedactionMetadata(saved.getId());
        assertNotNull(metadata);
        assertTrue(metadata.has("accountNumber"));
    }

    @Test
    void getRedactionMetadata_NoRedaction() {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("accountNumber", "1234567890");

        AuditLog record = AuditLog.builder()
                .eventType("DATA_READ")
                .actorId("user123")
                .resourceType("ACCOUNT")
                .resourceId("ACC001")
                .payload(payload)
                .eventTimestamp(java.time.LocalDateTime.now())
                .contentHash("originalHash")
                .previousHash("prevHash")
                .status("ACTIVE")
                .isArchived(false)
                .build();

        AuditLog saved = auditLogRepository.save(record);

        JsonNode metadata = redactionService.getRedactionMetadata(saved.getId());
        assertNull(metadata);
    }

    @Test
    void getRedactionMetadata_RecordNotFound() {
        JsonNode metadata = redactionService.getRedactionMetadata(99999L);
        assertNull(metadata);
    }

    @Test
    void redactSensitiveFields_MultipleFields() {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("accountNumber", "1234567890");
        payload.put("ssn", "987654321");
        payload.put("email", "test@example.com");

        AuditLog record = AuditLog.builder()
                .eventType("DATA_READ")
                .actorId("user123")
                .resourceType("ACCOUNT")
                .resourceId("ACC001")
                .payload(payload)
                .eventTimestamp(java.time.LocalDateTime.now())
                .contentHash("originalHash")
                .previousHash("prevHash")
                .status("ACTIVE")
                .isArchived(false)
                .build();

        AuditLog saved = auditLogRepository.save(record);

        boolean redacted = redactionService.redactSensitiveFields(saved.getId(), 
                List.of("payload.accountNumber", "payload.ssn", "payload.email"));

        assertTrue(redacted);

        AuditLog updated = auditLogRepository.findById(saved.getId()).orElse(null);
        assertNotNull(updated);
        assertEquals("[REDACTED]", updated.getPayload().get("accountNumber").asText());
        assertEquals("[REDACTED]", updated.getPayload().get("ssn").asText());
        assertEquals("[REDACTED]", updated.getPayload().get("email").asText());
    }
}
