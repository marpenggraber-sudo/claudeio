package org.example.repo;

import org.example.entity.RecommendCacheLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RecommendCacheLogRepository extends JpaRepository<RecommendCacheLog, Long> {
    Optional<RecommendCacheLog> findTopByUser_IdOrderByCreatedAtDesc(Long userId);
}
