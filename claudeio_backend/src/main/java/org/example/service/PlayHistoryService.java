package org.example.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.entity.PlayHistory;
import org.example.repository.PlayHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlayHistoryService {

    private final PlayHistoryRepository playHistoryRepository;

    /**
     * 记录播放历史
     */
    @Transactional
    public void recordPlay(Long userId, Long songId, String songName, String artist, Integer duration, Boolean completed) {
        PlayHistory history = new PlayHistory();
        history.setUserId(userId);
        history.setSongId(songId);
        history.setSongName(songName);
        history.setArtist(artist);
        history.setPlayDuration(duration != null ? duration : 0);
        history.setCompleted(completed != null ? completed : false);

        playHistoryRepository.save(history);
        log.info("记录播放历史: userId={}, songId={}, songName={}, artist={}", userId, songId, songName, artist);
    }

    /**
     * 获取用户最近的播放历史
     */
    public List<PlayHistory> getRecentHistory(Long userId, int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        List<PlayHistory> histories = playHistoryRepository.findRecentByUserId(userId, since);
        log.info("[MusicMemory] 查询最近听歌历史: userId={}, days={}, count={}", userId, days, histories.size());
        return histories;
    }

    /**
     * 获取用户所有播放历史
     */
    public List<PlayHistory> getAllHistory(Long userId) {
        List<PlayHistory> histories = playHistoryRepository.findByUserIdOrderByCreatedAtDesc(userId);
        log.info("[MusicMemory] 查询全部听歌历史: userId={}, count={}", userId, histories.size());
        return histories;
    }

    /**
     * 获取用户完整播放的历史
     */
    public List<PlayHistory> getCompletedHistory(Long userId) {
        List<PlayHistory> histories = playHistoryRepository.findCompletedByUserId(userId);
        log.info("[MusicMemory] 查询完整播放历史: userId={}, count={}", userId, histories.size());
        return histories;
    }

    public boolean hasRecommendationHistory(Long userId) {
        return getCompletedHistory(userId).stream().findAny().isPresent()
                || !getRecentHistory(userId, 30).isEmpty();
    }

    /**
     * 获取用户最近听过的歌曲 ID 列表（用于推荐去重）
     * 只返回完整播放的歌曲 ID，避免过滤掉用户跳过的歌曲
     *
     * @param userId 用户 ID
     * @param limit 返回的最大数量
     * @return 最近听过的歌曲 ID 列表
     */
    public List<Long> getRecentSongIds(Long userId, int limit) {
        List<PlayHistory> completedHistory = playHistoryRepository.findCompletedByUserId(userId);
        List<Long> songIds = completedHistory.stream()
            .limit(limit)
            .map(PlayHistory::getSongId)
            .distinct()  // 去重
            .toList();
        log.info("[MusicMemory] 查询最近听过的歌曲ID: userId={}, limit={}, count={}", userId, limit, songIds.size());
        return songIds;
    }

    /**
     * 获取用户最近的播放历史（按歌曲去重，供前端历史列表展示）
     */
    public List<PlayHistory> getUniqueRecentHistory(Long userId) {
        // 调用 Repository 已有的按时间倒序查询方法
        List<PlayHistory> allHistory = playHistoryRepository.findByUserIdOrderByCreatedAtDesc(userId);

        // 使用 Set 进行去重，保留最新的一条，限制返回 50 条
        Set<Long> seenSongIds = new HashSet<>();
        List<PlayHistory> distinctHistory = allHistory.stream()
                .filter(history -> seenSongIds.add(history.getSongId())) // add返回true表示之前没加过
                .limit(50) // 限制列表最大长度为 50，避免前端渲染卡顿
                .toList();

        log.info("[MusicMemory] 查询去重后的前端历史记录: userId={}, count={}", userId, distinctHistory.size());
        return distinctHistory;
    }

}
