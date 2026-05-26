package org.example.repository;

import org.example.entity.UserPreference;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserPreferenceRepository extends JpaRepository<UserPreference, Long> {

    Optional<UserPreference> findByUserIdAndArtist(Long userId, String artist);

    @Query("SELECT up FROM UserPreference up WHERE up.userId = :userId ORDER BY up.playCount DESC")
    List<UserPreference> findTopArtistsByUserId(@Param("userId") Long userId);

    @Query("SELECT up FROM UserPreference up WHERE up.userId = :userId ORDER BY up.playCount DESC LIMIT :limit")
    List<UserPreference> findTopArtistsByUserIdWithLimit(@Param("userId") Long userId, @Param("limit") int limit);

    @Query("SELECT up FROM UserPreference up WHERE up.userId = :userId ORDER BY up.playCount DESC, up.lastPlayedAt DESC")
    List<UserPreference> findTopArtistsByUserIdOrderByPlayCountAndRecency(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT up FROM UserPreference up WHERE up.userId = :userId AND up.lastPlayedAt >= :since ORDER BY up.lastPlayedAt DESC")
    List<UserPreference> findRecentActiveArtists(@Param("userId") Long userId, @Param("since") LocalDateTime since, Pageable pageable);
}
