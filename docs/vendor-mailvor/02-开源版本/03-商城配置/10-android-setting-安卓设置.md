> 来源: https://www.mailvor.com/02.%E5%BC%80%E6%BA%90%E7%89%88%E6%9C%AC/03.%E5%95%86%E5%9F%8E%E9%85%8D%E7%BD%AE/10.android-setting.html
> 抓取时间: 2026-10-29 22:00:09
> 版权归苏州迈沃通信有限公司所有，仅供本地部署参考，请勿外传

# 安卓设置

当前 Android 工程使用 JDK 17、SDK 37、NDK 28.2.13676358，最低 API 24（Android 7.0）。包名、namespace 与签名主要在 `android/app/build.gradle` 配置；以下截图为历史操作示意，字段以当前工程为准。完整构建步骤见 [APP 打包](https://www.mailvor.com/02.开源版本/02.部署文档/30.app-build.html)。

### 1. 修改包名

- 全局搜索`com.mailvor.sufenbao`，替换成自己的包名 ![img.png](https://www.mailvor.com/opensource/setting/img/img.png)

### 2. 按照包名结构修改MainActivity.java所在路径

![img1.png](https://www.mailvor.com/opensource/setting/img/img1.png)

### 3. 申请安卓证书

- jks或者keystore结尾的证书都可以，申请流程可以参考百度，申请后放到该目录 ![img2.png](https://www.mailvor.com/opensource/setting/img/img2.png)![img3.png](https://www.mailvor.com/opensource/setting/img/img3.png)

### 4. 修改淘宝联盟百川安全图

- 进入阿里妈妈<https://pub.alimama.com/third/manage/record/site.htm>![img.png](https://www.mailvor.com/opensource/setting/img/baichuan/img.png)![img_1.png](https://www.mailvor.com/opensource/setting/img/baichuan/img_1.png)![img_2.png](https://www.mailvor.com/opensource/setting/img/baichuan/img_2.png)![img_3.png](https://www.mailvor.com/opensource/setting/img/baichuan/img_3.png)![img_4.png](https://www.mailvor.com/opensource/setting/img/baichuan/img_4.png)![img_5.png](https://www.mailvor.com/opensource/setting/img/baichuan/img_5.png)![img_6.png](https://www.mailvor.com/opensource/setting/img/baichuan/img_6.png)![img_7.png](https://www.mailvor.com/opensource/setting/img/baichuan/img_7.png)

### 5. 修改极光推送配置

![img4.png](https://www.mailvor.com/opensource/setting/img/img4.png)
