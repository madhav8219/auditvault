package com.auditvault.auditvault.config;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

@Converter
public class ObjectNodeAttributeConverter implements AttributeConverter<ObjectNode, String> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(ObjectNode attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.toString();
    }

    @Override
    public ObjectNode convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        try {
            return (ObjectNode) OBJECT_MAPPER.readTree(dbData);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JSON payload stored in database", e);
        }
    }
}
