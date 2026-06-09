package org.example.controller;

import org.example.dto.LoginResponse;
import org.example.dto.QrImageResponse;
import org.example.dto.QrKeyResponse;
import org.example.dto.QrStatusResponse;
import org.example.service.*;
import org.example.tools.MusicTools;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * QR Login Controller Integration Tests
 */
@WebMvcTest(MusicController.class)
class QrLoginControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private QrLoginService qrLoginService;

    @MockBean
    private CaptchaLoginService captchaLoginService;

    @MockBean
    private NeteaseCookieService neteaseCookieService;

    @MockBean
    private MusicApiService musicApiService;

    @MockBean
    private MusicTools musicTools;

    @MockBean
    private AgentFacadeService agentFacadeService;

    @MockBean
    private AuthService authService;

    @MockBean
    private PlayHistoryService playHistoryService;

    @MockBean
    private UserPreferenceService userPreferenceService;

    @MockBean
    private GenreService genreService;

    @Test
    void shouldGenerateQrKeySuccessfully() throws Exception {
        QrKeyResponse mockResponse = QrKeyResponse.success("test-key-123");
        when(qrLoginService.generateQrKey()).thenReturn(mockResponse);

        mockMvc.perform(get("/api/music/qr-key"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.unikey").value("test-key-123"));
    }

    @Test
    void shouldCreateQrImageSuccessfully() throws Exception {
        QrImageResponse mockResponse = QrImageResponse.success(
            "https://music.163.com/login?codekey=test",
            "data:image/png;base64,abc123"
        );
        when(qrLoginService.createQrImage(anyString())).thenReturn(mockResponse);

        mockMvc.perform(get("/api/music/qr-create")
                .param("key", "test-key-123"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.qrurl").exists())
            .andExpect(jsonPath("$.qrimg").exists());
    }

    @Test
    void shouldCheckQrStatusSuccessfully() throws Exception {
        QrStatusResponse mockResponse = QrStatusResponse.waiting();
        when(qrLoginService.checkQrStatus(anyString())).thenReturn(mockResponse);

        mockMvc.perform(get("/api/music/qr-check")
                .param("key", "test-key-123"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(801))
            .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void shouldReturnSuccessWithCookieWhenQrLoginSuccessful() throws Exception {
        QrStatusResponse mockResponse = QrStatusResponse.success("MUSIC_U=test_cookie");
        when(qrLoginService.checkQrStatus(anyString())).thenReturn(mockResponse);

        mockMvc.perform(get("/api/music/qr-check")
                .param("key", "test-key-123"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(803))
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.cookie").value("MUSIC_U=test_cookie"));
    }

    @Test
    void shouldReturnBadRequestWhenKeyMissing() throws Exception {
        mockMvc.perform(get("/api/music/qr-check"))
            .andExpect(status().isBadRequest());
    }

    // ============ 新增：测试 /qr-login 端点 ============

    @Test
    void shouldCompleteQrLoginSuccessfully() throws Exception {
        // RED: 测试会失败，因为控制器中使用了错误的方法名 getUserId()
        // LoginResponse 是 record，应该用 userId() 而不是 getUserId()

        // Arrange
        String testCookie = "MUSIC_U=test_cookie_123";
        Long expectedUserId = 123456789L;
        String expectedMessage = "测试用户"; // 注意：LoginResponse 第二个字段是 message

        LoginResponse mockResponse = new LoginResponse(expectedUserId, expectedMessage);
        when(qrLoginService.completeQrLogin(anyString())).thenReturn(mockResponse);

        // Act & Assert
        mockMvc.perform(post("/api/music/qr-login")
                .param("cookie", testCookie))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userId").value(expectedUserId))
            .andExpect(jsonPath("$.message").value(expectedMessage));
    }

    @Test
    void shouldReturn400WhenCompleteQrLoginFails() throws Exception {
        // Arrange
        String testCookie = "MUSIC_U=invalid_cookie";
        LoginResponse mockResponse = new LoginResponse(null, "登录失败");
        when(qrLoginService.completeQrLogin(anyString())).thenReturn(mockResponse);

        // Act & Assert
        mockMvc.perform(post("/api/music/qr-login")
                .param("cookie", testCookie))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("登录失败"));
    }

    @Test
    void shouldReturn400WhenQrLoginCookieIsEmpty() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/music/qr-login")
                .param("cookie", ""))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Cookie 不能为空"));
    }
}
