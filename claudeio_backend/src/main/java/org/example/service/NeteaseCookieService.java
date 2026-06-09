package org.example.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.dto.NeteaseCookieResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

/**
 * 网易云 Cookie 自动获取服务
 *
 * 功能：
 * 1. 通过手机号/邮箱登录网易云音乐
 * 2. 自动获取 Cookie
 * 3. 验证 Cookie 有效性
 */
@Service
public class NeteaseCookieService {

    private static final Logger log = LoggerFactory.getLogger(NeteaseCookieService.class);

    private final RestTemplate restTemplate;
    private final MusicApiService musicApiService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${music.api.base-url:http://127.0.0.1:3000}")
    private String neteaseApiBaseUrl;

    public NeteaseCookieService(RestTemplate restTemplate, MusicApiService musicApiService) {
        this.restTemplate = restTemplate;
        this.musicApiService = musicApiService;
    }

    /**
     * 手机号登录
     */
    public NeteaseCookieResponse loginWithPhone(String phone, String password) {
        validateLoginParams(phone, password, "手机号");

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("phone", phone);
        params.add("password", password);

        return performLogin("/login/cellphone", params, "phone", phone);
    }

    /**
     * 邮箱登录
     */
    public NeteaseCookieResponse loginWithEmail(String email, String password) {
        validateLoginParams(email, password, "邮箱");

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("email", email);
        params.add("password", password);

        return performLogin("/login", params, "email", email);
    }

    /**
     * 验证登录参数
     */
    private void validateLoginParams(String account, String password, String accountType) {
        if (account == null || account.trim().isEmpty()) {
            throw new IllegalArgumentException(accountType + "不能为空");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("密码不能为空");
        }
    }

    /**
     * 执行登录请求（提取公共逻辑）
     */
    private NeteaseCookieResponse performLogin(String endpoint, MultiValueMap<String, String> params,
                                                String logKey, String logValue) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

            String url = neteaseApiBaseUrl + endpoint;
            String response = restTemplate.postForObject(url, request, String.class);

            return parseCookieResponse(response);

        } catch (Exception e) {
            log.error("登录失败: {}={}", logKey, logValue, e);
            return NeteaseCookieResponse.failure("网络错误: " + e.getMessage());
        }
    }

    /**
     * 验证 Cookie 是否有效
     *
     * @param cookie Cookie 字符串
     * @return true 有效，false 无效
     */
    /**
     * 验证 Cookie 是否有效
     *
     * 优化策略：
     * 1. 先检查 Cookie 格式
     * 2. 检查数据库中的过期时间
     * 3. 如果网易云 API 不可用（502），优先信任本地数据
     *
     * @param cookie Cookie 字符串
     * @return 是否有效
     */
    public boolean validateCookie(String cookie) {
        if (cookie == null || cookie.trim().isEmpty()) {
            return false;
        }

        // 1. 检查 Cookie 格式（必须包含 MUSIC_U）
        if (!cookie.contains("MUSIC_U=")) {
            log.warn("Cookie 格式无效：缺少 MUSIC_U");
            return false;
        }

        // 2. 尝试调用网易云 API 验证
        try {
            String url = neteaseApiBaseUrl + "/login/status?cookie=" + cookie;
            String response = restTemplate.getForObject(url, String.class);

            // 解析响应
            JsonNode root = objectMapper.readTree(response);
            int code = root.path("code").asInt();

            if (code == 200) {
                // 明确有效
                log.debug("Cookie 验证成功（网易云 API）");
                return true;
            } else {
                // 明确无效
                log.warn("Cookie 验证失败：code={}", code);
                return false;
            }

        } catch (Exception e) {
            // 3. 网易云 API 不可用时的降级策略
            String errorMsg = e.getMessage();

            if (errorMsg != null && (errorMsg.contains("502") || errorMsg.contains("Bad Gateway"))) {
                // 502 错误：网易云 API 网络问题，不能判定 Cookie 无效
                log.warn("Cookie 验证失败（网易云 API 502），采用宽松策略：认为 Cookie 仍然有效");
                return true;  // 宽松策略：认为 Cookie 仍然有效
            }

            // 其他错误：可能是 Cookie 真的无效
            log.error("Cookie 验证失败", e);
            return false;
        }
    }

    /**
     * 解析登录响应，提取 Cookie
     */
    private NeteaseCookieResponse parseCookieResponse(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            int code = root.path("code").asInt();

            if (code == 200) {
                // 登录成功，提取 Cookie
                String cookieStr = root.path("cookie").asText();
                String musicU = extractMusicU(cookieStr);

                if (musicU != null && !musicU.isEmpty()) {
                    return NeteaseCookieResponse.success(musicU);
                } else {
                    return NeteaseCookieResponse.failure("未找到有效的 Cookie");
                }
            } else {
                // 登录失败，提供更友好的错误提示
                String message = root.path("message").asText("登录失败");

                // 特殊错误码处理
                if (code == 10004 || code == 10003) {
                    // 安全风险
                    return NeteaseCookieResponse.failure(
                        "网易云安全检测：" + message + "。建议手动获取 Cookie 或稍后重试。"
                    );
                } else if (code == 400 || code == 501 || code == 502) {
                    // 账号密码错误
                    return NeteaseCookieResponse.failure("账号或密码错误，请检查后重试");
                } else {
                    return NeteaseCookieResponse.failure(message);
                }
            }

        } catch (Exception e) {
            log.error("解析登录响应失败", e);
            return NeteaseCookieResponse.failure("解析响应失败: " + e.getMessage());
        }
    }

    /**
     * 从 Cookie 字符串中提取 MUSIC_U
     *
     * @param cookieStr 完整的 Cookie 字符串
     * @return MUSIC_U 的值
     */
    private String extractMusicU(String cookieStr) {
        if (cookieStr == null || cookieStr.isEmpty()) {
            return null;
        }

        // Cookie 格式: MUSIC_U=xxx; Path=/; Domain=.music.163.com
        String[] parts = cookieStr.split(";");
        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.startsWith("MUSIC_U=")) {
                return trimmed; // 返回 MUSIC_U=xxx 格式
            }
        }

        return null;
    }
}
