package com.openapi.firma.service;

import com.openapi.firma.client.OpenApiFirmaClient;
import com.openapi.firma.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FirmaService {

    private final OpenApiFirmaClient firmaClient;

    /**
     * Create a new signature request
     */
    public ApiResponse<FirmaResponse> createSignatureRequest(FirmaRequest request) {
        log.info("Processing signature request for {} members", request.getMembers().size());

        // Validate request
        if (request.getContent() == null || request.getContent().isEmpty()) {
            return ApiResponse.error("PDF content is required");
        }

        if (request.getMembers() == null || request.getMembers().isEmpty()) {
            return ApiResponse.error("At least one member is required");
        }

        // Validate each member
        for (FirmaRequest.Member member : request.getMembers()) {
            if (member.getSigns() == null || member.getSigns().isEmpty()) {
                return ApiResponse.error("Each member must have at least one signature position");
            }
        }

        try {
            return firmaClient.createFirmaRequest(request);
        } catch (Exception e) {
            log.error("Error creating signature request", e);
            return ApiResponse.error("Failed to create signature request: " + e.getMessage());
        }
    }

    /**
     * Get all signature requests
     */
    public ApiResponse<List<FirmaResponse>> getAllSignatureRequests() {
        log.info("Fetching all signature requests");

        try {
            return firmaClient.getAllFirmaRequests();
        } catch (Exception e) {
            log.error("Error fetching signature requests", e);
            return ApiResponse.error("Failed to fetch signature requests: " + e.getMessage());
        }
    }

    /**
     * Get signature request by ID
     */
    public ApiResponse<FirmaResponse> getSignatureRequestById(String id) {
        log.info("Fetching signature request: {}", id);

        if (id == null || id.trim().isEmpty()) {
            return ApiResponse.error("Request ID is required");
        }

        try {
            return firmaClient.getFirmaRequestById(id);
        } catch (Exception e) {
            log.error("Error fetching signature request: {}", id, e);
            return ApiResponse.error("Failed to fetch signature request: " + e.getMessage());
        }
    }

    /**
     * Download signed document
     */
    public DownloadResponse downloadSignedDocument(String id) {
        log.info("Downloading signed document: {}", id);

        if (id == null || id.trim().isEmpty()) {
            return DownloadResponse.builder()
                    .success(false)
                    .error("Request ID is required")
                    .build();
        }

        try {
            return firmaClient.downloadSignedDocument(id);
        } catch (Exception e) {
            log.error("Error downloading signed document: {}", id, e);
            return DownloadResponse.builder()
                    .success(false)
                    .error("Failed to download signed document: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Get audit trail for a signature request
     */
    public ApiResponse<Object> getAuditTrail(String id) {
        log.info("Fetching audit trail: {}", id);

        if (id == null || id.trim().isEmpty()) {
            return ApiResponse.error("Request ID is required");
        }

        try {
            return firmaClient.getAuditTrail(id);
        } catch (Exception e) {
            log.error("Error fetching audit trail: {}", id, e);
            return ApiResponse.error("Failed to fetch audit trail: " + e.getMessage());
        }
    }
}
