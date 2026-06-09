package org.example.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.dto.ChatRequest;
import org.example.dto.PlayHistoryRequest;
import org.example.service.AgentFacadeService;
import org.example.service.PlayHistoryService;
import org.example.service.UserPreferenceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * MusicController 输入验证测试
 *
 * TDD RED 阶段：测试必须失败，因为 Controller 还没有添加 @Valid 注解
 */
@WebMvcTest(MusicController.class)
class MusicControllerValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AgentFacadeService agentFacadeService;

    @MockBean
    private PlayHistoryService playHistoryService;

    @MockBean
    private UserPreferenceService userPreferenceService;

    @MockBean
    private org.example.service.MusicApiService musicApiService;

    @MockBean
    private org.example.tools.MusicTools musicTools;

    @MockBean
    private org.example.service.AuthService authService;

    @MockBean
    private org.example.service.GenreService genreService;

    // ==================== ChatRequest 验证测试 ====================

    @Test
    void chat_withNullUserId_shouldReturn400() throws Exception {
        // Arrange
        ChatRequest request = new ChatRequest("你好", null);

        // Act & Assert
        mockMvc.perform(post("/api/music/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("用户ID不能为空"));
    }

    @Test
    void chat_withNegativeUserId_shouldReturn400() throws Exception {
        // Arrange
        ChatRequest request = new ChatRequest("你好", -1L);

        // Act & Assert
        mockMvc.perform(post("/api/music/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("用户ID必须为正数"));
    }

    @Test
    void chat_withBlankMessage_shouldReturn400() throws Exception {
        // Arrange
        ChatRequest request = new ChatRequest("", 1L);

        // Act & Assert
        mockMvc.perform(post("/api/music/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("消息内容不能为空"));
    }

    @Test
    void chat_withNullMessage_shouldReturn400() throws Exception {
        // Arrange
        ChatRequest request = new ChatRequest(null, 1L);

        // Act & Assert
        mockMvc.perform(post("/api/music/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("消息内容不能为空"));
    }

    @Test
    void chat_withValidInput_shouldReturn200() throws Exception {
        // Arrange
        ChatRequest request = new ChatRequest("你好", 1L);
        when(agentFacadeService.chat(anyString(), anyLong())).thenReturn(null);

        // Act & Assert
        mockMvc.perform(post("/api/music/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    // ==================== PlayHistoryRequest 验证测试 ====================

    @Test
    void recordPlayHistory_withNullUserId_shouldReturn400() throws Exception {
        // Arrange
        PlayHistoryRequest request = new PlayHistoryRequest(
            null, 123L, "青花瓷", "周杰伦", "我很忙", 240000L, true
        );

        // Act & Assert
        mockMvc.perform(post("/api/music/play-history")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("用户ID不能为空"));
    }

    @Test
    void recordPlayHistory_withNullSongId_shouldReturn400() throws Exception {
        // Arrange
        PlayHistoryRequest request = new PlayHistoryRequest(
            1L, null, "青花瓷", "周杰伦", "我很忙", 240000L, true
        );

        // Act & Assert
        mockMvc.perform(post("/api/music/play-history")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("歌曲ID不能为空"));
    }

    @Test
    void recordPlayHistory_withBlankSongName_shouldReturn400() throws Exception {
        // Arrange
        PlayHistoryRequest request = new PlayHistoryRequest(
            1L, 123L, "", "周杰伦", "我很忙", 240000L, true
        );

        // Act & Assert
        mockMvc.perform(post("/api/music/play-history")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("歌曲名称不能为空"));
    }

    @Test
    void recordPlayHistory_withBlankArtist_shouldReturn400() throws Exception {
        // Arrange
        PlayHistoryRequest request = new PlayHistoryRequest(
            1L, 123L, "青花瓷", "", "我很忙", 240000L, true
        );

        // Act & Assert
        mockMvc.perform(post("/api/music/play-history")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("艺术家不能为空"));
    }

    @Test
    void recordPlayHistory_withValidInput_shouldReturn200() throws Exception {
        // Arrange
        PlayHistoryRequest request = new PlayHistoryRequest(
            1L, 123L, "青花瓷", "周杰伦", "我很忙", 240000L, true
        );
        doNothing().when(playHistoryService).recordPlay(anyLong(), anyLong(), anyString(), anyString(), anyInt(), anyBoolean());
        doNothing().when(userPreferenceService).updatePreference(anyLong(), anyString());

        // Act & Assert
        mockMvc.perform(post("/api/music/play-history")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }
}
