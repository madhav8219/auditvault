package com.auditvault.auditvault.domain;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class AuditLogTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void onCreate_setsCreatedAtAndUsesCurrentTimestampWhenMissing() {
        AuditLog auditLog = new AuditLog();
        auditLog.setEventType("DATA_READ");
        auditLog.setActorId("actor-1");
        auditLog.setResourceType("ACCOUNT");
        auditLog.setResourceId("acct-101");
        auditLog.setPayload(objectMapper.createObjectNode().put("field", "value"));

        auditLog.onCreate();

        assertNotNull(auditLog.getCreatedAt());
        assertNotNull(auditLog.getEventTimestamp());
        assertTrue(auditLog.getCreatedAt().isBefore(LocalDateTime.now().plusSeconds(1)));
    }

    @Test
    void builder_defaults_and_onCreate_preserveExplicitTimestamp() {
        LocalDateTime explicitTimestamp = LocalDateTime.of(2024, 1, 2, 3, 4, 5);
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("customerId", "CUS-42");

        AuditLog auditLog = AuditLog.builder()
            .eventType("ACCOUNT_VIEWED")
            .actorId("audit-admin")
            .resourceType("ACCOUNT")
            .resourceId("acct-42")
            .payload(payload)
            .eventTimestamp(explicitTimestamp)
            .contentHash("hash-1")
            .previousHash("hash-0")
            .build();

        assertFalse(auditLog.getIsArchived());
        assertEquals("ACTIVE", auditLog.getStatus());

        auditLog.onCreate();

        assertEquals(explicitTimestamp, auditLog.getEventTimestamp());
        assertNotNull(auditLog.getCreatedAt());
    }
}
