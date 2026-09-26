package com.auditvault.auditvault.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

/**
 * Provides the concrete Jackson ObjectMapper expected by the app's service layer,
 * validation model, and Hibernate JSON column handling.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
