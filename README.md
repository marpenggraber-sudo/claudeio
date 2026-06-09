# Melodio - AI 音乐助手

一个基于 **AI Agent** 的智能音乐播放桌面应用，通过自然语言对话实现音乐搜索、播放、推荐等功能。

前端为**微信小程序**（可作为桌面端运行），后端为 **Spring Boot 3.x** 服务，集成 **LangChain4j + DeepSeek** 大模型和**网易云音乐**数据源。

## 核心功能

- **AI 对话点歌** - 自然语言与 AI 助手对话，说出想听的歌即可播放
- **智能意图识别** - 自动识别搜索、播放、推荐、控制等 10 种用户意图
- **个性化推荐** - 基于播放历史和偏好，AI 生成个性化歌单
- **音乐记忆** - 记住你的听歌历史和常听歌手，越用越懂你
- **歌词同步** - 实时逐字/逐行歌词滚动显示
- **RAG 知识库** - 内置音乐知识库（歌手、风格、理论），增强 AI 回答质量
- **风格主题切换** - 根据歌曲风格（流行/摇滚/电子/古典等）动态切换界面主题
- **播放器控制** - 语音控制暂停、上下首、音量等
- **账号管理** - 支持登录/注册/切换账号/退出登录
- **暗色/亮色主题** - 支持 DARK / LIGHT 双主题

## 技术栈

### 后端

| 组件 | 技术 |
|------|------|
| 框架 | Spring Boot 3.4.5 + Java 21 |
| AI 引擎 | LangChain4j 0.36.2 + DeepSeek (deepseek-v4-pro) |
| 向量检索 | LangChain4j Embeddings (all-MiniLM-L6-v2) + InMemoryEmbeddingStore |
| 数据库 | MySQL 8.x (JPA/Hibernate) |
| 缓存 | Redis |
| 音乐数据 | NeteaseCloudMusicApi (网易云音乐 API 代理) |

### 前端

| 组件 | 技术 |
|------|------|
| 框架 | 微信小程序原生开发 |
| 音频 | wx.createInnerAudioContext |
| UI 风格 | 自定义暗色霓虹主题 (Melodio) |
| 图表 | Canvas 2D 像素时钟 + 音频波形可视化 |

## 项目结构

```
claudeio/
├── claudeio_backend/                # Spring Boot 后端
│   ├── pom.xml
│   ├── src/main/java/org/example/
│   │   ├── MusicAgentApplication.java   # 启动类
│   │   ├── agent/
│   │   │   └── MusicAgent.java          # LangChain4j AI Agent 接口
│   │   ├── config/
│   │   │   ├── AiConfig.java            # DeepSeek 模型配置
│   │   │   ├── CorsConfig.java          # 跨域配置
│   │   │   ├── RagConfig.java           # RAG 向量检索配置
│   │   │   ├── RedisConfig.java         # Redis 配置
│   │   │   └── RestTemplateConfig.java  # HTTP 客户端配置
│   │   ├── controller/
│   │   │   ├── MusicController.java     # 音乐 API 控制器
│   │   │   └── KnowledgeController.java # 知识库管理控制器
│   │   ├── dto/                         # 数据传输对象
│   │   ├── entity/                      # JPA 实体
│   │   ├── repo/ & repository/          # 数据仓库
│   │   ├── service/
│   │   │   ├── ChatOrchestratorService.java  # 对话编排核心（意图分发）
│   │   │   ├── IntentClassifierService.java  # 意图分类器
│   │   │   ├── GenreService.java             # 音乐风格识别
│   │   │   ├── RagService.java               # RAG 知识检索
│   │   │   ├── UserPreferenceService.java    # 用户偏好分析
│   │   │   ├── PlayHistoryService.java       # 播放历史
│   │   │   ├── AuthService.java              # 认证服务
│   │   │   └── MusicApiService.java          # 网易云 API 封装
│   │   └── tools/
│   │       └── MusicTools.java          # LangChain4j Tool 定义
│   └── src/main/resources/
│       ├── application.properties       # 应用配置
│       ├── schema.sql                   # 建表脚本
│       └── db/migration/                # 数据库迁移脚本
│
└── claudeio_frontend/               # 微信小程序前端
    ├── app.js / app.json / app.wxss     # 应用入口
    ├── pages/
    │   ├── index/                       # 主页（播放器 + AI 对话）
    │   ├── login/                       # 登录/注册页
    │   ├── search/                      # 搜索页
    │   ├── settings/                    # 设置页
    │   └── logs/                        # 日志页
    ├── components/                      # 自定义组件
    ├── utils/
    │   ├── agent.js                     # 前端意图解析引擎
    │   ├── netease.js                   # 网易云 API 封装
    │   ├── player.js                    # 音频播放器控制
    │   └── mockData.js                  # Mock 数据
    └── styles/
        └── theme.wxss                   # 全局主题变量
```

## 环境要求

- **Java** 21+
- **Maven** 3.8+
- **MySQL** 8.x
- **Redis** 6.x+
- **Node.js** (可选，用于 NeteaseCloudMusicApi)
- **微信开发者工具** (用于运行前端)

## 快速启动

### 1. 启动基础设施

```bash
# 启动 MySQL，创建数据库
mysql -u root -p -e "CREATE DATABASE music_agent CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# 启动 Redis
redis-server
```

### 2. 启动网易云音乐 API 代理

```bash
# 克隆并启动 NeteaseCloudMusicApi（默认端口 3000）
git clone https://github.com/Binaryify/NeteaseCloudMusicApi.git
cd NeteaseCloudMusicApi
npm install
node app.js
```

### 3. 配置并启动后端

```bash
cd claudeio_backend

# 复制配置模板并编辑
cd src/main/resources
cp application.properties.template application.properties
# 编辑 application.properties，修改以下配置：
# - spring.datasource.password=你的MySQL密码
# - 其他需要自定义的配置

# 返回项目根目录
cd ../../..

# 设置 DeepSeek API Key（环境变量）
export DEEPSEEK_API_KEY="your-api-key-here"
# Windows 使用: set DEEPSEEK_API_KEY=your-api-key-here

# Maven 启动
mvn spring-boot:run
```

后端默认运行在 `http://localhost:8080`。

> **注意**: `application.properties` 包含敏感信息，已在 `.gitignore` 中排除，不会提交到 Git。请根据 `application.properties.template` 模板创建你自己的配置文件。

### 4. 启动前端

1. 打开**微信开发者工具**
2. 导入项目 `claudeio_frontend/` 目录
3. 在 `project.config.json` 中确认 AppID（或使用测试号）
4. 编译运行

## 获取网易云音乐 Cookie

登录需要提供网易云音乐的 `MUSIC_U` Cookie：

1. 浏览器访问 https://music.163.com 并登录
2. F12 打开开发者工具 -> Application -> Cookies
3. 找到 `MUSIC_U`，复制其 Value
4. 在小程序登录页的 Cookie 字段填写 `MUSIC_U=复制的值`

## 使用方式

1. 打开小程序后，点击右上角 **LOGIN** 登录
2. 登录后进入主界面，AI 助手会自动问候并推荐歌曲
3. 在底部输入框输入自然语言，例如：
   - "播放周杰伦的歌"
   - "推荐一些欢快的音乐"
   - "来点中文歌"
   - "下一首" / "暂停" / "音量调到50"
   - "我听过什么歌"
   - "切换账号"
   - "退出登录"

## API 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/music/login` | 登录 |
| POST | `/api/music/register` | 注册 |
| POST | `/api/music/chat` | AI 对话 |
| GET | `/api/music/search` | 搜索歌曲 |
| GET | `/api/music/play-url` | 获取播放链接 |
| GET | `/api/music/greeting` | 获取 AI 问候 |
| GET | `/api/music/lyric/new` | 获取歌词 |
| POST | `/api/music/play-history` | 记录播放历史 |
| GET | `/api/music/user-preference` | 查询用户偏好 |
| GET | `/api/music/genre` | 获取歌曲风格 |
| POST | `/api/music/genres/batch` | 批量获取风格 |
| POST | `/api/music/knowledge/add` | 添加知识库条目 |
| GET | `/api/music/knowledge/search` | 检索知识库 |

## 架构说明

```
用户输入 → 意图分类(IntentClassifier) → 对话编排(ChatOrchestrator)
                                              ↓
                    ┌─────────────────────────┼─────────────────────────┐
                    ↓                         ↓                         ↓
              RAG知识检索               音乐工具调用              AI对话生成
           (RagService)           (MusicTools/ApiService)    (DeepSeek LLM)
                    ↓                         ↓                         ↓
                    └─────────────────────────┼─────────────────────────┘
                                              ↓
                                        AgentReply 返回前端
```
