package com.auditvault.auditvault.service;

import com.auditvault.auditvault.domain.AuditLog;
import com.auditvault.auditvault.dto.AuditLogQueryFilter;
import com.auditvault.auditvault.dto.AuditLogResponse;
import com.auditvault.auditvault.dto.CreateAuditLogRequest;
import com.auditvault.auditvault.dto.VerificationResult;
import com.auditvault.auditvault.exception.AuditLogException;
import com.auditvault.auditvault.repository.AuditLogRepository;
import tools.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing audit logs with tamper-evidence through hash chaining
 */
@Service
@Transactional
@Slf4j
public class AuditLogService {

    private final AuditLogRepository repository;
    private final HashChainService hashChainService;
    private final ObjectMapper objectMapper;

    public AuditLogService(
            AuditLogRepository repository,
            HashChainService hashChainService,
            ObjectMapper objectMapper) {
        this.repository = repository;
        this.hashChainService = hashChainService;
        this.objectMapper = objectMapper;
    }

    /**
     * Creates and stores a new audit log entry
     * 
     * @param request The audit log creation request
     * @return The created audit log response
     * @throws AuditLogException if validation fails
     */
    public AuditLogResponse createAuditLog(CreateAuditLogRequest request) {
        log.info("Creating audit log for actorId={}, resourceId={}", request.getActorId(), request.getResourceId());

        // Get the previous record to compute the chain
        Optional<AuditLog> lastRecordOpt = repository.findLastRecord();
        String previousHash = lastRecordOpt
            .map(AuditLog::getContentHash)
            .orElseGet(hashChainService::getGenesisHash);

        // Set timestamp (use provided or server time)
        LocalDateTime timestamp = request.getTimestamp();
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }

        // Compute content hash
        String contentHash = hashChainService.computeContentHash(
            request.getEventType(),
            request.getActorId(),
            request.getResourceType(),
            request.getResourceId(),
            request.getPayload(),
            timestamp,
            previousHash
        );

        // Check for duplicate (should not happen in practice due to timestamps)
        if (repository.existsByContentHash(contentHash)) {
            throw new AuditLogException("Duplicate audit log detected");
        }

        // Create and save the audit log
        AuditLog auditLog = AuditLog.builder()
            .eventType(request.getEventType())
            .actorId(request.getActorId())
            .resourceType(request.getResourceType())
            .resourceId(request.getResourceId())
            .payload(request.getPayload())
            .eventTimestamp(timestamp)
            .contentHash(contentHash)
            .previousHash(previousHash)
            .status("ACTIVE")
            .isArchived(false)
            .build();

        AuditLog saved = repository.save(auditLog);
        log.info("Audit log created with id={}, contentHash={}", saved.getId(), saved.getContentHash());

        return mapToResponse(saved);
    }

    /**
     * Queries audit logs with filtering and pagination
     */
    public Page<AuditLogResponse> queryAuditLogs(AuditLogQueryFilter filter) {
        log.debug("Querying audit logs with filter: {}", filter);

        Page<AuditLog> page = repository.findWithFilters(
            filter.getActorId(),
            filter.getResourceType(),
            filter.getResourceId(),
            filter.getEventType(),
            filter.getFromTimestamp(),
            filter.getToTimestamp(),
            false,  // excludeArchived by default
            PageRequest.of(filter.getPageNumber(), filter.getPageSize())
        );

        return page.map(this::mapToResponse);
    }

    /**
     * Retrieves a single audit log by ID
     */
    @Transactional(readOnly = true)
    public Optional<AuditLogResponse> getAuditLogById(Long id) {
        return repository.findById(id).map(this::mapToResponse);
    }

    /**
     * Verifies the hash chain integrity
     * 
     * @return VerificationResult containing chain status and any violations found
     */
    @Transactional(readOnly = true)
    public VerificationResult verifyChain() {
        log.info("Starting hash chain verification");

        // Get all non-archived records in order
        List<AuditLog> records = repository.findAllNonArchivedOrderById();
        log.info("Verifying chain with {} records", records.size());

        if (records.isEmpty()) {
            return VerificationResult.builder()
                .isIntact(true)
                .message("No records to verify")
                .totalRecordsVerified(0L)
                .build();
        }

        String previousHash = hashChainService.getGenesisHash();

        for (AuditLog record : records) {
            log.debug("Verifying record id={}, contentHash={}", record.getId(), record.getContentHash());

            // Verify the previous hash link
            if (!record.getPreviousHash().equals(previousHash)) {
                log.error("Previous hash mismatch at record id={}. Expected previousHash={}, got={}",
                    record.getId(), previousHash, record.getPreviousHash());

                return VerificationResult.builder()
                    .isIntact(false)
                    .message("Chain broken: previous hash does not match")
                    .firstInconsistencyId(record.getId())
                    .violationType("PREVIOUS_HASH_MISMATCH")
                    .totalRecordsVerified((long) records.indexOf(record))
                    .build();
            }

            // Verify the content hash (handle both regular and redacted records)
            boolean hashValid;
            if (record.getRedactionMetadata() != null) {
                // Record has been redacted - verify using redacted hash computation
                hashValid = hashChainService.verifyRedactedRecordHash(
                    record.getContentHash(),
                    record.getEventType(),
                    record.getActorId(),
                    record.getResourceType(),
                    record.getResourceId(),
                    record.getPayload(),
                    record.getEventTimestamp(),
                    record.getPreviousHash(),
                    record.getRedactionMetadata()
                );
            } else {
                // Regular record - verify using normal hash computation
                hashValid = hashChainService.verifyRecordHash(
                    record.getContentHash(),
                    record.getEventType(),
                    record.getActorId(),
                    record.getResourceType(),
                    record.getResourceId(),
                    record.getPayload(),
                    record.getEventTimestamp(),
                    record.getPreviousHash()
                );
            }

            if (!hashValid) {
                log.error("Content hash invalid at record id={}", record.getId());

                return VerificationResult.builder()
                    .isIntact(false)
                    .message("Chain broken: content hash is invalid (record modified)")
                    .firstInconsistencyId(record.getId())
                    .violationType("CONTENT_HASH_INVALID")
                    .totalRecordsVerified((long) records.indexOf(record))
                    .build();
            }

            previousHash = record.getContentHash();
        }

        log.info("Chain verification passed. All {} records verified successfully.", records.size());

        return VerificationResult.builder()
            .isIntact(true)
            .message("Chain integrity verified successfully")
            .totalRecordsVerified((long) records.size())
            .build();
    }

    /**
     * Detects tampering by verifying the chain and identifying exact violation
     * Used for testing - modifies a record directly and verifies chain breaks
     */
    @Transactional(readOnly = true)
    public VerificationResult detectTampering() {
        return verifyChain();
    }

    /**
     * Returns count of all active (non-archived) records
     */
    @Transactional(readOnly = true)
    public long getRecordCount() {
        return repository.countByIsArchivedFalse();
    }

    /**
     * Maps AuditLog entity to DTO
     */
    private AuditLogResponse mapToResponse(AuditLog auditLog) {
        return AuditLogResponse.builder()
            .id(auditLog.getId())
            .eventType(auditLog.getEventType())
            .actorId(auditLog.getActorId())
            .resourceType(auditLog.getResourceType())
            .resourceId(auditLog.getResourceId())
            .payload(auditLog.getPayload())
            .eventTimestamp(auditLog.getEventTimestamp())
            .contentHash(auditLog.getContentHash())
            .previousHash(auditLog.getPreviousHash())
            .createdAt(auditLog.getCreatedAt())
            .isArchived(auditLog.getIsArchived())
            .status(auditLog.getStatus())
            .build();
    }
}
