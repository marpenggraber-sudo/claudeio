package org.example.service;

import org.example.dto.AgentReply;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Agent 上下文理解测试
 *
 * 测试场景：
 * 1. Agent: "今天还是听点流行音乐吗？"
 * 2. User: "好的"
 * 3. Agent 应该搜索"流行音乐"，而不是"好的"
 */
@SpringBootTest
public class AgentContextTest {

    @Autowired
    private ChatOrchestratorService chatService;

    private static final Long TEST_USER_ID = 13879884891L;

    /**
     * RED TEST 1: Agent 应该记住上一轮对话的主题
     */
    @Test
    void agent_shouldRememberPreviousTopic() {
        System.out.println("\n=== 测试 1: Agent 记住上一轮主题 ===");

        // 第一轮对话
        AgentReply firstReply = chatService.chat("推荐一些流行音乐", TEST_USER_ID);
        System.out.println("Agent 第一轮: " + firstReply.reply());

        // 第二轮对话 - 简单肯定回复
        AgentReply secondReply = chatService.chat("好的", TEST_USER_ID);
        System.out.println("Agent 第二轮: " + secondReply.reply());

        // 验证：第二轮不应该搜索"好的"
        assertThat(secondReply.reply())
            .as("Agent 不应该搜索'好的'这个关键词")
            .doesNotContain("搜索: 好的")
            .doesNotContain("未找到")
            .doesNotContain("没有找到相关");
    }

    /**
     * RED TEST 2: 识别肯定性回复（好的、可以、行等）
     */
    @Test
    void agent_shouldRecognizeAffirmativeResponses() {
        System.out.println("\n=== 测试 2: 识别肯定性回复 ===");

        String[] affirmativeResponses = {
            "好的",
            "可以",
            "行",
            "好",
            "嗯",
            "OK",
            "ok",
            "是的",
            "对",
            "没问题"
        };

        for (String response : affirmativeResponses) {
            // 第一轮：提问
            chatService.chat("推荐一些摇滚音乐", TEST_USER_ID);

            // 第二轮：肯定回复
            AgentReply agentReply = chatService.chat(response, TEST_USER_ID);
            String replyMessage = agentReply.reply();

            System.out.println("用户: " + response + " -> Agent: " +
                (replyMessage.length() > 50 ? replyMessage.substring(0, 50) + "..." : replyMessage));

            // 验证：不应该搜索这些肯定词
            assertThat(replyMessage)
                .as("肯定回复 '%s' 不应该被当作搜索关键词", response)
                .doesNotContain("搜索: " + response);
        }
    }

    /**
     * RED TEST 3: 识别否定性回复
     */
    @Test
    void agent_shouldRecognizeNegativeResponses() {
        System.out.println("\n=== 测试 3: 识别否定性回复 ===");

        String[] negativeResponses = {
            "不用了",
            "算了",
            "不要",
            "换一个",
            "不喜欢"
        };

        for (String response : negativeResponses) {
            // 第一轮：提问
            chatService.chat("推荐一些古典音乐", TEST_USER_ID);

            // 第二轮：否定回复
            AgentReply agentReply = chatService.chat(response, TEST_USER_ID);
            String replyMessage = agentReply.reply();

            System.out.println("用户: " + response + " -> Agent 应该识别为拒绝");

            // 验证：不应该搜索这些否定词
            assertThat(replyMessage)
                .as("否定回复 '%s' 不应该被当作搜索关键词", response)
                .doesNotContain("搜索: " + response);
        }
    }
}
