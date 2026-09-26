package com.auditvault.auditvault.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class HashChainServiceIntegrationTest {

    @Autowired
    private HashChainService hashChainService;

    @Autowired
    private ObjectMapper objectMapper;

    private JsonNode payload;
    private LocalDateTime timestamp;

    @BeforeEach
    void setUp() {
        payload = objectMapper.createObjectNode().put("action", "view");
        timestamp = LocalDateTime.now();
    }

    @Test
    void computeContentHash_Success() {
        String hash = hashChainService.computeContentHash(
                "DATA_READ",
                "user123",
                "ACCOUNT",
                "ACC001",
                payload,
                timestamp,
                "0000000000000000000000000000000000000000000000000000000000000000"
        );

        assertNotNull(hash);
        assertEquals(64, hash.length());
        assertFalse(hash.isEmpty());
    }

    @Test
    void computeContentHash_Deterministic() {
        String hash1 = hashChainService.computeContentHash(
                "DATA_READ",
                "user123",
                "ACCOUNT",
                "ACC001",
                payload,
                timestamp,
                "0000000000000000000000000000000000000000000000000000000000000000"
        );

        String hash2 = hashChainService.computeContentHash(
                "DATA_READ",
                "user123",
                "ACCOUNT",
                "ACC001",
                payload,
                timestamp,
                "0000000000000000000000000000000000000000000000000000000000000000"
        );

        assertEquals(hash1, hash2);
    }

    @Test
    void computeContentHash_DifferentInputs_DifferentHashes() {
        String hash1 = hashChainService.computeContentHash(
                "DATA_READ",
                "user123",
                "ACCOUNT",
                "ACC001",
                payload,
                timestamp,
                "0000000000000000000000000000000000000000000000000000000000000000"
        );

        String hash2 = hashChainService.computeContentHash(
                "DATA_UPDATED",
                "user123",
                "ACCOUNT",
                "ACC001",
                payload,
                timestamp,
                "0000000000000000000000000000000000000000000000000000000000000000"
        );

        assertNotEquals(hash1, hash2);
    }

    @Test
    void computeRedactedContentHash_Success() {
        ObjectNode redactedPayload = objectMapper.createObjectNode().put("accountNumber", "[REDACTED]");
        ObjectNode redactionMetadata = objectMapper.createObjectNode().put("accountNumber", "abc123");

        String hash = hashChainService.computeRedactedContentHash(
                "DATA_READ",
                "user123",
                "ACCOUNT",
                "ACC001",
                redactedPayload,
                timestamp,
                "0000000000000000000000000000000000000000000000000000000000000000",
                redactionMetadata
        );

        assertNotNull(hash);
        assertEquals(64, hash.length());
    }

    @Test
    void computeRedactedContentHash_Deterministic() {
        ObjectNode redactedPayload = objectMapper.createObjectNode().put("accountNumber", "[REDACTED]");
        ObjectNode redactionMetadata = objectMapper.createObjectNode().put("accountNumber", "abc123");

        String hash1 = hashChainService.computeRedactedContentHash(
                "DATA_READ",
                "user123",
                "ACCOUNT",
                "ACC001",
                redactedPayload,
                timestamp,
                "0000000000000000000000000000000000000000000000000000000000000000",
                redactionMetadata
        );

        String hash2 = hashChainService.computeRedactedContentHash(
                "DATA_READ",
                "user123",
                "ACCOUNT",
                "ACC001",
                redactedPayload,
                timestamp,
                "0000000000000000000000000000000000000000000000000000000000000000",
                redactionMetadata
        );

        assertEquals(hash1, hash2);
    }

    @Test
    void getGenesisHash() {
        String genesisHash = hashChainService.getGenesisHash();

        assertNotNull(genesisHash);
        assertEquals("0000000000000000000000000000000000000000000000000000000000000000", genesisHash);
    }

    @Test
    void verifyRecordHash_Valid() {
        String contentHash = hashChainService.computeContentHash(
                "DATA_READ",
                "user123",
                "ACCOUNT",
                "ACC001",
                payload,
                timestamp,
                "0000000000000000000000000000000000000000000000000000000000000000"
        );

        boolean isValid = hashChainService.verifyRecordHash(
                contentHash,
                "DATA_READ",
                "user123",
                "ACCOUNT",
                "ACC001",
                payload,
                timestamp,
                "0000000000000000000000000000000000000000000000000000000000000000"
        );

        assertTrue(isValid);
    }

    @Test
    void verifyRecordHash_Invalid() {
        String contentHash = "invalidhash1234567890123456789012345678901234567890123456789012345678";

        boolean isValid = hashChainService.verifyRecordHash(
                contentHash,
                "DATA_READ",
                "user123",
                "ACCOUNT",
                "ACC001",
                payload,
                timestamp,
                "0000000000000000000000000000000000000000000000000000000000000000"
        );

        assertFalse(isValid);
    }

    @Test
    void verifyRedactedRecordHash_Valid() {
        ObjectNode redactedPayload = objectMapper.createObjectNode().put("accountNumber", "[REDACTED]");
        ObjectNode redactionMetadata = objectMapper.createObjectNode().put("accountNumber", "abc123");

        String contentHash = hashChainService.computeRedactedContentHash(
                "DATA_READ",
                "user123",
                "ACCOUNT",
                "ACC001",
                redactedPayload,
                timestamp,
                "0000000000000000000000000000000000000000000000000000000000000000",
                redactionMetadata
        );

        boolean isValid = hashChainService.verifyRedactedRecordHash(
                contentHash,
                "DATA_READ",
                "user123",
                "ACCOUNT",
                "ACC001",
                redactedPayload,
                timestamp,
                "0000000000000000000000000000000000000000000000000000000000000000",
                redactionMetadata
        );

        assertTrue(isValid);
    }

    @Test
    void verifyRedactedRecordHash_Invalid() {
        ObjectNode redactedPayload = objectMapper.createObjectNode().put("accountNumber", "[REDACTED]");
        ObjectNode redactionMetadata = objectMapper.createObjectNode().put("accountNumber", "abc123");

        String contentHash = "invalidhash1234567890123456789012345678901234567890123456789012345678";

        boolean isValid = hashChainService.verifyRedactedRecordHash(
                contentHash,
                "DATA_READ",
                "user123",
                "ACCOUNT",
                "ACC001",
                redactedPayload,
                timestamp,
                "0000000000000000000000000000000000000000000000000000000000000000",
                redactionMetadata
        );

        assertFalse(isValid);
    }

    @Test
    void computeContentHash_DifferentPayload_DifferentHash() {
        JsonNode payload1 = objectMapper.createObjectNode().put("action", "view");
        JsonNode payload2 = objectMapper.createObjectNode().put("action", "update");

        String hash1 = hashChainService.computeContentHash(
                "DATA_READ",
                "user123",
                "ACCOUNT",
                "ACC001",
                payload1,
                timestamp,
                "0000000000000000000000000000000000000000000000000000000000000000"
        );

        String hash2 = hashChainService.computeContentHash(
                "DATA_READ",
                "user123",
                "ACCOUNT",
                "ACC001",
                payload2,
                timestamp,
                "0000000000000000000000000000000000000000000000000000000000000000"
        );

        assertNotEquals(hash1, hash2);
    }

    @Test
    void computeContentHash_DifferentPreviousHash_DifferentHash() {
        String hash1 = hashChainService.computeContentHash(
                "DATA_READ",
                "user123",
                "ACCOUNT",
                "ACC001",
                payload,
                timestamp,
                "0000000000000000000000000000000000000000000000000000000000000000"
        );

        String hash2 = hashChainService.computeContentHash(
                "DATA_READ",
                "user123",
                "ACCOUNT",
                "ACC001",
                payload,
                timestamp,
                "1111111111111111111111111111111111111111111111111111111111111111"
        );

        assertNotEquals(hash1, hash2);
    }
}
