package org.example.repo;

import org.example.entity.UserCookie;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserCookieRepository extends JpaRepository<UserCookie, Long> {
    Optional<UserCookie> findTopByUser_IdOrderByUpdatedAtDesc(Long userId);

    /**
     * 根据网易云音乐用户 ID 查找最新的 Cookie
     * @param musicUserId 网易云音乐用户 ID
     * @return 最新的 UserCookie
     */
    Optional<UserCookie> findTopByUser_MusicUserIdOrderByUpdatedAtDesc(Long musicUserId);
}
