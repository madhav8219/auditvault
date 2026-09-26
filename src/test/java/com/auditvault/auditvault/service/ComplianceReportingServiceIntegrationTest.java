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

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ComplianceReportingServiceIntegrationTest {

    @Autowired
    private ComplianceReportingService complianceReportingService;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        auditLogRepository.deleteAll();
    }

    @Test
    void generateComplianceReport_EmptyDatabase() {
        JsonNode report = complianceReportingService.generateComplianceReport(30);

        assertNotNull(report);
        assertTrue(report.has("header"));
        assertTrue(report.has("executiveSummary"));
        assertTrue(report.has("accessPatternsByActor"));
        assertTrue(report.has("resourceAccessSummary"));
        assertTrue(report.has("riskIndicators"));
        assertTrue(report.has("auditTrail"));
        assertTrue(report.has("recommendations"));

        assertEquals("COMPLIANCE_DATA_ACCESS_AUDIT", report.get("header").get("reportType").asText());
        assertEquals(0, report.get("header").get("totalRecordsAnalyzed").asLong());
        assertEquals(0, report.get("header").get("dataAccessEvents").asLong());
        assertEquals("GREEN", report.get("executiveSummary").get("complianceStatus").asText());
    }

    @Test
    void generateComplianceReport_WithDataAccessEvents() {
        // Create data access events
        for (int i = 0; i < 5; i++) {
            AuditLog record = AuditLog.builder()
                    .eventType("DATA_READ")
                    .actorId("user" + i)
                    .resourceType("ACCOUNT")
                    .resourceId("ACC00" + i)
                    .payload(objectMapper.createObjectNode().put("action", "view"))
                    .eventTimestamp(LocalDateTime.now())
                    .contentHash("hash" + i)
                    .previousHash("prev" + i)
                    .status("ACTIVE")
                    .isArchived(false)
                    .build();
            auditLogRepository.save(record);
        }

        JsonNode report = complianceReportingService.generateComplianceReport(30);

        assertNotNull(report);
        assertTrue(report.get("header").get("dataAccessEvents").asLong() > 0);
        assertTrue(report.get("executiveSummary").get("totalAccessEvents").asLong() > 0);
        assertTrue(report.get("executiveSummary").get("uniqueActors").asLong() > 0);
    }

    @Test
    void generateComplianceReport_WithDeleteOperations() {
        AuditLog record = AuditLog.builder()
                .eventType("DATA_DELETED")
                .actorId("user123")
                .resourceType("ACCOUNT")
                .resourceId("ACC001")
                .payload(objectMapper.createObjectNode())
                .eventTimestamp(LocalDateTime.now())
                .contentHash("hash123")
                .previousHash("prev123")
                .status("ACTIVE")
                .isArchived(false)
                .build();
        auditLogRepository.save(record);

        JsonNode report = complianceReportingService.generateComplianceReport(30);

        assertNotNull(report);
        JsonNode risks = report.get("riskIndicators");
        boolean hasDeleteRisk = false;
        for (JsonNode risk : risks) {
            if ("DELETE_OPERATIONS".equals(risk.get("type").asText())) {
                hasDeleteRisk = true;
                break;
            }
        }
        assertTrue(hasDeleteRisk);
    }

    @Test
    void generateComplianceReport_WithHighVolumeAccess() {
        // Create many access events for same actor
        for (int i = 0; i < 150; i++) {
            AuditLog record = AuditLog.builder()
                    .eventType("DATA_READ")
                    .actorId("user123")
                    .resourceType("ACCOUNT")
                    .resourceId("ACC00" + (i % 10))
                    .payload(objectMapper.createObjectNode().put("index", i))
                    .eventTimestamp(LocalDateTime.now())
                    .contentHash("hash" + i)
                    .previousHash("prev" + i)
                    .status("ACTIVE")
                    .isArchived(false)
                    .build();
            auditLogRepository.save(record);
        }

        JsonNode report = complianceReportingService.generateComplianceReport(30);

        assertNotNull(report);
        JsonNode risks = report.get("riskIndicators");
        boolean hasHighVolumeRisk = false;
        for (JsonNode risk : risks) {
            if ("HIGH_VOLUME_ACCESS".equals(risk.get("type").asText())) {
                hasHighVolumeRisk = true;
                break;
            }
        }
        assertTrue(hasHighVolumeRisk);
    }

    @Test
    void generateComplianceReport_WithPermissionChanges() {
        AuditLog record = AuditLog.builder()
                .eventType("PERMISSION_CHANGED")
                .actorId("admin123")
                .resourceType("ACCOUNT")
                .resourceId("ACC001")
                .payload(objectMapper.createObjectNode().put("permission", "admin"))
                .eventTimestamp(LocalDateTime.now())
                .contentHash("hash123")
                .previousHash("prev123")
                .status("ACTIVE")
                .isArchived(false)
                .build();
        auditLogRepository.save(record);

        JsonNode report = complianceReportingService.generateComplianceReport(30);

        assertNotNull(report);
        assertTrue(report.get("riskIndicators").isArray());
    }

    @Test
    void generateComplianceReport_AccessPatternsByActor() {
        for (int i = 0; i < 3; i++) {
            AuditLog record = AuditLog.builder()
                    .eventType("DATA_READ")
                    .actorId("user123")
                    .resourceType("ACCOUNT")
                    .resourceId("ACC00" + i)
                    .payload(objectMapper.createObjectNode())
                    .eventTimestamp(LocalDateTime.now())
                    .contentHash("hash" + i)
                    .previousHash("prev" + i)
                    .status("ACTIVE")
                    .isArchived(false)
                    .build();
            auditLogRepository.save(record);
        }

        JsonNode report = complianceReportingService.generateComplianceReport(30);

        assertNotNull(report);
        JsonNode patterns = report.get("accessPatternsByActor");
        assertTrue(patterns.isArray());
        assertTrue(patterns.size() > 0);
        assertTrue(patterns.get(0).has("actor"));
        assertTrue(patterns.get(0).get("totalAccess").asLong() > 0);
        assertTrue(patterns.get(0).get("uniqueResources").asLong() > 0);
    }

    @Test
    void generateComplianceReport_ResourceAccessSummary() {
        AuditLog record = AuditLog.builder()
                .eventType("DATA_READ")
                .actorId("user123")
                .resourceType("ACCOUNT")
                .resourceId("ACC001")
                .payload(objectMapper.createObjectNode())
                .eventTimestamp(LocalDateTime.now())
                .contentHash("hash123")
                .previousHash("prev123")
                .status("ACTIVE")
                .isArchived(false)
                .build();
        auditLogRepository.save(record);

        JsonNode report = complianceReportingService.generateComplianceReport(30);

        assertNotNull(report);
        JsonNode summary = report.get("resourceAccessSummary");
        assertTrue(summary.isArray());
        assertTrue(summary.size() > 0);
        assertTrue(summary.get(0).has("resource"));
        assertTrue(summary.get(0).get("accessCount").asLong() > 0);
    }

    @Test
    void generateComplianceReport_Recommendations() {
        AuditLog record = AuditLog.builder()
                .eventType("DATA_DELETED")
                .actorId("user123")
                .resourceType("ACCOUNT")
                .resourceId("ACC001")
                .payload(objectMapper.createObjectNode())
                .eventTimestamp(LocalDateTime.now())
                .contentHash("hash123")
                .previousHash("prev123")
                .status("ACTIVE")
                .isArchived(false)
                .build();
        auditLogRepository.save(record);

        JsonNode report = complianceReportingService.generateComplianceReport(30);

        assertNotNull(report);
        JsonNode recommendations = report.get("recommendations");
        assertTrue(recommendations.isArray());
        boolean hasDeleteRecommendation = false;
        for (JsonNode rec : recommendations) {
            if (rec.asText().contains("DELETE")) {
                hasDeleteRecommendation = true;
                break;
            }
        }
        assertTrue(hasDeleteRecommendation);
    }

    @Test
    void generateComplianceReport_CustomDaysBack() {
        JsonNode report = complianceReportingService.generateComplianceReport(60);

        assertNotNull(report);
        assertEquals("60 days", report.get("header").get("period").asText());
    }

    @Test
    void generateComplianceReport_AuditTrail() {
        AuditLog record = AuditLog.builder()
                .eventType("DATA_READ")
                .actorId("user123")
                .resourceType("ACCOUNT")
                .resourceId("ACC001")
                .payload(objectMapper.createObjectNode())
                .eventTimestamp(LocalDateTime.now())
                .contentHash("hash123")
                .previousHash("prev123")
                .status("ACTIVE")
                .isArchived(false)
                .build();
        auditLogRepository.save(record);

        JsonNode report = complianceReportingService.generateComplianceReport(30);

        assertNotNull(report);
        JsonNode trail = report.get("auditTrail");
        assertTrue(trail.isArray());
        assertTrue(trail.size() > 0);
        assertTrue(trail.get(0).has("timestamp"));
        assertTrue(trail.get(0).has("actor"));
        assertTrue(trail.get(0).has("action"));
        assertTrue(trail.get(0).has("resource"));
    }

    @Test
    void generateComplianceReport_WithArchivedRecords() {
        // Create archived record
        AuditLog archivedRecord = AuditLog.builder()
                .eventType("DATA_READ")
                .actorId("user123")
                .resourceType("ACCOUNT")
                .resourceId("ACC001")
                .payload(objectMapper.createObjectNode())
                .eventTimestamp(LocalDateTime.now())
                .contentHash("hash123")
                .previousHash("prev123")
                .status("ARCHIVED")
                .isArchived(true)
                .build();
        auditLogRepository.save(archivedRecord);

        // Create active record
        AuditLog activeRecord = AuditLog.builder()
                .eventType("DATA_READ")
                .actorId("user456")
                .resourceType("ACCOUNT")
                .resourceId("ACC002")
                .payload(objectMapper.createObjectNode())
                .eventTimestamp(LocalDateTime.now())
                .contentHash("hash456")
                .previousHash("prev456")
                .status("ACTIVE")
                .isArchived(false)
                .build();
        auditLogRepository.save(activeRecord);

        JsonNode report = complianceReportingService.generateComplianceReport(30);

        assertNotNull(report);
        // Archived records should be excluded
        assertEquals(1, report.get("header").get("dataAccessEvents").asLong());
    }
}
