package org.example.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TDD Test: 播放 URL 获取测试
 *
 * 场景：测试带 Cookie 和不带 Cookie 时获取播放 URL 的区别
 */
@SpringBootTest
class PlayUrlTest {

    @Autowired
    private MusicApiService musicApiService;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${music.api.base-url}")
    private String baseUrl;

    @Test
    void testGetPlayUrlWithoutCookie() {
        // RED: 不带 Cookie 测试（可能返回试听 URL 或 null）
        Long songId = 2653714443L;  // 晴天（女声版）

        String url = baseUrl + "/song/url/v1?id=" + songId + "&level=standard";

        System.out.println("\n=== 测试：不带 Cookie ===");
        System.out.println("请求 URL: " + url);

        try {
            String response = restTemplate.getForObject(url, String.class);
            System.out.println("响应: " + response.substring(0, Math.min(500, response.length())));

            // 分析响应
            if (response.contains("\"url\":\"http")) {
                System.out.println("✅ 不带 Cookie 也能获取播放 URL（可能是试听版）");
            } else if (response.contains("\"url\":null") || response.contains("\"url\":\"\"")) {
                System.out.println("❌ 不带 Cookie 无法获取播放 URL");
            }
        } catch (Exception e) {
            fail("获取播放 URL 失败: " + e.getMessage());
        }
    }

    @Test
    void testGetPlayUrlWithFakeCookie() {
        // GREEN: 带假 Cookie 测试
        Long songId = 2653714443L;  // 晴天（女声版）
        String fakeCookie = "MUSIC_U=fake_cookie_12345";

        String url = baseUrl + "/song/url/v1?id=" + songId + "&level=standard&cookie=" + fakeCookie;

        System.out.println("\n=== 测试：带假 Cookie ===");
        System.out.println("请求 URL: " + url.substring(0, Math.min(150, url.length())) + "...");

        try {
            String response = restTemplate.getForObject(url, String.class);
            System.out.println("响应前 300 字符: " + response.substring(0, Math.min(300, response.length())));

            // 分析响应
            if (response.contains("\"url\":\"http")) {
                System.out.println("✅ 假 Cookie 也能获取播放 URL");
            } else {
                System.out.println("❌ 假 Cookie 无法获取播放 URL");
            }
        } catch (Exception e) {
            System.out.println("❌ 请求失败: " + e.getMessage());
        }
    }

    @Test
    void testGetPlayUrlThroughService() {
        // IMPROVE: 通过 MusicApiService 测试
        Long songId = 2653714443L;  // 晴天（女声版）
        String fakeCookie = "fake_cookie_12345";

        System.out.println("\n=== 测试：通过 Service 获取 ===");
        System.out.println("歌曲 ID: " + songId);

        String playUrl = musicApiService.getPlayUrl(songId, fakeCookie);

        if (playUrl != null && !playUrl.isEmpty()) {
            System.out.println("✅ 成功获取播放 URL");
            System.out.println("URL 前缀: " + playUrl.substring(0, Math.min(50, playUrl.length())) + "...");
            assertTrue(playUrl.startsWith("http"), "播放 URL 应该以 http 开头");
        } else {
            System.out.println("❌ 未获取到播放 URL");
            System.out.println("可能原因：");
            System.out.println("1. 歌曲需要 VIP");
            System.out.println("2. 歌曲无版权");
            System.out.println("3. Cookie 无效");
        }
    }

    @Test
    void testDifferentSongs() {
        // 测试不同歌曲
        System.out.println("\n=== 测试：不同歌曲的版权情况 ===");

        // 周杰伦 - 晴天（原版，通常有版权）
        testSong(186016L, "晴天（周杰伦）");

        // 晴天（女声版）
        testSong(2653714443L, "晴天（女声版）");

        // 稻香（周杰伦，通常有版权）
        testSong(185938L, "稻香（周杰伦）");
    }

    private void testSong(Long songId, String songName) {
        String url = baseUrl + "/song/url/v1?id=" + songId + "&level=standard";

        try {
            String response = restTemplate.getForObject(url, String.class);
            boolean hasUrl = response.contains("\"url\":\"http");

            if (hasUrl) {
                System.out.println("✅ " + songName + " - 有播放 URL");
            } else {
                System.out.println("❌ " + songName + " - 无播放 URL");
            }
        } catch (Exception e) {
            System.out.println("⚠️  " + songName + " - 请求失败");
        }
    }
}
