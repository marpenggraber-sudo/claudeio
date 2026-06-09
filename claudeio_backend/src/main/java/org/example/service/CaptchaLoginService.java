package org.example.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.dto.CaptchaResponse;
import org.example.dto.NeteaseCookieResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * 验证码登录服务
 *
 * 功能：
 * 1. 发送手机验证码
 * 2. 使用验证码登录网易云
 * 3. 自动获取 Cookie
 */
@Service
public class CaptchaLoginService {

    private static final Logger log = LoggerFactory.getLogger(CaptchaLoginService.class);

    private final RestTemplate restTemplate;
    private final MusicApiService musicApiService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${music.api.base-url:http://127.0.0.1:3000}")
    private String neteaseApiBaseUrl;

    public CaptchaLoginService(RestTemplate restTemplate, MusicApiService musicApiService) {
        this.restTemplate = restTemplate;
        this.musicApiService = musicApiService;
    }

    /**
     * 发送验证码
     *
     * @param phone 手机号
     * @return 发送结果
     */
    public CaptchaResponse sendCaptcha(String phone) {
        // 参数验证
        if (phone == null || phone.trim().isEmpty()) {
            return CaptchaResponse.failure("手机号不能为空");
        }

        // 手机号格式验证（中国大陆手机号：1[3-9]xxxxxxxxx）
        if (!phone.matches("^1[3-9]\\d{9}$")) {
            return CaptchaResponse.failure("手机号格式错误");
        }

        try {
            // 调用网易云 API 发送验证码
            String url = neteaseApiBaseUrl + "/captcha/sent?phone=" + phone;
            String response = restTemplate.getForObject(url, String.class);

            return parseCaptchaResponse(response);

        } catch (Exception e) {
            log.error("发送验证码失败: phone={}", phone, e);
            return CaptchaResponse.failure("网络错误: " + e.getMessage());
        }
    }

    /**
     * 使用验证码登录
     *
     * @param phone   手机号
     * @param captcha 验证码
     * @return Cookie 响应
     */
    public NeteaseCookieResponse loginWithCaptcha(String phone, String captcha) {
        // 参数验证
        if (phone == null || phone.trim().isEmpty()) {
            return NeteaseCookieResponse.failure("手机号不能为空");
        }

        if (captcha == null || captcha.trim().isEmpty()) {
            return NeteaseCookieResponse.failure("验证码不能为空");
        }

        // 手机号格式验证
        if (!phone.matches("^1[3-9]\\d{9}$")) {
            return NeteaseCookieResponse.failure("手机号格式错误");
        }

        try {
            // 调用网易云 API 验证码登录
            String url = neteaseApiBaseUrl + "/login/cellphone" +
                "?phone=" + phone +
                "&captcha=" + captcha;

            String response = restTemplate.getForObject(url, String.class);

            return parseCookieResponse(response);

        } catch (Exception e) {
            log.error("验证码登录失败: phone={}", phone, e);
            return NeteaseCookieResponse.failure("网络错误: " + e.getMessage());
        }
    }

    /**
     * 解析验证码发送响应
     */
    private CaptchaResponse parseCaptchaResponse(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            int code = root.path("code").asInt();

            if (code == 200) {
                String message = root.path("message").asText("发送成功");
                return CaptchaResponse.success(message);
            } else {
                String message = root.path("message").asText("发送失败");
                return CaptchaResponse.failure(message);
            }

        } catch (Exception e) {
            log.error("解析验证码响应失败", e);
            return CaptchaResponse.failure("解析响应失败: " + e.getMessage());
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
                // 登录失败，提供友好的错误提示
                String message = root.path("message").asText("登录失败");

                // 特殊错误码处理
                if (code == 503) {
                    return NeteaseCookieResponse.failure("验证码错误");
                } else if (code == 504) {
                    return NeteaseCookieResponse.failure("验证码已过期，请重新获取");
                } else if (code == 400 || code == 501) {
                    return NeteaseCookieResponse.failure("手机号不存在");
                } else if (code == 10004) {
                    return NeteaseCookieResponse.failure("当前登录存在安全风险，请15-30分钟后重试");
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
}
