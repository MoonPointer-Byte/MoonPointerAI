# 命令行安装 Debug APK（Android Studio Run 不可用时的备用方案）
$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

Write-Host ">> 构建 Debug APK..."
& .\gradlew.bat :app:assembleDebug
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

$apk = "app\build\outputs\apk\debug\app-debug.apk"
if (-not (Test-Path $apk)) {
    Write-Error "未找到 APK: $apk"
}

$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
if (-not (Test-Path $adb)) {
    Write-Error "未找到 adb，请在 Android Studio 安装 Android SDK Platform-Tools"
}

Write-Host ">> 检查设备..."
& $adb devices
Write-Host ">> 安装到设备/模拟器..."
& $adb install -r $apk
Write-Host ">> 启动 MoonPointer..."
& $adb shell am start -n com.englishai.translator/.MainActivity
Write-Host "完成。"
