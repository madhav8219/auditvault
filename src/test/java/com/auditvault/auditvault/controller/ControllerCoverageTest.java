package com.auditvault.auditvault.controller;

import com.auditvault.auditvault.dto.AuditLogQueryFilter;
import com.auditvault.auditvault.dto.AuditLogResponse;
import com.auditvault.auditvault.dto.CreateAuditLogRequest;
import com.auditvault.auditvault.dto.VerificationResult;
import com.auditvault.auditvault.exception.AuditLogException;
import com.auditvault.auditvault.service.AuditLogService;
import com.auditvault.auditvault.service.BulkExportService;
import com.auditvault.auditvault.service.ComplianceReportingService;
import com.auditvault.auditvault.service.RedactionService;
import com.auditvault.auditvault.service.RetentionService;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ControllerCoverageTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void auditLogController_coversCreateQueryGetAndCount() {
        AuditLogService service = mock(AuditLogService.class);
        AuditLogController controller = new AuditLogController(service);

        AuditLogResponse response = AuditLogResponse.builder()
            .id(9L)
            .eventType("DATA_READ")
            .actorId("actor-1")
            .resourceType("ACCOUNT")
            .resourceId("acct-1")
            .payload(objectMapper.createObjectNode().put("k", "v"))
            .eventTimestamp(LocalDateTime.now())
            .contentHash("hash-1")
            .previousHash("hash-0")
            .createdAt(LocalDateTime.now())
            .isArchived(false)
            .status("ACTIVE")
            .build();

        when(service.createAuditLog(any(CreateAuditLogRequest.class))).thenReturn(response);
        when(service.queryAuditLogs(any(AuditLogQueryFilter.class))).thenReturn(new PageImpl<>(List.of(response)));
        when(service.getAuditLogById(9L)).thenReturn(Optional.of(response));
        when(service.getRecordCount()).thenReturn(3L);

        CreateAuditLogRequest request = CreateAuditLogRequest.builder()
            .eventType("DATA_READ")
            .actorId("actor-1")
            .resourceType("ACCOUNT")
            .resourceId("acct-1")
            .payload(objectMapper.createObjectNode().put("k", "v"))
            .build();

        ResponseEntity<AuditLogResponse> created = controller.createAuditLog(request);
        assertEquals(HttpStatus.CREATED, created.getStatusCode());
        assertEquals("DATA_READ", created.getBody().getEventType());

        ResponseEntity<Page<AuditLogResponse>> query = controller.queryAuditLogs(
            "actor-1", "ACCOUNT", "acct-1", "DATA_READ",
            LocalDateTime.now().minusDays(1), LocalDateTime.now(), 0, 10);
        assertEquals(HttpStatus.OK, query.getStatusCode());
        assertEquals(1, query.getBody().getTotalElements());

        ResponseEntity<AuditLogResponse> found = controller.getAuditLogById(9L);
        assertEquals(HttpStatus.OK, found.getStatusCode());

        ResponseEntity<AuditLogResponse> missing = controller.getAuditLogById(99L);
        assertEquals(HttpStatus.NOT_FOUND, missing.getStatusCode());

        ResponseEntity<Long> count = controller.getRecordCount();
        assertEquals(200, count.getStatusCode().value());
        assertEquals(3L, count.getBody());
    }

    @Test
    void chainVerificationController_coversVerifyAndDetectTampering() {
        AuditLogService service = mock(AuditLogService.class);
        ChainVerificationController controller = new ChainVerificationController(service);

        VerificationResult intactResult = VerificationResult.builder()
            .isIntact(true)
            .message("Chain integrity verified successfully")
            .totalRecordsVerified(2L)
            .build();

        VerificationResult tamperedResult = VerificationResult.builder()
            .isIntact(false)
            .message("Tampering detected")
            .violationType("CONTENT_HASH_INVALID")
            .firstInconsistencyId(42L)
            .build();

        when(service.verifyChain()).thenReturn(intactResult);
        when(service.detectTampering()).thenReturn(tamperedResult);

        ResponseEntity<VerificationResult> verifyResponse = controller.verifyChain();
        assertEquals(HttpStatus.OK, verifyResponse.getStatusCode());
        assertEquals(Boolean.TRUE, verifyResponse.getBody().getIsIntact());

        ResponseEntity<VerificationResult> tamperResponse = controller.detectTampering();
        assertEquals(HttpStatus.OK, tamperResponse.getStatusCode());
        assertFalse(tamperResponse.getBody().getIsIntact());
        assertEquals("CONTENT_HASH_INVALID", tamperResponse.getBody().getViolationType());
    }

    @Test
    void complianceReportingController_usesDefaultDaysWhenInvalid() {
        ComplianceReportingService service = mock(ComplianceReportingService.class);
        ComplianceReportingController controller = new ComplianceReportingController(service);

        JsonNode report = objectMapper.createObjectNode().put("status", "OK");
        when(service.generateComplianceReport(30)).thenReturn(report);

        ResponseEntity<JsonNode> response = controller.generateComplianceReport(-3);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("OK", response.getBody().get("status").asText());
        verify(service).generateComplianceReport(30);
    }

    @Test
    void retentionAndExportController_coversArchiveRestoreStatusAndExport() {
        RetentionService retentionService = mock(RetentionService.class);
        RedactionService redactionService = mock(RedactionService.class);
        BulkExportService bulkExportService = mock(BulkExportService.class);
        RetentionAndExportController controller = new RetentionAndExportController(
            retentionService, redactionService, bulkExportService);

        when(retentionService.archiveOldRecords()).thenReturn(2L);
        when(retentionService.restoreArchivedRecord(11L)).thenReturn(true);
        when(retentionService.getRetentionWindow()).thenReturn(365);
        when(retentionService.getActiveRecordCount()).thenReturn(4L);
        when(retentionService.getArchivedRecordCount()).thenReturn(1L);

        ResponseEntity<Map<String, Object>> archiveResponse = controller.archiveOldRecords();
        assertEquals(HttpStatus.OK, archiveResponse.getStatusCode());
        assertEquals(2L, archiveResponse.getBody().get("archivedCount"));

        ResponseEntity<Map<String, Object>> restoreResponse = controller.restoreArchivedRecord(11L);
        assertEquals(HttpStatus.OK, restoreResponse.getStatusCode());
        assertEquals(true, restoreResponse.getBody().get("restored"));

        ResponseEntity<Map<String, Object>> statusResponse = controller.getRetentionStatus();
        assertEquals(HttpStatus.OK, statusResponse.getStatusCode());
        assertEquals(365, statusResponse.getBody().get("retentionWindowDays"));

        JsonNode redactionMetadata = objectMapper.createObjectNode().put("password", "abc123");
        when(redactionService.redactSensitiveFields(22L, List.of("payload.password"))).thenReturn(true);
        when(redactionService.getRedactionMetadata(22L)).thenReturn(redactionMetadata);

        ResponseEntity<Map<String, Object>> redactResponse = controller.redactSensitiveFields(
            22L,
            new RetentionAndExportController.RedactRequest(List.of("payload.password"))
        );
        assertEquals(HttpStatus.OK, redactResponse.getStatusCode());
        assertEquals(true, redactResponse.getBody().get("redacted"));

        ResponseEntity<Map<String, Object>> metadataResponse = controller.getRedactionMetadata(22L);
        assertEquals(HttpStatus.OK, metadataResponse.getStatusCode());
        assertNotNull(metadataResponse.getBody().get("redactionMetadata"));

        JsonNode actorBundle = objectMapper.createObjectNode().put("export", "actor");
        JsonNode resourceBundle = objectMapper.createObjectNode().put("export", "resource");
        when(bulkExportService.exportByActorId("actor-1")).thenReturn(actorBundle);
        when(bulkExportService.exportByResourceId("acct-1")).thenReturn(resourceBundle);

        ResponseEntity<JsonNode> actorExport = controller.exportByActorId("actor-1");
        assertEquals(HttpStatus.OK, actorExport.getStatusCode());
        assertEquals("actor", actorExport.getBody().get("export").asText());

        ResponseEntity<JsonNode> resourceExport = controller.exportByResourceId("acct-1");
        assertEquals(HttpStatus.OK, resourceExport.getStatusCode());
        assertEquals("resource", resourceExport.getBody().get("export").asText());
    }

    @Test
    void globalExceptionHandler_handlesAuditLogAndGenericFailures() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        ResponseEntity<Map<String, Object>> badRequest = handler.handleAuditLogException(
            new AuditLogException("bad input")
        );
        assertEquals(HttpStatus.BAD_REQUEST, badRequest.getStatusCode());
        assertEquals("bad input", badRequest.getBody().get("message"));

        ResponseEntity<Map<String, Object>> internalError = handler.handleGenericException(new RuntimeException("boom"));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, internalError.getStatusCode());
        assertEquals("Internal Server Error", internalError.getBody().get("error"));
    }

    @Test
    void authController_coversRegisterAndLogin() {
        com.auditvault.auditvault.service.AuthService authService = mock(com.auditvault.auditvault.service.AuthService.class);
        AuthController controller = new AuthController(authService);

        com.auditvault.auditvault.dto.AuthResponse authResponse = com.auditvault.auditvault.dto.AuthResponse.builder()
            .token("jwt-token-123")
            .type("Bearer")
            .username("testuser")
            .email("test@example.com")
            .build();

        when(authService.register(any(com.auditvault.auditvault.dto.RegisterRequest.class))).thenReturn(authResponse);
        when(authService.login(any(com.auditvault.auditvault.dto.AuthRequest.class))).thenReturn(authResponse);

        com.auditvault.auditvault.dto.RegisterRequest registerRequest = com.auditvault.auditvault.dto.RegisterRequest.builder()
            .username("testuser")
            .email("test@example.com")
            .password("password123")
            .build();

        ResponseEntity<com.auditvault.auditvault.dto.AuthResponse> registerResponse = controller.register(registerRequest);
        assertEquals(HttpStatus.CREATED, registerResponse.getStatusCode());
        assertEquals("jwt-token-123", registerResponse.getBody().getToken());
        assertEquals("testuser", registerResponse.getBody().getUsername());

        com.auditvault.auditvault.dto.AuthRequest loginRequest = com.auditvault.auditvault.dto.AuthRequest.builder()
            .username("testuser")
            .password("password123")
            .build();

        ResponseEntity<com.auditvault.auditvault.dto.AuthResponse> loginResponse = controller.login(loginRequest);
        assertEquals(HttpStatus.OK, loginResponse.getStatusCode());
        assertEquals("jwt-token-123", loginResponse.getBody().getToken());
    }

    @Test
    void homeController_coversHomeEndpoint() {
        HomeController controller = new HomeController();

        Map<String, String> response = controller.home();
        assertEquals("Welcome to AuditVault API", response.get("message"));
        assertEquals("OK", response.get("status"));
        assertEquals("http://localhost:8080/swagger-ui/index.html", response.get("documentation"));
    }
}
