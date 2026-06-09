package org.example.service;

import org.example.dto.AgentReply;
import org.example.dto.MusicSongDto;
import org.example.entity.PlayHistory;
import org.example.repository.PlayHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 推荐多样性测试
 *
 * TDD RED 阶段：验证推荐歌曲会过滤掉用户最近听过的歌
 *
 * 问题：用户连续多次请求相同关键词，总是返回相同的歌曲
 * 期望：推荐时过滤掉用户最近听过的歌曲，增加推荐多样性
 */
@SpringBootTest
class RecommendationDiversityTest {

    @Autowired
    private ChatOrchestratorService chatOrchestratorService;

    @Autowired
    private PlayHistoryRepository playHistoryRepository;

    private final Long TEST_USER_ID = 9999888877776666L;  // 测试用户 ID

    @BeforeEach
    void setup() {
        // 清空测试用户的播放历史
        playHistoryRepository.deleteAll();
    }

    /**
     * 测试场景 1：用户第一次请求，没有播放历史
     * 应该返回搜索结果的前 N 首歌曲
     */
    @Test
    void recommend_withNoHistory_shouldReturnTopResults() {
        // Arrange - 无播放历史
        String message = "推荐健身音乐";

        // Act
        AgentReply reply = chatOrchestratorService.chat(message, TEST_USER_ID);

        // Assert
        assertThat(reply.songs())
            .as("第一次推荐应该返回歌曲列表")
            .isNotEmpty()
            .hasSizeLessThanOrEqualTo(20);  // 最多20首
    }

    /**
     * 测试场景 2：用户听过一些歌后再次请求
     * 应该过滤掉最近听过的歌曲
     */
    @Test
    void recommend_withRecentHistory_shouldExcludePlayedSongs() {
        // Arrange - 第一次推荐
        String message = "推荐健身音乐";
        AgentReply firstReply = chatOrchestratorService.chat(message, TEST_USER_ID);

        assertThat(firstReply.songs())
            .as("第一次推荐应该有结果")
            .isNotEmpty();

        // 模拟用户听了前 3 首歌
        List<MusicSongDto> playedSongs = firstReply.songs().stream().limit(3).toList();
        for (MusicSongDto song : playedSongs) {
            PlayHistory history = new PlayHistory();
            history.setUserId(TEST_USER_ID);
            history.setSongId(song.getId());
            history.setSongName(song.getName());
            history.setArtist(song.getArtist());
            history.setCompleted(true);  // 完整播放
            history.setPlayDuration(180);
            history.setCreatedAt(LocalDateTime.now());
            playHistoryRepository.save(history);
        }

        // Act - 第二次推荐（相同关键词）
        AgentReply secondReply = chatOrchestratorService.chat(message, TEST_USER_ID);

        // Assert - 第二次推荐不应包含已听过的歌曲
        List<Long> playedSongIds = playedSongs.stream()
            .map(MusicSongDto::getId)
            .toList();

        List<Long> secondReplySongIds = secondReply.songs().stream()
            .map(MusicSongDto::getId)
            .toList();

        assertThat(secondReplySongIds)
            .as("第二次推荐不应包含用户最近听过的歌曲")
            .doesNotContainAnyElementsOf(playedSongIds);
    }

    /**
     * 测试场景 3：用户听过很多歌，但搜索结果较少
     * 应该返回可用的歌曲，即使数量少于请求的数量
     */
    @Test
    void recommend_withManyPlayedSongs_shouldReturnAvailableSongs() {
        // Arrange - 第一次推荐
        String message = "推荐健身音乐";
        AgentReply firstReply = chatOrchestratorService.chat(message, TEST_USER_ID);

        // 模拟用户听了几乎所有歌（只留 2 首）
        List<MusicSongDto> allSongs = firstReply.songs();
        int songsToPlay = Math.max(allSongs.size() - 2, 0);

        for (int i = 0; i < songsToPlay; i++) {
            MusicSongDto song = allSongs.get(i);
            PlayHistory history = new PlayHistory();
            history.setUserId(TEST_USER_ID);
            history.setSongId(song.getId());
            history.setSongName(song.getName());
            history.setArtist(song.getArtist());
            history.setCompleted(true);
            history.setPlayDuration(180);
            history.setCreatedAt(LocalDateTime.now());
            playHistoryRepository.save(history);
        }

        // Act - 第二次推荐（请求 5 首）
        AgentReply secondReply = chatOrchestratorService.chat(message, TEST_USER_ID);

        // Assert - 应该返回剩余的可用歌曲（即使少于 5 首）
        assertThat(secondReply.songs())
            .as("即使可用歌曲很少，也应该返回剩余的歌曲")
            .isNotEmpty()
            .hasSizeLessThanOrEqualTo(5);

        // 确认返回的歌曲都是未听过的
        List<Long> playedSongIds = allSongs.stream()
            .limit(songsToPlay)
            .map(MusicSongDto::getId)
            .toList();

        List<Long> secondReplySongIds = secondReply.songs().stream()
            .map(MusicSongDto::getId)
            .toList();

        assertThat(secondReplySongIds)
            .as("返回的歌曲应该都是未听过的")
            .doesNotContainAnyElementsOf(playedSongIds);
    }

    /**
     * 测试场景 4：连续多次请求相同关键词
     * 每次推荐的歌曲应该不同（因为会过滤掉已听过的）
     */
    @Test
    void recommend_multipleRequests_shouldProvideDiversity() {
        // Arrange
        String message = "推荐健身音乐";

        // Act - 第一次推荐
        AgentReply reply1 = chatOrchestratorService.chat(message, TEST_USER_ID);
        assertThat(reply1.songs()).isNotEmpty();

        // 模拟用户听了第一批歌曲
        for (MusicSongDto song : reply1.songs()) {
            PlayHistory history = new PlayHistory();
            history.setUserId(TEST_USER_ID);
            history.setSongId(song.getId());
            history.setSongName(song.getName());
            history.setArtist(song.getArtist());
            history.setCompleted(true);
            history.setPlayDuration(180);
            history.setCreatedAt(LocalDateTime.now());
            playHistoryRepository.save(history);
        }

        // Act - 第二次推荐
        AgentReply reply2 = chatOrchestratorService.chat(message, TEST_USER_ID);

        // Assert - 第二次推荐的歌曲应该完全不同
        List<Long> songIds1 = reply1.songs().stream()
            .map(MusicSongDto::getId)
            .toList();

        List<Long> songIds2 = reply2.songs().stream()
            .map(MusicSongDto::getId)
            .toList();

        assertThat(songIds2)
            .as("第二次推荐应该完全不包含第一次的歌曲")
            .doesNotContainAnyElementsOf(songIds1);
    }

    /**
     * 测试场景 5：只过滤"完整播放"的歌曲
     * 未完整播放的歌曲（completed=false）不应该被过滤
     */
    @Test
    void recommend_withIncompleteHistory_shouldNotFilter() {
        // Arrange - 第一次推荐
        String message = "推荐健身音乐";
        AgentReply firstReply = chatOrchestratorService.chat(message, TEST_USER_ID);

        // 模拟用户播放了一首歌，但没有完整播放（跳过了）
        MusicSongDto skippedSong = firstReply.songs().get(0);
        PlayHistory incompleteHistory = new PlayHistory();
        incompleteHistory.setUserId(TEST_USER_ID);
        incompleteHistory.setSongId(skippedSong.getId());
        incompleteHistory.setSongName(skippedSong.getName());
        incompleteHistory.setArtist(skippedSong.getArtist());
        incompleteHistory.setCompleted(false);  // ❌ 未完整播放
        incompleteHistory.setPlayDuration(10);
        incompleteHistory.setCreatedAt(LocalDateTime.now());
        playHistoryRepository.save(incompleteHistory);

        // Act - 第二次推荐
        AgentReply secondReply = chatOrchestratorService.chat(message, TEST_USER_ID);

        // Assert - 未完整播放的歌曲仍然可能出现在推荐中
        // （因为用户可能不喜欢该歌曲才跳过，所以不应该完全过滤）
        // 这里我们只验证推荐系统正常工作即可
        assertThat(secondReply.songs())
            .as("即使有未完整播放的历史，推荐也应该正常工作")
            .isNotEmpty();
    }
}
