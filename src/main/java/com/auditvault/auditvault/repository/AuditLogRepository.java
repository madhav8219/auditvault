package com.auditvault.auditvault.repository;

import com.auditvault.auditvault.domain.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for AuditLog entity
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /**
     * Find the most recent audit log (last in chain)
     */
    @Query("SELECT a FROM AuditLog a WHERE a.isArchived = false ORDER BY a.id DESC LIMIT 1")
    Optional<AuditLog> findLastRecord();

    /**
     * Find all records ordered by id (for chain verification)
     */
    @Query("SELECT a FROM AuditLog a WHERE a.isArchived = false ORDER BY a.id ASC")
    List<AuditLog> findAllNonArchivedOrderById();

    /**
     * Query with multiple filters and pagination
     */
    @Query("""
        SELECT a FROM AuditLog a
        WHERE (a.isArchived = false OR :includeArchived = true)
        AND (:actorId IS NULL OR a.actorId = :actorId)
        AND (:resourceType IS NULL OR a.resourceType = :resourceType)
        AND (:resourceId IS NULL OR a.resourceId = :resourceId)
        AND (:eventType IS NULL OR a.eventType = :eventType)
        AND (:fromTimestamp IS NULL OR a.eventTimestamp >= :fromTimestamp)
        AND (:toTimestamp IS NULL OR a.eventTimestamp <= :toTimestamp)
        ORDER BY a.id DESC
        """)
    Page<AuditLog> findWithFilters(
        @Param("actorId") String actorId,
        @Param("resourceType") String resourceType,
        @Param("resourceId") String resourceId,
        @Param("eventType") String eventType,
        @Param("fromTimestamp") LocalDateTime fromTimestamp,
        @Param("toTimestamp") LocalDateTime toTimestamp,
        @Param("includeArchived") Boolean includeArchived,
        Pageable pageable
    );

    /**
     * Find records by actorId
     */
    Page<AuditLog> findByActorIdAndIsArchivedFalse(String actorId, Pageable pageable);

    /**
     * Find records by resourceType and resourceId
     */
    Page<AuditLog> findByResourceTypeAndResourceIdAndIsArchivedFalse(
        String resourceType,
        String resourceId,
        Pageable pageable
    );

    /**
     * Find records by eventType
     */
    Page<AuditLog> findByEventTypeAndIsArchivedFalse(String eventType, Pageable pageable);

    /**
     * Find records by timestamp range
     */
    Page<AuditLog> findByEventTimestampBetweenAndIsArchivedFalse(
        LocalDateTime from,
        LocalDateTime to,
        Pageable pageable
    );

    /**
     * Check if contentHash exists (for duplicate detection)
     */
    boolean existsByContentHash(String contentHash);

    /**
     * Find record by contentHash
     */
    Optional<AuditLog> findByContentHash(String contentHash);

    /**
     * Get the record count for chain verification
     */
    long countByIsArchivedFalse();

    /**
     * Find oldest non-archived record
     */
    @Query("SELECT a FROM AuditLog a WHERE a.isArchived = false ORDER BY a.id ASC LIMIT 1")
    Optional<AuditLog> findFirstNonArchivedRecord();
}
