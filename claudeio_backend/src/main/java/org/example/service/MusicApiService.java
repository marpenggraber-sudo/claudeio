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
import org.springframework.beans.factory.annotation.Autowired;
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
                           @Autowired(required = false) RedisTemplate<String, Object> redisTemplate,
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

        if (redisTemplate == null) {
            log.warn("MusicApiService: Redis 不可用，将只使用数据库存储");
        }
    }

    public Long verifyAndStoreCookie(String cookie) {
        // 生成一个随机的 userId（基于时间戳）
        Long userId = System.currentTimeMillis();

        log.info("Storing cookie without verification, generated userId={}", userId);

        // 存储到 Redis（如果可用）
        if (redisTemplate != null) {
            try {
                redisTemplate.opsForValue().set(authKey(userId), cookie, Duration.ofDays(30));
            } catch (Exception e) {
                log.warn("Redis 写入失败: {}", e.getMessage());
            }
        }

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

            // 不使用 UriComponentsBuilder，因为它会对 Cookie 进行 URL 编码
            // 网易云 API 需要原始的 Cookie 字符串
            String url = String.format(
                "%s/song/url/v1?id=%d&level=lossless&cookie=%s",
                baseUrl, songId, formattedCookie
            );

            log.debug("请求播放 URL: {}", url);

            JsonNode node = restTemplate.getForObject(url, JsonNode.class);
            JsonNode data = node == null ? null : node.path("data");

            if (data != null && data.isArray() && !data.isEmpty()) {
                JsonNode firstItem = data.get(0);
                String playUrl = firstItem.path("url").asText(null);
                String level = firstItem.path("level").asText("");
                String type = firstItem.path("type").asText("");
                long size = firstItem.path("size").asLong(0);
                long br = firstItem.path("br").asLong(0);

                log.info("获取播放 URL 成功: songId={}, level={}, type={}, size={} bytes, br={}",
                    songId, level, type, size, br);

                return playUrl;
            }

            log.warn("未找到播放 URL: songId={}", songId);
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

    /**
     * 根据网易云音乐用户 ID 获取认证 Cookie
     * @param musicUserId 网易云音乐用户 ID（不是数据库主键 ID）
     * @return Cookie 字符串
     */
    public String getAuthCookie(Long musicUserId) {
        // 优先从 Redis 读取（如果可用）
        if (redisTemplate != null) {
            try {
                Object value = redisTemplate.opsForValue().get(authKey(musicUserId));
                if (value != null) {
                    return value.toString();
                }
            } catch (Exception e) {
                log.warn("Redis 读取失败，降级到数据库: {}", e.getMessage());
            }
        }

        // 从数据库读取（使用 musicUserId 而不是数据库 ID）
        return userCookieRepository.findTopByUser_MusicUserIdOrderByUpdatedAtDesc(musicUserId)
                .map(UserCookie::getCookieCiphertext)
                .orElse(null);
    }

    public void cachePendingSwitchAccount(Long userId, String account) {
        if (redisTemplate != null) {
            try {
                redisTemplate.opsForValue().set(pendingSwitchKey(userId), account, Duration.ofMinutes(10));
            } catch (Exception e) {
                log.warn("Redis 写入失败: {}", e.getMessage());
            }
        }
    }

    public Optional<String> getPendingSwitchAccount(Long userId) {
        if (redisTemplate != null) {
            try {
                Object value = redisTemplate.opsForValue().get(pendingSwitchKey(userId));
                return value == null ? Optional.empty() : Optional.of(value.toString());
            } catch (Exception e) {
                log.warn("Redis 读取失败: {}", e.getMessage());
            }
        }
        return Optional.empty();
    }

    public void clearPendingSwitchAccount(Long userId) {
        if (redisTemplate != null) {
            try {
                redisTemplate.delete(pendingSwitchKey(userId));
            } catch (Exception e) {
                log.warn("Redis 删除失败: {}", e.getMessage());
            }
        }
    }

    public String getCurrentUserAccount(Long userId) {
        return userAccountRepository.findByMusicUserId(userId)
                .map(UserAccount::getAccount)
                .orElse(null);
    }

    public void cacheRecommend(Long userId, List<MusicSongDto> songs) {
        // 存储到 Redis（如果可用）
        if (redisTemplate != null) {
            try {
                redisTemplate.opsForValue().set(recommendKey(userId), songs, Duration.ofMinutes(30));
            } catch (Exception e) {
                log.warn("Redis 写入推荐缓存失败: {}", e.getMessage());
            }
        }

        // 保存到数据库
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
        // 优先从 Redis 读取（如果可用）
        if (redisTemplate != null) {
            try {
                Object value = redisTemplate.opsForValue().get(recommendKey(userId));
                if (value instanceof List<?> list) {
                    return list.stream().map(item -> objectMapper.convertValue(item, MusicSongDto.class)).toList();
                }
            } catch (Exception e) {
                log.warn("Redis 读取推荐缓存失败，尝试从数据库读取: {}", e.getMessage());
            }
        }

        // 从数据库读取最新的推荐记录
        UserAccount user = userAccountRepository.findByMusicUserId(userId).orElse(null);
        if (user != null) {
            return recommendCacheLogRepository.findTopByUser_IdOrderByCreatedAtDesc(user.getId())
                .map(cacheLog -> {
                    try {
                        return objectMapper.readValue(cacheLog.getSongsJson(),
                            objectMapper.getTypeFactory().constructCollectionType(List.class, MusicSongDto.class));
                    } catch (Exception e) {
                        log.warn("解析推荐缓存失败: {}", e.getMessage());
                        return List.<MusicSongDto>of();
                    }
                })
                .orElse(List.of());
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

        // 同时更新 Redis 缓存（如果可用）
        if (redisTemplate != null) {
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
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, String>> getCachedConversationHistory(Long userId, int limit) {
        String cacheKey = conversationHistoryKey(userId);

        // 优先从 Redis 读取（如果可用）
        if (redisTemplate != null) {
            try {
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
                log.warn("Failed to get cached conversation from Redis, falling back to database: {}", e.getMessage());
            }
        }

        // 从数据库读取
        UserAccount user = userAccountRepository.findByMusicUserId(userId).orElse(null);
        if (user != null) {
            List<AgentConversation> conversations = agentConversationRepository
                .findTop20ByUser_IdOrderByCreatedAtDesc(user.getId());

            return conversations.stream()
                .map(conv -> Map.of(
                    "role", conv.getRole(),
                    "content", conv.getContent(),
                    "timestamp", conv.getCreatedAt() != null ? conv.getCreatedAt().toString() : ""
                ))
                .toList();
        }

        return List.of();
    }

    public void clearConversationCache(Long userId) {
        if (redisTemplate != null) {
            try {
                String cacheKey = conversationHistoryKey(userId);
                redisTemplate.delete(cacheKey);
            } catch (Exception e) {
                log.warn("Redis 删除对话缓存失败: {}", e.getMessage());
            }
        }
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
