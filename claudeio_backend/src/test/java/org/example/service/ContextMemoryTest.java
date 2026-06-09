package org.example.service;

import org.example.dto.AgentReply;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 上下文记忆测试
 *
 * TDD RED 阶段：验证对话上下文记忆
 *
 * 问题：用户先说"来点健身的音乐"，再说"来10首吧"，系统应该理解这是延续"健身"上下文
 * 但实际上系统提取为"generic"，丢失了上下文
 */
@SpringBootTest
class ContextMemoryTest {

    @Autowired
    private ChatOrchestratorService chatOrchestratorService;

    /**
     * 测试场景 1：用户先请求"健身音乐"，再追加数量
     * 应该保持"健身"上下文，不应该变成 generic
     */
    @Test
    void chat_withFollowUpRequest_shouldRememberContext() {
        // Arrange - 第一轮对话：请求健身音乐
        Long userId = 1780807465532L;
        String firstMessage = "来点健身的音乐";

        // Act 1 - 第一次请求
        AgentReply firstReply = chatOrchestratorService.chat(firstMessage, userId);

        // Assert 1 - 第一次请求应该返回歌曲
        assertThat(firstReply.songs())
            .as("第一次应该返回歌曲列表")
            .isNotEmpty();

        // Act 2 - 第二次请求：追加数量（上下文延续）
        String secondMessage = "来10首吧";
        AgentReply secondReply = chatOrchestratorService.chat(secondMessage, userId);

        // Assert 2 - 第二次请求应该保持"健身"上下文
        assertThat(secondReply.songs())
            .as("第二次请求应该返回10首歌（或最多20首）")
            .hasSizeGreaterThanOrEqualTo(5)  // 至少比默认的5首多
            .hasSizeLessThanOrEqualTo(20);   // 最多20首

        // Assert 3 - 第二次推荐的歌曲应该仍然是健身主题
        assertThat(secondReply.reply())
            .as("第二次推荐应该延续健身主题")
            .containsAnyOf("健身", "运动", "力量", "训练", "节奏");
    }

    /**
     * 测试场景 2：用户改变主题
     * 当用户明确提到新场景时，应该切换上下文
     */
    @Test
    void chat_withNewContext_shouldSwitchContext() {
        // Arrange
        Long userId = 1780807465532L;
        String firstMessage = "来点健身的音乐";

        // Act 1 - 第一次请求
        AgentReply firstReply = chatOrchestratorService.chat(firstMessage, userId);

        // Assert 1
        assertThat(firstReply.reply())
            .containsAnyOf("健身", "运动");

        // Act 2 - 第二次请求：明确切换到"睡觉"场景
        String secondMessage = "换一些睡觉听的";
        AgentReply secondReply = chatOrchestratorService.chat(secondMessage, userId);

        // Assert 2 - 应该切换到睡觉主题
        assertThat(secondReply.reply())
            .as("明确提到新场景时，应该切换上下文")
            .containsAnyOf("睡觉", "轻音乐", "舒缓", "放松")
            .doesNotContain("健身");
    }

    /**
     * 测试场景 3：数量提取
     * 用户说"来10首"、"推荐20首"，应该正确解析数量
     */
    @Test
    void chat_withExplicitCount_shouldExtractCount() {
        // Arrange
        Long userId = 1780807465532L;
        String[] testMessages = {
            "推荐10首健身的音乐",
            "来20首跑步的歌",
            "给我15首民谣"
        };

        int[] expectedMinCounts = {8, 15, 12};  // 至少应该接近目标数量
        int[] expectedMaxCounts = {12, 20, 18}; // 但不应该超太多

        // Act & Assert
        for (int i = 0; i < testMessages.length; i++) {
            String message = testMessages[i];
            int minCount = expectedMinCounts[i];
            int maxCount = expectedMaxCounts[i];

            AgentReply reply = chatOrchestratorService.chat(message, userId);

            assertThat(reply.songs())
                .as("用户要求的数量应该被正确解析: %s", message)
                .hasSizeGreaterThanOrEqualTo(minCount)
                .hasSizeLessThanOrEqualTo(maxCount);
        }
    }

    /**
     * 测试场景 4：模糊追加（"再来点"）
     * 用户说"再来点"、"多来点"，应该保持上下文并增加数量
     */
    @Test
    void chat_withAmbiguousFollowUp_shouldKeepContext() {
        // Arrange
        Long userId = 1780807465532L;
        String firstMessage = "推荐跑步的音乐";

        // Act 1
        AgentReply firstReply = chatOrchestratorService.chat(firstMessage, userId);

        assertThat(firstReply.reply())
            .containsAnyOf("跑步", "运动");

        // Act 2 - 模糊追加
        String secondMessage = "再来点";
        AgentReply secondReply = chatOrchestratorService.chat(secondMessage, userId);

        // Assert 2 - 应该保持"跑步"主题
        assertThat(secondReply.reply())
            .as("模糊追加应该保持上下文")
            .containsAnyOf("跑步", "运动", "节奏");

        assertThat(secondReply.songs())
            .as("应该返回更多歌曲")
            .isNotEmpty();
    }
}
