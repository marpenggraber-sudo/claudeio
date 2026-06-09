package org.example.service;

import org.example.dto.NeteaseCookieResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * TDD Test for NeteaseCookieService
 *
 * 测试网易云 Cookie 自动获取功能
 */
@ExtendWith(MockitoExtension.class)
class NeteaseCookieServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private MusicApiService musicApiService;

    private NeteaseCookieService neteaseCookieService;

    @BeforeEach
    void setUp() {
        neteaseCookieService = new NeteaseCookieService(restTemplate, musicApiService);
    }

    /**
     * RED Test 1: 测试手机号登录成功
     *
     * Given: 有效的手机号和密码
     * When: 调用 loginWithPhone
     * Then: 返回包含 Cookie 的 NeteaseCookieResponse
     */
    @Test
    void shouldReturnCookieWhenLoginWithValidPhone() {
        // Arrange
        String phone = "13800138000";
        String password = "test123";
        String expectedCookie = "MUSIC_U=test_cookie_value";

        // Mock 网易云 API 响应
        String mockResponse = """
            {
                "code": 200,
                "cookie": "MUSIC_U=test_cookie_value; Path=/; Domain=.music.163.com"
            }
            """;

        when(restTemplate.postForObject(
            anyString(),
            any(),
            eq(String.class)
        )).thenReturn(mockResponse);

        // Act
        NeteaseCookieResponse response = neteaseCookieService.loginWithPhone(phone, password);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getCookie()).isEqualTo(expectedCookie);
        assertThat(response.getErrorMessage()).isNull();
    }

    /**
     * RED Test 2: 测试邮箱登录成功
     */
    @Test
    void shouldReturnCookieWhenLoginWithValidEmail() {
        // Arrange
        String email = "test@example.com";
        String password = "test123";
        String expectedCookie = "MUSIC_U=test_cookie_value";

        String mockResponse = """
            {
                "code": 200,
                "cookie": "MUSIC_U=test_cookie_value; Path=/; Domain=.music.163.com"
            }
            """;

        when(restTemplate.postForObject(
            anyString(),
            any(),
            eq(String.class)
        )).thenReturn(mockResponse);

        // Act
        NeteaseCookieResponse response = neteaseCookieService.loginWithEmail(email, password);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getCookie()).isEqualTo(expectedCookie);
    }

    /**
     * RED Test 3: 测试登录失败（密码错误）
     */
    @Test
    void shouldReturnErrorWhenPasswordIncorrect() {
        // Arrange
        String phone = "13800138000";
        String wrongPassword = "wrong_password";

        String mockResponse = """
            {
                "code": 502,
                "message": "密码错误"
            }
            """;

        when(restTemplate.postForObject(
            anyString(),
            any(),
            eq(String.class)
        )).thenReturn(mockResponse);

        // Act
        NeteaseCookieResponse response = neteaseCookieService.loginWithPhone(phone, wrongPassword);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getCookie()).isNull();
        assertThat(response.getErrorMessage()).contains("密码错误");
    }

    /**
     * RED Test 4: 测试 Cookie 验证
     */
    @Test
    void shouldReturnTrueWhenCookieIsValid() {
        // Arrange
        String validCookie = "MUSIC_U=test_cookie_value";

        String mockResponse = """
            {
                "code": 200,
                "account": {
                    "id": 123456
                }
            }
            """;

        when(restTemplate.getForObject(
            anyString(),
            eq(String.class)
        )).thenReturn(mockResponse);

        // Act
        boolean isValid = neteaseCookieService.validateCookie(validCookie);

        // Assert
        assertThat(isValid).isTrue();
    }

    /**
     * RED Test 5: 测试 Cookie 无效
     */
    @Test
    void shouldReturnFalseWhenCookieIsInvalid() {
        // Arrange
        String invalidCookie = "MUSIC_U=invalid_cookie";

        String mockResponse = """
            {
                "code": 301,
                "message": "需要登录"
            }
            """;

        when(restTemplate.getForObject(
            anyString(),
            eq(String.class)
        )).thenReturn(mockResponse);

        // Act
        boolean isValid = neteaseCookieService.validateCookie(invalidCookie);

        // Assert
        assertThat(isValid).isFalse();
    }

    /**
     * RED Test 6: 测试网络异常处理
     */
    @Test
    void shouldHandleNetworkException() {
        // Arrange
        String phone = "13800138000";
        String password = "test123";

        when(restTemplate.postForObject(
            anyString(),
            any(),
            eq(String.class)
        )).thenThrow(new RuntimeException("Network error"));

        // Act
        NeteaseCookieResponse response = neteaseCookieService.loginWithPhone(phone, password);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getErrorMessage()).contains("网络错误");
    }

    /**
     * RED Test 7: 测试空参数验证
     */
    @Test
    void shouldThrowExceptionWhenPhoneIsNull() {
        // Act & Assert
        assertThatThrownBy(() ->
            neteaseCookieService.loginWithPhone(null, "password")
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("手机号不能为空");
    }

    @Test
    void shouldThrowExceptionWhenPasswordIsNull() {
        // Act & Assert
        assertThatThrownBy(() ->
            neteaseCookieService.loginWithPhone("13800138000", null)
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("密码不能为空");
    }
}
