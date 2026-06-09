@echo off
chcp 65001 >nul
echo ========================================
echo   编译验证脚本
echo ========================================
echo.

cd C:\Users\otto_\Desktop\claudio\claudeio\claudeio_backend

echo [1/3] 清理项目...
call mvn clean >nul 2>&1

echo [2/3] 编译项目...
call mvn compile

if %errorlevel% equ 0 (
    echo.
    echo ✅ 编译成功！
    echo.
    echo [3/3] 运行测试...
    call mvn test -Dtest=QrLoginControllerTest

    if %errorlevel% equ 0 (
        echo.
        echo ========================================
        echo   ✅ 所有测试通过！
        echo ========================================
    ) else (
        echo.
        echo ========================================
        echo   ⚠️  测试失败，请检查日志
        echo ========================================
    )
) else (
    echo.
    echo ========================================
    echo   ❌ 编译失败，请检查错误信息
    echo ========================================
)

echo.
pause
