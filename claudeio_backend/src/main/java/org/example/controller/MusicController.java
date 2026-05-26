package org.example.controller;

import jakarta.validation.Valid;
import org.example.dto.AgentReply;
import org.example.dto.ChatRequest;
import org.example.dto.GenreResult;
import org.example.dto.LoginRequest;
import org.example.dto.LoginResponse;
import org.example.dto.MusicSongDto;
import org.example.dto.PlayUrlResponse;
import org.example.dto.RegisterRequest;
import org.example.dto.SearchResponse;
import org.example.service.AgentFacadeService;
import org.example.service.AuthService;
import org.example.service.GenreService;
import org.example.service.MusicApiService;
import org.example.service.PlayHistoryService;
import org.example.service.UserPreferenceService;
import org.example.tools.MusicTools;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/music")
public class MusicController {

    private final MusicApiService musicApiService;
    private final MusicTools musicTools;
    private final AgentFacadeService agentFacadeService;
    private final AuthService authService;
    private final PlayHistoryService playHistoryService;
    private final UserPreferenceService userPreferenceService;
    private final GenreService genreService;

    public MusicController(MusicApiService musicApiService,
                          MusicTools musicTools,
                          AgentFacadeService agentFacadeService,
                          AuthService authService,
                          PlayHistoryService playHistoryService,
                          UserPreferenceService userPreferenceService,
                          GenreService genreService) {
        this.musicApiService = musicApiService;
        this.musicTools = musicTools;
        this.agentFacadeService = agentFacadeService;
        this.authService = authService;
        this.playHistoryService = playHistoryService;
        this.userPreferenceService = userPreferenceService;
        this.genreService = genreService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request.account(), request.password(), request.cookie());
    }

    @PostMapping("/register")
    public LoginResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request.account(), request.password(), request.cookie());
    }

    @GetMapping("/search")
    public SearchResponse search(@RequestParam String keywords, @RequestParam Long userId) {
        List<MusicSongDto> songs = musicTools.searchSongs(keywords, userId);
        return new SearchResponse(songs);
    }

    @GetMapping("/play-url")
    public PlayUrlResponse playUrl(@RequestParam Long songId, @RequestParam Long userId) {
        return new PlayUrlResponse(songId, musicTools.getPlayUrl(songId, userId));
    }

    @GetMapping("/cache-song-id")
    public Long cacheSongId(@RequestParam int index, @RequestParam Long userId) {
        return musicTools.getSongFromCache(index, userId);
    }

    @PostMapping("/chat")
    public AgentReply chat(@Valid @RequestBody ChatRequest request) {
        return agentFacadeService.chat(request.message(), request.userId());
    }

    @GetMapping("/lyric/new")
    public ResponseEntity<?> lyric(@RequestParam Long id) {
        return ResponseEntity.ok(musicApiService.getLyric(id));
    }

    @GetMapping("/greeting")
    public AgentReply getGreeting(@RequestParam Long userId) {
        return agentFacadeService.generateGreeting(userId);
    }

    @PostMapping("/play-history")
    public ResponseEntity<?> recordPlayHistory(@RequestBody Map<String, Object> request) {
        Long userId = Long.valueOf(request.get("userId").toString());
        Long songId = Long.valueOf(request.get("songId").toString());
        String songName = request.get("songName").toString();
        String artist = request.get("artist").toString();
        Integer duration = request.get("duration") != null ? Integer.valueOf(request.get("duration").toString()) : 0;
        Boolean completed = request.get("completed") != null ? Boolean.valueOf(request.get("completed").toString()) : false;

        playHistoryService.recordPlay(userId, songId, songName, artist, duration, completed);
        userPreferenceService.updatePreference(userId, artist);

        return ResponseEntity.ok(Map.of("success", true, "message", "播放历史已记录"));
    }

    @GetMapping("/user-preference")
    public ResponseEntity<?> getUserPreference(@RequestParam Long userId) {
        Map<String, Object> analysis = userPreferenceService.analyzeUserPreference(userId);
        return ResponseEntity.ok(analysis);
    }

    /**
     * 获取单个歌曲的风格
     */
    @GetMapping("/genre")
    public GenreResult getGenre(@RequestParam Long songId,
                                @RequestParam String songName,
                                @RequestParam String artist) {
        return genreService.getGenre(songId, songName, artist);
    }

    /**
     * 批量获取歌曲风格
     */
    @PostMapping("/genres/batch")
    public Map<Long, String> getBatchGenres(@RequestBody List<MusicSongDto> songs) {
        return genreService.batchLoadGenres(songs);
    }
}
