package org.example.dto;

import jakarta.validation.constraints.NotBlank;

public record RegisterRequest(
        @NotBlank String account,
        @NotBlank String password,
        @NotBlank String cookie
) {}
