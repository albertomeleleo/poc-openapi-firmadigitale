package com.openapi.firma.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openapi.firma.config.OpenApiConfig;
import com.openapi.firma.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mock implementation of OpenAPI Firma Client
 * Used for development/testing without real API calls
 */
@Slf4j
@Component
@Profile("mock")
public class MockOpenApiFirmaClient extends OpenApiFirmaClient {

    private final Map<String, FirmaResponse> mockDatabase = new ConcurrentHashMap<>();

    public MockOpenApiFirmaClient(OpenApiConfig config, ObjectMapper objectMapper) {
        super(config, objectMapper);
        log.info("MockOpenApiFirmaClient initialized - No real API calls will be made");
    }

    @Override
    public ApiResponse<FirmaResponse> createFirmaRequest(FirmaRequest request) {
        log.info("[MOCK] Creating firma request for document: {}", request.getFilename());

        String requestId = UUID.randomUUID().toString();

        // Create mock member statuses
        List<FirmaResponse.MemberStatus> memberStatuses = new ArrayList<>();
        for (FirmaRequest.Member member : request.getMembers()) {
            String signLink = "https://mock.firmadigitale.com/sign/" + UUID.randomUUID().toString();

            FirmaResponse.MemberStatus memberStatus = FirmaResponse.MemberStatus.builder()
                    .firstname(member.getFirstname())
                    .lastname(member.getLastname())
                    .email(member.getEmail())
                    .phone(member.getPhone())
                    .status("pending")
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .signLink(signLink)
                    .build();

            memberStatuses.add(memberStatus);
        }

        // Create mock firma response
        FirmaResponse firmaResponse = FirmaResponse.builder()
                .id(requestId)
                .filename(request.getFilename())
                .title(request.getTitle())
                .description(request.getDescription())
                .members(memberStatuses)
                .status(FirmaResponse.FirmaStatus.created)
                .downloadLink(null) // Not available until signed
                .build();

        // Store in mock database
        mockDatabase.put(requestId, firmaResponse);

        log.info("[MOCK] Firma request created successfully with ID: {}", requestId);

        return ApiResponse.<FirmaResponse>builder()
                .data(firmaResponse)
                .success(true)
                .message("Signature request created successfully (MOCK)")
                .build();
    }

    @Override
    public ApiResponse<List<FirmaResponse>> getAllFirmaRequests() {
        log.info("[MOCK] Fetching all firma requests");

        List<FirmaResponse> requests = new ArrayList<>(mockDatabase.values());

        return ApiResponse.<List<FirmaResponse>>builder()
                .data(requests)
                .success(true)
                .message("Retrieved " + requests.size() + " requests (MOCK)")
                .build();
    }

    @Override
    public ApiResponse<FirmaResponse> getFirmaRequestById(String id) {
        log.info("[MOCK] Fetching firma request with ID: {}", id);

        FirmaResponse firmaResponse = mockDatabase.get(id);

        if (firmaResponse == null) {
            return ApiResponse.<FirmaResponse>builder()
                    .success(false)
                    .error("Request not found with ID: " + id)
                    .build();
        }

        // Simulate status progression
        if (firmaResponse.getStatus() == FirmaResponse.FirmaStatus.created) {
            firmaResponse.setStatus(FirmaResponse.FirmaStatus.started);

            // Update first member status
            if (!firmaResponse.getMembers().isEmpty()) {
                firmaResponse.getMembers().get(0).setStatus("signed");
                firmaResponse.getMembers().get(0).setUpdatedAt(LocalDateTime.now());
            }
        }

        return ApiResponse.<FirmaResponse>builder()
                .data(firmaResponse)
                .success(true)
                .message("Request retrieved (MOCK)")
                .build();
    }

    @Override
    public DownloadResponse downloadSignedDocument(String id) {
        log.info("[MOCK] Downloading signed document for ID: {}", id);

        FirmaResponse firmaResponse = mockDatabase.get(id);

        if (firmaResponse == null) {
            return DownloadResponse.builder()
                    .success(false)
                    .error("Request not found with ID: " + id)
                    .build();
        }

        // Mark as finished
        firmaResponse.setStatus(FirmaResponse.FirmaStatus.finished);
        firmaResponse.setDownloadLink("https://mock.firmadigitale.com/download/" + id);

        // Update all members as signed
        firmaResponse.getMembers().forEach(member -> {
            member.setStatus("signed");
            member.setUpdatedAt(LocalDateTime.now());
        });

        // Generate mock base64 PDF content (minimal valid PDF)
        String mockPdfBase64 = "JVBERi0xLjQKJeLjz9MKMSAwIG9iago8PAovVHlwZSAvQ2F0YWxvZwovUGFnZXMgMiAwIFIKPj4KZW5kb2JqCjIgMCBvYmoKPDwKL1R5cGUgL1BhZ2VzCi9LaWRzIFszIDAgUl0KL0NvdW50IDEKL01lZGlhQm94IFswIDAgNTk1IDg0Ml0KPj4KZW5kb2JqCjMgMCBvYmoKPDwKL1R5cGUgL1BhZ2UKL1BhcmVudCAyIDAgUgovUmVzb3VyY2VzIDw8Ci9Gb250IDw8Ci9GMSA0IDAgUgo+Pgo+PgovQ29udGVudHMgNSAwIFIKPj4KZW5kb2JqCjQgMCBvYmoKPDwKL1R5cGUgL0ZvbnQKL1N1YnR5cGUgL1R5cGUxCi9CYXNlRm9udCAvVGltZXMtUm9tYW4KPj4KZW5kb2JqCjUgMCBvYmoKPDwKL0xlbmd0aCA0NAo+PgpzdHJlYW0KQlQKL0YxIDEyIFRmCjEwMCA3MDAgVGQKKE1PQ0sgU0lHTkVEIERPQ1VNRU5UKSBUagpFVAplbmRzdHJlYW0KZW5kb2JqCnhyZWYKMCA2CjAwMDAwMDAwMDAgNjU1MzUgZiAKMDAwMDAwMDAxNSAwMDAwMCBuIAowMDAwMDAwMDY0IDAwMDAwIG4gCjAwMDAwMDAxMzMgMDAwMDAgbiAKMDAwMDAwMDI0NyAwMDAwMCBuIAowMDAwMDAwMzM2IDAwMDAwIG4gCnRyYWlsZXIKPDwKL1NpemUgNgovUm9vdCAxIDAgUgo+PgpzdGFydHhyZWYKNDI5CiUlRU9G";

        return DownloadResponse.builder()
                .content(mockPdfBase64)
                .success(true)
                .message("Document downloaded successfully (MOCK)")
                .build();
    }

    @Override
    public ApiResponse<Object> getAuditTrail(String id) {
        log.info("[MOCK] Fetching audit trail for ID: {}", id);

        FirmaResponse firmaResponse = mockDatabase.get(id);

        if (firmaResponse == null) {
            return ApiResponse.builder()
                    .success(false)
                    .error("Request not found with ID: " + id)
                    .build();
        }

        // Create mock audit trail
        Map<String, Object> auditTrail = Map.of(
                "requestId", id,
                "filename", firmaResponse.getFilename(),
                "createdAt", LocalDateTime.now().minusHours(1).toString(),
                "completedAt", LocalDateTime.now().toString(),
                "events", List.of(
                        Map.of("timestamp", LocalDateTime.now().minusHours(1).toString(), "event", "Request created"),
                        Map.of("timestamp", LocalDateTime.now().minusMinutes(30).toString(), "event", "Email sent to signers"),
                        Map.of("timestamp", LocalDateTime.now().toString(), "event", "Document signed by all members")
                ),
                "mockData", true
        );

        return ApiResponse.builder()
                .data(auditTrail)
                .success(true)
                .message("Audit trail retrieved (MOCK)")
                .build();
    }
}
