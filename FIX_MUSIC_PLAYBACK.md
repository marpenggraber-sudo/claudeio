# 音乐播放问题修复说明

## 问题描述

用户反馈：小程序无法播放音乐，一直提示"无版权"

## 问题根因

经过测试和数据库分析，发现了 ID 不匹配的问题：

### 数据流程

1. **二维码登录**：
   - 用户扫码后，后端从网易云 API 获取 `musicUserId`（例：13879884891）
   - 创建 `user_account` 记录：`id=4`, `music_user_id=13879884891`
   - 保存 Cookie 到 `user_cookie` 表：`user_id=4`（外键指向 user_account.id）

2. **前端保存**：
   ```javascript
   wx.setStorageSync('user_id', userId)  // userId = 13879884891 (musicUserId)
   ```

3. **播放音乐请求**：
   ```javascript
   // 前端发送
   GET /play-url?songId=xxx&userId=13879884891

   // 后端查询（修复前）
   WHERE user_account.id = 13879884891  ❌ 找不到（实际 id=4）
   
   // 返回 null → 前端显示"无版权"
   ```

### 问题示意图

```
┌─────────────────────────────────────────────────────────────┐
│ user_account 表                                              │
├─────┬──────────────┬─────────────────────────────────────────┤
│ id  │ music_user_id│ account      │ nickname                │
├─────┼──────────────┼──────────────┼─────────────────────────┤
│ 4   │ 13879884891  │ 13879884891  │ 用户                    │
└─────┴──────────────┴──────────────┴─────────────────────────┘
                  ↑
                  │ 前端使用这个值
                  │
┌─────────────────┴────────────────────────────────────────────┐
│ user_cookie 表                                                │
├─────┬─────────┬───────────────────────────────────────────────┤
│ id  │ user_id │ cookie_ciphertext        │ expires_at        │
├─────┼─────────┼──────────────────────────┼───────────────────┤
│ 4   │ 4       │ MUSIC_U=xxx...           │ 2026-07-08 22:50  │
└─────┴─────────┴──────────────────────────┴───────────────────┘
          ↑
          │ 外键指向 user_account.id（不是 music_user_id！）
          │
          │ 后端查询（修复前）
          └─ WHERE user_id = 13879884891 ❌ 找不到（应该是 4）
```

## 修复方案

### 方案选择

有两个选项：
1. ❌ 修改前端：保存数据库 `id` 而不是 `musicUserId`
2. ✅ **修改后端**：接受 `musicUserId` 并正确查询 Cookie

**选择方案 2** 的原因：
- `musicUserId` 是网易云的真实用户ID，对前端更自然
- 数据库 `id` 是内部实现细节，不应暴露给前端
- 后端应该能处理两种 ID 的映射关系

### 代码修改

#### 1. UserCookieRepository.java

添加新方法，根据 `musicUserId` 查询：

```java
public interface UserCookieRepository extends JpaRepository<UserCookie, Long> {
    Optional<UserCookie> findTopByUser_IdOrderByUpdatedAtDesc(Long userId);

    /**
     * 根据网易云音乐用户 ID 查找最新的 Cookie
     * @param musicUserId 网易云音乐用户 ID
     * @return 最新的 UserCookie
     */
    Optional<UserCookie> findTopByUser_MusicUserIdOrderByUpdatedAtDesc(Long musicUserId);
}
```

#### 2. MusicApiService.java

修改 `getAuthCookie()` 方法：

```java
/**
 * 根据网易云音乐用户 ID 获取认证 Cookie
 * @param musicUserId 网易云音乐用户 ID（不是数据库主键 ID）
 * @return Cookie 字符串
 */
public String getAuthCookie(Long musicUserId) {
    // 优先从 Redis 读取（如果可用）
    if (redisTemplate != null) {
        try {
            Object value = redisTemplate.opsForValue().get(authKey(musicUserId));
            if (value != null) {
                return value.toString();
            }
        } catch (Exception e) {
            log.warn("Redis 读取失败，降级到数据库: {}", e.getMessage());
        }
    }

    // 从数据库读取（使用 musicUserId 而不是数据库 ID）
    return userCookieRepository.findTopByUser_MusicUserIdOrderByUpdatedAtDesc(musicUserId)
            .map(UserCookie::getCookieCiphertext)
            .orElse(null);
}
```

### SQL 查询对比

**修复前**：
```sql
SELECT * FROM user_cookie uc
LEFT JOIN user_account ua ON ua.id = uc.user_id
WHERE ua.id = 13879884891  -- ❌ 找不到
```

**修复后**：
```sql
SELECT * FROM user_cookie uc
LEFT JOIN user_account ua ON ua.id = uc.user_id
WHERE ua.music_user_id = 13879884891  -- ✅ 找到了
```

## 测试验证

### 单元测试

创建了 `PlayUrlEndpointTest.java`，测试结果：

```
✅ 测试 1: userId 为 null 情况
✅ 测试 2: 有效的 userId (13879884891)
   结果: http://m801.music.126.net/20260608234825/1751f98c6...
✅ 测试 3: 不存在的 userId (999999999)
✅ 测试 4: 完整播放流程

Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### 手动测试

```bash
# 测试播放 URL API
curl "http://localhost:8080/api/music/play-url?songId=2653714443&userId=13879884891"

# 预期响应（修复前）
{"songId":2653714443,"url":null}  ❌

# 预期响应（修复后）
{"songId":2653714443,"url":"http://m801.music.126.net/..."}  ✅
```

## 部署步骤

1. **停止后端服务**
2. **重新编译**：
   ```bash
   mvn clean package -DskipTests
   ```
3. **重启后端服务**
4. **验证修复**：
   - 登录小程序（使用二维码登录）
   - 尝试播放音乐
   - 应该能正常播放，不再提示"无版权"

## 注意事项

### 关于"无版权"提示

修复后仍可能遇到真实的版权问题：

1. **版权限制**：
   - 部分歌曲确实没有版权（例如周杰伦原唱）
   - 但翻唱版本通常有版权

2. **判断方法**：
   - 如果返回 `url: null` → 真的没有版权
   - 如果返回有效 URL → 前端应该能播放

3. **前端处理**：
   ```javascript
   if (res.data.url) {
     // 有 URL，可以播放
     this.playMusic(res.data.url)
   } else {
     // 真的没有版权
     wx.showToast({ title: '该歌曲暂无版权', icon: 'none' })
   }
   ```

## 相关文件

- `src/main/java/org/example/repo/UserCookieRepository.java`
- `src/main/java/org/example/service/MusicApiService.java`
- `src/test/java/org/example/controller/PlayUrlEndpointTest.java`

## 修复时间

2026-06-08

## 修复人员

Claude Code Agent
