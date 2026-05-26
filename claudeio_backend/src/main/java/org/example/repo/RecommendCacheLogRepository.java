package org.example.repo;

import org.example.entity.RecommendCacheLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecommendCacheLogRepository extends JpaRepository<RecommendCacheLog, Long> {
}
