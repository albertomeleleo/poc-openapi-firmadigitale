package com.openapi.firma.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openapi.firma.config.OpenApiConfig;
import com.openapi.firma.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@Profile("!mock")
public class OpenApiFirmaClient {
    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();

        // Rimuovi i converter di default
        restTemplate.getMessageConverters().clear();

        // Aggiungi StringHttpMessageConverter senza charset
        StringHttpMessageConverter stringConverter = new StringHttpMessageConverter(StandardCharsets.UTF_8);
        stringConverter.setWriteAcceptCharset(false);

        // Supporta solo application/json senza charset
        List<MediaType> supportedMediaTypes = new ArrayList<>();
        supportedMediaTypes.add(MediaType.APPLICATION_JSON);
        stringConverter.setSupportedMediaTypes(supportedMediaTypes);

        restTemplate.getMessageConverters().add(stringConverter);
        restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter());

        return restTemplate;
    }
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
        headers.set("Content-Type", "application/json");
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
        request.getMembers().get(0).getSigns().get(0).setPosition("10,15,45,35");
        // Log content length to verify it's not truncated
        int contentLength = request.getContent() != null ? request.getContent().length() : 0;
        log.info("Request content (base64) length: {} characters", contentLength);
        if (request.getTitle() == null) request.setTitle("");
        if (request.getDescription() == null) request.setDescription("");
        if (request.getUi() == null) request.setUi("");

        // Validazione pre-invio
        if (request.getContent() == null || request.getContent().isEmpty()) {
            log.error("Content is null or empty!");
        }
        if (request.getMembers() == null || request.getMembers().isEmpty()) {
            log.error("Members list is null or empty!");
        }
        // Verifica che ogni member abbia signs
        for (FirmaRequest.Member member : request.getMembers()) {
            if (member.getSigns() == null || member.getSigns().isEmpty()) {
                log.error("Member {} has no signs!", member.getEmail());
            }
        }
        HttpHeaders headers = createAuthHeaders();

        String url = config.getActiveBaseUrl() + "/firma_elettronica/base";
        String requestJson = null;

        try {
            // Serialize to JSON string first
            requestJson = objectMapper.writeValueAsString(request);

            // Log only the size and structure, not the full content
            log.info("POST {} - JSON size: {} bytes", url, requestJson.length());
            log.info("Request headers: {}", headers);
            System.out.println("LENGTH JSON: " + requestJson.length());
            // Verify the content field in the JSON
            int contentStartIdx = requestJson.indexOf("\"content\":\"");
            if (contentStartIdx != -1) {
                int contentEndIdx = requestJson.indexOf("\"", contentStartIdx + 11);
                if (contentEndIdx != -1) {
                    int jsonContentLength = contentEndIdx - (contentStartIdx + 11);
                    log.info("Content field in JSON: {} characters", jsonContentLength);
                    if (jsonContentLength != contentLength) {
                        log.error("CONTENT TRUNCATION DETECTED! Original: {}, In JSON: {}", contentLength, jsonContentLength);
                    }
                }
            }

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
