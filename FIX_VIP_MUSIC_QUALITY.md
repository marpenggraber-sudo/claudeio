# VIP 用户音乐不完整问题修复 (TDD)

## 问题描述

用户反馈：作为 VIP 用户，播放的音乐不完整

## TDD 流程

### 1. RED - 编写失败测试

创建了 `VipMusicQualityTest.java`，包含 4 个测试：
1. `vipUser_shouldGetPlayUrl` - VIP 用户应该能获取播放 URL
2. `checkApiResponse_shouldContainQualityInfo` - 检查 API 响应结构
3. `checkDifferentQualityLevels` - 测试不同音质等级
4. `checkVipStatus` - 检查 VIP 状态

### 2. 问题诊断

通过测试发现：

**用户信息（测试 4）：**
```
VIP 类型: 11 (黑胶 VIP) ✅
昵称: 你的网易云音乐
用户 ID: 13879884891
```

**音质对比（测试 3）：**
```
音质: standard   | 大小:    4301262 bytes | 比特率: 128000
音质: higher     | 大小:    6451871 bytes | 比特率: 192000
音质: exhigh     | 大小:   10753089 bytes | 比特率: 320000
音质: lossless   | 大小:   35649024 bytes | 比特率: 1061180 ✅ 应该获取这个
音质: hires      | 大小:   35649024 bytes | 比特率: 1061180
```

**实际获取结果（修复前）：**
```
获取播放 URL 成功: level=exhigh, type=mp3, size=10753089 bytes, br=320000 ❌
播放 URL: .../c40389617c83e44325124550d0ab0b47.mp3 ❌
```

### 3. 根因分析

代码已经设置了 `level=lossless`，但网易云 API 返回的是 `exhigh` (320k MP3)。

对比发现：
- **测试 2**（直接调用 API）：使用 `String.format()` 拼接 URL → 返回 **FLAC 无损**
- **测试 1**（通过 `MusicApiService`）：使用 `UriComponentsBuilder.queryParam()` → 返回 **MP3 320k**

**问题根因：**

`UriComponentsBuilder.queryParam()` 会对 Cookie 进行 **URL 编码**，例如：
```
原始 Cookie: MUSIC_U=003443EA5414D19951BF9C5467D4663F...
URL 编码后: MUSIC_U%3D003443EA5414D19951BF9C5467D4663F...
```

网易云 API **不接受 URL 编码的 Cookie**，导致 VIP 权限未正确传递，API 自动降级音质。

### 4. GREEN - 修复实现

#### 修改文件：`MusicApiService.java`

**修复前代码：**
```java
String url = UriComponentsBuilder.fromUriString(baseUrl)
        .path("/song/url/v1")
        .queryParam("id", songId)
        .queryParam("level", "lossless")
        .queryParam("cookie", formattedCookie)  // ❌ 会 URL 编码 Cookie
        .toUriString();
```

**修复后代码：**
```java
// 不使用 UriComponentsBuilder，因为它会对 Cookie 进行 URL 编码
// 网易云 API 需要原始的 Cookie 字符串
String url = String.format(
    "%s/song/url/v1?id=%d&level=lossless&cookie=%s",
    baseUrl, songId, formattedCookie  // ✅ 直接使用原始 Cookie
);
```

**添加详细日志：**
```java
log.info("获取播放 URL 成功: songId={}, level={}, type={}, size={} bytes, br={}",
    songId, level, type, size, br);
```

### 5. 测试结果

**修复后测试 1：**
```
获取播放 URL 成功: level=lossless, type=flac, size=35649024 bytes, br=1061180 ✅
播放 URL: .../362d523cb5b31183ba715906ec2f1cbf.flac ✅
```

**修复前后对比：**

| 项目 | 修复前 | 修复后 | 改善 |
|------|--------|--------|------|
| 音质等级 | `exhigh` | `lossless` | ✅ |
| 文件格式 | `.mp3` | `.flac` | ✅ |
| 文件大小 | 10.7 MB | 35.6 MB | +231% |
| 比特率 | 320 kbps | 1061 kbps | +231% |

**所有测试通过：**
```
Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### 6. IMPROVE - 测试覆盖率

创建的测试文件：
1. `VipMusicQualityTest.java` - 完整的 VIP 音质测试套件
2. `CookieFormatTest.java` - Cookie 格式测试（辅助诊断）

测试覆盖：
- ✅ VIP 用户获取播放 URL
- ✅ API 响应结构验证
- ✅ 不同音质等级对比
- ✅ VIP 状态检查
- ✅ Cookie 格式影响

## 技术细节

### Cookie 编码问题

**URL 编码的影响：**

网易云 Cookie 长度约 842 字符，包含特殊字符（如 `=`, `+`）。当使用 `UriComponentsBuilder.queryParam()` 时：

1. `=` → `%3D`
2. `+` → `%2B`
3. `/` → `%2F`

网易云 API 解析时无法正确识别 VIP 权限，导致降级到普通用户音质。

### 为什么不影响其他功能？

其他端点（如搜索、歌词）对 Cookie 编码不敏感，但**音质获取端点**需要精确的 VIP 权限验证，必须使用原始 Cookie。

## 部署步骤

1. **重启后端服务**
2. **验证修复**：
   - 登录小程序（VIP 账号）
   - 播放任意歌曲
   - 检查音质：应该显示"无损"或"Hi-Res"
   - 文件应该更大（通常 >30MB）

## 用户体验改善

**修复前：**
- VIP 用户只能播放 320k MP3
- 音质受限，文件不完整（相对于无损）
- 体验与普通用户无差异

**修复后：**
- VIP 用户获取无损 FLAC
- 比特率提升 231%（320k → 1061k）
- 文件大小提升 231%（10.7MB → 35.6MB）
- 音质体验显著提升

## 相关文件

- `src/main/java/org/example/service/MusicApiService.java` - 修复 Cookie 编码问题
- `src/test/java/org/example/service/VipMusicQualityTest.java` - 完整测试套件
- `src/test/java/org/example/service/CookieFormatTest.java` - Cookie 格式测试

## 修复时间

2026-06-08

## 修复人员

Claude Code Agent (TDD 流程)
