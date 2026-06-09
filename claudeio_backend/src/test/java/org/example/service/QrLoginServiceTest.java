package org.example.service;

import org.example.dto.QrImageResponse;
import org.example.dto.QrKeyResponse;
import org.example.dto.QrStatusResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * TDD Test for QR Login Service
 *
 * 测试二维码登录相关功能
 */
@ExtendWith(MockitoExtension.class)
class QrLoginServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private QrLoginService qrLoginService;

    /**
     * RED Test 1: 生成二维码 Key 成功
     */
    @Test
    void shouldGenerateQrKeySuccessfully() {
        // Arrange
        String mockResponse = "{\"data\":{\"code\":200,\"unikey\":\"test-key-123\"},\"code\":200}";
        when(restTemplate.getForObject(anyString(), eq(String.class)))
            .thenReturn(mockResponse);

        // Act
        QrKeyResponse response = qrLoginService.generateQrKey();

        // Assert
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getUnikey()).isEqualTo("test-key-123");
    }

    /**
     * RED Test 2: 生成二维码图片成功
     */
    @Test
    void shouldCreateQrImageSuccessfully() {
        // Arrange
        String testKey = "test-key-123";
        String mockResponse = "{\"code\":200,\"data\":{\"qrurl\":\"https://music.163.com/login?codekey=test\",\"qrimg\":\"data:image/png;base64,abc123\"}}";
        when(restTemplate.getForObject(anyString(), eq(String.class)))
            .thenReturn(mockResponse);

        // Act
        QrImageResponse response = qrLoginService.createQrImage(testKey);

        // Assert
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getQrurl()).contains("music.163.com");
        assertThat(response.getQrimg()).startsWith("data:image/png;base64");
    }

    /**
     * RED Test 3: 检查二维码状态 - 等待扫码（801）
     */
    @Test
    void shouldReturnWaitingStatusWhenNotScanned() {
        // Arrange
        String testKey = "test-key-123";
        String mockResponse = "{\"code\":801,\"message\":\"等待扫码\"}";
        when(restTemplate.getForObject(anyString(), eq(String.class)))
            .thenReturn(mockResponse);

        // Act
        QrStatusResponse response = qrLoginService.checkQrStatus(testKey);

        // Assert
        assertThat(response.getCode()).isEqualTo(801);
        assertThat(response.getMessage()).contains("等待");
        assertThat(response.isSuccess()).isFalse();
    }

    /**
     * RED Test 4: 检查二维码状态 - 已扫码（802）
     */
    @Test
    void shouldReturnScannedStatusWhenScanned() {
        // Arrange
        String testKey = "test-key-123";
        String mockResponse = "{\"code\":802,\"message\":\"已扫码，等待确认\"}";
        when(restTemplate.getForObject(anyString(), eq(String.class)))
            .thenReturn(mockResponse);

        // Act
        QrStatusResponse response = qrLoginService.checkQrStatus(testKey);

        // Assert
        assertThat(response.getCode()).isEqualTo(802);
        assertThat(response.getMessage()).contains("已扫码");
        assertThat(response.isSuccess()).isFalse();
    }

    /**
     * RED Test 5: 检查二维码状态 - 登录成功（803）
     */
    @Test
    void shouldReturnSuccessWithCookieWhenLoginSuccessful() {
        // Arrange
        String testKey = "test-key-123";
        String mockResponse = "{\"code\":803,\"message\":\"授权登录成功\",\"cookie\":\"MUSIC_U=test_cookie_value\"}";
        when(restTemplate.getForObject(anyString(), eq(String.class)))
            .thenReturn(mockResponse);

        // Act
        QrStatusResponse response = qrLoginService.checkQrStatus(testKey);

        // Assert
        assertThat(response.getCode()).isEqualTo(803);
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getCookie()).isNotNull();
        assertThat(response.getCookie()).contains("MUSIC_U");
    }

    /**
     * RED Test 6: 检查二维码状态 - 二维码过期（800）
     */
    @Test
    void shouldReturnExpiredStatusWhenQrExpired() {
        // Arrange
        String testKey = "test-key-123";
        String mockResponse = "{\"code\":800,\"message\":\"二维码不存在或已过期\"}";
        when(restTemplate.getForObject(anyString(), eq(String.class)))
            .thenReturn(mockResponse);

        // Act
        QrStatusResponse response = qrLoginService.checkQrStatus(testKey);

        // Assert
        assertThat(response.getCode()).isEqualTo(800);
        assertThat(response.getMessage()).contains("过期");
        assertThat(response.isSuccess()).isFalse();
    }

    /**
     * RED Test 7: 网络错误时生成 Key 失败
     */
    @Test
    void shouldHandleNetworkErrorWhenGeneratingKey() {
        // Arrange
        when(restTemplate.getForObject(anyString(), eq(String.class)))
            .thenThrow(new RuntimeException("Network error"));

        // Act
        QrKeyResponse response = qrLoginService.generateQrKey();

        // Assert
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getErrorMessage()).contains("网络错误");
    }
}
