# MoonPointer Android

Android 端通过 **WebView 加载与 Web 完全相同的前端**，功能与界面保持一致（实时字幕、视频学习、笔记、耳机模式等）。

## 启动顺序（测试前必做）

需要 **同时** 运行后端和 Web 前端：

```powershell
# 终端 1：后端
cd D:\EnglishAI
.\start-backend.ps1

# 终端 2：Web 前端（需 host: true，已配置在 vite.config.ts）
cd D:\EnglishAI\web
npm run dev
```

然后在 Android Studio **Run ▶** 安装 App。

## 默认地址

| 环境 | Web 前端 | 后端 API |
|------|----------|----------|
| 模拟器 | `http://10.0.2.2:3000` | `http://10.0.2.2:8080` |
| 真机 | `http://<电脑IP>:3000` | `http://<电脑IP>:8080` |

App 右下角 **齿轮** 可修改地址并刷新。

## 权限

首次启动会请求 **麦克风**；Web 内使用耳机模式时按浏览器提示授权。

## 命令行安装

```powershell
cd D:\EnglishAI\android
.\run-app.ps1
```

## 与纯原生版的区别

旧版原生界面已替换为 WebView 方案，确保与 `web/` 功能同步，无需维护两套 UI。
