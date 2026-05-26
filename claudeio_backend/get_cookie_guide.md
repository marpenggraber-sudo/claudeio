# 获取网易云音乐有效 Cookie 指南

## 方法1：浏览器获取（最简单）

1. 打开 Chrome/Edge 浏览器
2. 访问 https://music.163.com
3. 登录你的网易云账号
4. 按 F12 打开开发者工具
5. 切换到 `Application` 标签
6. 左侧展开 `Cookies` → `https://music.163.com`
7. 找到 `MUSIC_U` 这一行
8. 复制整个 Value 值（一长串字符）
9. 在前端登录时，Cookie 字段填写：`MUSIC_U=你复制的值`

**示例格式：**
```
MUSIC_U=00BC494ADC98647F9C18323AAB873E07E94A341528E92CC91B9392FEA20AB70B02AA808B3299CC97FE61561468DD8713AD8EB62F24AEF71609EA6F45DF983FDA327768CF0FBC
```

## 方法2：通过 API 获取（需要手机验证码）

### 步骤1：发送验证码
```bash
curl "http://localhost:3000/captcha/sent?phone=你的手机号"
```

### 步骤2：使用验证码登录
```bash
curl "http://localhost:3000/login/cellphone?phone=你的手机号&captcha=收到的验证码"
```

### 步骤3：从返回结果中提取 cookie
返回的 JSON 中会包含 `cookie` 字段，复制整个 cookie 字符串。

## 方法3：使用账号密码登录（如果你记得密码）

```bash
curl "http://localhost:3000/login/cellphone?phone=你的手机号&password=你的密码"
```

或者邮箱登录：
```bash
curl "http://localhost:3000/login?email=你的邮箱&password=你的密码"
```

## 验证 Cookie 是否有效

获取 cookie 后，可以测试：
```bash
curl "http://localhost:3000/login/status?cookie=你的完整cookie字符串"
```

如果返回的 JSON 中 `account` 和 `profile` 不为 null，说明 cookie 有效。

## 注意事项

1. Cookie 格式必须包含 `MUSIC_U=` 前缀
2. Cookie 有效期通常为 1-3 个月
3. 不要分享你的 Cookie（相当于账号密码）
4. 如果提示 "需要验证"，可能需要在网易云 APP 上确认登录
