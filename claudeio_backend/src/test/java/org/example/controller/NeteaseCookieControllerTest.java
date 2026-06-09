package org.example.controller;

import org.example.dto.NeteaseCookieResponse;
import org.example.service.*;
import org.example.tools.MusicTools;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * TDD Test for Netease Login API Endpoints
 *
 * 测试网易云登录相关的 REST API
 */
@WebMvcTest(MusicController.class)
class NeteaseCookieControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NeteaseCookieService neteaseCookieService;

    // Mock 其他依赖
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

    /**
     * RED Test 1: POST /api/music/netease-login (手机号登录成功)
     */
    @Test
    void shouldReturnCookieWhenLoginWithPhoneSuccess() throws Exception {
        // Arrange
        String expectedCookie = "MUSIC_U=test_cookie_value";
        NeteaseCookieResponse mockResponse = NeteaseCookieResponse.success(expectedCookie);

        when(neteaseCookieService.loginWithPhone(anyString(), anyString()))
            .thenReturn(mockResponse);

        // Act & Assert
        mockMvc.perform(post("/api/music/netease-login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "loginType": "phone",
                        "account": "13800138000",
                        "password": "test123"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.cookie").value(expectedCookie))
            .andExpect(jsonPath("$.errorMessage").doesNotExist());
    }

    /**
     * RED Test 2: POST /api/music/netease-login (邮箱登录成功)
     */
    @Test
    void shouldReturnCookieWhenLoginWithEmailSuccess() throws Exception {
        // Arrange
        String expectedCookie = "MUSIC_U=test_cookie_value";
        NeteaseCookieResponse mockResponse = NeteaseCookieResponse.success(expectedCookie);

        when(neteaseCookieService.loginWithEmail(anyString(), anyString()))
            .thenReturn(mockResponse);

        // Act & Assert
        mockMvc.perform(post("/api/music/netease-login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "loginType": "email",
                        "account": "test@example.com",
                        "password": "test123"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.cookie").value(expectedCookie));
    }

    /**
     * RED Test 3: POST /api/music/netease-login (登录失败)
     */
    @Test
    void shouldReturnErrorWhenLoginFails() throws Exception {
        // Arrange
        NeteaseCookieResponse mockResponse = NeteaseCookieResponse.failure("密码错误");

        when(neteaseCookieService.loginWithPhone(anyString(), anyString()))
            .thenReturn(mockResponse);

        // Act & Assert
        mockMvc.perform(post("/api/music/netease-login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "loginType": "phone",
                        "account": "13800138000",
                        "password": "wrong"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.cookie").doesNotExist())
            .andExpect(jsonPath("$.errorMessage").value("密码错误"));
    }

    /**
     * RED Test 4: POST /api/music/netease-login (缺少必填字段)
     */
    @Test
    void shouldReturnBadRequestWhenMissingFields() throws Exception {
        mockMvc.perform(post("/api/music/netease-login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "loginType": "phone"
                    }
                    """))
            .andExpect(status().isBadRequest());
    }

    /**
     * RED Test 5: POST /api/music/netease-login (无效的登录类型)
     */
    @Test
    void shouldReturnBadRequestWhenInvalidLoginType() throws Exception {
        mockMvc.perform(post("/api/music/netease-login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "loginType": "invalid",
                        "account": "test",
                        "password": "test123"
                    }
                    """))
            .andExpect(status().isBadRequest());
    }

    /**
     * RED Test 6: GET /api/music/cookie-status (Cookie 有效)
     */
    @Test
    void shouldReturnValidWhenCookieIsValid() throws Exception {
        // Arrange
        when(neteaseCookieService.validateCookie(anyString()))
            .thenReturn(true);

        // Act & Assert
        mockMvc.perform(get("/api/music/cookie-status")
                .param("cookie", "MUSIC_U=test_cookie"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.valid").value(true));
    }

    /**
     * RED Test 7: GET /api/music/cookie-status (Cookie 无效)
     */
    @Test
    void shouldReturnInvalidWhenCookieIsInvalid() throws Exception {
        // Arrange
        when(neteaseCookieService.validateCookie(anyString()))
            .thenReturn(false);

        // Act & Assert
        mockMvc.perform(get("/api/music/cookie-status")
                .param("cookie", "MUSIC_U=invalid"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.valid").value(false));
    }

    /**
     * RED Test 8: GET /api/music/cookie-status (缺少 cookie 参数)
     */
    @Test
    void shouldReturnBadRequestWhenCookieMissing() throws Exception {
        mockMvc.perform(get("/api/music/cookie-status"))
            .andExpect(status().isBadRequest());
    }
}
