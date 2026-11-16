> 来源: https://www.mailvor.com/02.%E5%BC%80%E6%BA%90%E7%89%88%E6%9C%AC/01.%E5%BC%80%E5%8F%91%E6%96%87%E6%A1%A3/60.start-app.html
> 抓取时间: 2026-10-29 22:00:06
> 版权归苏州迈沃通信有限公司所有，仅供本地部署参考，请勿外传

# 快速启动（APP项目）

## 1. 准备 Flutter 环境

当前开源 APP 已验证 **Flutter 3.47.3 / Dart 3.13.3**，`pubspec.yaml` 约束 Flutter ≥ 3.44、Dart ≥ 3.12。Flutter 已包含 Dart，不需要单独安装 Dart。

```bash
flutter --version
flutter doctor -v
```

| 平台 | 当前构建环境 |
| --- | --- |
| Android | JDK 17、SDK 37、NDK 28.2.13676358；AGP 8.11.1、Gradle 8.14 |
| iOS | macOS、Xcode、CocoaPods；最低 iOS 15 |

APP 最低支持 Android 7.0（API 24）。Android 使用的 JDK 17 与服务端 Java 8 分开配置。

## 2. 获取代码与依赖

```bash
git clone https://gitee.com/jacky4631/sfb.git
cd sfb
flutter pub get
```

保留 `package/` 中的本地组件，以及 `pubspec.lock` 锁定的依赖。插件存在 Git 依赖，需要能够访问对应仓库。

## 3. 配置 APP 服务地址

先启动后端 `mshop-app`。修改 `lib/base.dart`：

```dart
static final baseHost = kDebugMode
    ? 'http://192.168.1.100:8008'
    : 'https://api.example.com';
```

IP 与域名只是示例。真机使用手机可以访问的开发机 IP，不能填写手机自身的 `localhost` 或 `127.0.0.1`。客户端会追加 `/api`，`baseHost` 不要重复添加它，也不要连接管理端 `8001/admin`。

包名、签名、微信、百川、极光等参数见 [APP 准备](https://www.mailvor.com/02.开源版本/01.开发文档/30.app-prepare.html)。

## 4. 运行与检查

```bash
flutter devices
flutter run
```

多设备连接时使用 `flutter run -d DEVICE_ID` 指定设备。服务地址和顶层网络客户端初始化参数变化后，需要热重启或重新运行，热重载不会重建客户端。

```bash
flutter analyze --no-pub --no-fatal-warnings --no-fatal-infos
flutter test --no-pub test/single_flight_test.dart
```

当前静态检查仍有历史 warning / info；上述参数不会忽略编译 error。iOS 首次运行先完成 `pod install` 并在 Xcode 中配置签名，详见[APP 打包](https://www.mailvor.com/02.开源版本/02.部署文档/30.app-build.html)。
