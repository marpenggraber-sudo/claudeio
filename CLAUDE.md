# Melodio - AI 音乐助手项目文档

## 项目概述

Melodio 是一个基于 AI Agent 的智能音乐播放应用，通过自然语言对话实现音乐搜索、播放、推荐等功能。

**技术栈**：
- 后端：Spring Boot 3.4.5 + Java 21 + LangChain4j + DeepSeek
- 前端：微信小程序原生开发
- 数据库：MySQL 8.x + Redis
- AI 能力：RAG 知识检索、意图识别、个性化推荐

## 项目结构

```
claudeio/
├── claudeio_backend/          # Spring Boot 后端服务
│   ├── src/main/java/org/example/
│   │   ├── agent/             # LangChain4j AI Agent
│   │   ├── config/            # 配置类（AI、Redis、CORS、RAG）
│   │   ├── controller/        # REST API 控制器
│   │   ├── dto/               # 数据传输对象
│   │   ├── entity/            # JPA 实体
│   │   ├── repository/        # 数据仓库
│   │   ├── service/           # 业务逻辑层
│   │   └── tools/             # LangChain4j Tool 定义
│   └── src/main/resources/
│       └── application.properties
│
├── claudeio_frontend/         # 微信小程序前端
│   ├── pages/                 # 页面（播放器、登录、搜索、设置）
│   ├── components/            # 自定义组件
│   └── utils/                 # 工具类（agent、player、netease API）
│
└── music_agent.sql            # 数据库初始化脚本
```

## 核心架构

### AI Agent 工作流

```
用户输入 → 意图分类 → 对话编排
              ↓
    ┌─────────┼─────────┐
    ↓         ↓         ↓
RAG检索  音乐工具  AI对话
    ↓         ↓         ↓
    └─────────┼─────────┘
              ↓
        返回结果
```

### 核心服务

| 服务 | 职责 |
|------|------|
| `ChatOrchestratorService` | 对话编排核心，意图分发 |
| `IntentClassifierService` | 意图分类器（10种意图） |
| `RagService` | RAG 知识检索 |
| `GenreService` | 音乐风格识别 |
| `UserPreferenceService` | 用户偏好分析 |
| `MusicApiService` | 网易云音乐 API 封装 |
| `AuthService` | 用户认证服务 |

### 支持的意图类型

1. SEARCH - 搜索歌曲
2. PLAY - 播放音乐
3. RECOMMEND - 推荐歌曲
4. CONTROL - 播放器控制（暂停/下一首/音量）
5. HISTORY - 查询播放历史
6. PREFERENCE - 查询用户偏好
7. KNOWLEDGE - 音乐知识问答
8. ACCOUNT - 账号管理（登录/注册/切换/退出）
9. GREETING - 问候
10. UNKNOWN - 未知意图

## 开发规范

### 后端开发规范

#### 代码结构
- 遵循 Spring Boot 3.x 最佳实践
- 使用 Java 21 特性（Records、Sealed Classes、Pattern Matching）
- 构造器注入，避免 `@Autowired` 字段注入
- 使用 `@ConfigurationProperties` 进行类型安全配置

#### API 设计
- RESTful 风格，使用标准 HTTP 方法和状态码
- 统一响应格式（`AgentReply`、`ChatResponse` 等）
- 使用 `@Valid` 进行请求参数验证
- 全局异常处理（`@RestControllerAdvice`）

#### 数据访问
- JPA 实体使用 `@Entity` + `@Table`
- Repository 接口继承 `JpaRepository`
- 使用 `@Transactional` 管理事务
- Redis 用于缓存和会话管理

#### AI Agent 开发
- 使用 LangChain4j 框架定义 Agent 和 Tools
- Tools 使用 `@Tool` 注解标注
- 意图分类使用 DeepSeek 模型
- RAG 使用 InMemoryEmbeddingStore + all-MiniLM-L6-v2 嵌入模型

### 前端开发规范

#### 微信小程序规范
- 使用原生小程序开发，不依赖第三方框架
- 组件化开发，复用性强
- 使用 `wx.createInnerAudioContext` 管理音频播放
- 主题切换基于歌曲风格动态调整

#### 代码组织
- 页面逻辑在 `pages/` 目录
- 可复用组件在 `components/` 目录
- 工具函数在 `utils/` 目录
- 全局样式在 `styles/` 目录

## 环境配置

### 必需环境

```bash
# Java 21+
java -version

# Maven 3.8+
mvn -version

# MySQL 8.x
mysql --version

# Redis 6.x+
redis-server --version

# Node.js（用于网易云音乐 API）
node -v
```

### 环境变量

后端需要配置以下环境变量：

```bash
# DeepSeek API Key（必需）
export DEEPSEEK_API_KEY="your-api-key-here"

# 数据库配置（可选，默认值见 application.properties）
export DB_URL="jdbc:mysql://localhost:3306/music_agent"
export DB_USERNAME="root"
export DB_PASSWORD="your-password"

# Redis 配置（可选）
export REDIS_HOST="localhost"
export REDIS_PORT="6379"
```

### 启动步骤

1. **启动 MySQL**
   ```bash
   mysql -u root -p -e "CREATE DATABASE music_agent CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
   mysql -u root -p music_agent < music_agent.sql
   ```

2. **启动 Redis**
   ```bash
   redis-server
   ```

3. **启动网易云音乐 API**
   ```bash
   git clone https://github.com/Binaryify/NeteaseCloudMusicApi.git
   cd NeteaseCloudMusicApi
   npm install
   node app.js  # 默认端口 3000
   ```

4. **启动后端**
   ```bash
   cd claudeio_backend
   export DEEPSEEK_API_KEY="your-key"
   mvn spring-boot:run
   # 或使用 IDE 运行 MusicAgentApplication.java
   ```

5. **启动前端**
   - 打开微信开发者工具
   - 导入 `claudeio_frontend/` 目录
   - 编译运行

## API 接口文档

### 认证相关

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/music/login` | 用户登录 |
| POST | `/api/music/register` | 用户注册 |

### 音乐相关

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/music/chat` | AI 对话接口 |
| GET | `/api/music/search` | 搜索歌曲 |
| GET | `/api/music/play-url` | 获取播放链接 |
| GET | `/api/music/lyric/new` | 获取歌词 |
| GET | `/api/music/greeting` | 获取 AI 问候 |
| GET | `/api/music/genre` | 获取歌曲风格 |
| POST | `/api/music/genres/batch` | 批量获取风格 |

### 用户数据

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/music/play-history` | 记录播放历史 |
| GET | `/api/music/user-preference` | 查询用户偏好 |

### 知识库

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/music/knowledge/add` | 添加知识库条目 |
| GET | `/api/music/knowledge/search` | 检索知识库 |

## 开发指南

### 添加新的意图类型

1. 在 `IntentType.java` 枚举中添加新意图
2. 在 `IntentClassifierService.java` 中更新意图识别逻辑
3. 在 `ChatOrchestratorService.java` 中添加对应的处理分支
4. 如需新工具，在 `MusicTools.java` 中添加 `@Tool` 方法

### 扩展 RAG 知识库

1. 准备知识文档（文本格式）
2. 调用 `/api/music/knowledge/add` 接口添加
3. 系统自动进行向量化和索引
4. 对话时自动检索相关知识

### 添加新的音乐数据源

1. 在 `MusicApiService.java` 中添加新的 API 调用方法
2. 在 `MusicTools.java` 中封装为 Tool
3. 更新 Agent 的系统提示词，告知新数据源

## 测试

### 后端测试

```bash
cd claudeio_backend
mvn test
```

### 前端测试

在微信开发者工具中使用真机调试或模拟器测试。

## 部署

### 后端部署

```bash
# 打包
mvn clean package -DskipTests

# 运行
java -jar target/claudeio_backend-1.0-SNAPSHOT.jar \
  --spring.profiles.active=prod \
  --DEEPSEEK_API_KEY=your-key
```

### 前端部署

1. 在微信公众平台注册小程序
2. 配置服务器域名（后端 API 地址）
3. 上传代码并提交审核

## 常见问题

### Q: DeepSeek API 调用失败
A: 检查 `DEEPSEEK_API_KEY` 是否正确配置，确认 API 额度是否充足。

### Q: 网易云音乐 API 无法访问
A: 确认 NeteaseCloudMusicApi 服务是否启动（默认端口 3000），检查 `application.properties` 中的 `netease.api.base-url` 配置。

### Q: 歌曲无法播放
A: 检查网易云音乐 Cookie 是否有效，部分歌曲可能需要 VIP 权限。

### Q: RAG 检索效果不佳
A: 增加知识库条目数量，优化文档质量，调整相似度阈值。

## 贡献指南

1. Fork 本项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'feat: add some amazing feature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 提交 Pull Request

## 许可证

本项目仅供学习和研究使用。

## 联系方式

如有问题或建议，请提交 Issue。
