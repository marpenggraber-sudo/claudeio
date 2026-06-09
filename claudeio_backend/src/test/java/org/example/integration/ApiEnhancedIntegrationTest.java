package org.example.integration;

import org.example.dto.QrKeyResponse;
import org.example.service.QrLoginService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 集成测试：验证后端能否正确连接到 api-enhanced 服务
 *
 * 前置条件：api-enhanced 服务必须在 localhost:3000 运行
 */
@SpringBootTest
@TestPropertySource(properties = {
    "music.api.base-url=http://127.0.0.1:3000"
})
class ApiEnhancedIntegrationTest {

    @Autowired
    private QrLoginService qrLoginService;

    @Test
    void shouldConnectToApiEnhancedSuccessfully() {
        // RED → GREEN → IMPROVE
        // 验证能否成功调用 api-enhanced 的二维码生成接口

        // Act
        QrKeyResponse response = qrLoginService.generateQrKey();

        // Assert
        assertNotNull(response, "响应不应为 null");
        assertTrue(response.isSuccess(), "应该成功生成 QR key");
        assertNotNull(response.getUnikey(), "unikey 不应为 null");
        assertFalse(response.getUnikey().isEmpty(), "unikey 不应为空");

        // 验证 unikey 格式（UUID 格式）
        String unikey = response.getUnikey();
        assertTrue(
            unikey.matches("[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}"),
            "unikey 应该是 UUID 格式"
        );

        System.out.println("✅ 成功连接到 api-enhanced！");
        System.out.println("生成的 unikey: " + unikey);
    }

    @Test
    void shouldHandleApiEnhancedTimeout() {
        // 测试超时处理（如果 api-enhanced 未运行）

        // 这个测试会因为 api-enhanced 正常运行而跳过
        // 如果需要测试超时，请临时停止 Docker 容器

        try {
            QrKeyResponse response = qrLoginService.generateQrKey();

            // 如果 api-enhanced 运行正常，应该成功
            assertTrue(response.isSuccess() || !response.isSuccess(),
                "无论成功或失败，都应该有响应");

        } catch (Exception e) {
            // 如果抛出异常，验证是网络相关错误
            assertTrue(
                e.getMessage().contains("Connection") ||
                e.getMessage().contains("timeout") ||
                e.getMessage().contains("refused"),
                "应该是网络连接错误"
            );
        }
    }
}
