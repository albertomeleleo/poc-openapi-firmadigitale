package com.openapi.firma.controller;

import com.openapi.firma.dto.*;
import com.openapi.firma.service.FirmaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/firma")
@RequiredArgsConstructor
@Validated
public class FirmaController {

    private final FirmaService firmaService;

    /**
     * POST /api/firma - Create a new signature request
     */
    @PostMapping
    public ResponseEntity<ApiResponse<FirmaResponse>> createSignatureRequest(
            @Valid @RequestBody FirmaRequest request) {

        log.info("Received signature request for document: {}", request.getFilename());

        ApiResponse<FirmaResponse> response = firmaService.createSignatureRequest(request);

        if (response.isSuccess()) {
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * GET /api/firma - Get all signature requests
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<FirmaResponse>>> getAllSignatureRequests() {
        log.info("Fetching all signature requests");

        ApiResponse<List<FirmaResponse>> response = firmaService.getAllSignatureRequests();

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * GET /api/firma/{id} - Get signature request by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FirmaResponse>> getSignatureRequestById(
            @PathVariable String id) {

        log.info("Fetching signature request: {}", id);

        ApiResponse<FirmaResponse> response = firmaService.getSignatureRequestById(id);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    /**
     * GET /api/firma/{id}/download - Download signed document
     */
    @GetMapping("/{id}/download")
    public ResponseEntity<DownloadResponse> downloadSignedDocument(
            @PathVariable String id) {

        log.info("Downloading signed document: {}", id);

        DownloadResponse response = firmaService.downloadSignedDocument(id);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    /**
     * GET /api/firma/{id}/audit - Get audit trail
     */
    @GetMapping("/{id}/audit")
    public ResponseEntity<ApiResponse<Object>> getAuditTrail(
            @PathVariable String id) {

        log.info("Fetching audit trail: {}", id);

        ApiResponse<Object> response = firmaService.getAuditTrail(id);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    /**
     * GET /api/firma/health - Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> healthCheck() {
        return ResponseEntity.ok(
            ApiResponse.success("Firma service is running", "OK")
        );
    }
}
