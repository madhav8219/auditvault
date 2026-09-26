package com.auditvault.auditvault.service;

import com.auditvault.auditvault.domain.AuditLog;
import com.auditvault.auditvault.repository.AuditLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for managing retention policies on audit logs
 * 
 * Scenario B: Records older than a configurable window should be archivable or soft-deletable.
 * The chain verification endpoint must handle archived records correctly without reporting
 * false positives.
 */
@Service
@Transactional
@Slf4j
public class RetentionService {

    private final AuditLogRepository repository;
    private final HashChainService hashChainService;

    /**
     * Retention window in days (default 365 days = 1 year)
     * Configurable via application.properties: app.retention.days
     */
    @Value("${app.retention.days:365}")
    private Integer retentionDays;

    public RetentionService(
            AuditLogRepository repository,
            HashChainService hashChainService) {
        this.repository = repository;
        this.hashChainService = hashChainService;
    }

    /**
     * Archives records older than the retention window
     * Archived records are soft-deleted (isArchived = true)
     * but remain in the database for chain verification purposes
     * 
     * @return Number of records archived
     */
    public long archiveOldRecords() {
        log.info("Starting archive of records older than {} days", retentionDays);

        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(retentionDays);
        List<AuditLog> oldRecords = repository.findByEventTimestampBetweenAndIsArchivedFalse(
            LocalDateTime.of(2000, 1, 1, 0, 0),
            cutoffDate,
            org.springframework.data.domain.Pageable.unpaged()
        ).getContent();

        if (oldRecords.isEmpty()) {
            log.info("No records found older than cutoff date: {}", cutoffDate);
            return 0L;
        }

        long archivedCount = 0;
        for (AuditLog record : oldRecords) {
            record.setIsArchived(true);
            record.setStatus("ARCHIVED");
            repository.save(record);
            archivedCount++;
            log.debug("Archived record id={}, eventType={}", record.getId(), record.getEventType());
        }

        log.info("Successfully archived {} records", archivedCount);
        return archivedCount;
    }

    /**
     * Restores (un-archives) a specific record
     * 
     * @param recordId The ID of the record to restore
     * @return true if successful, false if record not found
     */
    public boolean restoreArchivedRecord(Long recordId) {
        log.info("Restoring archived record id={}", recordId);

        return repository.findById(recordId)
            .map(record -> {
                record.setIsArchived(false);
                record.setStatus("RESTORED");
                repository.save(record);
                log.info("Successfully restored record id={}", recordId);
                return true;
            })
            .orElse(false);
    }

    /**
     * Gets the current retention window in days
     */
    public Integer getRetentionWindow() {
        return retentionDays;
    }

    /**
     * Sets the retention window in days
     */
    public void setRetentionWindow(Integer days) {
        if (days <= 0) {
            throw new IllegalArgumentException("Retention window must be positive");
        }
        this.retentionDays = days;
        log.info("Retention window updated to {} days", days);
    }

    /**
     * Gets count of archived records
     */
    public long getArchivedRecordCount() {
        return repository.findAll().stream()
            .filter(AuditLog::getIsArchived)
            .count();
    }

    /**
     * Gets count of active (non-archived) records
     */
    public long getActiveRecordCount() {
        return repository.countByIsArchivedFalse();
    }
}
