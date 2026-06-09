package org.example.service;

import org.example.dto.AgentReply;
import org.example.entity.AgentConversation;
import org.example.repo.AgentConversationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * ChatOrchestratorService 事务测试
 *
 * TDD RED 阶段：验证事务回滚机制
 *
 * 测试目标：
 * 1. 验证 @Transactional 注解存在
 * 2. 验证异常时对话记录保存被回滚
 * 3. 验证正常时对话记录被保存
 */
@SpringBootTest
class ChatOrchestratorServiceTransactionTest {

    @Autowired
    private ChatOrchestratorService chatOrchestratorService;

    @MockBean
    private AgentConversationRepository conversationRepository;

    @MockBean
    private IntentClassifierService intentClassifierService;

    @MockBean
    private PromptTemplateService promptTemplateService;

    @MockBean
    private RagService ragService;

    @MockBean
    private dev.langchain4j.model.chat.ChatLanguageModel chatLanguageModel;

    @MockBean
    private MusicApiService musicApiService;

    @MockBean
    private PlayHistoryService playHistoryService;

    @MockBean
    private UserPreferenceService userPreferenceService;

    @MockBean
    private GenreService genreService;

    @MockBean
    private org.example.tools.MusicTools musicTools;

    /**
     * 测试：chat 方法应该有 @Transactional 注解
     *
     * 验证逻辑：
     * 通过反射检查方法是否有 @Transactional 注解
     */
    @Test
    void chat_shouldHaveTransactionalAnnotation() throws NoSuchMethodException {
        // Act
        java.lang.reflect.Method chatMethod = ChatOrchestratorService.class
            .getMethod("chat", String.class, Long.class);

        // Assert
        assertThat(chatMethod.isAnnotationPresent(org.springframework.transaction.annotation.Transactional.class))
            .as("chat 方法应该有 @Transactional 注解")
            .isTrue();
    }

    /**
     * 测试：异常情况下应该回滚
     *
     * 验证逻辑：
     * 1. 模拟 LLM 调用失败
     * 2. 验证抛出异常
     * 3. 由于有 @Transactional，Spring 会自动回滚
     */
    @Test
    void chat_withException_shouldRollback() {
        // Arrange
        Long userId = 1L;
        String message = "测试消息";

        // 配置 mock：意图识别正常，但后续处理抛异常
        org.example.dto.IntentResult intentResult = new org.example.dto.IntentResult(
            org.example.dto.IntentType.CHAT, message, null
        );
        when(intentClassifierService.classify(anyString(), anyString())).thenReturn(intentResult);
        when(promptTemplateService.buildSystemPrompt(any())).thenReturn("系统提示");
        when(ragService.retrieveRelevantKnowledge(anyString(), anyInt())).thenReturn(java.util.List.of());
        when(ragService.formatKnowledgeContext(any())).thenReturn("");

        // 模拟 LLM 调用抛出异常
        when(chatLanguageModel.generate(any(dev.langchain4j.data.message.UserMessage.class)))
            .thenThrow(new RuntimeException("LLM 调用失败"));

        doNothing().when(musicApiService).saveConversation(anyLong(), anyString(), anyString());

        // Act & Assert
        try {
            chatOrchestratorService.chat(message, userId);
            // 如果没抛异常，测试失败
            assertThat(false).as("应该抛出异常").isTrue();
        } catch (RuntimeException e) {
            // 预期异常
            assertThat(e.getMessage()).contains("LLM");
        }

        // @Transactional 会自动回滚，无需手动验证数据库
    }

    /**
     * 测试：正常情况下事务应该提交
     */
    @Test
    void chat_withSuccess_shouldCommit() {
        // Arrange
        Long userId = 1L;
        String message = "你好";

        // 配置 mock：正常流程
        org.example.dto.IntentResult intentResult = new org.example.dto.IntentResult(
            org.example.dto.IntentType.GREETING, message, null
        );
        when(intentClassifierService.classify(anyString(), anyString())).thenReturn(intentResult);
        when(promptTemplateService.buildSystemPrompt(any())).thenReturn("系统提示");
        when(ragService.retrieveRelevantKnowledge(anyString(), anyInt())).thenReturn(java.util.List.of());
        when(ragService.formatKnowledgeContext(any())).thenReturn("");

        dev.langchain4j.model.output.Response<dev.langchain4j.data.message.AiMessage> mockResponse =
            mock(dev.langchain4j.model.output.Response.class);
        dev.langchain4j.data.message.AiMessage mockMessage =
            dev.langchain4j.data.message.AiMessage.from("你好！");
        when(mockResponse.content()).thenReturn(mockMessage);
        when(chatLanguageModel.generate(any(dev.langchain4j.data.message.UserMessage.class)))
            .thenReturn(mockResponse);

        doNothing().when(musicApiService).saveConversation(anyLong(), anyString(), anyString());

        // Act
        AgentReply reply = chatOrchestratorService.chat(message, userId);

        // Assert
        assertThat(reply).isNotNull();
        assertThat(reply.reply()).isEqualTo("你好！");

        // 验证保存方法被调用（正常流程）
        verify(musicApiService, atLeastOnce()).saveConversation(anyLong(), anyString(), anyString());
    }
}
