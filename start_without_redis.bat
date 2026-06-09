@echo off
chcp 65001 >nul
echo ========================================
echo   Redis 可选配置验证
echo ========================================
echo.

cd C:\Users\otto_\Desktop\claudio\claudeio\claudeio_backend

echo [1/3] 编译项目...
call mvn clean compile -q

if %errorlevel% neq 0 (
    echo ❌ 编译失败
    pause
    exit /b 1
)

echo ✅ 编译成功
echo.

echo [2/3] 运行 Redis 可选配置测试...
call mvn test -Dtest=RedisOptionalConfigTest -q

if %errorlevel% neq 0 (
    echo ❌ 测试失败
    pause
    exit /b 1
)

echo ✅ 测试通过
echo.

echo [3/3] 启动应用（无 Redis）...
echo.
echo 🚀 正在启动后端服务...
echo.
echo ⚠️  如果看到以下日志，说明成功：
echo    "Redis 不可用，将只使用数据库存储"
echo.
echo 按 Ctrl+C 可停止服务
echo.

call mvn spring-boot:run

pause
