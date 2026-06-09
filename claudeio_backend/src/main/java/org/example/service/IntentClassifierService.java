package org.example.service;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.example.dto.IntentResult;
import org.example.dto.IntentType;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class IntentClassifierService {

    private static final Pattern INDEX_PATTERN = Pattern.compile(".*?(?:第)?(\\d+)(?:首|个|条).*?");

    private final ChatLanguageModel chatLanguageModel;

    public IntentClassifierService(ChatLanguageModel chatLanguageModel) {
        this.chatLanguageModel = chatLanguageModel;
    }

    public IntentResult classify(String message) {
        return classify(message, null);
    }

    public IntentResult classify(String message, String conversationHistory) {
        if (message == null || message.isBlank()) {
            return new IntentResult(IntentType.UNKNOWN, "", null);
        }

        String text = message.trim();

        // 明确的意图，不需要上下文
        if (isGreeting(text)) {
            return new IntentResult(IntentType.GREETING, text, null);
        }

        if (isPlayerControlIntent(text)) {
            return new IntentResult(IntentType.PLAYER_CONTROL, text, null);
        }

        if (isLogoutIntent(text)) {
            return new IntentResult(IntentType.LOGOUT, text, null);
        }

        if (isSwitchAccountIntent(text)) {
            return new IntentResult(IntentType.SWITCH_ACCOUNT, text, null);
        }

        if (isMusicMemoryIntent(text)) {
            return new IntentResult(IntentType.MUSIC_MEMORY, text, null);
        }

        if (isKnowledgeIntent(text)) {
            return new IntentResult(IntentType.KNOWLEDGE, text, null);
        }

        Matcher matcher = INDEX_PATTERN.matcher(text);
        if (matcher.matches() && isPlayIntent(text)) {
            return new IntentResult(IntentType.PLAY_BY_INDEX, text, Integer.parseInt(matcher.group(1)));
        }

        if (isSearchIntent(text)) {
            return new IntentResult(IntentType.SEARCH, text, null);
        }

        if (isPlayIntent(text)) {
            return new IntentResult(IntentType.PLAY_BY_KEYWORD, text, null);
        }

        // 对于简短或模糊的消息，使用 AI 结合上下文判断
        if (text.length() < 20 && conversationHistory != null && !conversationHistory.isBlank()) {
            return classifyWithContext(text, conversationHistory);
        }

        if (text.length() < 20) {
            return new IntentResult(IntentType.CHAT, text, null);
        }

        return new IntentResult(IntentType.UNKNOWN, text, null);
    }

    private IntentResult classifyWithContext(String message, String conversationHistory) {
        // 先检查是否是简单的肯定/否定回复
        if (isAffirmativeResponse(message)) {
            // 肯定回复 - 从对话历史中提取上一次提到的主题
            return new IntentResult(IntentType.CHAT, message, null);
        }

        if (isNegativeResponse(message)) {
            // 否定回复 - 也当作聊天处理
            return new IntentResult(IntentType.CHAT, message, null);
        }

        String prompt = """
            根据对话历史和用户当前消息，判断用户的意图。

            对话历史：
            %s

            用户当前消息：%s

            特别注意：
            - 如果用户只是说"好的"、"可以"、"行"等肯定回复，应该识别为 CHAT，不要当作搜索关键词
            - 如果用户说"不要"、"算了"、"换一个"等否定回复，应该识别为 CHAT

            请判断用户意图，只返回以下关键词之一：
            - SEARCH（用户想搜索/推荐歌曲，如回答"中文歌"、"周杰伦"等具体内容）
            - PLAY_BY_INDEX（用户想播放第几首，如"1"、"第2首"）
            - PLAY_BY_KEYWORD（用户想播放特定歌曲）
            - CHAT（普通聊天、肯定/否定回复）

            只返回关键词，不要其他内容。
            """.formatted(conversationHistory, message);

        String result = chatLanguageModel.generate(UserMessage.from(prompt))
            .content().text().trim().toUpperCase();

        return switch (result) {
            case "SEARCH" -> new IntentResult(IntentType.SEARCH, message, null);
            case "PLAY_BY_INDEX" -> {
                Matcher matcher = INDEX_PATTERN.matcher(message);
                if (matcher.matches()) {
                    yield new IntentResult(IntentType.PLAY_BY_INDEX, message, Integer.parseInt(matcher.group(1)));
                }
                // 如果是纯数字
                try {
                    int index = Integer.parseInt(message);
                    yield new IntentResult(IntentType.PLAY_BY_INDEX, message, index);
                } catch (NumberFormatException e) {
                    yield new IntentResult(IntentType.CHAT, message, null);
                }
            }
            case "PLAY_BY_KEYWORD" -> new IntentResult(IntentType.PLAY_BY_KEYWORD, message, null);
            default -> new IntentResult(IntentType.CHAT, message, null);
        };
    }

    /**
     * 判断是否是肯定性回复
     */
    private boolean isAffirmativeResponse(String text) {
        String lower = text.toLowerCase().trim();
        return lower.equals("好的") || lower.equals("好") ||
               lower.equals("可以") || lower.equals("行") ||
               lower.equals("嗯") || lower.equals("嗯嗯") ||
               lower.equals("ok") || lower.equals("okay") ||
               lower.equals("是的") || lower.equals("对") ||
               lower.equals("对的") || lower.equals("没问题") ||
               lower.equals("👌") || lower.equals("✅");
    }

    /**
     * 判断是否是否定性回复
     */
    private boolean isNegativeResponse(String text) {
        String lower = text.toLowerCase().trim();
        return lower.equals("不用了") || lower.equals("不用") ||
               lower.equals("算了") || lower.equals("不要") ||
               lower.equals("不") || lower.equals("换一个") ||
               lower.equals("不喜欢") || lower.equals("不想") ||
               lower.equals("no") || lower.equals("nope") ||
               lower.equals("❌");
    }

    private boolean isGreeting(String text) {
        return text.equals("你好") || text.equals("您好") || text.equalsIgnoreCase("hi") || text.equalsIgnoreCase("hello");
    }

    private boolean isSearchIntent(String text) {
        return text.contains("推荐") || text.contains("找歌") ||
               text.contains("搜索") || text.contains("来点") ||
               text.contains("想听") || text.contains("给我") ||
               text.contains("听歌") || text.contains("放歌") ||
               (text.endsWith("歌") && text.length() <= 10) ||
               text.matches(".*[中日英粤韩]文歌.*") ||
               text.matches(".*[中日英粤韩]语歌.*") ||
               text.contains("欧美歌") || text.contains("流行歌") ||
               text.contains("摇滚") || text.contains("民谣") ||
               text.contains("说唱") || text.contains("古风");
    }

    private boolean isPlayIntent(String text) {
        if (isSearchIntent(text)) {
            return false;
        }
        return text.contains("播放") || text.contains("放");
    }

    private boolean isLogoutIntent(String text) {
        String lower = text.toLowerCase();
        return text.contains("退出登录") || text.contains("登出") ||
               text.contains("退出账号") || text.contains("注销") ||
               lower.contains("logout") || lower.contains("log out") ||
               lower.contains("sign out");
    }

    private boolean isMusicMemoryIntent(String text) {
        return text.contains("记得我") || text.contains("我听过") ||
               text.contains("听歌历史") || text.contains("播放记录") ||
               text.contains("听歌记录") || text.contains("最近听") ||
               text.contains("常听") || text.contains("偏好") ||
               text.contains("我喜欢什么") || text.contains("喜欢的歌手") ||
               text.contains("我喜欢的歌") || text.contains("我的喜好");
    }

    private boolean isKnowledgeIntent(String text) {
        // 音乐知识问答特征：什么是、是谁、介绍、讲讲、知道吗
        return text.contains("什么是") || text.contains("是什么") ||
               text.contains("是谁") || text.contains("介绍") ||
               text.contains("讲讲") || text.contains("了解") ||
               text.contains("知道") && text.contains("吗") ||
               text.contains("告诉我") && (text.contains("关于") || text.contains("的")) ||
               text.matches(".*[周陈王李张]\\S{1,3}是.*") ||  // 如：周杰伦是谁
               text.matches(".*[R&BrockjazzJazz]{2,5}.*") && (text.contains("是") || text.contains("什么")) ||
               text.contains("风格") && !text.contains("推荐") ||
               text.contains("流派") || text.contains("音乐类型") ||
               text.contains("乐器") || text.contains("曲风");
    }

    private boolean isSwitchAccountIntent(String text) {
        String lower = text.toLowerCase();
        return text.contains("切换账号") || text.contains("换账号") ||
               text.contains("切换用户") || text.contains("换个账号") ||
               text.contains("登录其他账号") ||
               lower.contains("switch account") || lower.contains("change account") ||
               lower.contains("switch user");
    }

    private boolean isPlayerControlIntent(String text) {
        String lower = text.toLowerCase();
        return text.contains("暂停") || text.contains("停止") || lower.contains("pause") ||
               text.contains("继续") || lower.contains("resume") || lower.contains("play") ||
               text.contains("下一首") || text.contains("切歌") || lower.contains("next") ||
               text.contains("上一首") || lower.contains("previous") || lower.contains("prev") ||
               text.contains("音量") || text.contains("声音") || lower.contains("volume") ||
               text.contains("大声") || text.contains("小声") || text.contains("调高") || text.contains("调低");
    }
}
