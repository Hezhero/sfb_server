> 来源: https://syh.hzhero.cn/02.%E5%BC%80%E6%BA%90%E7%89%88%E6%9C%AC/02.%E9%83%A8%E7%BD%B2%E6%96%87%E6%A1%A3/30.app-build.html
> 抓取时间: 2026-10-29 22:00:08
> 版权归济南隽宸信息科技有限公司所有，仅供本地部署参考，请勿外传

# app打包

## 1. 确认构建环境与生产配置

当前开源 APP 使用 Flutter 3.47.3 / Dart 3.13.3 验证。Android 构建使用 JDK 17、SDK 37、NDK 28.2.13676358，最低 Android 7.0；iOS 最低 15，需要 macOS、Xcode 与 CocoaPods。

在 `lib/base.dart` 设置自己的生产域名：

```dart
static final baseHost = kDebugMode
    ? 'http://192.168.1.100:8008'
    : 'https://api.example.com';
```

客户端会追加 `/api`。确认包名、图标、启动图、签名、权限、第三方账号和服务端配置已同步，见 [APP 准备](https://syh.hzhero.cn/02.开源版本/01.开发文档/30.app-prepare.html)。

## 2. Android 打包

在 `android/app/build.gradle` 设置自己的证书路径、别名、密码，然后执行：

```bash
flutter pub get
flutter build apk --release
```

产物：`build/app/outputs/flutter-apk/app-release.apk`。若 Flutter 选中了 Android Studio 自带的其他 JDK，先用 `flutter doctor -v` 检查，确保该工程使用 JDK 17。

## 3. iOS 本地打包

```bash
cd ios
pod install
cd ..
open ios/Runner.xcworkspace
```

打开 **Runner.xcworkspace**，配置 Bundle ID、Team、描述文件、Capabilities 与权限说明，再用 Xcode Archive 归档。编译验证可执行：

```bash
flutter build ios --debug --no-codesign
```

未签名构建不能直接安装真机；真机安装和发布需要有效签名。

## 4. 插件与安装兼容性

当前工程混合使用 Swift Package Manager 和 CocoaPods，保留工程中的百川资源复制、静态 framework 清理阶段，不要只复制一份旧 Xcode 工程覆盖它。

已有工作区若锁定了旧人脸核验原生 SDK，在 `ios/` 执行 `pod update --no-repo-update` 后重新构建。CocoaPods 与插件安装脚本应使用同一套 Ruby 环境，微信 / 支付宝相关脚本需要能够加载 `plist` gem。

Windows 可开发和打包 Android；本节 iOS 本地打包需要 macOS。商业版在线打包是另一项能力，见[版本对比](https://syh.hzhero.cn/01.介绍/30.functional.html)。
