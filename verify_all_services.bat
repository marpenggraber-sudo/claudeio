@echo off
chcp 65001 >nul
echo ========================================
echo   所有服务 Redis 可选配置验证
echo ========================================
echo.

cd C:\Users\otto_\Desktop\claudio\claudeio\claudeio_backend

echo [1/4] 清理项目...
call mvn clean -q

echo [2/4] 编译项目...
call mvn compile -q

if %errorlevel% neq 0 (
    echo.
    echo ❌ 编译失败，请检查错误信息
    pause
    exit /b 1
)

echo ✅ 编译成功
echo.

echo [3/4] 运行测试...
call mvn test -Dtest=AllServicesWithoutRedisTest -q

if %errorlevel% neq 0 (
    echo ❌ 测试失败
    pause
    exit /b 1
)

echo ✅ 测试通过
echo.

echo [4/4] 启动应用...
echo.
echo 🚀 正在启动后端服务（无 Redis）...
echo.
echo 预期日志：
echo   - "AuthService: Redis 不可用，将只使用数据库存储"
echo   - "MusicApiService: Redis 不可用，将只使用数据库存储"
echo   - "Started MusicAgentApplication"
echo.
echo 按 Ctrl+C 可停止服务
echo.

call mvn spring-boot:run

pause
