package org.example.controller;

import jakarta.validation.Valid;
import org.example.dto.*;
import org.example.entity.PlayHistory;
import org.example.service.AgentFacadeService;
import org.example.service.AuthService;
import org.example.service.CaptchaLoginService;
import org.example.service.GenreService;
import org.example.service.MusicApiService;
import org.example.service.NeteaseCookieService;
import org.example.service.PlayHistoryService;
import org.example.service.QrLoginService;
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
    private final NeteaseCookieService neteaseCookieService;
    private final CaptchaLoginService captchaLoginService;
    private final QrLoginService qrLoginService;

    public MusicController(MusicApiService musicApiService,
                          MusicTools musicTools,
                          AgentFacadeService agentFacadeService,
                          AuthService authService,
                          PlayHistoryService playHistoryService,
                          UserPreferenceService userPreferenceService,
                          GenreService genreService,
                          NeteaseCookieService neteaseCookieService,
                          CaptchaLoginService captchaLoginService,
                          QrLoginService qrLoginService) {
        this.musicApiService = musicApiService;
        this.musicTools = musicTools;
        this.agentFacadeService = agentFacadeService;
        this.authService = authService;
        this.playHistoryService = playHistoryService;
        this.userPreferenceService = userPreferenceService;
        this.genreService = genreService;
        this.neteaseCookieService = neteaseCookieService;
        this.captchaLoginService = captchaLoginService;
        this.qrLoginService = qrLoginService;
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
    public ResponseEntity<?> recordPlayHistory(@Valid @RequestBody PlayHistoryRequest request) {
        playHistoryService.recordPlay(
            request.userId(),
            request.songId(),
            request.songName(),
            request.artist(),
            request.duration() != null ? request.duration().intValue() : 0,
            request.completed() != null ? request.completed() : false
        );
        userPreferenceService.updatePreference(request.userId(), request.artist());

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

    /**
     * 网易云音乐登录（自动获取 Cookie）
     *
     * @param request 登录请求（loginType: phone/email, account, password）
     * @return Cookie 响应
     */
    @PostMapping("/netease-login")
    public ResponseEntity<NeteaseCookieResponse> neteaseLogin(@Valid @RequestBody NeteaseLoginRequest request) {
        NeteaseCookieResponse response;

        if ("phone".equals(request.loginType())) {
            response = neteaseCookieService.loginWithPhone(request.account(), request.password());
        } else if ("email".equals(request.loginType())) {
            response = neteaseCookieService.loginWithEmail(request.account(), request.password());
        } else {
            return ResponseEntity.badRequest()
                .body(NeteaseCookieResponse.failure("无效的登录类型"));
        }

        return ResponseEntity.ok(response);
    }

    /**
     * 检查 Cookie 状态
     *
     * @param cookie Cookie 字符串
     * @return Cookie 是否有效
     */
    @GetMapping("/cookie-status")
    public ResponseEntity<CookieStatusResponse> checkCookieStatus(
            @RequestParam(required = true) String cookie) {

        if (cookie == null || cookie.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        boolean valid = neteaseCookieService.validateCookie(cookie);
        return ResponseEntity.ok(new CookieStatusResponse(valid));
    }

    /**
     * 发送验证码
     *
     * @param phone 手机号
     * @return 发送结果
     */
    @PostMapping("/send-captcha")
    public ResponseEntity<CaptchaResponse> sendCaptcha(
            @RequestParam(required = true) String phone) {

        if (phone == null || phone.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        CaptchaResponse response = captchaLoginService.sendCaptcha(phone);
        return ResponseEntity.ok(response);
    }

    /**
     * 验证码登录（自动获取 Cookie）
     *
     * @param request 验证码登录请求（手机号 + 验证码）
     * @return Cookie 响应
     */
    @PostMapping("/captcha-login")
    public ResponseEntity<NeteaseCookieResponse> captchaLogin(
            @Valid @RequestBody CaptchaLoginRequest request) {

        NeteaseCookieResponse response = captchaLoginService.loginWithCaptcha(
            request.phone(),
            request.captcha()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * 生成二维码 Key
     *
     * @return QrKeyResponse
     */
    @GetMapping("/qr-key")
    public ResponseEntity<QrKeyResponse> generateQrKey() {
        QrKeyResponse response = qrLoginService.generateQrKey();
        return ResponseEntity.ok(response);
    }

    /**
     * 创建二维码图片
     *
     * @param key 二维码 Key
     * @return QrImageResponse
     */
    @GetMapping("/qr-create")
    public ResponseEntity<QrImageResponse> createQrImage(
            @RequestParam(required = true) String key) {

        if (key == null || key.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        QrImageResponse response = qrLoginService.createQrImage(key);
        return ResponseEntity.ok(response);
    }

    /**
     * 检查二维码状态
     *
     * @param key 二维码 Key
     * @return QrStatusResponse
     */
    @GetMapping("/qr-check")
    public ResponseEntity<QrStatusResponse> checkQrStatus(
            @RequestParam(required = true) String key) {

        if (key == null || key.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        QrStatusResponse response = qrLoginService.checkQrStatus(key);
        return ResponseEntity.ok(response);
    }

    /**
     * 完成二维码登录（扫码成功后自动登录）
     *
     * @param cookie MUSIC_U cookie
     * @return LoginResponse（包含 userId 和 message）
     */
    @PostMapping("/qr-login")
    public ResponseEntity<LoginResponse> completeQrLogin(
            @RequestParam(required = true) String cookie) {

        if (cookie == null || cookie.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                .body(new LoginResponse(null, "Cookie 不能为空"));
        }

        LoginResponse response = qrLoginService.completeQrLogin(cookie);

        // 修复：Java record 使用 userId() 而不是 getUserId()
        if (response.userId() == null) {
            return ResponseEntity.status(400).body(response);
        }

        return ResponseEntity.ok(response);
    }

    /**
     * 获取用户的播放历史列表（前端歌词页侧滑调用）
     */
    @GetMapping("/play-history")
    public ResponseEntity<List<PlayHistory>> getPlayHistory(@RequestParam Long userId) {
        if (userId == null) {
            return ResponseEntity.badRequest().build();
        }
        // 调用我们刚刚在 Service 中写好的去重方法
        List<PlayHistory> history = playHistoryService.getUniqueRecentHistory(userId);
        return ResponseEntity.ok(history);
    }

}
