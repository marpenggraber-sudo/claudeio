package org.example.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.dto.LoginResponse;
import org.example.dto.QrImageResponse;
import org.example.dto.QrKeyResponse;
import org.example.dto.QrStatusResponse;
import org.example.entity.UserAccount;
import org.example.entity.UserCookie;
import org.example.repo.UserAccountRepository;
import org.example.repo.UserCookieRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 二维码登录服务
 *
 * 功能：
 * 1. 生成二维码 Key
 * 2. 创建二维码图片
 * 3. 轮询检查二维码状态
 * 4. 获取登录 Cookie
 */
@Service
public class QrLoginService {

    private static final Logger log = LoggerFactory.getLogger(QrLoginService.class);

    private final RestTemplate restTemplate;
    private final UserAccountRepository userAccountRepository;
    private final UserCookieRepository userCookieRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${music.api.base-url:http://127.0.0.1:3000}")
    private String neteaseApiBaseUrl;

    public QrLoginService(RestTemplate restTemplate,
                         UserAccountRepository userAccountRepository,
                         UserCookieRepository userCookieRepository) {
        this.restTemplate = restTemplate;
        this.userAccountRepository = userAccountRepository;
        this.userCookieRepository = userCookieRepository;
    }

    /**
     * 生成二维码 Key
     *
     * @return QrKeyResponse
     */
    public QrKeyResponse generateQrKey() {
        try {
            String url = neteaseApiBaseUrl + "/login/qr/key";
            String response = restTemplate.getForObject(url, String.class);

            JsonNode root = objectMapper.readTree(response);
            int code = root.path("code").asInt();

            if (code == 200) {
                String unikey = root.path("data").path("unikey").asText();
                return QrKeyResponse.success(unikey);
            } else {
                return QrKeyResponse.failure("生成二维码 Key 失败");
            }

        } catch (Exception e) {
            log.error("生成二维码 Key 失败", e);
            return QrKeyResponse.failure("网络错误: " + e.getMessage());
        }
    }

    /**
     * 创建二维码图片
     *
     * @param key 二维码 Key
     * @return QrImageResponse
     */
    public QrImageResponse createQrImage(String key) {
        try {
            String url = neteaseApiBaseUrl + "/login/qr/create?key=" + key + "&qrimg=true";
            String response = restTemplate.getForObject(url, String.class);

            JsonNode root = objectMapper.readTree(response);
            int code = root.path("code").asInt();

            if (code == 200) {
                String qrurl = root.path("data").path("qrurl").asText();
                String qrimg = root.path("data").path("qrimg").asText();
                return QrImageResponse.success(qrurl, qrimg);
            } else {
                return QrImageResponse.failure("生成二维码图片失败");
            }

        } catch (Exception e) {
            log.error("生成二维码图片失败: key={}", key, e);
            return QrImageResponse.failure("网络错误: " + e.getMessage());
        }
    }

    /**
     * 检查二维码状态
     *
     * @param key 二维码 Key
     * @return QrStatusResponse
     */
    public QrStatusResponse checkQrStatus(String key) {
        try {
            // 添加 timestamp 参数防止缓存
            long timestamp = System.currentTimeMillis();
            String url = neteaseApiBaseUrl + "/login/qr/check?key=" + key + "&timestamp=" + timestamp;
            String response = restTemplate.getForObject(url, String.class);

            JsonNode root = objectMapper.readTree(response);
            int code = root.path("code").asInt();
            String message = root.path("message").asText("");
            String cookie = root.path("cookie").asText(null);

            // 状态码映射
            switch (code) {
                case 800:
                    return QrStatusResponse.expired();
                case 801:
                    return QrStatusResponse.waiting();
                case 802:
                    return QrStatusResponse.scanned();
                case 803:
                    // 提取 MUSIC_U cookie
                    String musicU = extractMusicU(cookie);
                    return QrStatusResponse.success(musicU);
                default:
                    return new QrStatusResponse(code, message, null);
            }

        } catch (Exception e) {
            log.error("检查二维码状态失败: key={}", key, e);
            return new QrStatusResponse(-1, "网络错误: " + e.getMessage(), null);
        }
    }

    /**
     * 从 Cookie 字符串中提取 MUSIC_U
     */
    private String extractMusicU(String cookieStr) {
        if (cookieStr == null || cookieStr.isEmpty()) {
            return null;
        }

        // 格式：MUSIC_U=xxx; Path=/; ...
        String[] cookies = cookieStr.split(";");
        for (String cookie : cookies) {
            String trimmed = cookie.trim();
            if (trimmed.startsWith("MUSIC_U=")) {
                return trimmed;
            }
        }

        return null;
    }

    /**
     * 完成二维码登录：从 cookie 获取用户信息并自动登录
     *
     * @param cookie MUSIC_U cookie
     * @return LoginResponse（包含 userId 和 nickname）
     */
    @Transactional
    public LoginResponse completeQrLogin(String cookie) {
        try {
            // 1. 使用 cookie 调用 /login/status 获取用户信息
            String url = neteaseApiBaseUrl + "/login/status?cookie=" + cookie;
            String response = restTemplate.getForObject(url, String.class);

            JsonNode root = objectMapper.readTree(response);
            JsonNode data = root.path("data");

            if (data.isMissingNode() || data.path("account").isMissingNode()) {
                log.error("获取用户信息失败: {}", response);
                return new LoginResponse(null, "获取用户信息失败");
            }

            JsonNode account = data.path("account");
            Long userId = account.path("id").asLong();
            String nickname = account.path("nickname").asText("用户");
            String avatarUrl = account.path("avatarUrl").asText("");

            if (userId == null || userId == 0) {
                return new LoginResponse(null, "无效的用户信息");
            }

            // 2. 检查用户是否已存在
            Optional<UserAccount> existingUser = userAccountRepository.findByMusicUserId(userId);

            UserAccount user;
            if (existingUser.isPresent()) {
                // 用户存在，更新信息
                user = existingUser.get();
                user.setNickname(nickname);
                user.setAvatarUrl(avatarUrl);
                user.setUpdatedAt(LocalDateTime.now());
                log.info("更新已存在用户: userId={}, nickname={}", userId, nickname);
            } else {
                // 用户不存在，自动注册
                user = new UserAccount();
                user.setMusicUserId(userId);
                user.setAccount(String.valueOf(userId)); // 使用 userId 作为账号
                user.setPassword("qr_login_" + System.currentTimeMillis()); // 随机密码
                user.setNickname(nickname);
                user.setAvatarUrl(avatarUrl);
                user.setCreatedAt(LocalDateTime.now());
                user.setUpdatedAt(LocalDateTime.now());
                log.info("自动注册新用户: userId={}, nickname={}", userId, nickname);
            }

            user = userAccountRepository.save(user);

            // 3. 保存/更新 cookie
            UserCookie userCookie = userCookieRepository
                .findTopByUser_IdOrderByUpdatedAtDesc(user.getId())
                .orElse(new UserCookie());

            userCookie.setUser(user);
            userCookie.setCookieCiphertext(cookie);
            userCookie.setExpiresAt(LocalDateTime.now().plusDays(30));
            userCookie.setCreatedAt(userCookie.getCreatedAt() == null ? LocalDateTime.now() : userCookie.getCreatedAt());
            userCookie.setUpdatedAt(LocalDateTime.now());
            userCookieRepository.save(userCookie);

            log.info("二维码登录成功: userId={}, nickname={}", userId, nickname);
            return new LoginResponse(userId, nickname);

        } catch (Exception e) {
            log.error("完成二维码登录失败", e);
            return new LoginResponse(null, "登录失败: " + e.getMessage());
        }
    }
}
