package org.example.repo;

import org.example.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    Optional<UserAccount> findByMusicUserId(Long musicUserId);
    Optional<UserAccount> findByAccount(String account);
}
