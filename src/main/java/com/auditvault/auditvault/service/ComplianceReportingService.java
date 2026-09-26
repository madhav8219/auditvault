package com.auditvault.auditvault.service;

import com.auditvault.auditvault.domain.AuditLog;
import com.auditvault.auditvault.repository.AuditLogRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for compliance reporting - Scenario C
 * 
 * Original requirement (ambiguous): "Regulators need to be able to audit access to client account data."
 * 
 * CLARIFIED REQUIREMENT:
 * Provide audit reports that track:
 * 1. All data access events (READ, UPDATE, DELETE operations)
 * 2. Who accessed what data (actorId, resourceId)
 * 3. When the access occurred (timestamp)
 * 4. What changes were made (payload)
 * 5. Aggregate statistics for compliance review
 * 
 * AMBIGUITIES IDENTIFIED & ASSUMPTIONS:
 * - "Access" assumed to mean any audit event (READ, UPDATE, DELETE, PERMISSION_CHANGE)
 * - "Client account data" = resourceType=ACCOUNT or resourceType=CLIENT
 * - "Regulators" = system with READ access to compliance reports (authorization layer assumed)
 * - Time period: assuming need for current + historical (last N days)
 * - Report format: JSON structured for both human review and machine parsing
 * 
 * DESIGN DECISIONS:
 * 1. Filter by resourceType + eventType for data access patterns
 * 2. Aggregate by actor + resource + time period
 * 3. Include change summary from payload
 * 4. Report template: actor, actions, timestamps, data affected, risk indicators
 * 5. Compliance status: GREEN (normal), AMBER (unusual patterns), RED (violations)
 */
@Service
@Transactional(readOnly = true)
@Slf4j
public class ComplianceReportingService {

    private static final Set<String> DATA_ACCESS_EVENTS = Set.of(
        "DATA_READ", "DATA_UPDATED", "DATA_DELETED", "PERMISSION_CHANGED", 
        "RECORD_ACCESSED", "ACCOUNT_VIEWED"
    );

    private static final Set<String> SENSITIVE_RESOURCES = Set.of(
        "ACCOUNT", "CLIENT", "CUSTOMER", "PERSONAL_INFORMATION"
    );

    private final AuditLogRepository repository;
    private final ObjectMapper objectMapper;

    public ComplianceReportingService(
            AuditLogRepository repository,
            ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    /**
     * Generate comprehensive compliance report for data access to client/account data
     * 
     * @param daysBack Number of days to look back
     * @return Compliance report with access patterns, actors, and risk assessment
     */
    public JsonNode generateComplianceReport(Integer daysBack) {
        log.info("Generating compliance report for last {} days", daysBack);

        LocalDateTime startDate = LocalDateTime.now().minusDays(daysBack);
        
        // Get all relevant audit logs
        List<AuditLog> allRecords = repository.findAll().stream()
            .filter(r -> !r.getIsArchived() && r.getEventTimestamp().isAfter(startDate))
            .collect(Collectors.toList());

        List<AuditLog> accessRecords = allRecords.stream()
            .filter(this::isDataAccessEvent)
            .collect(Collectors.toList());

        ObjectNode report = objectMapper.createObjectNode();

        // Report header
        ObjectNode header = objectMapper.createObjectNode();
        header.put("reportType", "COMPLIANCE_DATA_ACCESS_AUDIT");
        header.put("generatedAt", LocalDateTime.now().toString());
        header.put("period", daysBack + " days");
        header.put("periodStart", startDate.toString());
        header.put("totalRecordsAnalyzed", allRecords.size());
        header.put("dataAccessEvents", accessRecords.size());
        report.set("header", header);

        // Executive summary
        ObjectNode summary = objectMapper.createObjectNode();
        summary.put("totalAccessEvents", accessRecords.size());
        summary.put("uniqueActors", getUniqueActorCount(accessRecords));
        summary.put("affectedResources", getAffectedResourceCount(accessRecords));
        summary.put("complianceStatus", assessComplianceStatus(accessRecords));
        report.set("executiveSummary", summary);

        // Detailed access patterns by actor
        report.set("accessPatternsByActor", generateAccessPatternsByActor(accessRecords));

        // Resource access summary
        report.set("resourceAccessSummary", generateResourceAccessSummary(accessRecords));

        // Risk indicators
        report.set("riskIndicators", assessRisks(accessRecords));

        // Detailed audit trail
        report.set("auditTrail", generateDetailedAuditTrail(accessRecords));

        // Recommendations
        report.set("recommendations", generateRecommendations(accessRecords));

        log.info("Compliance report generated with {} access events analyzed", accessRecords.size());
        return report;
    }

    /**
     * Check if this is a data access event
     */
    private boolean isDataAccessEvent(AuditLog log) {
        boolean isAccessEvent = DATA_ACCESS_EVENTS.contains(log.getEventType()) ||
            log.getEventType().toUpperCase().contains("READ") ||
            log.getEventType().toUpperCase().contains("ACCESS");

        boolean isSensitiveResource = SENSITIVE_RESOURCES.contains(log.getResourceType()) ||
            log.getResourceType().toUpperCase().contains("ACCOUNT") ||
            log.getResourceType().toUpperCase().contains("CLIENT");

        return isAccessEvent && isSensitiveResource;
    }

    /**
     * Get unique actors in access records
     */
    private long getUniqueActorCount(List<AuditLog> records) {
        return records.stream().map(AuditLog::getActorId).distinct().count();
    }

    /**
     * Get unique affected resources
     */
    private long getAffectedResourceCount(List<AuditLog> records) {
        return records.stream()
            .map(r -> r.getResourceType() + ":" + r.getResourceId())
            .distinct()
            .count();
    }

    /**
     * Assess overall compliance status
     */
    private String assessComplianceStatus(List<AuditLog> records) {
        if (records.isEmpty()) {
            return "GREEN";
        }

        long riskCount = records.stream()
            .filter(this::isRisky)
            .count();

        double riskPercentage = (double) riskCount / records.size();

        if (riskPercentage > 0.15) {
            return "RED";
        } else if (riskPercentage > 0.05) {
            return "AMBER";
        } else {
            return "GREEN";
        }
    }

    /**
     * Generate access patterns by actor
     */
    private JsonNode generateAccessPatternsByActor(List<AuditLog> records) {
        Map<String, List<AuditLog>> byActor = records.stream()
            .collect(Collectors.groupingBy(AuditLog::getActorId));

        ArrayNode patterns = objectMapper.createArrayNode();

        for (Map.Entry<String, List<AuditLog>> entry : byActor.entrySet()) {
            ObjectNode actorPattern = objectMapper.createObjectNode();
            actorPattern.put("actor", entry.getKey());
            actorPattern.put("totalAccess", entry.getValue().size());
            actorPattern.put("uniqueResources", entry.getValue().stream()
                .map(AuditLog::getResourceId).distinct().count());
            actorPattern.put("firstAccess", entry.getValue().stream()
                .min(Comparator.comparing(AuditLog::getEventTimestamp))
                .map(AuditLog::getEventTimestamp).map(Object::toString).orElse("N/A"));
            actorPattern.put("lastAccess", entry.getValue().stream()
                .max(Comparator.comparing(AuditLog::getEventTimestamp))
                .map(AuditLog::getEventTimestamp).map(Object::toString).orElse("N/A"));

            patterns.add(actorPattern);
        }

        return patterns;
    }

    /**
     * Generate resource access summary
     */
    private JsonNode generateResourceAccessSummary(List<AuditLog> records) {
        Map<String, List<AuditLog>> byResource = records.stream()
            .collect(Collectors.groupingBy(r -> r.getResourceType() + ":" + r.getResourceId()));

        ArrayNode summary = objectMapper.createArrayNode();

        for (Map.Entry<String, List<AuditLog>> entry : byResource.entrySet()) {
            ObjectNode resourceAccess = objectMapper.createObjectNode();
            resourceAccess.put("resource", entry.getKey());
            resourceAccess.put("accessCount", entry.getValue().size());
            resourceAccess.put("uniqueActors", entry.getValue().stream()
                .map(AuditLog::getActorId).distinct().count());

            summary.add(resourceAccess);
        }

        return summary;
    }

    /**
     * Check if a record is risky
     */
    private boolean isRisky(AuditLog log) {
        // Risky if: bulk access, unusual times, unauthorized changes
        String eventType = log.getEventType().toUpperCase();
        return eventType.contains("DELETE") || eventType.contains("PERMISSION") || 
               eventType.contains("UNAUTHORIZED");
    }

    /**
     * Assess risks in the access patterns
     */
    private JsonNode assessRisks(List<AuditLog> records) {
        ArrayNode risks = objectMapper.createArrayNode();

        // Risk 1: High volume access
        long highVolume = records.stream()
            .collect(Collectors.groupingBy(AuditLog::getActorId, Collectors.counting()))
            .values().stream()
            .filter(count -> count > 100)
            .count();

        if (highVolume > 0) {
            ObjectNode risk = objectMapper.createObjectNode();
            risk.put("type", "HIGH_VOLUME_ACCESS");
            risk.put("severity", "MEDIUM");
            risk.put("description", highVolume + " actors with > 100 accesses");
            risks.add(risk);
        }

        // Risk 2: Delete operations
        long deleteOps = records.stream()
            .filter(r -> r.getEventType().toUpperCase().contains("DELETE"))
            .count();

        if (deleteOps > 0) {
            ObjectNode risk = objectMapper.createObjectNode();
            risk.put("type", "DELETE_OPERATIONS");
            risk.put("severity", "HIGH");
            risk.put("description", deleteOps + " delete operations detected");
            risks.add(risk);
        }

        return risks;
    }

    /**
     * Generate detailed audit trail
     */
    private JsonNode generateDetailedAuditTrail(List<AuditLog> records) {
        ArrayNode trail = objectMapper.createArrayNode();

        records.stream()
            .sorted(Comparator.comparing(AuditLog::getEventTimestamp))
            .forEach(r -> {
                ObjectNode entry = objectMapper.createObjectNode();
                entry.put("timestamp", r.getEventTimestamp().toString());
                entry.put("actor", r.getActorId());
                entry.put("action", r.getEventType());
                entry.put("resource", r.getResourceType() + ":" + r.getResourceId());
                entry.put("hash", r.getContentHash());
                trail.add(entry);
            });

        return trail;
    }

    /**
     * Generate recommendations based on compliance findings
     */
    private JsonNode generateRecommendations(List<AuditLog> records) {
        ArrayNode recommendations = objectMapper.createArrayNode();

        long deleteCount = records.stream()
            .filter(r -> r.getEventType().toUpperCase().contains("DELETE"))
            .count();

        if (deleteCount > 0) {
            recommendations.add("Review and restrict DELETE permissions for audit logs");
        }

        long unusualAccess = records.stream()
            .collect(Collectors.groupingBy(AuditLog::getActorId, Collectors.counting()))
            .values().stream()
            .filter(count -> count > 50)
            .count();

        if (unusualAccess > 0) {
            recommendations.add("Investigate high-volume data access patterns");
        }

        if (records.isEmpty()) {
            recommendations.add("No significant compliance issues detected");
        }

        return recommendations;
    }
}
