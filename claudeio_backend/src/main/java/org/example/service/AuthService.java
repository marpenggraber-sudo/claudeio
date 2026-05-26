package org.example.service;

import org.example.dto.LoginResponse;
import org.example.entity.UserAccount;
import org.example.entity.UserCookie;
import org.example.repo.UserAccountRepository;
import org.example.repo.UserCookieRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class AuthService {

    private final UserAccountRepository userAccountRepository;
    private final UserCookieRepository userCookieRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    public AuthService(UserAccountRepository userAccountRepository,
                      UserCookieRepository userCookieRepository,
                      RedisTemplate<String, Object> redisTemplate) {
        this.userAccountRepository = userAccountRepository;
        this.userCookieRepository = userCookieRepository;
        this.redisTemplate = redisTemplate;
    }

    public LoginResponse login(String account, String password, String cookie) {
        // 1. 查找用户
        Optional<UserAccount> userOpt = userAccountRepository.findByAccount(account);
        if (userOpt.isEmpty()) {
            return new LoginResponse(null, "用户不存在");
        }

        UserAccount user = userOpt.get();

        // 2. 验证密码
        if (!user.getPassword().equals(password)) {
            return new LoginResponse(null, "密码错误");
        }

        // 3. 更新 cookie（如果提供了新的）
        if (cookie != null && !cookie.isBlank()) {
            // 更新数据库中的 cookie
            UserCookie userCookie = userCookieRepository
                .findTopByUser_IdOrderByUpdatedAtDesc(user.getId())
                .orElse(new UserCookie());
            userCookie.setUser(user);
            userCookie.setCookieCiphertext(cookie);
            userCookie.setExpiresAt(LocalDateTime.now().plusDays(30));
            userCookieRepository.save(userCookie);

            // 更新 Redis
            redisTemplate.opsForValue().set("music:auth:" + user.getMusicUserId(), cookie, Duration.ofDays(30));
        }

        return new LoginResponse(user.getMusicUserId(), user.getNickname());
    }

    public LoginResponse register(String account, String password, String cookie) {
        // 1. 检查用户名是否已存在
        Optional<UserAccount> existing = userAccountRepository.findByAccount(account);
        if (existing.isPresent()) {
            return new LoginResponse(null, "用户名已存在");
        }

        // 2. 生成 musicUserId（时间戳）
        Long musicUserId = System.currentTimeMillis();

        // 3. 创建用户
        UserAccount user = new UserAccount();
        user.setAccount(account);
        user.setPassword(password);
        user.setMusicUserId(musicUserId);
        user.setNickname(account);
        user = userAccountRepository.save(user);

        // 4. 存储 cookie
        UserCookie userCookie = new UserCookie();
        userCookie.setUser(user);
        userCookie.setCookieCiphertext(cookie);
        userCookie.setExpiresAt(LocalDateTime.now().plusDays(30));
        userCookieRepository.save(userCookie);

        // 5. 存储到 Redis
        redisTemplate.opsForValue().set("music:auth:" + musicUserId, cookie, Duration.ofDays(30));

        return new LoginResponse(musicUserId, user.getNickname());
    }
}
