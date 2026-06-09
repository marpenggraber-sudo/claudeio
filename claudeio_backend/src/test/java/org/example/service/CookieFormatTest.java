package org.example.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 测试 Cookie 格式对音质的影响
 */
@SpringBootTest
public class CookieFormatTest {

    @Autowired
    private MusicApiService musicApiService;

    private static final Long VIP_USER_ID = 13879884891L;
    private static final Long TEST_SONG_ID = 2653714443L;

    @Test
    void compareCookieFormats() {
        System.out.println("\n=== Cookie 格式测试 ===");

        // 获取原始 Cookie
        String cookie = musicApiService.getAuthCookie(VIP_USER_ID);
        System.out.println("原始 Cookie: " + cookie);
        System.out.println("Cookie 长度: " + (cookie != null ? cookie.length() : 0));
        System.out.println("是否包含 MUSIC_U=: " + (cookie != null && cookie.contains("MUSIC_U=")));

        // 测试通过 getPlayUrl 获取
        System.out.println("\n--- 通过 getPlayUrl() 获取 ---");
        String playUrl = musicApiService.getPlayUrl(TEST_SONG_ID, cookie);
        System.out.println("结果 URL: " + (playUrl != null ? playUrl.substring(0, Math.min(100, playUrl.length())) : "null"));
    }
}
