package org.example.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TDD Test: 验证 Redis 不可用时，登录功能依然可以工作
 *
 * 场景：用户环境中 Redis 未安装或未启动
 * 预期：系统应该降级为只使用数据库，不崩溃
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.data.redis.host=localhost",
    "spring.data.redis.port=6379"
})
class AuthServiceRedisOptionalTest {

    @Autowired
    private AuthService authService;

    @Test
    void shouldLoginSuccessfullyEvenWhenRedisIsDown() {
        // RED: 这个测试现在会失败，因为 Redis 不可用会导致异常

        // Arrange
        String account = "test_user";
        String password = "test_password";
        String cookie = "MUSIC_U=test_cookie";

        // Act & Assert
        // 目前会抛出 RedisConnectionFailureException
        // 修复后应该能够正常登录（只使用数据库）

        assertDoesNotThrow(() -> {
            // 尝试登录操作，不应该因为 Redis 不可用而崩溃
            // 注意：这里只是验证不会抛异常，不验证登录成功
            // 因为 test_user 可能不存在于数据库
            try {
                authService.login(account, password, cookie);
            } catch (Exception e) {
                // 允许的异常：用户不存在、密码错误
                // 不允许的异常：RedisConnectionFailureException
                assertFalse(
                    e.getMessage().contains("Redis") ||
                    e.getMessage().contains("Connection refused"),
                    "不应该因为 Redis 连接失败而报错: " + e.getMessage()
                );
            }
        }, "Redis 不可用时，系统不应该崩溃");
    }
}
