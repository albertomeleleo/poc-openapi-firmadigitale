package com.openapi.firma.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "openapi.firma")
public class OpenApiConfig {

    private Sandbox sandbox;
    private Production production;
    private String apiKey;
    private String environment;
    private RateLimit rateLimit;

    @Data
    public static class Sandbox {
        private String baseUrl;
    }

    @Data
    public static class Production {
        private String baseUrl;
    }

    @Data
    public static class RateLimit {
        private int maxRequests;
        private long timeWindow;
    }

    public String getActiveBaseUrl() {
        if ("production".equalsIgnoreCase(environment)) {
            return production.getBaseUrl();
        }
        return sandbox.getBaseUrl();
    }
}
