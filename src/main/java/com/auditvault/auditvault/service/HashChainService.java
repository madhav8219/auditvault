package com.auditvault.auditvault.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;

/**
 * Service for computing and verifying hash chains
 *
 * Uses SHA-256 algorithm for cryptographic hashing.
 * Each record's contentHash is computed from its content + previous record's hash.
 */
@Service
@Slf4j
public class HashChainService {

    private static final String ALGORITHM = "SHA-256";
    private static final String GENESIS_HASH = "0000000000000000000000000000000000000000000000000000000000000000";

    private final ObjectMapper objectMapper;

    public HashChainService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Computes the content hash for a record
     * Hash is computed from: eventType + actorId + resourceType + resourceId + payload + eventTimestamp + previousHash
     */
    public String computeContentHash(
            String eventType,
            String actorId,
            String resourceType,
            String resourceId,
            JsonNode payload,
            LocalDateTime eventTimestamp,
            String previousHash) {

        // Build the content string to hash
        String content = buildContentString(
            eventType,
            actorId,
            resourceType,
            resourceId,
            payload,
            eventTimestamp,
            previousHash
        );

        return sha256Hash(content);
    }

    /**
     * Computes the content hash for a redacted record
     * When fields are redacted, we use their redaction hashes instead of original values
     */
    public String computeRedactedContentHash(
            String eventType,
            String actorId,
            String resourceType,
            String resourceId,
            JsonNode redactedPayload,
            LocalDateTime eventTimestamp,
            String previousHash,
            JsonNode redactionMetadata) {

        String content = buildRedactedContentString(
            eventType,
            actorId,
            resourceType,
            resourceId,
            redactedPayload,
            eventTimestamp,
            previousHash,
            redactionMetadata
        );

        return sha256Hash(content);
    }

    /**
     * Gets the genesis hash for the first record
     */
    public String getGenesisHash() {
        return GENESIS_HASH;
    }

    /**
     * Verifies that a single record's hash is correct
     */
    public boolean verifyRecordHash(
            String contentHash,
            String eventType,
            String actorId,
            String resourceType,
            String resourceId,
            JsonNode payload,
            LocalDateTime eventTimestamp,
            String previousHash) {

        String computedHash = computeContentHash(
            eventType,
            actorId,
            resourceType,
            resourceId,
            payload,
            eventTimestamp,
            previousHash
        );

        boolean isValid = computedHash.equals(contentHash);
        if (!isValid) {
            log.warn("Hash mismatch: expected={}, computed={}", contentHash, computedHash);
        }
        return isValid;
    }

    /**
     * Verifies that a redacted record's hash is correct
     */
    public boolean verifyRedactedRecordHash(
            String contentHash,
            String eventType,
            String actorId,
            String resourceType,
            String resourceId,
            JsonNode redactedPayload,
            LocalDateTime eventTimestamp,
            String previousHash,
            JsonNode redactionMetadata) {

        String computedHash = computeRedactedContentHash(
            eventType,
            actorId,
            resourceType,
            resourceId,
            redactedPayload,
            eventTimestamp,
            previousHash,
            redactionMetadata
        );

        boolean isValid = computedHash.equals(contentHash);
        if (!isValid) {
            log.warn("Redacted hash mismatch: expected={}, computed={}", contentHash, computedHash);
        }
        return isValid;
    }

    /**
     * Computes SHA-256 hash of input string
     */
    private String sha256Hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            byte[] encodedHash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(encodedHash);
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 algorithm not available", e);
            throw new RuntimeException("Cryptographic algorithm not available", e);
        }
    }

    /**
     * Converts bytes to hexadecimal string
     */
    private String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    /**
     * Builds the canonical content string for hashing
     */
    private String buildContentString(
            String eventType,
            String actorId,
            String resourceType,
            String resourceId,
            JsonNode payload,
            LocalDateTime eventTimestamp,
            String previousHash) {

        return String.join("|",
            eventType,
            actorId,
            resourceType,
            resourceId,
            payload.toString(),
            eventTimestamp.toString(),
            previousHash
        );
    }

    /**
     * Builds the canonical content string for redacted records
     */
    private String buildRedactedContentString(
            String eventType,
            String actorId,
            String resourceType,
            String resourceId,
            JsonNode redactedPayload,
            LocalDateTime eventTimestamp,
            String previousHash,
            JsonNode redactionMetadata) {

        String redactionStr = redactionMetadata != null ? redactionMetadata.toString() : "";

        return String.join("|",
            eventType,
            actorId,
            resourceType,
            resourceId,
            redactedPayload.toString(),
            eventTimestamp.toString(),
            previousHash,
            redactionStr
        );
    }
}
