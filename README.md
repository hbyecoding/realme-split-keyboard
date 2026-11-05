# Realme Split Keyboard · v0.1

把安卓手机变成 Mac 的蓝牙分体键盘。原生 Kotlin 实现，通过 Android Bluetooth HID Device API 发送按键，无需 Mac 配套程序，也不使用网络中转。

## 下载

前往 [v0.1 Release](https://github.com/hbyecoding/realme-split-keyboard/releases/tag/v0.1)，在 **Assets** 下载 `realme-split-keyboard-v0.1.apk`，传到手机并允许安装。`SHA256SUMS.txt` 提供校验值。

v0.1 是供体验的测试版本，APK 使用 Android 调试签名。后续更换签名时可能需要卸载旧版再安装。

## 功能

开发分支新增按键触感（J 特殊触感及定位标记）、右手键区右移和左下角设置入口；上述改动尚未包含在 v0.1 Release APK 中。

- 同一 APK 切换左右手布局，记住上次选择；两侧都有 B、Shift、Cmd、Opt、Ctrl 和空格。
- 同手机多点触控组合键；切后台、触摸取消和切布局时释放按键。
- 三行字母错位，右手 B 向 H 外侧突出，退格为普通字母键的 1.5 倍宽。
- 四行同高铺满可用高度，底行贴底，空格填满三个修饰键以外的宽度。
- 左底行：**Ctrl → Opt → Cmd → 空格**；右底行：**空格 → Cmd → Opt → Ctrl**。
- 状态在外侧底角，点状态或齿轮展开设置；前台恢复 HID 注册并重连上次选择的主机。

为保证触摸面积，v0.1 没有数字行和独立方向键行。右手保留 `;`、`,`、`.`、`/`、回车和退格。

## 已验证与限制

已在 **realme GT5 Pro / Android 14 / realme UI 5** 与 **macOS 15.5 (24F74)** 上验证系统配对、ANSI 键盘识别、单手机普通输入，以及 Shift 组合与松开恢复。最新布局仍需按个人手感调整。

双手机分别连接同一 Mac 是项目设计方向，**跨手机组合键、两侧同时按同一个修饰键，以及长时间断线恢复尚未完成实测**。单手机通过不代表双手机已验证。

需要 Android 9（API 28）以上，且系统实际支持 Bluetooth HID Device。不同厂商系统可能限制该能力。App 需保持前台，暂不提供后台键盘服务。

## 连接 Mac

1. 打开 App，点外侧底角状态或齿轮，点“启用蓝牙”，授予附近设备权限。
2. 点“配对”并允许可发现；Mac 打开“系统设置 → 蓝牙”，选择手机，完成系统配对。
3. 若 Mac 搜不到手机，点 App 的“系统配对”，在手机系统蓝牙设置中配对，再返回 App。
4. 返回 App，点“连接 Mac”，选择已配对的 Mac。系统蓝牙显示已连接不一定代表 HID 输入就绪，应实际打字确认。
5. Mac 若弹出键盘设置助理，第一步用左手布局按 **Z**；第二步用右手布局按 **/**（右 Shift 左边），类型选择 **ANSI（美国）**。
6. 在 Mac 文本编辑测试字母和 Shift 组合。中文输入由 Mac 当前输入法处理。

两台手机安装同一 APK，分别选左右布局，分别配对连接同一 Mac。跨设备行为按 [测试清单](TESTING.md) 验证。

## 构建

需要 JDK 17、Android SDK Platform 35、Build Tools 34.0.0。可用 Android Studio 打开项目并配置 SDK，或设置 `ANDROID_HOME`；也可在未跟踪的 `local.properties` 中设置 `sdk.dir`。

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

生成 APK：`app/build/outputs/apk/debug/app-debug.apk`。

```sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

源码在 `app/src/main/java/dev/splitkeyboard/`：`KeyState.kt` 管理 HID 报告与键位，`KeyboardView.kt` 管理绘制与多点触控，`HidKeyboard.kt` 管理蓝牙，`MainActivity.kt` 管理设置与界面。

## 权限与数据

App 申请蓝牙和震动权限，不申请网络权限，不上传输入。日志不记录具体按键内容，但蓝牙状态可能包含设备名称或地址；反馈前请隐去个人设备信息。

## License

[MIT](LICENSE)
