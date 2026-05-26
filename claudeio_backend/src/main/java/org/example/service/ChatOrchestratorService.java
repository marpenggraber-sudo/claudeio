package org.example.service;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.example.dto.AgentReply;
import org.example.dto.IntentResult;
import org.example.dto.IntentType;
import org.example.dto.KnowledgeDocument;
import org.example.dto.MusicSongDto;
import org.example.entity.AgentConversation;
import org.example.entity.PlayHistory;
import org.example.repo.AgentConversationRepository;
import org.example.tools.MusicTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ChatOrchestratorService {

    private static final Logger log = LoggerFactory.getLogger(ChatOrchestratorService.class);

    private final IntentClassifierService intentClassifierService;
    private final PromptTemplateService promptTemplateService;
    private final MusicTools musicTools;
    private final ChatLanguageModel chatLanguageModel;
    private final AgentConversationRepository agentConversationRepository;
    private final MusicApiService musicApiService;
    private final UserPreferenceService userPreferenceService;
    private final PlayHistoryService playHistoryService;
    private final RagService ragService;

    public ChatOrchestratorService(IntentClassifierService intentClassifierService,
                                   PromptTemplateService promptTemplateService,
                                   MusicTools musicTools,
                                   ChatLanguageModel chatLanguageModel,
                                   AgentConversationRepository agentConversationRepository,
                                   MusicApiService musicApiService,
                                   UserPreferenceService userPreferenceService,
                                   PlayHistoryService playHistoryService,
                                   RagService ragService) {
        this.intentClassifierService = intentClassifierService;
        this.promptTemplateService = promptTemplateService;
        this.musicTools = musicTools;
        this.chatLanguageModel = chatLanguageModel;
        this.agentConversationRepository = agentConversationRepository;
        this.musicApiService = musicApiService;
        this.userPreferenceService = userPreferenceService;
        this.playHistoryService = playHistoryService;
        this.ragService = ragService;
    }

    public AgentReply chat(String message, Long userId) {
        // 第1步：保存用户消息
        musicApiService.saveConversation(userId, "user", message);

        // 第2步：加载对话历史（最近10条）
        String conversationHistory = loadConversationHistory(userId);

        // 第3步：分类意图（传入对话历史以支持上下文理解）
        IntentResult intent = intentClassifierService.classify(message, conversationHistory);
        log.info("[AgentIntent] userId={}, intent={}, message={}", userId, intent.type(), message);
        if (musicApiService.getPendingSwitchAccount(userId).isPresent()) {
            intent = new IntentResult(IntentType.SWITCH_ACCOUNT, message, null);
            log.info("[AgentIntent] userId={}, pendingSwitchAccount=true, forceIntent={}", userId, intent.type());
        }
        String systemPrompt = promptTemplateService.buildSystemPrompt(intent);

        // 第4步：使用RAG检索相关知识
        List<KnowledgeDocument> relevantKnowledge = ragService.retrieveRelevantKnowledge(message, 3);
        String knowledgeContext = ragService.formatKnowledgeContext(relevantKnowledge);

        if (!relevantKnowledge.isEmpty()) {
            log.info("[RAG] 为用户消息检索到 {} 条相关知识", relevantKnowledge.size());
        }

        // 第5步：将历史上下文和知识库上下文添加到系统提示中
        String promptWithHistory = systemPrompt + knowledgeContext + "\n\n对话历史：\n" + conversationHistory + "\n\n当前用户消息：" + message;

        // 第6步：处理不同意图
        AgentReply reply = switch (intent.type()) {
            case GREETING -> handleGreeting(promptWithHistory, userId);
            case SEARCH -> handleSearch(intent, message, userId, promptWithHistory);
            case PLAY_BY_INDEX -> handlePlayByIndex(intent, userId, promptWithHistory);
            case PLAY_BY_KEYWORD -> handlePlayByKeyword(message, userId, promptWithHistory);
            case MUSIC_MEMORY -> handleMusicMemory(message, userId, promptWithHistory);
            case LOGOUT -> handleLogout(userId, promptWithHistory);
            case SWITCH_ACCOUNT -> handleSwitchAccount(message, userId, promptWithHistory);
            case PLAYER_CONTROL -> handlePlayerControl(message, userId, promptWithHistory);
            case CHAT, UNKNOWN -> handleChat(promptWithHistory, userId);
        };

        // 第7步：保存 AI 回复
        musicApiService.saveConversation(userId, "assistant", reply.reply());

        return reply;
    }

    private String loadConversationHistory(Long userId) {
        // 第1步：尝试从 Redis 缓存获取
        List<Map<String, String>> cachedHistory = musicApiService.getCachedConversationHistory(userId, 10);

        if (!cachedHistory.isEmpty()) {
            return cachedHistory.stream()
                .map(msg -> msg.get("role") + ": " + msg.get("content"))
                .collect(Collectors.joining("\n"));
        }

        // 第2步：如果 Redis 没有，从 MySQL 加载
        List<AgentConversation> history = agentConversationRepository
            .findTop10ByUser_MusicUserIdOrderByCreatedAtDesc(userId);

        if (history.isEmpty()) {
            return "（这是新对话）";
        }

        // 反转顺序，让最早的在前面
        return history.reversed().stream()
            .map(c -> c.getRole() + ": " + c.getContent())
            .collect(Collectors.joining("\n"));
    }

    private AgentReply handleGreeting(String prompt, Long userId) {
        String reply = chatLanguageModel.generate(UserMessage.from(prompt)).content().text();
        return new AgentReply(reply, List.of(), null, null, null, null);
    }

    private AgentReply handleChat(String prompt, Long userId) {
        String reply = chatLanguageModel.generate(UserMessage.from(prompt)).content().text();
        return new AgentReply(reply, List.of(), null, null, null, null);
    }

    private AgentReply handleMusicMemory(String message, Long userId, String systemPrompt) {
        log.info("[MusicMemory] 开始查询用户听歌历史: userId={}, message={}", userId, message);
        List<PlayHistory> recentHistory = playHistoryService.getRecentHistory(userId, 30);
        List<String> topArtists = userPreferenceService.getTopArtists(userId, 5);
        log.info("[MusicMemory] 数据库查询结果: userId={}, recentHistoryCount={}, topArtists={}", userId, recentHistory.size(), topArtists);

        if (recentHistory.isEmpty() && topArtists.isEmpty()) {
            log.info("[MusicMemory] 用户没有听歌历史: userId={}", userId);
            return new AgentReply("我这里还没有你的听歌记录。", List.of(), null, null, null, null);
        }

        String recentSongs = recentHistory.stream()
                .limit(5)
                .map(history -> history.getSongName() + " - " + history.getArtist() + (Boolean.TRUE.equals(history.getCompleted()) ? "（完整播放）" : ""))
                .collect(Collectors.joining("\n"));

        long completedCount = recentHistory.stream()
                .filter(PlayHistory::getCompleted)
                .count();

        String preferenceContext = "最近30天播放次数：" + recentHistory.size() +
                "\n最近30天完整播放次数：" + completedCount +
                "\n常听歌手：" + (topArtists.isEmpty() ? "暂无" : String.join("、", topArtists)) +
                "\n最近听过的歌曲：\n" + (recentSongs.isBlank() ? "暂无" : recentSongs);

        log.info("[MusicMemory] 注入给 agent 的用户听歌历史: userId={}, context={}", userId, preferenceContext.replace("\n", " | "));

        // 提取最近听过的歌曲列表（用于返回给前端）
        List<MusicSongDto> recentSongsList = extractRecentSongs(recentHistory, 5);
        log.info("[MusicMemory] 提取最近听过的歌曲: userId={}, songsCount={}", userId, recentSongsList.size());

        String reply = chatLanguageModel.generate(UserMessage.from(
                systemPrompt + "\n用户：" + message +
                "\n\n数据库里的听歌记忆：\n" + preferenceContext +
                "\n\n重要规则：" +
                "\n1. 只基于上面的数据库记录回答，不要编造" +
                "\n2. 可以提到最近听过的歌曲，系统会自动返回这些歌曲供用户播放" +
                "\n3. 如果用户问最近听过什么，就重点说最近听过的歌曲" +
                "\n4. 如果用户问喜欢什么，就重点说常听歌手和偏好" +
                "\n5. 回复简短自然，不要使用 emoji 表情符号"))
                .content().text();

        log.info("[MusicMemory] agent 基于听歌历史生成回复: userId={}, reply={}, songsCount={}", userId, reply, recentSongsList.size());

        // 缓存歌曲列表供前端使用
        if (!recentSongsList.isEmpty()) {
            musicApiService.cacheRecommend(userId, recentSongsList);
        }

        return new AgentReply(reply, recentSongsList, null, null, null, null);
    }

    /**
     * 从播放历史中提取最近听过的歌曲
     */
    private List<MusicSongDto> extractRecentSongs(List<PlayHistory> history, int limit) {
        return history.stream()
                .limit(limit)
                .map(h -> new MusicSongDto(h.getSongId(), h.getSongName(), h.getArtist()))
                .distinct()  // 去重
                .collect(Collectors.toList());
    }

    private AgentReply handleSearch(IntentResult intent, String message, Long userId, String systemPrompt) {
        log.info("[Recommend] 开始处理搜索/推荐请求: userId={}, message={}", userId, message);

        // 第1步：检测用户是否有可用于推荐的播放历史
        boolean hasPlayHistory = playHistoryService.hasRecommendationHistory(userId);
        log.info("[Recommend] 用户是否有可用于推荐的听歌历史: userId={}, hasPlayHistory={}", userId, hasPlayHistory);

        // 第2步：检测是否为推荐请求（没有明确歌名或歌手）
        String detectPrompt = "用户说：" + message +
            "\n\n判断用户是否在请求推荐音乐（没有明确指定歌名或歌手）。" +
            "\n如果用户说’我想听歌’、’推荐一些歌’、’来点音乐’等，返回’recommend’" +
            "\n如果用户明确说了歌名或歌手（如’周杰伦的歌’、’晴天’），返回’search’" +
            "\n只返回’recommend’或’search’，不要其他内容。";

        String requestType = chatLanguageModel.generate(UserMessage.from(detectPrompt))
            .content().text().trim().toLowerCase();

        log.info("[Recommend] 请求类型: userId={}, requestType={}", userId, requestType);

        // 第3步：让 AI 提取搜索关键词和数量
        String extractPrompt = "用户说：" + message +
            "\n\n请从用户输入中提取音乐搜索关键词和数量。" +
            "\n如果用户没有明确说明歌名或歌手，返回’generic’" +
            "\n如果用户明确说了歌名或歌手，提取具体的关键词" +
            "\n\n如果用户明确要求数量（如’推荐10首’），返回格式：关键词|数量" +
            "\n如果没有明确数量，只返回关键词。";

        String extracted = chatLanguageModel.generate(UserMessage.from(extractPrompt))
            .content().text().trim();

        log.info("[Recommend] AI 提取搜索关键词: userId={}, extracted={}", userId, extracted);

        // 第4步：解析关键词和数量
        String keywords;
        int limit = 5; // 默认5首
        if (extracted.contains("|")) {
            String[] parts = extracted.split("\\|");
            keywords = parts[0].trim();
            try {
                limit = Integer.parseInt(parts[1].trim());
                limit = Math.min(limit, 20); // 最多20首
            } catch (NumberFormatException e) {
                log.warn("无法解析数量，使用默认值5");
            }
        } else {
            keywords = extracted;
        }

        String recommendationConstraint = extractRecommendationConstraint(message);
        log.info("[Recommend] 用户本次推荐约束: userId={}, constraint={}", userId, recommendationConstraint.isBlank() ? "无" : recommendationConstraint);

        // 第5步：如果是推荐请求且没有明确关键词，使用个性化推荐
        String userPreferenceContext = "";
        if ("recommend".equals(requestType) && "generic".equalsIgnoreCase(keywords)) {
            if (!recommendationConstraint.isBlank()) {
                keywords = recommendationConstraint;
                log.info("[Recommend] 使用用户当前约束，跳过历史偏好覆盖: userId={}, keywords={}", userId, keywords);
            } else if (hasPlayHistory) {
                List<String> recommendKeywords = userPreferenceService.getRecommendationKeywords(userId);
                if (!recommendKeywords.isEmpty()) {
                    keywords = recommendKeywords.get(0);
                    log.info("[Recommend] 使用个性化推荐关键词: userId={}, keywords={}, allKeywords={}", userId, keywords, recommendKeywords);

                    List<String> topArtists = userPreferenceService.getTopArtists(userId, 3);
                    userPreferenceContext = "用户最喜欢的艺术家：" + String.join("、", topArtists);
                    log.info("[Recommend] 注入用户偏好上下文: userId={}, context={}", userId, userPreferenceContext);
                } else {
                    keywords = "热门音乐";
                    log.info("[Recommend] 无法获取推荐关键词，使用默认关键词: userId={}, keywords={}", userId, keywords);
                }
            } else {
                keywords = "热门音乐";
                log.info("[Recommend] 用户无播放历史，使用默认关键词: userId={}, keywords={}", userId, keywords);
            }
        }

        log.info("[Recommend] 最终搜索关键词: userId={}, keywords={}, limit={}", userId, keywords, limit);

        // 第3步：用提取的关键词搜索
        List<MusicSongDto> songs = musicTools.searchSongs(keywords, userId);
        log.info("[Recommend] 搜索完成: userId={}, keywords={}, resultCount={}", userId, keywords, songs.size());

        // 第4步：检查是否搜索失败（API 错误）
        if (songs.isEmpty()) {
            String reply = chatLanguageModel.generate(UserMessage.from(
                systemPrompt + "\n用户：" + message +
                "\n\n搜索关键词：" + keywords +
                "\n搜索结果：未找到歌曲或音乐服务暂时不可用" +
                "\n\n请友好地告知用户暂时无法搜索歌曲，可能是网络问题或服务暂时不可用，建议稍后再试。"))
                .content().text();
            return new AgentReply(reply, List.of(), null, null, null, null);
        }

        // 第5步：限制返回数量
        List<MusicSongDto> limitedSongs = songs.stream().limit(limit).toList();
        musicApiService.cacheRecommend(userId, limitedSongs);

        // 第6步：构建简洁的歌曲列表文本
        String songsList = limitedSongs.isEmpty() ? "未找到相关歌曲" :
            limitedSongs.stream()
                .map(s -> s.getName() + " - " + s.getArtist())
                .collect(Collectors.joining("\n"));

        log.info("[Recommend] 返回推荐结果: userId={}, resultCount={}", userId, limitedSongs.size());

        // 第7步：生成简洁的回复
        String replyPrompt = systemPrompt + "\n用户：" + message +
            "\n\n搜索关键词：" + keywords +
            "\n找到的歌曲数量：" + limitedSongs.size();

        if (!userPreferenceContext.isEmpty()) {
            replyPrompt += "\n\n" + userPreferenceContext;
        }

        if (!recommendationConstraint.isBlank()) {
            replyPrompt += "\n\n用户本次明确要求：" + recommendationConstraint;
        }

        replyPrompt += "\n\n重要规则：" +
            "\n1. 只回复一句话，不要多句" +
            "\n2. 绝对不要列举歌名，歌单已经返回给前端显示" +
            "\n3. 用类似’已经为您准备好歌单了’、’歌单已整理好’这样的表达" +
            "\n4. 如果用户本次明确要求了语言或类型，回复必须符合这个要求" +
            "\n5. 不要使用 emoji 表情符号" +
            "\n6. 回复要自然、口语化";

        String reply = chatLanguageModel.generate(UserMessage.from(replyPrompt))
                .content().text();

        log.info("[Recommend] agent 推荐回复: userId={}, reply={}", userId, reply);

        return new AgentReply(reply, limitedSongs, null, null, null, null);
    }

    private AgentReply handlePlayByIndex(IntentResult intent, Long userId, String systemPrompt) {
        Long songId = musicTools.getSongFromCache(intent.index(), userId);
        if (songId == null) {
            return new AgentReply("缓存里没有你要的那一首，请先让我搜索一组歌曲。", List.of(), null, null, null, null);
        }
        String reply = chatLanguageModel.generate(UserMessage.from(
            systemPrompt + "\n用户：播放第" + intent.index() + "首。" +
            "\n\n重要规则：只回复一句话，简短告知正在播放。"))
                .content().text();
        return new AgentReply(reply, List.of(new MusicSongDto(songId, "缓存歌曲", "Unknown")), songId, null, null, null);
    }

    private AgentReply handlePlayByKeyword(String message, Long userId, String systemPrompt) {

        // 第1步：让 AI 提取搜索关键词和数量
        String extractPrompt = "用户说：" + message +
            "\n\n请从用户输入中提取音乐搜索关键词和数量。" +
            "\n如果用户说'播放XXX'，提取歌名或歌手名。" +
            "\n如果用户明确要求数量（如'播放10首周杰伦的歌'），返回格式：关键词|数量" +
            "\n如果没有明确数量，只返回关键词。";

        String extracted = chatLanguageModel.generate(UserMessage.from(extractPrompt))
            .content().text().trim();

        // 第2步：解析关键词和数量
        String keywords;
        int limit = 5; // 默认5首
        if (extracted.contains("|")) {
            String[] parts = extracted.split("\\|");
            keywords = parts[0].trim();
            try {
                limit = Integer.parseInt(parts[1].trim());
                limit = Math.min(limit, 20); // 最多20首
            } catch (NumberFormatException e) {
                log.warn("无法解析数量，使用默认值5");
            }
        } else {
            keywords = extracted;
        }

        log.info("[PlayByKeyword] 搜索关键词: userId={}, keywords={}, limit={}", userId, keywords, limit);

        // 第3步：用提取的关键词搜索
        List<MusicSongDto> songs = musicTools.searchSongs(keywords, userId);


        // 第4步：检查是否搜索失败（API 错误）
        if (songs.isEmpty()) {
            String reply = chatLanguageModel.generate(UserMessage.from(
                systemPrompt + "\n用户：" + message +
                "\n\n搜索关键词：" + keywords +
                "\n搜索结果：未找到歌曲或音乐服务暂时不可用" +
                "\n\n请友好地告知用户暂时无法搜索歌曲，可能是网络问题或服务暂时不可用，建议稍后再试。"))
                .content().text();
            return new AgentReply(reply, List.of(), null, null, null, null);
        }

        // 第5步：限制返回数量
        List<MusicSongDto> limitedSongs = songs.stream().limit(limit).toList();

        // 第6步：构建简洁的歌曲列表文本
        String songsList = limitedSongs.isEmpty() ? "未找到相关歌曲" :
            limitedSongs.stream()
                .map(s -> s.getName() + " - " + s.getArtist())
                .collect(Collectors.joining("\n"));



        // 第7步：生成简洁的回复
        String reply = chatLanguageModel.generate(UserMessage.from(
            systemPrompt + "\n用户：" + message +
            "\n\n搜索关键词：" + keywords +
            "\n找到的歌曲数量：" + limitedSongs.size() +
            "\n\n重要规则：" +
            "\n1. 只回复一句话，不要多句" +
            "\n2. 绝对不要列举歌名，歌单已经返回给前端显示" +
            "\n3. 用类似'已经为您准备好歌单了'、'歌单已整理好'这样的表达" +
            "\n4. 不要使用 emoji 表情符号" +
            "\n5. 回复要自然、口语化"))
                .content().text();



        return new AgentReply(reply, limitedSongs, null, null, null, null);
    }

    private String extractRecommendationConstraint(String message) {
        String text = message == null ? "" : message.trim();
        String language = "";
        if (text.contains("中文") || text.contains("华语") || text.contains("國語") || text.contains("国语") || text.contains("普通话")) {
            language = "中文歌";
        } else if (text.contains("粤语")) {
            language = "粤语歌";
        } else if (text.contains("日语") || text.contains("日文")) {
            language = "日语歌";
        } else if (text.contains("英文") || text.contains("欧美")) {
            language = "英文歌";
        }

        String mood = "";
        if (text.contains("心情不好") || text.contains("伤感") || text.contains("难过") || text.contains("emo")) {
            mood = "伤感";
        } else if (text.contains("开心") || text.contains("轻松") || text.contains("治愈")) {
            mood = "治愈";
        }

        if (language.isBlank() && mood.isBlank()) {
            return "";
        }

        if (language.isBlank()) {
            return mood + "歌曲";
        }

        return mood.isBlank() ? language : mood + language;
    }

    private AgentReply handleLogout(Long userId, String systemPrompt) {
        String result = musicTools.logout(userId);
        String reply = chatLanguageModel.generate(UserMessage.from(
            systemPrompt + "\n用户要求退出登录。\n请简短告知用户已成功退出。"))
            .content().text();
        return new AgentReply(reply, List.of(), null, "logout", null, null);
    }

    private AgentReply handleSwitchAccount(String message, Long userId, String systemPrompt) {

        String extractPrompt = "用户说：" + message +
            "\n\n请从用户输入中提取切换账号需要的账号和密码。" +
            "\n如果用户只说想切换账号但没有提供账号密码，返回'need_input'。" +
            "\n如果只提供了账号没有密码，返回格式：account:账号名,password:missing" +
            "\n如果只提供了密码没有账号，返回格式：account:missing,password:密码" +
            "\n如果只是普通聊天，不像账号密码信息，返回'need_input'。" +
            "\n如果已提供完整信息，返回格式：account:账号名,password:密码" +
            "\n只返回提取结果，不要其他内容。";

        String extracted = chatLanguageModel.generate(UserMessage.from(extractPrompt))
            .content().text().trim();

        try {
            String pendingAccount = musicApiService.getPendingSwitchAccount(userId).orElse(null);
            String account = extractField(extracted, "account");
            String password = extractField(extracted, "password");

            if (isMissing(account) && pendingAccount != null && !containsAccountHint(message)) {
                account = pendingAccount;
            }

            if (isMissing(account)) {
                return new AgentReply("请先告诉我你要切换到哪个账号。", List.of(), null, null, null, null);
            }

            String currentAccount = musicApiService.getCurrentUserAccount(userId);
            if (currentAccount != null && currentAccount.equals(account)) {
                musicApiService.clearPendingSwitchAccount(userId);
                return new AgentReply("你当前已经是这个账号，不需要切换。", List.of(), null, null, null, null);
            }

            if (isMissing(password)) {
                if (!containsAccountHint(message) && pendingAccount != null) {
                    password = extractPasswordFromMessage(message);
                }
            }

            if (isMissing(password)) {
                musicApiService.cachePendingSwitchAccount(userId, account);
                return new AgentReply("请把这个账号的密码发给我。", List.of(), null, null, null, null);
            }

            String result = musicTools.switchAccount(account, password, userId);

            if (result.startsWith("error:")) {
                String error = result.substring(6);
                musicApiService.cachePendingSwitchAccount(userId, account);
                return new AgentReply("切换失败：" + error + "。请重新发一次密码。", List.of(), null, null, null, null);
            }

            String[] parts = result.substring(8).split(":", 2);
            Long newUserId = Long.parseLong(parts[0]);
            String newNickname = parts[1];
            musicApiService.clearPendingSwitchAccount(userId);

            return new AgentReply("已经切换到 " + newNickname + "。", List.of(), null, "switch_account", newUserId, newNickname);
        } catch (Exception e) {
            log.error("解析账号密码失败", e);
            return new AgentReply("请按“账号 + 密码”的格式重新发我。", List.of(), null, null, null, null);
        }
    }

    private String extractField(String extracted, String field) {
        String prefix = field + ":";
        for (String part : extracted.split(",")) {
            String trimmed = part.trim();
            if (trimmed.startsWith(prefix)) {
                return trimmed.substring(prefix.length()).trim();
            }
        }
        return null;
    }

    private boolean containsAccountHint(String message) {
        return message.contains("账号") || message.contains("帐号") || message.contains("账户") || message.contains("用户名");
    }

    private String extractPasswordFromMessage(String message) {
        String text = message.trim();
        if (text.startsWith("密码")) {
            text = text.substring(2).trim();
        }
        if (text.startsWith("：") || text.startsWith(":")) {
            text = text.substring(1).trim();
        }
        if (text.startsWith("密码")) {
            text = text.substring(2).trim();
        }
        return text;
    }

    private boolean isMissing(String value) {
        return value == null || value.isBlank() || "missing".equalsIgnoreCase(value) || "need_input".equalsIgnoreCase(value);
    }

    public AgentReply generateGreeting(Long userId) {
        // 第1步：获取用户最近的聊天历史（最多10条）
        List<AgentConversation> history = agentConversationRepository
            .findTop10ByUser_MusicUserIdOrderByCreatedAtDesc(userId);

        // 第2步：分析用户偏好
        String historyText = history.isEmpty() ? "这是新用户，没有历史记录" :
            history.stream()
                .map(c -> c.getRole() + ": " + c.getContent())
                .collect(Collectors.joining("\n"));

        // 第3步：让 AI 生成个性化问候
        String greetingPrompt = "你是一个专业的音乐助手。\n\n" +
            "用户的聊天历史：\n" + historyText + "\n\n" +
            "请根据用户的历史偏好，生成一个友好的问候语。" +
            "如果是新用户，询问他们今天的心情或想听什么类型的音乐。" +
            "\n\n重要规则：" +
            "\n1. 只回复一句话" +
            "\n2. 回复要简短、自然、友好" +
            "\n3. 不要使用 emoji 表情符号";

        String greeting = chatLanguageModel.generate(UserMessage.from(greetingPrompt))
            .content().text();

        // 第4步：根据历史提取推荐关键词
        String recommendPrompt = "根据用户的聊天历史：\n" + historyText + "\n\n" +
            "提取用户可能喜欢的音乐搜索关键词。" +
            "如果是新用户，返回'热门音乐'。" +
            "只返回关键词，不要其他内容。";

        String keywords = chatLanguageModel.generate(UserMessage.from(recommendPrompt))
            .content().text().trim();

        // 第5步：搜索推荐歌曲，限制为5首
        List<MusicSongDto> songs = musicTools.searchSongs(keywords, userId);
        List<MusicSongDto> limitedSongs = songs.stream().limit(5).toList();

        // 第6步：保存对话
        musicApiService.saveConversation(userId, "assistant", greeting);

        return new AgentReply(greeting, limitedSongs, null, null, null, null);
    }

    private AgentReply handlePlayerControl(String message, Long userId, String systemPrompt) {
        // 使用 AI 提取控制指令
        String extractPrompt = """
            用户说：%s

            请判断用户想要执行的播放器操作，只返回以下关键词之一：
            - pause（暂停/停止）
            - resume（继续播放/播放）
            - next（下一首/切歌）
            - previous（上一首）
            - volume:数字（调整音量，如 volume:50、volume:80）

            只返回操作关键词，不要其他内容。
            """.formatted(message);

        String command = chatLanguageModel.generate(UserMessage.from(extractPrompt))
            .content().text().trim().toLowerCase();

        String action = null;
        String replyText = "";

        if (command.equals("pause")) {
            action = musicTools.pauseMusic(userId);
            replyText = "已暂停播放。";
        } else if (command.equals("resume")) {
            action = musicTools.resumeMusic(userId);
            replyText = "继续播放。";
        } else if (command.equals("next")) {
            action = musicTools.playNext(userId);
            replyText = "正在播放下一首。";
        } else if (command.equals("previous")) {
            action = musicTools.playPrevious(userId);
            replyText = "正在播放上一首。";
        } else if (command.startsWith("volume:")) {
            try {
                int volume = Integer.parseInt(command.substring(7));
                action = musicTools.setVolume(volume, userId);
                if (action.startsWith("error:")) {
                    replyText = action.substring(6);
                    action = null;
                } else {
                    replyText = "音量已调整到 " + volume + "。";
                }
            } catch (NumberFormatException e) {
                replyText = "抱歉，我没理解你想调整到多少音量。";
            }
        } else {
            replyText = "抱歉，我没理解你的操作。你可以说暂停、下一首、音量调到50等。";
        }

        return new AgentReply(replyText, List.of(), null, action, null, null);
    }
}
