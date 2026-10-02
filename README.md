# 净屏 JingPing v0.1

一个不 Root、不占 VPN 槽的 Android 广告界面自动处理原型。

## 当前功能

- AccessibilityService 自动识别并点击常见“跳过 / 跳过广告 / 关闭广告 / ×”控件
- 保守模式 / 激进模式
- 常驻通知栏状态 + 一键暂停/开启
- 桌面小组件：状态、累计处理次数、一键暂停/开启
- 快捷设置磁贴（需要用户在通知栏编辑页面手动添加“净屏”）
- 本地拦截计数
- 不声明 INTERNET 权限，不上传屏幕数据
- 不使用 VpnService，不与 v2rayNG、Clash、公司 VPN、游戏加速器争抢系统 VPN 槽

## 安装后怎么用

1. 打开“净屏”。
2. 点“开启 / 管理无障碍服务”。
3. 在系统无障碍列表里找到“净屏广告自动处理”并允许。
4. 回到净屏，状态显示“保护运行中”。
5. 可选：打开“激进模式”。
6. 桌面长按 → 小组件 → 添加“净屏”。
7. 下拉通知栏 → 编辑快捷开关 → 添加“净屏”磁贴。

Android 13+ 如果是侧载 APK，系统可能要求先在“应用信息”右上角允许受限制的设置，之后才能开启无障碍。这是 Android 的安全机制。

## 构建

推荐 Android Studio：

- JDK 17
- compileSdk 35
- Android Gradle Plugin 8.7.3

打开工程后执行 `assembleDebug`，APK 位于：

`app/build/outputs/apk/debug/app-debug.apk`

仓库还附带 `.github/workflows/android.yml`，放到 GitHub 后可以手动运行 Actions 生成 APK。

## 规则原则

当前默认规则偏保守：

- 明确“关闭广告 / 跳过广告”优先处理
- “跳过”主要限制在进入 App 后约 16 秒内
- “× / X / 关闭”默认需要右上角位置特征；激进模式阈值更低
- 排除系统设置、系统 UI、权限控制器、安装器和输入法，降低误触风险

## 下一版本建议

- App 独立规则 JSON（包名 + View ID + 文本 + 坐标）
- 本地视觉识别兜底（自绘 Canvas 广告）
- 用户“这个广告没拦住”反馈入口
- 规则包签名与热更新
- 可选通知营销过滤
- 可选 Private DNS 配置引导，不占 VPN 槽

## Windows 一键构建
双击 `BUILD_WINDOWS.bat`。脚本会自动准备 JDK 17、Android SDK 35、Gradle 8.9，并输出 `JingPing-v0.2-debug.apk`。

## 一键上传 GitHub 并触发 Actions

Windows 上双击 `PUSH_GITHUB_AND_BUILD.bat`。脚本会把当前工程自动推送到：

`https://github.com/chinesed051-source/Android`

随后 GitHub Actions 会自动执行 Android APK 构建。
