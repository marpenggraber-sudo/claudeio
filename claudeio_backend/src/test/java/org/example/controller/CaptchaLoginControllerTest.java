package org.example.controller;

import org.example.dto.CaptchaResponse;
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

@WebMvcTest(MusicController.class)
class CaptchaLoginControllerTest {

    @Autowired
    private MockMvc mockMvc;

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
    void shouldSendCaptchaSuccessfully() throws Exception {
        CaptchaResponse mockResponse = CaptchaResponse.success("发送成功");
        when(captchaLoginService.sendCaptcha(anyString()))
            .thenReturn(mockResponse);

        mockMvc.perform(post("/api/music/send-captcha")
                .param("phone", "13800138000"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("发送成功"));
    }

    @Test
    void shouldReturnBadRequestWhenPhoneMissing() throws Exception {
        mockMvc.perform(post("/api/music/send-captcha"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnErrorWhenPhoneFormatInvalid() throws Exception {
        CaptchaResponse mockResponse = CaptchaResponse.failure("手机号格式错误");
        when(captchaLoginService.sendCaptcha(anyString()))
            .thenReturn(mockResponse);

        mockMvc.perform(post("/api/music/send-captcha")
                .param("phone", "12345"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").value("手机号格式错误"));
    }

    @Test
    void shouldLoginSuccessfullyWithValidCaptcha() throws Exception {
        String expectedCookie = "MUSIC_U=test_cookie_value";
        NeteaseCookieResponse mockResponse = NeteaseCookieResponse.success(expectedCookie);
        when(captchaLoginService.loginWithCaptcha(anyString(), anyString()))
            .thenReturn(mockResponse);

        mockMvc.perform(post("/api/music/captcha-login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"phone\":\"13800138000\",\"captcha\":\"123456\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.cookie").value(expectedCookie));
    }

    @Test
    void shouldReturnErrorWhenCaptchaInvalid() throws Exception {
        NeteaseCookieResponse mockResponse = NeteaseCookieResponse.failure("验证码错误");
        when(captchaLoginService.loginWithCaptcha(anyString(), anyString()))
            .thenReturn(mockResponse);

        mockMvc.perform(post("/api/music/captcha-login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"phone\":\"13800138000\",\"captcha\":\"999999\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.errorMessage").value("验证码错误"));
    }

    @Test
    void shouldReturnBadRequestWhenFieldsMissing() throws Exception {
        mockMvc.perform(post("/api/music/captcha-login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"phone\":\"13800138000\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnErrorWhenCaptchaExpired() throws Exception {
        NeteaseCookieResponse mockResponse = NeteaseCookieResponse.failure("验证码已过期");
        when(captchaLoginService.loginWithCaptcha(anyString(), anyString()))
            .thenReturn(mockResponse);

        mockMvc.perform(post("/api/music/captcha-login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"phone\":\"13800138000\",\"captcha\":\"123456\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.errorMessage").value("验证码已过期"));
    }
}
