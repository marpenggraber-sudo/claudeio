package org.example.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * 播放历史记录请求 DTO
 */
public record PlayHistoryRequest(
    @NotNull(message = "用户ID不能为空")
    @Positive(message = "用户ID必须为正数")
    Long userId,

    @NotNull(message = "歌曲ID不能为空")
    @Positive(message = "歌曲ID必须为正数")
    Long songId,

    @NotBlank(message = "歌曲名称不能为空")
    String songName,

    @NotBlank(message = "艺术家不能为空")
    String artist,

    String album,

    Long duration,

    Boolean completed
) {}
