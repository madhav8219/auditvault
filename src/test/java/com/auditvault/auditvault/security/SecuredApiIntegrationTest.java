package com.auditvault.auditvault.security;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class SecuredApiIntegrationTest {

    @LocalServerPort
    private int port;

    private RestTemplate restTemplate;
    private String baseUrl;
    private String token;
    private String username;
    private String password;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        baseUrl = "http://localhost:" + port;
        username = "user-" + UUID.randomUUID();
        password = "Pass!" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> registerRequest = new HttpEntity<>(
            String.format("{\"username\":\"%s\",\"email\":\"%s@example.com\",\"password\":\"%s\"}", username, username, password),
            headers
        );

        ResponseEntity<String> registerResponse = restTemplate.exchange(
            baseUrl + "/auth/register",
            HttpMethod.POST,
            registerRequest,
            String.class
        );

        assertThat(registerResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode registerBody = objectMapper.readTree(registerResponse.getBody());
        token = registerBody.get("token").asText();
        assertThat(token).isNotBlank();
    }

    @Test
    void publicApisStayAccessibleWithoutToken() {
        ResponseEntity<String> root = restTemplate.getForEntity(baseUrl + "/", String.class);
        assertThat(root.getStatusCode()).isEqualTo(HttpStatus.OK);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String uniqueUser = "user-" + UUID.randomUUID();
        String uniquePassword = "Pass!" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        HttpEntity<String> registerRequest = new HttpEntity<>(
            String.format("{\"username\":\"%s\",\"email\":\"%s@example.com\",\"password\":\"%s\"}", uniqueUser, uniqueUser, uniquePassword),
            headers
        );

        ResponseEntity<String> registerResponse = restTemplate.exchange(
            baseUrl + "/auth/register",
            HttpMethod.POST,
            registerRequest,
            String.class
        );

        assertThat(registerResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void securedApisRequireAuthentication() {
        assertThatThrownBy(() -> restTemplate.getForEntity(baseUrl + "/audit/count", String.class))
            .isInstanceOf(HttpClientErrorException.class)
            .satisfies(ex -> assertThat(((HttpClientErrorException) ex).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> restTemplate.postForEntity(baseUrl + "/audit/create", "{\"eventType\":\"CREATE\",\"actorId\":\"admin\",\"resourceType\":\"invoice\",\"resourceId\":\"INV-1001\",\"payload\":{\"orderId\":\"A-42\"}}", String.class))
            .isInstanceOf(HttpClientErrorException.class)
            .satisfies(ex -> assertThat(((HttpClientErrorException) ex).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    void allSecuredApisWorkWithValidJwt() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> createRequest = new HttpEntity<>(
            "{\"eventType\":\"CREATE\",\"actorId\":\"" + username + "\",\"resourceType\":\"invoice\",\"resourceId\":\"INV-1001\",\"payload\":{\"orderId\":\"A-42\",\"amount\":125.50},\"timestamp\":\"2026-08-19T12:00:00\"}",
            headers
        );

        ResponseEntity<String> createResponse = restTemplate.exchange(
            baseUrl + "/audit/create",
            HttpMethod.POST,
            createRequest,
            String.class
        );
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Long createdId = objectMapper.readTree(createResponse.getBody()).get("id").asLong();

        HttpEntity<Void> authEntity = new HttpEntity<>(headers);

        ResponseEntity<String> countResponse = restTemplate.exchange(
            baseUrl + "/audit/count",
            HttpMethod.GET,
            authEntity,
            String.class
        );
        assertThat(countResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> queryResponse = restTemplate.exchange(
            baseUrl + "/audit/query?pageNumber=0&pageSize=10",
            HttpMethod.GET,
            authEntity,
            String.class
        );
        assertThat(queryResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> byIdResponse = restTemplate.exchange(
            baseUrl + "/audit/" + createdId,
            HttpMethod.GET,
            authEntity,
            String.class
        );
        assertThat(byIdResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> verifyResponse = restTemplate.exchange(
            baseUrl + "/audit/verify",
            HttpMethod.GET,
            authEntity,
            String.class
        );
        assertThat(verifyResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> tamperResponse = restTemplate.exchange(
            baseUrl + "/audit/detect-tampering",
            HttpMethod.GET,
            authEntity,
            String.class
        );
        assertThat(tamperResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> reportResponse = restTemplate.exchange(
            baseUrl + "/audit/compliance/report?daysBack=30",
            HttpMethod.GET,
            authEntity,
            String.class
        );
        assertThat(reportResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> archiveResponse = restTemplate.exchange(
            baseUrl + "/audit/retention/archive",
            HttpMethod.POST,
            authEntity,
            String.class
        );
        assertThat(archiveResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> statusResponse = restTemplate.exchange(
            baseUrl + "/audit/retention/status",
            HttpMethod.GET,
            authEntity,
            String.class
        );
        assertThat(statusResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> restoreResponse = restTemplate.exchange(
            baseUrl + "/audit/retention/restore/" + createdId,
            HttpMethod.POST,
            authEntity,
            String.class
        );
        assertThat(restoreResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        HttpEntity<String> redactRequest = new HttpEntity<>(
            "{\"fieldsToRedact\":[\"payload.amount\"]}",
            headers
        );

        ResponseEntity<String> redactResponse = restTemplate.exchange(
            baseUrl + "/audit/redact/" + createdId,
            HttpMethod.POST,
            redactRequest,
            String.class
        );
        assertThat(redactResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> redactionMetaResponse = restTemplate.exchange(
            baseUrl + "/audit/redaction/" + createdId,
            HttpMethod.GET,
            authEntity,
            String.class
        );
        assertThat(redactionMetaResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> exportActorResponse = restTemplate.exchange(
            baseUrl + "/audit/export/actor/" + username,
            HttpMethod.GET,
            authEntity,
            String.class
        );
        assertThat(exportActorResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> exportResourceResponse = restTemplate.exchange(
            baseUrl + "/audit/export/resource/INV-1001",
            HttpMethod.GET,
            authEntity,
            String.class
        );
        assertThat(exportResourceResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
