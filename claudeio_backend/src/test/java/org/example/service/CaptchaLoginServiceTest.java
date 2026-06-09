package org.example.service;

import org.example.dto.CaptchaResponse;
import org.example.dto.NeteaseCookieResponse;
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
 * TDD Test for Captcha Login Service
 *
 * 测试验证码登录相关功能
 */
@ExtendWith(MockitoExtension.class)
class CaptchaLoginServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private MusicApiService musicApiService;

    @InjectMocks
    private CaptchaLoginService captchaLoginService;

    private String testPhone;
    private String testCaptcha;

    @BeforeEach
    void setUp() {
        testPhone = "13800138000";
        testCaptcha = "123456";
    }

    /**
     * RED Test 1: 发送验证码成功
     */
    @Test
    void shouldSendCaptchaSuccessfullyWhenPhoneValid() {
        // Arrange
        String mockResponse = "{\"code\":200,\"message\":\"发送成功\"}";
        when(restTemplate.getForObject(anyString(), eq(String.class)))
            .thenReturn(mockResponse);

        // Act
        CaptchaResponse response = captchaLoginService.sendCaptcha(testPhone);

        // Assert
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).contains("发送成功");
    }

    /**
     * RED Test 2: 手机号为空时发送验证码失败
     */
    @Test
    void shouldFailWhenPhoneIsEmpty() {
        // Act
        CaptchaResponse response = captchaLoginService.sendCaptcha("");

        // Assert
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getMessage()).contains("手机号不能为空");
    }

    /**
     * RED Test 3: 手机号格式错误时发送验证码失败
     */
    @Test
    void shouldFailWhenPhoneFormatInvalid() {
        // Act
        CaptchaResponse response = captchaLoginService.sendCaptcha("12345");

        // Assert
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getMessage()).contains("手机号格式错误");
    }

    /**
     * RED Test 4: 验证码登录成功
     */
    @Test
    void shouldLoginSuccessfullyWithValidCaptcha() {
        // Arrange
        String mockResponse = "{\"code\":200,\"cookie\":\"MUSIC_U=test_cookie_value\"}";
        when(restTemplate.getForObject(anyString(), eq(String.class)))
            .thenReturn(mockResponse);

        // Act
        NeteaseCookieResponse response = captchaLoginService.loginWithCaptcha(testPhone, testCaptcha);

        // Assert
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getCookie()).isNotNull();
        assertThat(response.getCookie()).startsWith("MUSIC_U=");
    }

    /**
     * RED Test 5: 验证码错误时登录失败
     */
    @Test
    void shouldFailLoginWhenCaptchaInvalid() {
        // Arrange
        String mockResponse = "{\"code\":503,\"message\":\"验证码错误\"}";
        when(restTemplate.getForObject(anyString(), eq(String.class)))
            .thenReturn(mockResponse);

        // Act
        NeteaseCookieResponse response = captchaLoginService.loginWithCaptcha(testPhone, "wrong");

        // Assert
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getErrorMessage()).contains("验证码错误");
    }

    /**
     * RED Test 6: 验证码为空时登录失败
     */
    @Test
    void shouldFailLoginWhenCaptchaEmpty() {
        // Act
        NeteaseCookieResponse response = captchaLoginService.loginWithCaptcha(testPhone, "");

        // Assert
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getErrorMessage()).contains("验证码不能为空");
    }

    /**
     * RED Test 7: 验证码过期时登录失败
     */
    @Test
    void shouldFailLoginWhenCaptchaExpired() {
        // Arrange
        String mockResponse = "{\"code\":504,\"message\":\"验证码已过期\"}";
        when(restTemplate.getForObject(anyString(), eq(String.class)))
            .thenReturn(mockResponse);

        // Act
        NeteaseCookieResponse response = captchaLoginService.loginWithCaptcha(testPhone, testCaptcha);

        // Assert
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getErrorMessage()).contains("验证码已过期");
    }

    /**
     * RED Test 8: 网络异常时发送验证码失败
     */
    @Test
    void shouldHandleNetworkErrorWhenSendingCaptcha() {
        // Arrange
        when(restTemplate.getForObject(anyString(), eq(String.class)))
            .thenThrow(new RuntimeException("Network error"));

        // Act
        CaptchaResponse response = captchaLoginService.sendCaptcha(testPhone);

        // Assert
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getMessage()).contains("网络错误");
    }

    /**
     * RED Test 9: 安全风险（10004）时登录失败
     */
    @Test
    void shouldFailLoginWhenSecurityRiskDetected() {
        // Arrange - 模拟网易云返回 10004 安全风险错误
        String mockResponse = "{\"code\":10004,\"message\":\"当前登录存在安全风险，请稍后再试\"}";
        when(restTemplate.getForObject(anyString(), eq(String.class)))
            .thenReturn(mockResponse);

        // Act
        NeteaseCookieResponse response = captchaLoginService.loginWithCaptcha(testPhone, testCaptcha);

        // Assert
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getErrorMessage()).contains("安全风险");
    }
}
