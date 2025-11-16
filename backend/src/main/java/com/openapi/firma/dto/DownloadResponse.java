package com.openapi.firma.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DownloadResponse {
    private String content; // Base64 encoded PDF
    private boolean success;
    private String message;
    private String error;
}
