package org.example.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ChatRequest(
        @NotBlank(message = "消息内容不能为空") String message,
        @NotNull(message = "用户ID不能为空")
        @Positive(message = "用户ID必须为正数") Long userId
) {}
