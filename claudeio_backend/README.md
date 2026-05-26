# 音乐 Agent 后端项目

这是一个基于 **Spring Boot 3.x + LangChain4j + DeepSeek + Redis + MySQL** 的音乐 Agent 后端项目，配套前端位于 `frontend` 目录。

## DeepSeek 调用说明

当前后端默认使用 DeepSeek：

- Base URL: `https://api.deepseek.com`
- Model: `deepseek-v4-pro`
- thinking: enabled
- reasoning_effort: high

API Key 通过环境变量读取：

```text
DEEPSEEK_API_KEY
```

PowerShell 示例：

```powershell
$env:DEEPSEEK_API_KEY="你的新 DeepSeek API Key"
mvn spring-boot:run
```

---

## 登录方式

前端右上角 `LOGIN` 已经可以点击，会进入登录页。

登录时请填写：

- 账号
- 密码
- `MUSIC_U Cookie`

真正用于身份验证的是 `Cookie`。

---

## 启动顺序

1. Redis
2. MySQL
3. `netease_cloud_music_api`
4. 后端 Spring Boot
5. 前端小程序
