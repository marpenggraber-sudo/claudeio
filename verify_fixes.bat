@echo off
chcp 65001 >nul
echo ========================================
echo   TDD 修复验证脚本
echo ========================================
echo.

echo [1/4] 检查后端是否运行...
curl -s http://localhost:8080/api/music/qr-key >nul 2>&1
if %errorlevel% equ 0 (
    echo ✅ 后端正常运行
) else (
    echo ❌ 后端未运行，请先启动：
    echo    cd claudeio_backend
    echo    mvn spring-boot:run
    exit /b 1
)

echo.
echo [2/4] 测试二维码 Key 生成...
curl -s http://localhost:8080/api/music/qr-key
echo.

echo.
echo [3/4] 检查 api-enhanced 是否运行...
curl -s http://localhost:3000/login/qr/key >nul 2>&1
if %errorlevel% equ 0 (
    echo ✅ api-enhanced 正常运行
) else (
    echo ⚠️  api-enhanced 未运行或无响应
    echo    Docker: docker ps ^| findstr ncm-api
)

echo.
echo [4/4] 完整测试流程...
echo.
echo 📋 请按以下步骤测试：
echo.
echo 1. 打开微信开发者工具
echo 2. 运行小程序（claudeio_frontend）
echo 3. 进入登录页
echo 4. 点击"扫码"标签
echo 5. 点击"生成二维码"
echo 6. 用手机网易云 APP 扫码
echo 7. 在手机上确认登录
echo 8. 预期：自动跳转到播放器页面（无需输入账号密码）
echo.

echo ========================================
echo   验证完成！
echo ========================================
echo.
echo 📖 详细文档: TDD_FIX_SUMMARY.md
echo.

pause
