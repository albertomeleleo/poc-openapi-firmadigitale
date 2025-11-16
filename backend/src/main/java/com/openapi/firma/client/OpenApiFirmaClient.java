package com.openapi.firma.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openapi.firma.config.OpenApiConfig;
import com.openapi.firma.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@Profile("!mock")
public class OpenApiFirmaClient {

    private final RestTemplate restTemplate;
    private final OpenApiConfig config;
    private final ObjectMapper objectMapper;

    public OpenApiFirmaClient(OpenApiConfig config, ObjectMapper objectMapper) {
        this.config = config;
        this.objectMapper = objectMapper;

        // Create RestTemplate with proper Jackson message converter
        this.restTemplate = new RestTemplate();
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setObjectMapper(objectMapper);

        // Keep default converters and add our configured Jackson converter at the beginning
        List<org.springframework.http.converter.HttpMessageConverter<?>> converters = new ArrayList<>();
        converters.add(converter);
        converters.addAll(this.restTemplate.getMessageConverters());
        this.restTemplate.setMessageConverters(converters);

        if (config.getApiKey() == null || config.getApiKey().isEmpty()) {
            log.warn("OpenAPI API Key is not configured. Set OPENAPI_API_KEY environment variable.");
        } else {
            log.info("OpenAPI Client initialized with API Key for environment: {}",
                    config.getEnvironment());
        }
    }

    /**
     * Create authenticated headers with API Key
     */
    private HttpHeaders createAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        // Set content type with UTF-8 charset explicitly
        headers.setContentType(new MediaType("application", "json", java.nio.charset.StandardCharsets.UTF_8));

        // OpenAPI uses Bearer token authentication
        headers.set("Authorization", "Bearer " + config.getApiKey());

        log.debug("Created auth headers with Bearer token");

        return headers;
    }

    /**
     * POST /firma_elettronica/base - Create signature request
     */
    public ApiResponse<FirmaResponse> createFirmaRequest(FirmaRequest request) {
        log.info("Creating firma request for document: {}", request.getFilename());

        HttpHeaders headers = createAuthHeaders();

        String url = config.getActiveBaseUrl() + "/firma_elettronica/base";
        String requestJson = null;

        try {
            // Serialize to JSON string first
            requestJson = objectMapper.writeValueAsString(request);
            log.info("POST {} with body: {}", url, requestJson);
            log.info("Request headers: {}", headers);

            // Send JSON as a String to ensure proper serialization
            HttpEntity<String> entity = new HttpEntity<>(requestJson, headers);

            ResponseEntity<ApiResponse<FirmaResponse>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<ApiResponse<FirmaResponse>>() {}
            );

            log.info("Firma request created successfully with ID: {}",
                    response.getBody() != null && response.getBody().getData() != null
                            ? response.getBody().getData().getId() : "unknown");

            return response.getBody();
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            log.error("HTTP error creating firma request. Status: {}, Response: {}",
                e.getStatusCode(), e.getResponseBodyAsString());
            log.error("Request JSON was: {}", requestJson);
            return ApiResponse.error("Failed to create signature request: " + e.getMessage());
        } catch (Exception e) {
            log.error("Failed to create firma request", e);
            return ApiResponse.error("Failed to create signature request: " + e.getMessage());
        }
    }

    /**
     * GET /firma_elettronica - List all signature requests
     */
    public ApiResponse<List<FirmaResponse>> getAllFirmaRequests() {
        log.info("Fetching all firma requests");

        HttpHeaders headers = createAuthHeaders();
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        String url = config.getActiveBaseUrl() + "/firma_elettronica";

        try {
            ResponseEntity<ApiResponse<List<FirmaResponse>>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<ApiResponse<List<FirmaResponse>>>() {}
            );

            return response.getBody();
        } catch (Exception e) {
            log.error("Failed to fetch firma requests", e);
            return ApiResponse.error("Failed to fetch signature requests: " + e.getMessage());
        }
    }

    /**
     * GET /firma_elettronica/{id} - Get signature request by ID
     */
    public ApiResponse<FirmaResponse> getFirmaRequestById(String id) {
        log.info("Fetching firma request with ID: {}", id);

        HttpHeaders headers = createAuthHeaders();
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        String url = config.getActiveBaseUrl() + "/firma_elettronica/" + id;

        try {
            ResponseEntity<ApiResponse<FirmaResponse>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<ApiResponse<FirmaResponse>>() {}
            );

            return response.getBody();
        } catch (Exception e) {
            log.error("Failed to fetch firma request with ID: {}", id, e);
            return ApiResponse.error("Failed to fetch signature request: " + e.getMessage());
        }
    }

    /**
     * GET /firma_elettronica/{id}/download - Download signed document
     */
    public DownloadResponse downloadSignedDocument(String id) {
        log.info("Downloading signed document for ID: {}", id);

        HttpHeaders headers = createAuthHeaders();
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        String url = config.getActiveBaseUrl() + "/firma_elettronica/" + id + "/download";

        try {
            ResponseEntity<DownloadResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    DownloadResponse.class
            );

            return response.getBody();
        } catch (Exception e) {
            log.error("Failed to download signed document for ID: {}", id, e);
            return DownloadResponse.builder()
                    .success(false)
                    .error("Failed to download signed document: " + e.getMessage())
                    .build();
        }
    }

    /**
     * GET /firma_elettronica/{id}/audit - Get audit trail
     */
    public ApiResponse<Object> getAuditTrail(String id) {
        log.info("Fetching audit trail for ID: {}", id);

        HttpHeaders headers = createAuthHeaders();
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        String url = config.getActiveBaseUrl() + "/firma_elettronica/" + id + "/audit";

        try {
            ResponseEntity<ApiResponse<Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<ApiResponse<Object>>() {}
            );

            return response.getBody();
        } catch (Exception e) {
            log.error("Failed to fetch audit trail for ID: {}", id, e);
            return ApiResponse.error("Failed to fetch audit trail: " + e.getMessage());
        }
    }
}
