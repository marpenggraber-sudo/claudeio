package org.example.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TDD Test: 验证所有服务在 Redis 不可用时正常工作
 *
 * 场景：Redis 配置缺失，所有依赖 Redis 的服务应该降级到数据库
 * 预期：应用正常启动，所有服务正常创建
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.data.redis.host=",
    "spring.data.redis.port="
})
class AllServicesWithoutRedisTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void shouldStartAllServicesWithoutRedis() {
        // RED: 测试会失败，因为 MusicApiService 和 GenreService 需要 Redis

        assertNotNull(applicationContext, "ApplicationContext 应该成功加载");
    }

    @Test
    void shouldHaveAuthServiceWithoutRedis() {
        assertTrue(applicationContext.containsBean("authService"),
            "AuthService 应该存在");
    }

    @Test
    void shouldHaveMusicApiServiceWithoutRedis() {
        // RED: 这个测试会失败，因为 MusicApiService 需要 RedisTemplate

        assertTrue(applicationContext.containsBean("musicApiService"),
            "MusicApiService 应该存在，即使没有 Redis");
    }

    @Test
    void shouldHaveGenreServiceWithoutRedis() {
        // RED: 这个测试会失败，因为 GenreService 需要 RedisTemplate

        assertTrue(applicationContext.containsBean("genreService"),
            "GenreService 应该存在，即使没有 Redis");
    }
}
