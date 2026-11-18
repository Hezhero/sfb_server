> 来源: https://syh.hzhero.cn/02.%E5%BC%80%E6%BA%90%E7%89%88%E6%9C%AC/01.%E5%BC%80%E5%8F%91%E6%96%87%E6%A1%A3/70.other-prepare.html
> 抓取时间: 2026-10-29 22:00:07
> 版权归济南隽宸信息科技有限公司所有，仅供本地部署参考，请勿外传

# 常见问题

### 1.redis外网访问

- 如果redis由宝塔创建，可以在宝塔设置redis外网访问。 ![](https://syh.hzhero.cn/opensource/develop/img/redis1.png)![](https://syh.hzhero.cn/opensource/develop/img/redis2.png)

### 2.服务端易宝支付yop报错解决

- 解压缩`lib/yop-java-sdk-4.3.3-20230421155139-saas.zip`![](https://syh.hzhero.cn/opensource/develop/img/yop1.png)
- 使用ide单独打开该项目，然后`maven clean install`即可，也可以使用idea的窗口操作maven ![](https://syh.hzhero.cn/opensource/develop/img/yop2.png)

### 3.服务端启动循环引用报错

- 一般发生在macos系统，建议使用jdk17和maven3.8.1，然后重新mvn clean install即可解决 ![](https://syh.hzhero.cn/opensource/develop/img/loop.png)

### 4.我用Windows可以开发App吗

- Windows可以开发Flutter App，但是建议用Mac，因为Windows不能打包iOS。当然也可以用其他途径打包iOS，可以自行上网搜索。

### 5.服务端meituan-union-java报错解决

- 解压缩`lib/meituan-union-java.zip`![](https://syh.hzhero.cn/opensource/develop/img/meituan1.png)
- 使用ide单独打开该项目，然后`maven clean install`即可，也可以使用idea的窗口操作maven ![](https://syh.hzhero.cn/opensource/develop/img/meituan2.png)
