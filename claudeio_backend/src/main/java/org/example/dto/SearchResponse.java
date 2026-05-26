package org.example.dto;

import java.util.List;

public record SearchResponse(List<MusicSongDto> songs) {}
