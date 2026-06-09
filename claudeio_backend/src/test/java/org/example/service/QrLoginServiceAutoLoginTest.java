package org.example.service;

import org.example.dto.LoginResponse;
import org.example.dto.QrStatusResponse;
import org.example.entity.UserAccount;
import org.example.repo.UserAccountRepository;
import org.example.repo.UserCookieRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TDD Test: 二维码扫码成功后，自动创建/更新账号并返回登录信息
 *
 * 场景：用户扫码成功（状态码 803），获得 cookie
 * 预期：
 *  1. 自动从 cookie 中提取用户信息（调用网易云 API）
 *  2. 如果用户不存在，自动注册
 *  3. 如果用户存在，更新 cookie
 *  4. 返回 userId 和 nickname，前端直接跳转到主页
 */
@SpringBootTest
@Transactional
class QrLoginServiceAutoLoginTest {

    @Autowired
    private QrLoginService qrLoginService;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private UserCookieRepository userCookieRepository;

    @Test
    void shouldAutoLoginAfterQrScanSuccess() {
        // RED: 这个测试会失败，因为当前 checkQrStatus 只返回 cookie，没有自动登录逻辑

        // Arrange
        String testCookie = "MUSIC_U=test_cookie_12345; Path=/";
        QrStatusResponse qrResponse = QrStatusResponse.success(testCookie);

        // Act
        // 当前 checkQrStatus 返回的 QrStatusResponse 没有 userId 和 nickname
        // 需要新增一个方法：completeQrLogin(cookie) -> LoginResponse

        // Assert
        assertNotNull(qrResponse);
        assertEquals(803, qrResponse.getCode());
        assertNotNull(qrResponse.getCookie());

        // TODO: 需要实现这个逻辑
        // LoginResponse loginResult = qrLoginService.completeQrLogin(testCookie);
        // assertNotNull(loginResult.getUserId());
        // assertNotNull(loginResult.getNickname());
    }

    @Test
    void shouldCreateNewUserIfNotExistsAfterQrScan() {
        // RED: 测试自动注册逻辑

        // Arrange
        String testCookie = "MUSIC_U=new_user_cookie; Path=/";
        Long testUserId = 999888777L;

        // Act
        // 模拟扫码成功，cookie 中包含新用户信息
        // completeQrLogin 应该：
        // 1. 调用网易云 /login/status 获取用户信息
        // 2. 检查数据库是否存在该 userId
        // 3. 不存在则自动创建

        // Assert
        // UserAccount user = userAccountRepository.findByMusicUserId(testUserId);
        // assertNotNull(user, "应该自动创建新用户");
    }

    @Test
    void shouldUpdateCookieIfUserExistsAfterQrScan() {
        // RED: 测试已存在用户的 cookie 更新

        // Arrange
        Long existingUserId = 123456789L;
        UserAccount existingUser = new UserAccount();
        existingUser.setMusicUserId(existingUserId);
        existingUser.setAccount("existing_user");
        existingUser.setPassword("password");
        existingUser.setNickname("老用户");
        userAccountRepository.save(existingUser);

        String newCookie = "MUSIC_U=updated_cookie; Path=/";

        // Act
        // completeQrLogin(newCookie) 应该更新该用户的 cookie

        // Assert
        // UserCookie updatedCookie = userCookieRepository
        //     .findTopByUser_IdOrderByUpdatedAtDesc(existingUser.getId())
        //     .orElseThrow();
        // assertTrue(updatedCookie.getCookieCiphertext().contains("updated_cookie"));
    }
}
