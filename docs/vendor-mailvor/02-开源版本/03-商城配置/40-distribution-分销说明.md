> 来源: https://syh.hzhero.cn/02.%E5%BC%80%E6%BA%90%E7%89%88%E6%9C%AC/03.%E5%95%86%E5%9F%8E%E9%85%8D%E7%BD%AE/40.distribution.html
> 抓取时间: 2026-10-29 22:00:10
> 版权归济南隽宸信息科技有限公司所有，仅供本地部署参考，请勿外传

# 分销说明

### 1. 分销层级：2-3级

设置分销级别，打开管理后台“会员管理”->“会员”->“APP基础信息配置” ![img.png](https://syh.hzhero.cn/opensource/setting/img/distribution/img.png)

### 2. 分销说明：目前三盈会采用的多会员机制

- 例如淘店、京店等（也有其他叫法，比如淘星选会员），可以付费升级，也可以邀请好友升级（目前移除） 每个店铺对应各电商平台的分佣

### 3. 分销设置：会员管理->会员等级

- 每个店铺默认等级=3，升级后等级=5，不要随意更改，要修改，需同步修改后台，对应的数据库表：mw\_system\_user\_level ![img_1.png](https://syh.hzhero.cn/opensource/setting/img/distribution/img_1.png)

### 4. 分销比例：会员管理->会员等级

- 可以设置每个店铺自身、1级和2级分佣比例，不存在的店铺比如美团外卖走淘宝的分佣比例 ![img_2.png](https://syh.hzhero.cn/opensource/setting/img/distribution/img_2.png)
- 也可以单独设置分销比例：分销配置->【拆红包】比例（一般不用） ![img_3.png](https://syh.hzhero.cn/opensource/setting/img/distribution/img_3.png)

### 5. 分销绑定

- 设置登录强制邀请码：分销管理->分销配置，开启登录强制邀请码，app登录后会跳转到邀请码页面，关闭会直接跳过该页面 ![img_4.png](https://syh.hzhero.cn/opensource/setting/img/distribution/img_4.png)
- 设置登录强制邀请码：会员管理->会员，点击按钮"APP基础信息配置"，开启强制邀请码，登录必须输入邀请码，选择跳过，在邀请码界面会出现按钮可跳过，只要没有绑定上级，每次登录会跳转该页面 ![img_5.png](https://syh.hzhero.cn/opensource/setting/img/distribution/img_5.png)

### 6. 分销对应数据库字段

- 上级用户的字段：mw\_user的spread\_uid和spread\_time ![img_6.png](https://syh.hzhero.cn/opensource/setting/img/distribution/img_6.png)
- 用户等级的字段:mw\_user的level level\_jd等，过期时间的字段expired expired\_jd等 ![img_7.png](https://syh.hzhero.cn/opensource/setting/img/distribution/img_7.png)![img_8.png](https://syh.hzhero.cn/opensource/setting/img/distribution/img_8.png)
