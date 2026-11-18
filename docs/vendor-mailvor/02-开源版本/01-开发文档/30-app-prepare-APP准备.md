> 来源: https://syh.hzhero.cn/02.%E5%BC%80%E6%BA%90%E7%89%88%E6%9C%AC/01.%E5%BC%80%E5%8F%91%E6%96%87%E6%A1%A3/30.app-prepare.html
> 抓取时间: 2026-10-29 22:00:05
> 版权归济南隽宸信息科技有限公司所有，仅供本地部署参考，请勿外传

# APP准备

当前开源 APP 已验证 Flutter 3.47.3 / Dart 3.13.3，Android 使用 JDK 17、SDK 37、NDK 28.2.13676358；最低 Android 7.0 / iOS 15。以下旧截图用于说明操作位置，配置字段以当前源码为准。

| 准备内容 | 当前主要位置 |
| --- | --- |
| APP 服务地址 | `lib/base.dart`，填写 APP 服务域名或真机可访问的 IP，不重复追加 `/api` |
| 包名与 Android 签名 | `android/app/build.gradle`、MainActivity 包路径与 Manifest |
| iOS 标识、签名、权限 | `ios/Runner.xcworkspace`、`ios/Runner/Info.plist` |
| 微信 / 极光等参数 | `lib/util/global.dart` 与两端原生配置 |

使用AndroidStudio打开sfb，提前安装好`Flutter`插件，`Flutter`插件安装参考[Flutter中文网](https://doc.flutterchina.club/get-started/install/)

### 1. 修改包名

- 全局搜索com.junchen.sanyinghui，替换成自己的包名。 ![](https://syh.hzhero.cn/opensource/develop/img/app1.png)
- 按照包名结构修改MainActivity.java所在路径。 ![](https://syh.hzhero.cn/opensource/develop/img/app2.png)

## 2. 申请安卓证书

- jks或者keystore结尾的证书都可以，申请流程后续完善，先可以参考百度，申请后放到该目录 ![](https://syh.hzhero.cn/opensource/develop/img/app3.png)
- 修改证书配置 ![](https://syh.hzhero.cn/opensource/develop/img/app4.png)

## 3. 申请百川安全图

tip

百川安全图作用：淘宝联盟渠道授权，可以自动绑定订单。

- 进入[阿里妈妈](https://pub.alimama.com/third/manage/record/site.htm)![](https://syh.hzhero.cn/opensource/develop/img/tao1.png)![](https://syh.hzhero.cn/opensource/develop/img/tao2.png)![](https://syh.hzhero.cn/opensource/develop/img/tao3.png)![](https://syh.hzhero.cn/opensource/develop/img/tao4.png)![](https://syh.hzhero.cn/opensource/develop/img/tao5.png)![](https://syh.hzhero.cn/opensource/develop/img/tao6.png)![](https://syh.hzhero.cn/opensource/develop/img/tao7.png)![](https://syh.hzhero.cn/opensource/develop/img/tao8.png)

## 4. 极光推送额外修改

![](https://syh.hzhero.cn/opensource/develop/img/jiguang1.png)
