package org.example.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.tools.MusicTools;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * VIP 用户音质测试
 *
 * 测试目标：
 * 1. VIP 用户应该能获取到完整的播放 URL
 * 2. 返回的 URL 应该包含音质信息
 * 3. 返回的 URL 应该是有效的（能访问）
 */
@SpringBootTest
public class VipMusicQualityTest {

    @Autowired
    private MusicApiService musicApiService;

    @Autowired
    private MusicTools musicTools;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private static final Long VIP_USER_ID = 13879884891L;  // 用户的 VIP userId
    private static final Long TEST_SONG_ID = 2653714443L;  // 测试歌曲 ID

    /**
     * RED TEST 1: VIP 用户应该能获取到播放 URL
     */
    @Test
    void vipUser_shouldGetPlayUrl() {
        System.out.println("\n=== 测试 1: VIP 用户获取播放 URL ===");

        // Act
        String playUrl = musicTools.getPlayUrl(TEST_SONG_ID, VIP_USER_ID);

        // Assert
        System.out.println("播放 URL: " + playUrl);
        assertThat(playUrl)
                .as("VIP 用户应该能获取到播放 URL")
                .isNotNull()
                .isNotEmpty()
                .startsWith("http");
    }

    /**
     * RED TEST 2: 检查 API 返回的完整响应
     */
    @Test
    void checkApiResponse_shouldContainQualityInfo() {
        System.out.println("\n=== 测试 2: 检查 API 响应结构 ===");

        // Arrange
        String cookie = musicApiService.getAuthCookie(VIP_USER_ID);
        assertThat(cookie).isNotNull();

        // Act - 直接调用网易云 API 查看完整响应
        String apiUrl = String.format(
            "http://127.0.0.1:3000/song/url/v1?id=%d&level=lossless&cookie=%s",
            TEST_SONG_ID, cookie
        );

        try {
            String response = restTemplate.getForObject(apiUrl, String.class);
            System.out.println("API 完整响应:");
            System.out.println(response);

            JsonNode root = objectMapper.readTree(response);
            JsonNode data = root.path("data");

            assertThat(data.isArray()).as("data 应该是数组").isTrue();
            assertThat(data.size()).as("data 应该有元素").isGreaterThan(0);

            JsonNode firstItem = data.get(0);
            System.out.println("\n=== 歌曲信息 ===");
            System.out.println("ID: " + firstItem.path("id").asLong());
            System.out.println("URL: " + firstItem.path("url").asText());
            System.out.println("Size: " + firstItem.path("size").asLong() + " bytes");
            System.out.println("Br (比特率): " + firstItem.path("br").asLong());
            System.out.println("Level: " + firstItem.path("level").asText());
            System.out.println("Type: " + firstItem.path("type").asText());
            System.out.println("Fee (收费类型): " + firstItem.path("fee").asInt());

            // 检查关键字段
            assertThat(firstItem.path("url").asText()).as("URL 不应为空").isNotEmpty();
            assertThat(firstItem.path("size").asLong()).as("文件大小应该 > 0").isGreaterThan(0);

        } catch (Exception e) {
            System.err.println("API 调用失败: " + e.getMessage());
            throw new RuntimeException("API 测试失败", e);
        }
    }

    /**
     * RED TEST 3: 测试不同音质等级
     */
    @Test
    void checkDifferentQualityLevels() {
        System.out.println("\n=== 测试 3: 不同音质等级对比 ===");

        String cookie = musicApiService.getAuthCookie(VIP_USER_ID);
        assertThat(cookie).isNotNull();

        String[] levels = {"standard", "higher", "exhigh", "lossless", "hires"};

        for (String level : levels) {
            try {
                String apiUrl = String.format(
                    "http://127.0.0.1:3000/song/url/v1?id=%d&level=%s&cookie=%s",
                    TEST_SONG_ID, level, cookie
                );

                String response = restTemplate.getForObject(apiUrl, String.class);
                JsonNode root = objectMapper.readTree(response);
                JsonNode data = root.path("data");

                if (data.isArray() && data.size() > 0) {
                    JsonNode firstItem = data.get(0);
                    String url = firstItem.path("url").asText(null);
                    long size = firstItem.path("size").asLong(0);
                    long br = firstItem.path("br").asLong(0);

                    System.out.println(String.format(
                        "音质: %-10s | 有 URL: %-5s | 大小: %10d bytes | 比特率: %d",
                        level,
                        (url != null && !url.isEmpty()),
                        size,
                        br
                    ));
                }

            } catch (Exception e) {
                System.err.println("测试音质 " + level + " 失败: " + e.getMessage());
            }
        }
    }

    /**
     * RED TEST 4: 检查 VIP 状态
     */
    @Test
    void checkVipStatus() {
        System.out.println("\n=== 测试 4: 检查 VIP 状态 ===");

        String cookie = musicApiService.getAuthCookie(VIP_USER_ID);
        assertThat(cookie).isNotNull();

        try {
            String apiUrl = String.format(
                "http://127.0.0.1:3000/login/status?cookie=%s",
                cookie
            );

            String response = restTemplate.getForObject(apiUrl, String.class);
            JsonNode root = objectMapper.readTree(response);
            JsonNode data = root.path("data");
            JsonNode profile = data.path("profile");
            JsonNode account = data.path("account");

            System.out.println("用户信息:");
            System.out.println("  昵称: " + profile.path("nickname").asText());
            System.out.println("  用户 ID: " + profile.path("userId").asLong());
            System.out.println("  VIP 类型: " + account.path("vipType").asInt());
            System.out.println("  黑胶 VIP: " + account.path("vipType").asInt());

            // 0=非会员, 1=VIP, 11=黑胶VIP
            int vipType = account.path("vipType").asInt();
            System.out.println("\n当前 VIP 等级: " + (vipType == 0 ? "普通用户" :
                                                     vipType == 1 ? "VIP" :
                                                     vipType == 11 ? "黑胶 VIP" :
                                                     "未知(" + vipType + ")"));

        } catch (Exception e) {
            System.err.println("检查 VIP 状态失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
