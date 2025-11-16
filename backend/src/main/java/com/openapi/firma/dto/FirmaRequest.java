package com.openapi.firma.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class FirmaRequest {

    private String title;
    private String description;
    private String ui;

    @Builder.Default
    private String filename = "fes_" + System.currentTimeMillis() + ".pdf";

    @NotBlank(message = "Content is required")
    private String content; // Base64 encoded PDF

    @NotEmpty(message = "At least one member is required")
    @Valid
    private List<Member> members;

    @Valid
    private Callback callback;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public static class Member {
        @NotBlank(message = "Firstname is required")
        private String firstname;

        @NotBlank(message = "Lastname is required")
        private String lastname;

        @NotBlank(message = "Email is required")
        private String email;

        @NotBlank(message = "Phone is required")
        @Pattern(regexp = "^\\+[1-9]\\d{1,14}$", message = "Phone must include international prefix starting with '+'")
        private String phone;

        @NotEmpty(message = "At least one signature position is required")
        @Valid
        private List<SignPosition> signs;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public static class SignPosition {
        private Integer page; // Page number (starts at 1)
        private String position; // Bounding box: "x1,y1,x2,y2"
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public static class Callback {
        private String field;
        private String url;
        private Object headers;
    }
}
