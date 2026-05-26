package org.example.dto;

import java.util.List;

public record AgentReply(
    String reply,
    List<MusicSongDto> songs,
    Long songId,
    String action,
    Long newUserId,
    String newNickname
) {}
