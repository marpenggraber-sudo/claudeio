package org.example.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.dto.MusicSongDto;
import org.example.entity.AgentConversation;
import org.example.entity.RecommendCacheLog;
import org.example.entity.UserAccount;
import org.example.entity.UserCookie;
import org.example.repo.AgentConversationRepository;
import org.example.repo.RecommendCacheLogRepository;
import org.example.repo.UserAccountRepository;
import org.example.repo.UserCookieRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class MusicApiService {

    private static final Logger log = LoggerFactory.getLogger(MusicApiService.class);

    private final RestTemplate restTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final UserAccountRepository userAccountRepository;
    private final UserCookieRepository userCookieRepository;
    private final RecommendCacheLogRepository recommendCacheLogRepository;
    private final AgentConversationRepository agentConversationRepository;
    private final String baseUrl;

    public MusicApiService(RestTemplate restTemplate,
                           RedisTemplate<String, Object> redisTemplate,
                           ObjectMapper objectMapper,
                           UserAccountRepository userAccountRepository,
                           UserCookieRepository userCookieRepository,
                           RecommendCacheLogRepository recommendCacheLogRepository,
                           AgentConversationRepository agentConversationRepository,
                           @Value("${music.api.base-url:http://localhost:3000}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.userAccountRepository = userAccountRepository;
        this.userCookieRepository = userCookieRepository;
        this.recommendCacheLogRepository = recommendCacheLogRepository;
        this.agentConversationRepository = agentConversationRepository;
        this.baseUrl = baseUrl;
    }

    public Long verifyAndStoreCookie(String cookie) {
        // 生成一个随机的 userId（基于时间戳）
        Long userId = System.currentTimeMillis();

        log.info("Storing cookie without verification, generated userId={}", userId);

        // 直接存储到 Redis
        redisTemplate.opsForValue().set(authKey(userId), cookie, Duration.ofDays(30));

        // 保存到数据库
        UserAccount user = userAccountRepository.findByMusicUserId(userId).orElseGet(UserAccount::new);
        user.setMusicUserId(userId);
        user.setNickname("User_" + userId);
        user = userAccountRepository.save(user);

        UserCookie userCookie = new UserCookie();
        userCookie.setUser(user);
        userCookie.setCookieCiphertext(cookie);
        userCookie.setExpiresAt(LocalDateTime.now().plusDays(30));
        userCookieRepository.save(userCookie);

        log.info("Cookie stored, userId={} saved to redis/mysql", userId);
        return userId;
    }

    public List<MusicSongDto> searchSongs(String keywords) {
        try {
            JsonNode node = restTemplate.getForObject(baseUrl + "/search?keywords={keywords}&type=1", JsonNode.class, keywords);
            JsonNode songsNode = node == null ? null : node.path("result").path("songs");
            List<MusicSongDto> songs = new ArrayList<>();
            if (songsNode != null && songsNode.isArray()) {
                songsNode.forEach(song -> {
                    JsonNode artistsNode = song.path("artists");
                    if (artistsNode.isMissingNode()) {
                        artistsNode = song.path("ar");
                    }

                    String artist = Optional.ofNullable(artistsNode)
                            .filter(JsonNode::isArray)
                            .map(arr -> {
                                List<String> names = new ArrayList<>();
                                arr.forEach(a -> names.add(a.path("name").asText("Unknown")));
                                return String.join("/", names);
                            })
                            .orElse("Unknown");
                    songs.add(new MusicSongDto(song.path("id").asLong(), song.path("name").asText("Unknown"), artist));
                });
            }
            return songs;
        } catch (Exception e) {
            log.error("搜索歌曲失败，关键词: {}, 错误: {}", keywords, e.getMessage(), e);
            return List.of();
        }
    }

    public String getPlayUrl(Long songId, String cookie) {
        try {
            String formattedCookie = cookie;
            if (!cookie.contains("MUSIC_U=")) {
                formattedCookie = "MUSIC_U=" + cookie;
            }

            String url = UriComponentsBuilder.fromHttpUrl(baseUrl)
                    .path("/song/url/v1")
                    .queryParam("id", songId)
                    .queryParam("level", "lossless")
                    .queryParam("cookie", formattedCookie)
                    .toUriString();

            JsonNode node = restTemplate.getForObject(url, JsonNode.class);
            JsonNode data = node == null ? null : node.path("data");

            if (data != null && data.isArray() && !data.isEmpty()) {
                JsonNode firstItem = data.get(0);
                return firstItem.path("url").asText(null);
            }

            return null;
        } catch (Exception e) {
            log.error("获取播放链接失败，songId: {}, 错误: {}", songId, e.getMessage(), e);
            return null;
        }
    }

    public Map<String, Object> getLyric(Long songId) {
        String url = baseUrl + "/lyric/new?id=" + songId;

        try {
            JsonNode response = restTemplate.getForObject(url, JsonNode.class);

            if (response != null && response.path("code").asInt() == 200) {
                Map<String, Object> result = new HashMap<>();

                // 获取逐字歌词
                JsonNode yrc = response.path("yrc");
                if (!yrc.isMissingNode() && !yrc.isNull()) {
                    result.put("yrc", Map.of(
                        "version", yrc.path("version").asInt(0),
                        "lyric", yrc.path("lyric").asText("")
                    ));
                }

                // 获取普通歌词（作为备用）
                JsonNode lrc = response.path("lrc");
                if (!lrc.isMissingNode() && !lrc.isNull()) {
                    result.put("lrc", Map.of(
                        "version", lrc.path("version").asInt(0),
                        "lyric", lrc.path("lyric").asText("")
                    ));
                }

                return result;
            }

            log.warn("Failed to get lyric for songId={}, response code: {}",
                songId, response != null ? response.path("code").asInt() : "null");
        } catch (Exception e) {
            log.error("Error fetching lyric for songId={}", songId, e);
        }

        return Map.of();
    }

    public String getAuthCookie(Long userId) {
        Object value = redisTemplate.opsForValue().get(authKey(userId));
        if (value != null) {
            return value.toString();
        }
        return userCookieRepository.findTopByUser_IdOrderByUpdatedAtDesc(userId)
                .map(UserCookie::getCookieCiphertext)
                .orElse(null);
    }

    public void cachePendingSwitchAccount(Long userId, String account) {
        redisTemplate.opsForValue().set(pendingSwitchKey(userId), account, Duration.ofMinutes(10));
    }

    public Optional<String> getPendingSwitchAccount(Long userId) {
        Object value = redisTemplate.opsForValue().get(pendingSwitchKey(userId));
        return value == null ? Optional.empty() : Optional.of(value.toString());
    }

    public void clearPendingSwitchAccount(Long userId) {
        redisTemplate.delete(pendingSwitchKey(userId));
    }

    public String getCurrentUserAccount(Long userId) {
        return userAccountRepository.findByMusicUserId(userId)
                .map(UserAccount::getAccount)
                .orElse(null);
    }

    public void cacheRecommend(Long userId, List<MusicSongDto> songs) {
        redisTemplate.opsForValue().set(recommendKey(userId), songs, Duration.ofMinutes(30));
        UserAccount user = userAccountRepository.findByMusicUserId(userId).orElse(null);
        if (user != null) {
            RecommendCacheLog logEntity = new RecommendCacheLog();
            logEntity.setUser(user);
            logEntity.setQueryText("recommend");
            logEntity.setSongsJson(writeJson(songs));
            recommendCacheLogRepository.save(logEntity);
        }
    }

    @SuppressWarnings("unchecked")
    public List<MusicSongDto> getCachedRecommend(Long userId) {
        Object value = redisTemplate.opsForValue().get(recommendKey(userId));
        if (value instanceof List<?> list) {
            return list.stream().map(item -> objectMapper.convertValue(item, MusicSongDto.class)).toList();
        }
        return List.of();
    }

    public void saveConversation(Long userId, String role, String content) {
        if (userId == null) {
            return;
        }
        UserAccount user = userAccountRepository.findByMusicUserId(userId).orElse(null);
        if (user == null) {
            return;
        }

        // 保存到 MySQL
        AgentConversation conversation = new AgentConversation();
        conversation.setUser(user);
        conversation.setRole(role);
        conversation.setContent(content);
        agentConversationRepository.save(conversation);

        // 同时更新 Redis 缓存（保存最近 20 条对话）
        String cacheKey = conversationHistoryKey(userId);
        try {
            // 使用 List 结构存储对话历史
            Map<String, String> message = Map.of(
                "role", role,
                "content", content,
                "timestamp", LocalDateTime.now().toString()
            );
            redisTemplate.opsForList().rightPush(cacheKey, message);

            // 限制列表长度为 20 条
            Long size = redisTemplate.opsForList().size(cacheKey);
            if (size != null && size > 20) {
                redisTemplate.opsForList().trim(cacheKey, size - 20, -1);
            }

            // 设置过期时间为 1 小时
            redisTemplate.expire(cacheKey, Duration.ofHours(1));
        } catch (Exception e) {
            log.warn("Failed to cache conversation to Redis for userId={}", userId, e);
        }
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, String>> getCachedConversationHistory(Long userId, int limit) {
        String cacheKey = conversationHistoryKey(userId);
        try {
            // 从 Redis 获取最近的对话
            List<Object> cached = redisTemplate.opsForList().range(cacheKey, -limit, -1);
            if (cached != null && !cached.isEmpty()) {
                List<Map<String, String>> result = new ArrayList<>();
                for (Object item : cached) {
                    Map<String, String> map = objectMapper.convertValue(item, Map.class);
                    result.add(map);
                }
                return result;
            }
        } catch (Exception e) {
            log.warn("Failed to get cached conversation from Redis for userId={}", userId, e);
        }
        return List.of();
    }

    public void clearConversationCache(Long userId) {
        String cacheKey = conversationHistoryKey(userId);
        redisTemplate.delete(cacheKey);
    }

    private String writeJson(List<MusicSongDto> songs) {
        try {
            return objectMapper.writeValueAsString(songs);
        } catch (Exception e) {
            return "[]";
        }
    }

    private String authKey(Long userId) {
        return "music:auth:" + userId;
    }

    private String recommendKey(Long userId) {
        return "music:recommend:" + userId;
    }

    private String pendingSwitchKey(Long userId) {
        return "music:pending-switch:" + userId;
    }

    private String conversationHistoryKey(Long userId) {
        return "conversation:history:" + userId;
    }
}
