package org.example.repo;

import org.example.entity.UserCookie;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserCookieRepository extends JpaRepository<UserCookie, Long> {
    Optional<UserCookie> findTopByUser_IdOrderByUpdatedAtDesc(Long userId);
}
