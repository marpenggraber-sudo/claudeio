package org.example.service;

import dev.langchain4j.model.chat.ChatLanguageModel;
import org.example.dto.GenreResult;
import org.example.dto.MusicSongDto;
import org.example.entity.PlayHistory;
import org.example.repository.PlayHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 音乐风格识别服务
 * 使用三级缓存策略：Redis → 数据库 → AI 推断
 */
@Service
public class GenreService {

    private static final Logger log = LoggerFactory.getLogger(GenreService.class);
    private static final String GENRE_CACHE_PREFIX = "music:genre:";
    private static final long GENRE_CACHE_TTL = 30 * 24 * 60 * 60; // 30天

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private PlayHistoryRepository playHistoryRepository;

    @Autowired
    private ChatLanguageModel chatLanguageModel;

    /**
     * 获取歌曲风格（三级缓存）
     */
    public GenreResult getGenre(Long songId, String songName, String artist) {
        // L1: Redis 缓存
        String cacheKey = GENRE_CACHE_PREFIX + songId;
        String cachedGenre = redisTemplate.opsForValue().get(cacheKey);
        if (cachedGenre != null && !cachedGenre.isEmpty()) {
            log.debug("从 Redis 缓存获取风格: songId={}, genre={}", songId, cachedGenre);
            return GenreResult.fromCache(cachedGenre);
        }

        // L2: 数据库查询（最近播放记录）
        Optional<PlayHistory> recentPlay = playHistoryRepository
                .findTopBySongIdAndGenreIsNotNullOrderByCreatedAtDesc(songId);
        if (recentPlay.isPresent() && recentPlay.get().getGenre() != null) {
            String genre = recentPlay.get().getGenre();
            log.debug("从数据库获取风格: songId={}, genre={}", songId, genre);
            cacheToRedis(songId, genre);
            return GenreResult.fromDatabase(genre);
        }

        // L3: AI 推断（异步，带超时保护）
        return inferGenreAsync(songId, songName, artist);
    }

    /**
     * AI 推断风格（带超时保护）
     */
    private GenreResult inferGenreAsync(Long songId, String songName, String artist) {
        try {
            CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
                return inferGenreByAI(songName, artist);
            });

            // 2秒超时
            String genre = future.get(2, TimeUnit.SECONDS);
            cacheToRedis(songId, genre);
            log.info("AI 推断风格成功: songId={}, songName={}, artist={}, genre={}",
                    songId, songName, artist, genre);
            return GenreResult.fromAI(genre);

        } catch (TimeoutException e) {
            log.warn("AI 推断超时，使用降级策略: songId={}, songName={}", songId, songName);
            String genre = guessGenreByArtist(artist);
            cacheToRedis(songId, genre);
            return GenreResult.fromDefault(genre);
        } catch (Exception e) {
            log.error("AI 推断失败: songId={}, error={}", songId, e.getMessage());
            String genre = guessGenreByArtist(artist);
            cacheToRedis(songId, genre);
            return GenreResult.fromDefault(genre);
        }
    }

    /**
     * 使用 DeepSeek AI 推断风格
     */
    private String inferGenreByAI(String songName, String artist) {
        String prompt = String.format(
                "请判断歌曲《%s》- %s 的音乐风格。\n\n" +
                        "从以下风格中选择最匹配的一个：\n" +
                        "pop(流行), rock(摇滚), electronic(电子), classical(古典), " +
                        "folk(民谣), hiphop(说唱), jazz(爵士), rnb(R&B), " +
                        "blues(蓝调), metal(金属), ambient(轻音乐), country(乡村)\n\n" +
                        "只返回英文风格代码，不要解释。",
                songName, artist
        );

        try {
            String response = chatLanguageModel.generate(prompt);
            return normalizeGenre(response.trim().toLowerCase());
        } catch (Exception e) {
            log.error("调用 AI 模型失败: {}", e.getMessage());
            return "default";
        }
    }

    /**
     * 根据艺术家名简单猜测风格（降级策略）
     */
    private String guessGenreByArtist(String artist) {
        if (artist == null || artist.isEmpty()) {
            return "default";
        }

        String lower = artist.toLowerCase();

        // 电子音乐
        if (lower.contains("dj") || lower.contains("电音") || lower.contains("alan walker")
                || lower.contains("marshmello") || lower.contains("avicii")) {
            return "electronic";
        }

        // 摇滚
        if (lower.contains("beyond") || lower.contains("五月天") || lower.contains("linkin park")
                || lower.contains("bon jovi") || lower.contains("guns n' roses")) {
            return "rock";
        }

        // 说唱
        if (lower.contains("rapper") || lower.contains("mc") || lower.contains("eminem")
                || lower.contains("drake") || lower.contains("gai") || lower.contains("vava")) {
            return "hiphop";
        }

        // 古典
        if (lower.contains("beethoven") || lower.contains("mozart") || lower.contains("chopin")
                || lower.contains("贝多芬") || lower.contains("莫扎特") || lower.contains("肖邦")) {
            return "classical";
        }

        // 爵士
        if (lower.contains("jazz") || lower.contains("爵士")) {
            return "jazz";
        }

        // 民谣
        if (lower.contains("赵雷") || lower.contains("宋冬野") || lower.contains("陈粒")
                || lower.contains("李志") || lower.contains("folk")) {
            return "folk";
        }

        // 流行（默认）
        if (lower.contains("周杰伦") || lower.contains("jay") || lower.contains("邓紫棋")
                || lower.contains("taylor swift") || lower.contains("ed sheeran")) {
            return "pop";
        }

        return "default";
    }

    /**
     * 批量预加载播放队列风格
     */
    public Map<Long, String> batchLoadGenres(List<MusicSongDto> songs) {
        Map<Long, String> result = new HashMap<>();

        // 并行处理
        songs.parallelStream().forEach(song -> {
            GenreResult genreResult = getGenre(song.getId(), song.getName(), song.getArtist());
            result.put(song.getId(), genreResult.getGenre());
        });

        return result;
    }

    /**
     * 缓存到 Redis
     */
    private void cacheToRedis(Long songId, String genre) {
        String cacheKey = GENRE_CACHE_PREFIX + songId;
        redisTemplate.opsForValue().set(cacheKey, genre, GENRE_CACHE_TTL, TimeUnit.SECONDS);
    }

    /**
     * 标准化风格名称
     */
    private String normalizeGenre(String genre) {
        // 移除可能的标点和空格
        genre = genre.replaceAll("[\\s\\p{Punct}]", "");

        // 中文到英文映射
        Map<String, String> mapping = Map.ofEntries(
                Map.entry("流行", "pop"),
                Map.entry("摇滚", "rock"),
                Map.entry("电子", "electronic"),
                Map.entry("古典", "classical"),
                Map.entry("民谣", "folk"),
                Map.entry("说唱", "hiphop"),
                Map.entry("嘻哈", "hiphop"),
                Map.entry("爵士", "jazz"),
                Map.entry("rb", "rnb"),
                Map.entry("蓝调", "blues"),
                Map.entry("金属", "metal"),
                Map.entry("轻音乐", "ambient"),
                Map.entry("氛围", "ambient"),
                Map.entry("乡村", "country")
        );

        String normalized = mapping.getOrDefault(genre, genre);

        // 验证是否是有效的风格
        List<String> validGenres = List.of(
                "pop", "rock", "electronic", "classical", "folk", "hiphop",
                "jazz", "rnb", "blues", "metal", "ambient", "country"
        );

        return validGenres.contains(normalized) ? normalized : "default";
    }
}
