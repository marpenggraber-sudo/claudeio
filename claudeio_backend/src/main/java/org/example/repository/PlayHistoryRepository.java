package org.example.repository;

import org.example.entity.PlayHistory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PlayHistoryRepository extends JpaRepository<PlayHistory, Long> {

    List<PlayHistory> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("SELECT ph FROM PlayHistory ph WHERE ph.userId = :userId AND ph.createdAt >= :since ORDER BY ph.createdAt DESC")
    List<PlayHistory> findRecentByUserId(@Param("userId") Long userId, @Param("since") LocalDateTime since);

    @Query("SELECT ph FROM PlayHistory ph WHERE ph.userId = :userId AND ph.completed = true ORDER BY ph.createdAt DESC")
    List<PlayHistory> findCompletedByUserId(@Param("userId") Long userId);

    @Query("SELECT ph FROM PlayHistory ph WHERE ph.userId = :userId AND ph.completed = true ORDER BY ph.createdAt DESC")
    List<PlayHistory> findCompletedPlaysByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT COUNT(ph) FROM PlayHistory ph WHERE ph.userId = :userId AND ph.songId = :songId")
    Long countPlaysBySongId(@Param("userId") Long userId, @Param("songId") Long songId);

    @Query("SELECT DISTINCT ph.songId FROM PlayHistory ph WHERE ph.userId = :userId AND ph.createdAt >= :since")
    List<Long> findRecentPlayedSongIds(@Param("userId") Long userId, @Param("since") LocalDateTime since);

    // 查询指定歌曲最近的播放记录（有风格信息）
    Optional<PlayHistory> findTopBySongIdAndGenreIsNotNullOrderByCreatedAtDesc(Long songId);
}
