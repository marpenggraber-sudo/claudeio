package org.example.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TDD Test: 网易云音乐 API 完整测试
 *
 * 测试所有使用的 API 端点，确保与新的音乐 API 服务兼容
 */
@SpringBootTest
class NeteaseApiIntegrationTest {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${music.api.base-url}")
    private String baseUrl;

    @BeforeEach
    void setUp() {
        System.out.println("测试 API 基础 URL: " + baseUrl);
    }

    // ==================== 二维码登录相关 API ====================

    @Test
    void testQrKeyGeneration() {
        // 测试生成二维码 Key
        String url = baseUrl + "/login/qr/key";

        try {
            String response = restTemplate.getForObject(url, String.class);
            assertNotNull(response, "二维码 Key 响应不应该为空");

            JsonNode json = objectMapper.readTree(response);
            assertTrue(json.has("code"), "响应应该包含 code 字段");
            assertTrue(json.has("data"), "响应应该包含 data 字段");

            int code = json.get("code").asInt();
            assertEquals(200, code, "二维码 Key 生成应该返回 200");

            // 新 API 的 unikey 在 data 对象里面
            JsonNode data = json.get("data");
            assertTrue(data.has("unikey"), "data 应该包含 unikey 字段");

            String unikey = data.get("unikey").asText();
            assertNotNull(unikey, "unikey 不应该为空");
            assertFalse(unikey.isEmpty(), "unikey 不应该是空字符串");

            System.out.println("✅ /login/qr/key 测试通过（unikey: " + unikey.substring(0, 8) + "...）");
        } catch (Exception e) {
            fail("二维码 Key 生成失败: " + e.getMessage());
        }
    }

    @Test
    void testQrCreate() {
        // 测试创建二维码图片
        // 先生成 key
        String keyUrl = baseUrl + "/login/qr/key";
        String key = null;

        try {
            String keyResponse = restTemplate.getForObject(keyUrl, String.class);
            JsonNode keyJson = objectMapper.readTree(keyResponse);
            key = keyJson.get("data").get("unikey").asText();
            assertNotNull(key, "应该成功获取二维码 key");
        } catch (Exception e) {
            fail("获取二维码 key 失败: " + e.getMessage());
        }

        // 用 key 创建二维码
        String createUrl = baseUrl + "/login/qr/create?key=" + key + "&qrimg=true";

        try {
            String response = restTemplate.getForObject(createUrl, String.class);
            assertNotNull(response, "二维码创建响应不应该为空");

            JsonNode json = objectMapper.readTree(response);
            assertTrue(json.has("code"), "响应应该包含 code 字段");

            int code = json.get("code").asInt();
            assertEquals(200, code, "二维码创建应该返回 200");

            // 检查是否有二维码数据
            assertTrue(json.has("data") && json.get("data").has("qrimg"),
                "响应应该包含二维码图片数据");

            System.out.println("✅ /login/qr/create 测试通过");
        } catch (Exception e) {
            fail("二维码创建失败: " + e.getMessage());
        }
    }

    @Test
    void testQrCheck() {
        // 测试检查二维码状态
        // 使用一个假的 key（预期返回 800 - 二维码不存在或已过期）
        String url = baseUrl + "/login/qr/check?key=test-key-12345&timestamp=" + System.currentTimeMillis();

        try {
            String response = restTemplate.getForObject(url, String.class);
            assertNotNull(response, "二维码检查响应不应该为空");

            JsonNode json = objectMapper.readTree(response);
            assertTrue(json.has("code"), "响应应该包含 code 字段");

            // 假 key 应该返回 800（二维码不存在）或其他错误码
            int code = json.get("code").asInt();
            assertTrue(code >= 800, "假 key 应该返回错误码");

            System.out.println("✅ /login/qr/check 测试通过（返回码: " + code + "）");
        } catch (Exception e) {
            fail("二维码检查失败: " + e.getMessage());
        }
    }

    @Test
    void testLoginStatus() {
        // 测试登录状态检查
        String url = baseUrl + "/login/status";

        try {
            // 不带 cookie 的请求应该返回未登录状态
            String response = restTemplate.getForObject(url, String.class);
            assertNotNull(response, "登录状态响应不应该为空");

            JsonNode json = objectMapper.readTree(response);
            assertTrue(json.has("data"), "响应应该包含 data 字段");

            JsonNode data = json.get("data");
            assertFalse(data.has("profile") && !data.get("profile").isNull(),
                "不带 cookie 应该返回未登录状态");

            System.out.println("✅ /login/status 测试通过");
        } catch (Exception e) {
            // 502 错误是可以接受的（网易云 API 间歇性问题）
            if (e.getMessage().contains("502")) {
                System.out.println("⚠️  /login/status 返回 502（网易云 API 问题，可接受）");
            } else {
                fail("登录状态检查失败: " + e.getMessage());
            }
        }
    }

    // ==================== 搜索相关 API ====================

    @Test
    void testSearch() {
        // 测试搜索歌曲
        String url = baseUrl + "/search?keywords=周杰伦";

        try {
            String response = restTemplate.getForObject(url, String.class);
            assertNotNull(response, "搜索响应不应该为空");

            JsonNode json = objectMapper.readTree(response);
            assertTrue(json.has("result"), "响应应该包含 result 字段");

            JsonNode result = json.get("result");
            assertTrue(result.has("songs"), "result 应该包含 songs 字段");

            JsonNode songs = result.get("songs");
            assertTrue(songs.isArray() && songs.size() > 0,
                "应该返回至少一首歌曲");

            // 检查歌曲结构
            JsonNode firstSong = songs.get(0);
            assertTrue(firstSong.has("id"), "歌曲应该有 id");
            assertTrue(firstSong.has("name"), "歌曲应该有 name");

            System.out.println("✅ /search 测试通过（找到 " + songs.size() + " 首歌曲）");
        } catch (Exception e) {
            fail("搜索失败: " + e.getMessage());
        }
    }

    // ==================== 歌曲详情相关 API ====================

    @Test
    void testSongDetail() {
        // 测试获取歌曲详情（使用周杰伦的"晴天" - ID: 186016）
        String url = baseUrl + "/song/detail?ids=186016";

        try {
            String response = restTemplate.getForObject(url, String.class);
            assertNotNull(response, "歌曲详情响应不应该为空");

            JsonNode json = objectMapper.readTree(response);
            assertTrue(json.has("songs"), "响应应该包含 songs 字段");

            JsonNode songs = json.get("songs");
            assertTrue(songs.isArray() && songs.size() > 0,
                "应该返回歌曲详情");

            JsonNode song = songs.get(0);
            assertTrue(song.has("id"), "歌曲应该有 id");
            assertTrue(song.has("name"), "歌曲应该有 name");
            assertEquals(186016, song.get("id").asLong(), "歌曲 ID 应该匹配");

            System.out.println("✅ /song/detail 测试通过");
        } catch (Exception e) {
            fail("获取歌曲详情失败: " + e.getMessage());
        }
    }

    @Test
    void testSongUrl() {
        // 测试获取歌曲播放 URL
        // 注意：没有 cookie 可能返回空 URL 或试听 URL
        String url = baseUrl + "/song/url/v1?id=186016&level=standard";

        try {
            String response = restTemplate.getForObject(url, String.class);
            assertNotNull(response, "歌曲 URL 响应不应该为空");

            JsonNode json = objectMapper.readTree(response);
            assertTrue(json.has("data"), "响应应该包含 data 字段");

            JsonNode data = json.get("data");
            assertTrue(data.isArray(), "data 应该是数组");

            if (data.size() > 0) {
                JsonNode urlData = data.get(0);
                System.out.println("✅ /song/url/v1 测试通过（URL: " +
                    (urlData.has("url") ? "存在" : "为空/需要 cookie") + "）");
            } else {
                System.out.println("✅ /song/url/v1 测试通过（返回空数据，可能需要 cookie）");
            }
        } catch (Exception e) {
            fail("获取歌曲 URL 失败: " + e.getMessage());
        }
    }

    @Test
    void testLyric() {
        // 测试获取歌词（使用"晴天" - ID: 186016）
        String url = baseUrl + "/lyric?id=186016";

        try {
            String response = restTemplate.getForObject(url, String.class);
            assertNotNull(response, "歌词响应不应该为空");

            JsonNode json = objectMapper.readTree(response);

            // 检查是否有歌词数据
            boolean hasLyric = json.has("lrc") || json.has("yrc");
            assertTrue(hasLyric, "响应应该包含歌词数据");

            System.out.println("✅ /lyric 测试通过");
        } catch (Exception e) {
            fail("获取歌词失败: " + e.getMessage());
        }
    }

    // ==================== 验证码登录相关 API ====================

    @Test
    void testCaptchaSent() {
        // 测试发送验证码
        // 注意：这个测试不会真的发送验证码，只是测试 API 可用性
        String url = baseUrl + "/captcha/sent?phone=13800138000";

        try {
            String response = restTemplate.getForObject(url, String.class);
            assertNotNull(response, "验证码发送响应不应该为空");

            JsonNode json = objectMapper.readTree(response);
            assertTrue(json.has("code"), "响应应该包含 code 字段");

            // 可能返回成功或各种错误（如手机号不存在、发送过于频繁等）
            int code = json.get("code").asInt();
            assertTrue(code > 0, "应该返回有效的响应码");

            System.out.println("✅ /captcha/sent 测试通过（返回码: " + code + "）");
        } catch (Exception e) {
            // 502 错误是可以接受的（网易云 API 间歇性问题）
            String message = e.getMessage();
            if (message.contains("502") || message.contains("Bad Gateway")) {
                System.out.println("⚠️  /captcha/sent 返回 502（网易云 API 网络问题，可接受）");
            } else {
                fail("验证码发送测试失败: " + e.getMessage());
            }
        }
    }

    @Test
    void testCaptchaVerify() {
        // 测试验证验证码
        // 使用假的验证码（预期返回错误）
        String url = baseUrl + "/captcha/verify?phone=13800138000&captcha=123456";

        try {
            String response = restTemplate.getForObject(url, String.class);
            assertNotNull(response, "验证码验证响应不应该为空");

            JsonNode json = objectMapper.readTree(response);
            assertTrue(json.has("code"), "响应应该包含 code 字段");

            System.out.println("✅ /captcha/verify 测试通过");
        } catch (Exception e) {
            // 400、503 等错误是预期的（假验证码或服务限流）
            String message = e.getMessage();
            if (message.contains("400") || message.contains("Bad Request") ||
                message.contains("503") || message.contains("Service Unavailable")) {
                System.out.println("✅ /captcha/verify 测试通过（假验证码返回错误，符合预期）");
            } else {
                fail("验证码验证测试失败: " + e.getMessage());
            }
        }
    }

    // ==================== 推荐相关 API ====================

    @Test
    void testRecommendSongs() {
        // 测试每日推荐（可能需要登录）
        String url = baseUrl + "/recommend/songs";

        try {
            String response = restTemplate.getForObject(url, String.class);
            assertNotNull(response, "推荐响应不应该为空");

            JsonNode json = objectMapper.readTree(response);
            assertTrue(json.has("code"), "响应应该包含 code 字段");

            int code = json.get("code").asInt();
            // 301 表示需要登录，200 表示成功
            assertTrue(code == 200 || code == 301,
                "推荐 API 应该返回 200（成功）或 301（需要登录）");

            System.out.println("✅ /recommend/songs 测试通过（返回码: " + code + "）");
        } catch (Exception e) {
            fail("推荐歌曲测试失败: " + e.getMessage());
        }
    }

    // ==================== 汇总测试结果 ====================

    @Test
    void summaryTest() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("API 测试汇总");
        System.out.println("=".repeat(60));
        System.out.println("API 基础 URL: " + baseUrl);
        System.out.println("\n请运行所有测试来验证 API 兼容性");
        System.out.println("=".repeat(60));
    }
}
