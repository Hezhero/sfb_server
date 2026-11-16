> 来源: https://www.mailvor.com/02.%E5%BC%80%E6%BA%90%E7%89%88%E6%9C%AC/03.%E5%95%86%E5%9F%8E%E9%85%8D%E7%BD%AE/30.settle.html
> 抓取时间: 2026-10-29 22:00:10
> 版权归苏州迈沃通信有限公司所有，仅供本地部署参考，请勿外传

# 订单结算机制

### 1. 订单结算时间设置：“会员管理”->“会员”，点击“订单解锁配置”

- 订单经过一定天数解锁，然后用户可以手动拆订单得到红包，拆红包比例无限接近于“会员等级”设置的自身拆红包比例
- 会员和普通用户拆红包单独配置
- 系统会自动根据用户的退款次数延长拆红包时间 ![img.png](https://www.mailvor.com/opensource/setting/img/settle/img.png)

### 2. 订单自动解锁，“系统工具”->"定时任务"，开启“自购订单-拆红包”

- 如果订单解锁后，用户一直没有拆，开启该任务，系统会在解锁后的两天左右自动拆，拆完自动到余额 ![img_1.png](https://www.mailvor.com/opensource/setting/img/settle/img_1.png)

### 3. 退款自动扣除余额：“系统工具”->"定时任务"，开启“订单失效-扣除余额”

- 这里有个机制，如果用户余额一直没有成功扣除，该任务每执行一次会使用户的退款次数+1，退款次数变多时，拆红包天数延长
- 如何判定退款订单：用户拆红包拿到返利后，又去退款，直接表现为订单失效，淘宝的订单为维权订单 ![img_2.png](https://www.mailvor.com/opensource/setting/img/settle/img_2.png)

### 4. 刷单检测：“系统工具”->"定时任务"，开启“A订单刷单检查任务”

- 如果用户前一天有效订单下单超过5单 并且佣金比例超过>30%，退款次数设置为10（既最大红包解锁天数）
- 目前该配置不可修改，可以通过代码修改，搜索OrderCheckConfigDto ![img_3.png](https://www.mailvor.com/opensource/setting/img/settle/img_3.png)

### 5. 清除用户退款次数：“会员管理”->“会员”

- 点击用户右测按钮“重置退款次数”重置后退款次数变为0，回归正常订单解锁时间 ![img_4.png](https://www.mailvor.com/opensource/setting/img/settle/img_4.png)
- 退款次数对应的数据库字段：mw\_user\_pool的refund ![img_5.png](https://www.mailvor.com/opensource/setting/img/settle/img_5.png)
