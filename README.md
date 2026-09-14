# LumaPad

LumaPad is an unofficial Moonlight Android fork designed for PC games that need touch-friendly
keyboard and mouse controls. It connects directly to a normal Sunshine host; no additional
LumaPad server is required on Windows.

LumaPad 是一款非官方的 Moonlight Android 分支，專為需要觸控鍵盤、滑鼠操作的 PC 遊戲設計。
它可直接連線既有的 Sunshine 主機，Windows 不需要另外安裝 LumaPad 專用伺服器。

## Download and guides / 下載與教學

- [Download the latest APK / 下載最新版 APK](https://github.com/DeifiedXE/lumapad-android/releases/latest)
- [繁體中文安裝與使用教學](docs/USER_GUIDE.zh-TW.md)
- [English installation and user guide](docs/USER_GUIDE.md)
- [Feature and developer notes](LUMAPAD.md)

## Highlights / 主要功能

- Mixed keyboard and mouse touch controls / 鍵盤與滑鼠混合觸控
- Manually selected, named control profiles / 可手動切換的命名按鍵配置
- Addable, removable, and fully rebindable buttons / 可新增、刪除及自由綁定的虛擬按鍵
- Full-screen PC keyboard or classic list binding editor / 滿版 PC 鍵盤或傳統清單式設定
- Free-swipe mouse aiming area with a touch-held binding / 可附帶按鍵的自由滑動滑鼠瞄準區
- Adjustable mouse sensitivity for uncovered stream areas / 可調整空白串流區域的滑鼠靈敏度
- Configurable radial shortcut menus / 可自訂的輻射快捷輪盤
- Sunshine monitor switching button / Sunshine 串流螢幕切換按鈕

## Quick start / 快速開始

### English

1. Install and configure [Sunshine](https://docs.lizardbyte.dev/projects/sunshine/latest/md_docs_2getting__started.html)
   on the gaming PC. If regular Moonlight already connects, this step is complete.
2. Download and install the LumaPad APK from [Releases](https://github.com/DeifiedXE/lumapad-android/releases/latest).
3. Pair LumaPad with Sunshine, then enable **Settings > On-screen Controls Settings > Show
   on-screen controls** and set **Touch control type** to **Keyboard + mouse (mixed sticks)**.

### 繁體中文

1. 在遊戲電腦安裝並設定 [Sunshine](https://docs.lizardbyte.dev/projects/sunshine/latest/md_docs_2getting__started.html)；
   如果原版 Moonlight 已能連線，這一步已完成。
2. 從 [Releases](https://github.com/DeifiedXE/lumapad-android/releases/latest) 下載並安裝 LumaPad APK。
3. 將 LumaPad 與 Sunshine 配對，接著開啟**設定 > 螢幕控制按鈕設定 > 顯示螢幕控制按鈕**，
   再把**觸控輸入類型**設成**鍵盤＋滑鼠（混合搖桿）**。

## Building

See [LUMAPAD.md](LUMAPAD.md#build) for the Android SDK, NDK, and Gradle build instructions.

## License and attribution

LumaPad is based on [Moonlight Android](https://github.com/moonlight-stream/moonlight-android)
and moonlight-common-c. It remains licensed under GNU GPL v3; see [LICENSE.txt](LICENSE.txt).
LumaPad is not an official Moonlight project.
