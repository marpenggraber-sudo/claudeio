package org.example.service;

import org.example.dto.ChatResponse;
import org.example.dto.MusicSongDto;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class MusicAgentService {

    private static final Pattern INDEX_PATTERN = Pattern.compile(".*?(?:第)?(\\d+)(?:首|个|条).*?");

    private final MusicApiService musicApiService;

    public MusicAgentService(MusicApiService musicApiService) {
        this.musicApiService = musicApiService;
    }

    public ChatResponse chat(String message, Long userId) {
        musicApiService.saveConversation(userId, "user", message);

        if (message == null || message.isBlank()) {
            return new ChatResponse("请告诉我你想听什么歌。", List.of(), null);
        }

        if (isGreeting(message)) {
            musicApiService.saveConversation(userId, "assistant", "你好，有什么想听的歌吗？你可以直接说歌名、歌手，或者说‘推荐一些歌’。");
            return new ChatResponse("你好，有什么想听的歌吗？你可以直接说歌名、歌手，或者说‘推荐一些歌’。", List.of(), null);
        }

        if (isRecommendationIntent(message)) {
            List<MusicSongDto> songs = musicApiService.searchSongs(message);
            if (userId != null) {
                musicApiService.cacheRecommend(userId, songs);
            }
            musicApiService.saveConversation(userId, "assistant", "我找到了这些歌，你可以继续说‘播放第二首’。");
            return new ChatResponse("我找到了这些歌，你可以继续说‘播放第二首’。", songs, null);
        }

        if (isPlayIntent(message)) {
            List<MusicSongDto> songs = musicApiService.searchSongs(message);
            if (userId != null) {
                musicApiService.cacheRecommend(userId, songs);
            }
            musicApiService.saveConversation(userId, "assistant", "已根据你的描述整理出歌曲列表。");
            return new ChatResponse("已根据你的描述整理出歌曲列表。", songs, null);
        }

        musicApiService.saveConversation(userId, "assistant", "你可以直接说歌名、歌手，或者说‘推荐一些歌’。\n");
        return new ChatResponse("你可以直接说歌名、歌手，或者说‘推荐一些歌’。", List.of(), null);
    }

    private boolean isGreeting(String message) {
        String text = message.trim();
        return text.equals("你好") || text.equals("您好") || text.equalsIgnoreCase("hi") || text.equalsIgnoreCase("hello");
    }

    private boolean isRecommendationIntent(String message) {
        return message.contains("推荐") || message.contains("找歌") || message.contains("搜索") || message.contains("来点");
    }

    private boolean isPlayIntent(String message) {
        return message.contains("播放") || message.contains("放") || message.contains("听") || message.contains("来");
    }
}
