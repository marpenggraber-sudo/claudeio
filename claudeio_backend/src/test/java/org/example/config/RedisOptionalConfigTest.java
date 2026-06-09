package org.example.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TDD Test: 验证 Redis 配置为可选
 *
 * 场景 1: Redis 配置注释掉，应用正常启动
 * 场景 2: RedisTemplate bean 不存在时，服务依然正常工作
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.data.redis.host=",  // 清空 Redis 配置
    "spring.data.redis.port="
})
class RedisOptionalConfigTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void shouldStartApplicationWithoutRedis() {
        // GREEN: 应用应该能正常启动，即使没有 Redis 配置
        assertNotNull(applicationContext, "ApplicationContext 应该成功加载");
    }

    @Test
    void shouldNotCreateRedisTemplateBeanWhenRedisNotConfigured() {
        // GREEN: 当 Redis 未配置时，不应该创建 RedisTemplate bean

        boolean hasRedisTemplate = applicationContext.containsBean("redisTemplate");

        assertFalse(hasRedisTemplate,
            "Redis 未配置时，不应该创建 RedisTemplate bean");
    }

    @Test
    void shouldHaveAuthServiceEvenWithoutRedis() {
        // GREEN: AuthService 应该存在，即使 Redis 不可用

        boolean hasAuthService = applicationContext.containsBean("authService");

        assertTrue(hasAuthService,
            "AuthService 应该正常创建，不依赖 Redis");
    }
}
