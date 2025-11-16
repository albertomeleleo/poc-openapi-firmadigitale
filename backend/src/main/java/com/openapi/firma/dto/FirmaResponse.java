package com.openapi.firma.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FirmaResponse {
    private String id;
    private String filename;
    private String title;
    private String description;
    private List<MemberStatus> members;
    private FirmaStatus status;
    private String downloadLink;
    private String callbackStatus;
    private Object callback;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemberStatus {
        private String firstname;
        private String lastname;
        private String email;
        private String phone;
        private String status;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private String signLink;
    }

    public enum FirmaStatus {
        created,
        started,
        finished,
        refused,
        expired,
        request_failed,
        file_validation_failed,
        error
    }
}
