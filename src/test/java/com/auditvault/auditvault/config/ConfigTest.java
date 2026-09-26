package com.auditvault.auditvault.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import static org.junit.jupiter.api.Assertions.*;

class ConfigTest {

    @Test
    void jacksonConfig_createsObjectMapperBean() {
        JacksonConfig config = new JacksonConfig();
        ObjectMapper mapper = config.objectMapper();

        assertNotNull(mapper);
        assertTrue(mapper instanceof ObjectMapper);
    }

    @Test
    void jsonNodeAttributeConverter_convertsToJsonString() {
        JsonNodeAttributeConverter converter = new JsonNodeAttributeConverter();
        ObjectMapper mapper = new ObjectMapper();

        JsonNode jsonNode = mapper.createObjectNode()
            .put("key1", "value1")
            .put("key2", 123);

        String result = converter.convertToDatabaseColumn(jsonNode);

        assertNotNull(result);
        assertTrue(result.contains("key1"));
        assertTrue(result.contains("value1"));
    }

    @Test
    void jsonNodeAttributeConverter_convertsNullToNull() {
        JsonNodeAttributeConverter converter = new JsonNodeAttributeConverter();

        String result = converter.convertToDatabaseColumn(null);

        assertNull(result);
    }

    @Test
    void jsonNodeAttributeConverter_convertsStringToJsonNode() {
        JsonNodeAttributeConverter converter = new JsonNodeAttributeConverter();

        String jsonString = "{\"key\":\"value\",\"number\":42}";

        JsonNode result = converter.convertToEntityAttribute(jsonString);

        assertNotNull(result);
        assertEquals("value", result.get("key").asText());
        assertEquals(42, result.get("number").asInt());
    }

    @Test
    void jsonNodeAttributeConverter_convertsNullStringToNull() {
        JsonNodeAttributeConverter converter = new JsonNodeAttributeConverter();

        JsonNode result = converter.convertToEntityAttribute(null);

        assertNull(result);
    }

    @Test
    void jsonNodeAttributeConverter_convertsBlankStringToNull() {
        JsonNodeAttributeConverter converter = new JsonNodeAttributeConverter();

        JsonNode result = converter.convertToEntityAttribute("   ");

        assertNull(result);
    }

    @Test
    void jsonNodeAttributeConverter_throwsExceptionForInvalidJson() {
        JsonNodeAttributeConverter converter = new JsonNodeAttributeConverter();

        String invalidJson = "not valid json";

        assertThrows(IllegalArgumentException.class, () -> {
            converter.convertToEntityAttribute(invalidJson);
        });
    }

    @Test
    void objectNodeAttributeConverter_convertsToJsonString() {
        ObjectNodeAttributeConverter converter = new ObjectNodeAttributeConverter();
        ObjectMapper mapper = new ObjectMapper();

        ObjectNode objectNode = mapper.createObjectNode()
            .put("key1", "value1")
            .put("key2", 123);

        String result = converter.convertToDatabaseColumn(objectNode);

        assertNotNull(result);
        assertTrue(result.contains("key1"));
        assertTrue(result.contains("value1"));
    }

    @Test
    void objectNodeAttributeConverter_convertsNullToNull() {
        ObjectNodeAttributeConverter converter = new ObjectNodeAttributeConverter();

        String result = converter.convertToDatabaseColumn(null);

        assertNull(result);
    }

    @Test
    void objectNodeAttributeConverter_convertsStringToObjectNode() {
        ObjectNodeAttributeConverter converter = new ObjectNodeAttributeConverter();

        String jsonString = "{\"key\":\"value\",\"number\":42}";

        ObjectNode result = converter.convertToEntityAttribute(jsonString);

        assertNotNull(result);
        assertEquals("value", result.get("key").asText());
        assertEquals(42, result.get("number").asInt());
        assertTrue(result instanceof ObjectNode);
    }

    @Test
    void objectNodeAttributeConverter_convertsNullStringToNull() {
        ObjectNodeAttributeConverter converter = new ObjectNodeAttributeConverter();

        ObjectNode result = converter.convertToEntityAttribute(null);

        assertNull(result);
    }

    @Test
    void objectNodeAttributeConverter_convertsBlankStringToNull() {
        ObjectNodeAttributeConverter converter = new ObjectNodeAttributeConverter();

        ObjectNode result = converter.convertToEntityAttribute("   ");

        assertNull(result);
    }

    @Test
    void objectNodeAttributeConverter_throwsExceptionForInvalidJson() {
        ObjectNodeAttributeConverter converter = new ObjectNodeAttributeConverter();

        String invalidJson = "not valid json";

        assertThrows(IllegalArgumentException.class, () -> {
            converter.convertToEntityAttribute(invalidJson);
        });
    }

    @Test
    void objectNodeAttributeConverter_throwsExceptionForArrayJson() {
        ObjectNodeAttributeConverter converter = new ObjectNodeAttributeConverter();

        String arrayJson = "[1,2,3]";

        assertThrows(IllegalArgumentException.class, () -> {
            converter.convertToEntityAttribute(arrayJson);
        });
    }

    @Test
    void openApiConfig_createsOpenAPIBean() {
        OpenApiConfig config = new OpenApiConfig();
        // Simulate the @Value injection by setting the field directly
        try {
            java.lang.reflect.Field field = OpenApiConfig.class.getDeclaredField("serverPort");
            field.setAccessible(true);
            field.set(config, "8080");
        } catch (Exception e) {
            fail("Failed to set serverPort field: " + e.getMessage());
        }

        OpenAPI openAPI = config.auditVaultOpenAPI();

        assertNotNull(openAPI);
        assertNotNull(openAPI.getInfo());
        assertEquals("AuditVault API", openAPI.getInfo().getTitle());
        assertEquals("1.0", openAPI.getInfo().getVersion());
        assertNotNull(openAPI.getInfo().getContact());
        assertEquals("AuditVault Support", openAPI.getInfo().getContact().getName());
        assertEquals("support@auditvault.com", openAPI.getInfo().getContact().getEmail());
        assertNotNull(openAPI.getInfo().getLicense());
        assertEquals("MIT License", openAPI.getInfo().getLicense().getName());
        assertNotNull(openAPI.getServers());
        assertEquals(1, openAPI.getServers().size());
        assertNotNull(openAPI.getComponents());
        assertNotNull(openAPI.getComponents().getSecuritySchemes());
        assertNotNull(openAPI.getSecurity());
        assertFalse(openAPI.getSecurity().isEmpty());
    }

    @Test
    void openApiConfig_serverUrlUsesConfiguredPort() {
        OpenApiConfig config = new OpenApiConfig();
        try {
            java.lang.reflect.Field field = OpenApiConfig.class.getDeclaredField("serverPort");
            field.setAccessible(true);
            field.set(config, "9090");
        } catch (Exception e) {
            fail("Failed to set serverPort field: " + e.getMessage());
        }

        OpenAPI openAPI = config.auditVaultOpenAPI();

        Server server = openAPI.getServers().get(0);
        assertEquals("http://localhost:9090", server.getUrl());
        assertEquals("Development server", server.getDescription());
    }

    @Test
    void openApiConfig_descriptionContainsExpectedContent() {
        OpenApiConfig config = new OpenApiConfig();
        try {
            java.lang.reflect.Field field = OpenApiConfig.class.getDeclaredField("serverPort");
            field.setAccessible(true);
            field.set(config, "8080");
        } catch (Exception e) {
            fail("Failed to set serverPort field: " + e.getMessage());
        }

        OpenAPI openAPI = config.auditVaultOpenAPI();

        String description = openAPI.getInfo().getDescription();
        assertNotNull(description);
        assertTrue(description.contains("tamper-evident"));
        assertTrue(description.contains("hash chain verification"));
        assertTrue(description.contains("compliance reporting"));
        assertTrue(description.contains("cryptographic hashing"));
    }

    @Test
    void openApiConfig_configuresJwtSecurityScheme() {
        OpenApiConfig config = new OpenApiConfig();
        try {
            java.lang.reflect.Field field = OpenApiConfig.class.getDeclaredField("serverPort");
            field.setAccessible(true);
            field.set(config, "8080");
        } catch (Exception e) {
            fail("Failed to set serverPort field: " + e.getMessage());
        }

        OpenAPI openAPI = config.auditVaultOpenAPI();

        assertNotNull(openAPI.getComponents());
        assertNotNull(openAPI.getComponents().getSecuritySchemes());
        
        io.swagger.v3.oas.models.security.SecurityScheme securityScheme = 
            openAPI.getComponents().getSecuritySchemes().get("Bearer Authentication");
        
        assertNotNull(securityScheme);
        assertEquals(io.swagger.v3.oas.models.security.SecurityScheme.Type.HTTP, securityScheme.getType());
        assertEquals("bearer", securityScheme.getScheme());
        assertEquals("JWT", securityScheme.getBearerFormat());
        assertTrue(securityScheme.getDescription().contains("JWT token authentication"));
    }

    @Test
    void createAuditLogRequestSchema_explicitlyDefinesJsonObjectPayload() throws Exception {
        java.lang.reflect.Field payloadField = com.auditvault.auditvault.dto.CreateAuditLogRequest.class.getDeclaredField("payload");
        io.swagger.v3.oas.annotations.media.Schema schema = payloadField.getAnnotation(io.swagger.v3.oas.annotations.media.Schema.class);

        assertNotNull(schema, "payload field should include Swagger schema metadata");
        assertEquals("object", schema.type());
        assertTrue(schema.description().contains("JSON object payload"));
        assertEquals("tools.jackson.databind.JsonNode", payloadField.getType().getName());
    }
}
