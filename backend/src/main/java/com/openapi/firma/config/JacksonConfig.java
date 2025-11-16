package com.openapi.firma.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.StreamReadConstraints;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
        ObjectMapper objectMapper = builder.build();

        // Increase the maximum string length to 100MB to support large Base64 PDFs
        objectMapper.getFactory().setStreamReadConstraints(
            StreamReadConstraints.builder()
                .maxStringLength(100_000_000) // 100MB
                .build()
        );

        return objectMapper;
    }
}
