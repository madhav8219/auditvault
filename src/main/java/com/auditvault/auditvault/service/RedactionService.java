package com.auditvault.auditvault.service;

import com.auditvault.auditvault.domain.AuditLog;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;
import com.auditvault.auditvault.repository.AuditLogRepository;
import com.auditvault.auditvault.exception.AuditLogException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Optional;

/**
 * Service for handling structured redaction of sensitive fields in audit logs
 * 
 * Scenario B: Redaction challenge
 * - Original payload hash includes sensitive values
 * - Simple removal would invalidate the hash
 * - Solution: Store original value as redaction hash, update payload with marker
 * 
 * Design:
 * 1. Store hash of original sensitive value in redactionMetadata
 * 2. Replace sensitive value in payload with redaction marker [REDACTED]
 * 3. Update contentHash to include both redacted payload and redactionMetadata
 * 4. Chain verification uses redacted hash computation
 * 
 * This maintains tamper-evidence while protecting sensitive data.
 */
@Service
@Transactional
@Slf4j
public class RedactionService {

    private static final String ALGORITHM = "SHA-256";
    private static final String REDACTION_MARKER = "[REDACTED]";

    private final AuditLogRepository repository;
    private final HashChainService hashChainService;
    private final ObjectMapper objectMapper;

    public RedactionService(
            AuditLogRepository repository,
            HashChainService hashChainService,
            ObjectMapper objectMapper) {
        this.repository = repository;
        this.hashChainService = hashChainService;
        this.objectMapper = objectMapper;
    }

    /**
     * Redacts sensitive fields from an audit log record
     * 
     * @param recordId The ID of the record to redact
     * @param fieldsToRedact List of JSON paths to redact (e.g., ["payload.accountNumber"])
     * @return true if successful, false if record not found
     * @throws AuditLogException if redaction fails
     */
    public boolean redactSensitiveFields(Long recordId, List<String> fieldsToRedact) {
        log.info("Redacting {} fields from record id={}", fieldsToRedact.size(), recordId);

        Optional<AuditLog> recordOpt = repository.findById(recordId);
        if (recordOpt.isEmpty()) {
            return false;
        }

        AuditLog record = recordOpt.get();

        // Create copies for modification
        ObjectNode redactedPayload = (ObjectNode) record.getPayload().deepCopy();
        ObjectNode redactionMetadata = objectMapper.createObjectNode();

        // Redact each field
        for (String fieldPath : fieldsToRedact) {
            redactField(redactedPayload, redactionMetadata, fieldPath);
        }

        // Update the contentHash with redacted values
        String newContentHash = hashChainService.computeRedactedContentHash(
            record.getEventType(),
            record.getActorId(),
            record.getResourceType(),
            record.getResourceId(),
            redactedPayload,
            record.getEventTimestamp(),
            record.getPreviousHash(),
            redactionMetadata
        );

        // Update the record
        record.setPayload(redactedPayload);
        record.setContentHash(newContentHash);
        record.setRedactionMetadata(redactionMetadata);
        record.setStatus("REDACTED");

        repository.save(record);
        log.info("Successfully redacted fields from record id={}", recordId);

        return true;
    }

    /**
     * Redacts a single field in the payload
     * Stores the hash of original value for verification purposes
     */
    private void redactField(ObjectNode payload, ObjectNode metadata, String fieldPath) {
        // Simple implementation for root-level payload fields
        // For nested paths, would need more sophisticated parsing
        
        if (!fieldPath.startsWith("payload.")) {
            throw new AuditLogException("Invalid field path: " + fieldPath);
        }

        String fieldName = fieldPath.substring("payload.".length());
        
        if (!payload.has(fieldName)) {
            log.warn("Field {} not found in payload", fieldName);
            return;
        }

        JsonNode originalValue = payload.get(fieldName);
        String originalHash = computeHash(originalValue.asText());

        // Store the hash of the original value
        metadata.put(fieldName, originalHash);

        // Replace with redaction marker
        payload.put(fieldName, REDACTION_MARKER);

        log.debug("Redacted field: {}, hash: {}", fieldName, originalHash);
    }

    /**
     * Verifies redaction integrity - that a redacted field hasn't been modified
     * 
     * @param recordId The ID of the record
     * @param fieldName The name of the redacted field
     * @param supposedOriginalValue The value that is supposed to be redacted
     * @return true if the redaction is valid for this value
     */
    public boolean verifyRedactionIntegrity(Long recordId, String fieldName, String supposedOriginalValue) {
        log.debug("Verifying redaction integrity for field {} in record {}", fieldName, recordId);

        Optional<AuditLog> recordOpt = repository.findById(recordId);
        if (recordOpt.isEmpty()) {
            return false;
        }

        AuditLog record = recordOpt.get();

        if (record.getRedactionMetadata() == null || !record.getRedactionMetadata().has(fieldName)) {
            return false;
        }

        String storedHash = record.getRedactionMetadata().get(fieldName).asText();
        String computedHash = computeHash(supposedOriginalValue);

        boolean matches = storedHash.equals(computedHash);
        log.debug("Redaction verification result: {}", matches ? "VALID" : "INVALID");

        return matches;
    }

    /**
     * Gets redaction metadata for a record (without revealing original values)
     */
    public JsonNode getRedactionMetadata(Long recordId) {
        return repository.findById(recordId)
            .map(AuditLog::getRedactionMetadata)
            .orElse(null);
    }

    /**
     * Computes SHA-256 hash of input string
     */
    private String computeHash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            byte[] encodedHash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(encodedHash);
        } catch (NoSuchAlgorithmException e) {
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
}
