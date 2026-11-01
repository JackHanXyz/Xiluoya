# Xiluoya（汐洛雅）

基于 [Shizuku](https://github.com/RikkaApps/Shizuku) 与 [LSPatch](https://github.com/LSPosed/LSPatch) 的安卓工具箱——通过本机 ADB 启动服务、给 APK 打补丁并内嵌 Xposed 模块，全程只需要一台手机。

## 功能

- **本机 ADB 启动**：手机自己通过 127.0.0.1 拉起服务，不需要电脑
- **APK 补丁**：内置 LSPatch 引擎（本地移植），给任意 APK 打补丁
- **内嵌 Xposed 模块**：打补丁时选择模块 APK 一起打进产物，安装后模块免 root 生效
- **补丁历史**：记录打过的包，一键重装
- **桌面快捷方式**：长按图标一键启动服务 / 打补丁
- **通知栏状态**：服务运行时常驻通知
- **应用管理**：已授权应用列表，支持搜索

## 构建

```bash
git clone https://github.com/<you>/Xiluoya.git
cd Xiluoya
./gradlew :manager:assembleDebug
```

环境要求：JDK 21、Android SDK 36（compileSdk）、NDK 27.1、minSdk 24。

产物：`manager/build/outputs/apk/debug/`

## 项目结构

| 模块 | 说明 |
|---|---|
| `manager` | 主应用（UI、本机 ADB 启动、补丁入口） |
| `lspatchengine` | LSPatch 引擎（自 LSPatch 移植） |
| `server` / `starter` / `shell` | Shizuku 服务端 |
| `api` | Shizuku API（自 [Shizuku-API](https://github.com/RikkaApps/Shizuku-API) 内联） |

## 许可

- 本项目包含 [LSPatch](https://github.com/LSPosed/LSPatch) 引擎的移植代码（GPL-3.0），因此整体以 **GPL-3.0** 发布，见 [LICENSE](LICENSE)
- [Shizuku](https://github.com/RikkaApps/Shizuku)、[Shizuku-API](https://github.com/RikkaApps/Shizuku-API) 采用 Apache-2.0
- 内置 [MiSans](https://hyperos.mi.com/font/) 字体版权归小米所有，可免费商用

## 致谢

- [RikkaApps/Shizuku](https://github.com/RikkaApps/Shizuku)
- [LSPosed/LSPatch](https://github.com/LSPosed/LSPatch)
- [topjohnwu/libsu](https://github.com/topjohnwu/libsu)
