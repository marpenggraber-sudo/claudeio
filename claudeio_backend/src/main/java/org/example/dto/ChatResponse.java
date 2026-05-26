package org.example.dto;

import java.util.List;

public record ChatResponse(String reply, List<MusicSongDto> songs, Long songId) {}
