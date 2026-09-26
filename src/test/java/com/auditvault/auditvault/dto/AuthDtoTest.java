package com.auditvault.auditvault.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AuthDtoTest {

    @Test
    void authRequest_builderCreatesValidObject() {
        AuthRequest request = AuthRequest.builder()
            .username("testuser")
            .password("password123")
            .build();

        assertEquals("testuser", request.getUsername());
        assertEquals("password123", request.getPassword());
    }

    @Test
    void authRequest_settersAndGetters() {
        AuthRequest request = new AuthRequest();
        request.setUsername("testuser");
        request.setPassword("password123");

        assertEquals("testuser", request.getUsername());
        assertEquals("password123", request.getPassword());
    }

    @Test
    void authRequest_allArgsConstructor() {
        AuthRequest request = new AuthRequest("testuser", "password123");

        assertEquals("testuser", request.getUsername());
        assertEquals("password123", request.getPassword());
    }

    @Test
    void registerRequest_builderCreatesValidObject() {
        RegisterRequest request = RegisterRequest.builder()
            .username("testuser")
            .email("test@example.com")
            .password("password123")
            .build();

        assertEquals("testuser", request.getUsername());
        assertEquals("test@example.com", request.getEmail());
        assertEquals("password123", request.getPassword());
    }

    @Test
    void registerRequest_settersAndGetters() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("testuser");
        request.setEmail("test@example.com");
        request.setPassword("password123");

        assertEquals("testuser", request.getUsername());
        assertEquals("test@example.com", request.getEmail());
        assertEquals("password123", request.getPassword());
    }

    @Test
    void registerRequest_allArgsConstructor() {
        RegisterRequest request = new RegisterRequest("testuser", "test@example.com", "password123");

        assertEquals("testuser", request.getUsername());
        assertEquals("test@example.com", request.getEmail());
        assertEquals("password123", request.getPassword());
    }

    @Test
    void authResponse_builderCreatesValidObject() {
        AuthResponse response = AuthResponse.builder()
            .token("jwt-token-123")
            .type("Bearer")
            .username("testuser")
            .email("test@example.com")
            .build();

        assertEquals("jwt-token-123", response.getToken());
        assertEquals("Bearer", response.getType());
        assertEquals("testuser", response.getUsername());
        assertEquals("test@example.com", response.getEmail());
    }

    @Test
    void authResponse_defaultTypeIsBearer() {
        AuthResponse response = new AuthResponse();
        assertEquals("Bearer", response.getType());
    }

    @Test
    void authResponse_settersAndGetters() {
        AuthResponse response = new AuthResponse();
        response.setToken("jwt-token-123");
        response.setUsername("testuser");
        response.setEmail("test@example.com");

        assertEquals("jwt-token-123", response.getToken());
        assertEquals("Bearer", response.getType());
        assertEquals("testuser", response.getUsername());
        assertEquals("test@example.com", response.getEmail());
    }

    @Test
    void authResponse_allArgsConstructor() {
        AuthResponse response = new AuthResponse("jwt-token-123", "Bearer", "testuser", "test@example.com");

        assertEquals("jwt-token-123", response.getToken());
        assertEquals("Bearer", response.getType());
        assertEquals("testuser", response.getUsername());
        assertEquals("test@example.com", response.getEmail());
    }

    @Test
    void verificationResult_builderCreatesValidObject() {
        VerificationResult result = VerificationResult.builder()
            .isIntact(true)
            .message("Chain is intact")
            .totalRecordsVerified(100L)
            .build();

        assertTrue(result.getIsIntact());
        assertEquals("Chain is intact", result.getMessage());
        assertEquals(100L, result.getTotalRecordsVerified());
        assertNull(result.getFirstInconsistencyId());
        assertNull(result.getViolationType());
    }

    @Test
    void verificationResult_withTamperingDetails() {
        VerificationResult result = VerificationResult.builder()
            .isIntact(false)
            .message("Tampering detected")
            .violationType("CONTENT_HASH_INVALID")
            .firstInconsistencyId(42L)
            .totalRecordsVerified(50L)
            .build();

        assertFalse(result.getIsIntact());
        assertEquals("Tampering detected", result.getMessage());
        assertEquals("CONTENT_HASH_INVALID", result.getViolationType());
        assertEquals(42L, result.getFirstInconsistencyId());
        assertEquals(50L, result.getTotalRecordsVerified());
    }

    @Test
    void verificationResult_settersAndGetters() {
        VerificationResult result = new VerificationResult();
        result.setIsIntact(true);
        result.setMessage("Test message");
        result.setTotalRecordsVerified(10L);

        assertTrue(result.getIsIntact());
        assertEquals("Test message", result.getMessage());
        assertEquals(10L, result.getTotalRecordsVerified());
    }

    @Test
    void auditLogQueryFilter_builderCreatesValidObject() {
        AuditLogQueryFilter filter = AuditLogQueryFilter.builder()
            .actorId("actor-1")
            .resourceType("ACCOUNT")
            .resourceId("acct-1")
            .eventType("DATA_READ")
            .pageNumber(0)
            .pageSize(10)
            .build();

        assertEquals("actor-1", filter.getActorId());
        assertEquals("ACCOUNT", filter.getResourceType());
        assertEquals("acct-1", filter.getResourceId());
        assertEquals("DATA_READ", filter.getEventType());
        assertEquals(0, filter.getPageNumber());
        assertEquals(10, filter.getPageSize());
    }

    @Test
    void auditLogQueryFilter_defaultPageValues() {
        AuditLogQueryFilter filter = new AuditLogQueryFilter();

        assertEquals(0, filter.getPageNumber());
        assertEquals(50, filter.getPageSize());
    }

    @Test
    void auditLogQueryFilter_nullPageValuesUseDefaults() {
        AuditLogQueryFilter filter = AuditLogQueryFilter.builder()
            .pageNumber(null)
            .pageSize(null)
            .build();

        assertEquals(0, filter.getPageNumber());
        assertEquals(50, filter.getPageSize());
    }

    @Test
    void auditLogQueryFilter_withTimestampRange() {
        java.time.LocalDateTime from = java.time.LocalDateTime.now().minusDays(1);
        java.time.LocalDateTime to = java.time.LocalDateTime.now();

        AuditLogQueryFilter filter = AuditLogQueryFilter.builder()
            .fromTimestamp(from)
            .toTimestamp(to)
            .build();

        assertEquals(from, filter.getFromTimestamp());
        assertEquals(to, filter.getToTimestamp());
    }
}
