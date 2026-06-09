package org.example.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 音乐播放 URL 有效性测试
 *
 * 测试场景：
 * 1. 获取的播放 URL 应该是完整的 HTTP/HTTPS URL
 * 2. URL 应该包含必要的参数
 * 3. URL 应该在有效期内可访问
 */
@SpringBootTest
public class MusicPlayUrlValidityTest {

    @Autowired
    private MusicApiService musicApiService;

    private static final Long VIP_USER_ID = 13879884891L;
    private static final Long TEST_SONG_ID = 2653714443L;

    /**
     * RED TEST 1: 播放 URL 应该是完整的 HTTP URL
     */
    @Test
    void playUrl_shouldBeCompleteHttpUrl() {
        System.out.println("\n=== 测试 1: 播放 URL 格式验证 ===");

        // 获取 Cookie
        String cookie = musicApiService.getAuthCookie(VIP_USER_ID);
        assertThat(cookie).isNotNull();

        // 获取播放 URL
        String playUrl = musicApiService.getPlayUrl(TEST_SONG_ID, cookie);
        System.out.println("播放 URL: " + playUrl);

        // 验证 URL 格式
        assertThat(playUrl)
            .as("播放 URL 应该不为空")
            .isNotNull()
            .isNotEmpty();

        assertThat(playUrl)
            .as("播放 URL 应该以 http:// 或 https:// 开头")
            .matches("^https?://.*");

        assertThat(playUrl)
            .as("播放 URL 应该包含域名")
            .contains("music.126.net");

        assertThat(playUrl)
            .as("播放 URL 不应该被截断（应该有文件扩展名或参数）")
            .matches(".*\\.(mp3|flac|m4a)(\\?.*)?$");
    }

    /**
     * RED TEST 2: 播放 URL 应该包含必要的参数
     */
    @Test
    void playUrl_shouldContainNecessaryParameters() {
        System.out.println("\n=== 测试 2: URL 参数验证 ===");

        String cookie = musicApiService.getAuthCookie(VIP_USER_ID);
        String playUrl = musicApiService.getPlayUrl(TEST_SONG_ID, cookie);

        System.out.println("完整 URL: " + playUrl);

        // 检查 URL 长度（有效的播放 URL 通常很长，包含签名等）
        assertThat(playUrl.length())
            .as("有效的播放 URL 应该足够长（包含路径和参数）")
            .isGreaterThan(50);

        // 检查是否包含文件路径
        assertThat(playUrl)
            .as("URL 应该包含文件路径")
            .matches(".*/.+\\.(mp3|flac|m4a).*");
    }

    /**
     * RED TEST 3: 测试 URL 获取的稳定性（多次获取应该都成功）
     */
    @Test
    void playUrl_shouldBeConsistentlyAvailable() {
        System.out.println("\n=== 测试 3: URL 获取稳定性 ===");

        String cookie = musicApiService.getAuthCookie(VIP_USER_ID);

        // 连续 3 次获取 URL
        for (int i = 1; i <= 3; i++) {
            String playUrl = musicApiService.getPlayUrl(TEST_SONG_ID, cookie);

            System.out.println("第 " + i + " 次获取:");
            System.out.println("  URL: " + (playUrl != null ? playUrl.substring(0, Math.min(80, playUrl.length())) + "..." : "null"));
            System.out.println("  长度: " + (playUrl != null ? playUrl.length() : 0));

            assertThat(playUrl)
                .as("第 %d 次获取应该成功", i)
                .isNotNull()
                .isNotEmpty();

            // 短暂延迟
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * RED TEST 4: URL 不应该包含特殊字符导致的问题
     */
    @Test
    void playUrl_shouldNotContainProblematicCharacters() {
        System.out.println("\n=== 测试 4: URL 特殊字符检查 ===");

        String cookie = musicApiService.getAuthCookie(VIP_USER_ID);
        String playUrl = musicApiService.getPlayUrl(TEST_SONG_ID, cookie);

        System.out.println("检查 URL: " + playUrl);

        // 检查是否包含空格（应该被编码为 %20）
        assertThat(playUrl)
            .as("URL 不应该包含未编码的空格")
            .doesNotContain(" ");

        // 检查是否包含换行符
        assertThat(playUrl)
            .as("URL 不应该包含换行符")
            .doesNotContain("\n")
            .doesNotContain("\r");

        // 检查是否被截断（URL 末尾应该完整）
        assertThat(playUrl)
            .as("URL 不应该以不完整的字符结尾")
            .matches(".*[a-zA-Z0-9=]$");
    }
}
