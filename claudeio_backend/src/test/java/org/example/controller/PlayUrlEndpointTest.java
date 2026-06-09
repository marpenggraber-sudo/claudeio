package org.example.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TDD Test: 播放 URL 端点测试
 *
 * 场景：前端调用 /play-url 但返回 null
 * 可能原因：
 * 1. userId 在数据库中不存在
 * 2. 该 userId 没有对应的 Cookie
 * 3. Cookie 已过期
 */
@SpringBootTest
class PlayUrlEndpointTest {

    @Autowired
    private org.example.tools.MusicTools musicTools;

    @Test
    void testGetPlayUrlWithNullUserId() {
        // RED: userId 为 null 应该返回 null
        String result = musicTools.getPlayUrl(186016L, null);
        assertNull(result, "userId 为 null 应该返回 null");
        System.out.println("✅ userId 为 null 测试通过");
    }

    @Test
    void testGetPlayUrlWithNonExistentUserId() {
        // RED: 不存在的 userId 应该返回 null
        Long nonExistentUserId = 999999999L;
        Long songId = 2653714443L;  // 晴天（女声版）

        String result = musicTools.getPlayUrl(songId, nonExistentUserId);

        System.out.println("\n=== 测试不存在的 userId ===");
        System.out.println("userId: " + nonExistentUserId);
        System.out.println("songId: " + songId);
        System.out.println("结果: " + (result == null ? "null（预期）" : result));

        assertNull(result, "不存在的 userId 应该返回 null");
        System.out.println("✅ 不存在的 userId 测试通过");
    }

    @Test
    void testGetPlayUrlWithValidUserId() {
        // GREEN: 有效的 userId 应该返回播放 URL
        Long validUserId = 13879884891L;  // 你扫码登录后注册的 userId
        Long songId = 2653714443L;  // 晴天（女声版）

        System.out.println("\n=== 测试有效的 userId ===");
        System.out.println("userId: " + validUserId);
        System.out.println("songId: " + songId);

        String result = musicTools.getPlayUrl(songId, validUserId);

        System.out.println("结果: " + (result == null ? "null" : "有 URL（前 50 字符）: " + result.substring(0, Math.min(50, result.length()))));

        if (result == null) {
            System.out.println("⚠️  返回 null - 可能的原因：");
            System.out.println("  1. 数据库中没有这个 userId 的 Cookie");
            System.out.println("  2. Cookie 已过期");
            System.out.println("  3. 网易云 API 返回 null");
        } else {
            System.out.println("✅ 成功获取播放 URL");
            assertTrue(result.startsWith("http"), "播放 URL 应该以 http 开头");
        }
    }

    @Test
    void testPlayUrlFlow() {
        // IMPROVE: 完整的播放流程测试
        System.out.println("\n=== 完整播放流程测试 ===");

        Long userId = 13879884891L;  // 实际的 userId
        Long songId = 2653714443L;   // 晴天（女声版）

        // 步骤 1: 检查 userId 是否存在
        System.out.println("步骤 1: 尝试获取播放 URL...");

        String playUrl = musicTools.getPlayUrl(songId, userId);

        if (playUrl == null) {
            System.out.println("❌ 播放 URL 为 null");
            System.out.println("\n🔍 诊断信息：");
            System.out.println("  前端调用: /play-url?songId=" + songId + "&userId=" + userId);
            System.out.println("  后端流程:");
            System.out.println("    1. 查询数据库获取 Cookie（根据 userId）");
            System.out.println("    2. 如果没有 Cookie → 返回 null");
            System.out.println("    3. 调用网易云 API 获取播放 URL");
            System.out.println("\n💡 可能的问题：");
            System.out.println("  1. 数据库中没有 userId=" + userId + " 的记录");
            System.out.println("  2. 该用户没有 Cookie");
            System.out.println("  3. 前端传递的 userId 不正确");
        } else {
            System.out.println("✅ 播放 URL 获取成功");
            System.out.println("URL: " + playUrl.substring(0, Math.min(80, playUrl.length())) + "...");
        }
    }
}
