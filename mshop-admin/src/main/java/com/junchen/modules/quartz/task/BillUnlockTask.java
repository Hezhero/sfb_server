package com.junchen.modules.quartz.task;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.junchen.modules.tk.service.FoulOrderService;
import com.junchen.modules.user.domain.MwUserBill;
import com.junchen.modules.user.service.MwUserBillService;
import com.junchen.modules.user.service.MwUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.Set;

/**
 * 解锁账单（真正的结算闸门）。
 *
 * <p>把到期的待解锁账单转成用户余额。工单 04 在这里加了一道违规闸门：
 * 淘宝联盟判定违规并冻结佣金的订单，其账单在冻结期间<b>暂缓解锁</b>——
 * 否则平台已经把佣金扣掉了，本地却照常把钱发给推客，就是真实资损。</p>
 *
 * <p><b>为什么是「暂缓」而不是「作废」</b>：退款冲正
 * （{@code OrderRefundTask} → {@code decHbAndRefundOrder}）判断账单该不该扣钱时，
 * 依据的是账单的 {@code unlock_status}。如果违规逻辑抢先把待解锁账单置为失效，
 * 之后真实退款发生时冲正会误判为「已解锁」而直接从余额扣钱——用户从来没拿到过
 * 这笔钱却被扣，重复惩罚。暂缓不动账单状态，两种情形都保持正确：</p>
 * <ul>
 *   <li>一直冻结 → 账单一直躺在待解锁队列里，钱不发出；</li>
 *   <li>上游解冻 → 下一轮正常解锁；</li>
 *   <li>期间真实退款 → 冲正看到的仍是待解锁，按原逻辑置失效且不扣余额。</li>
 * </ul>
 *
 * @author Zheng Jie
 * @date 2022-10-04
 */
@Slf4j
@Component
public class BillUnlockTask {
    @Resource
    private MwUserBillService userBillService;

    @Resource
    private MwUserService userService;

    @Resource
    private FoulOrderService foulOrderService;

    protected void run(String paramStr) {
        //淘宝 京东 拼多多 抖音 唯品会都需执行
        //淘宝 查找30天内 bind=1 并且tkStatus=13的订单，扣除奖励红包， 积分记录, 订单bind改为2
        //todo 其他平台暂未验证
        JSONObject jsonObject = JSON.parseObject(paramStr);
//        Integer day = jsonObject.getInteger("day");
        Integer limit = jsonObject == null ? null : jsonObject.getInteger("limit");
        if(limit == null || limit == 0) {
            limit = 20;
        }
        List<MwUserBill> userBills = userBillService.getUnlockList(limit);
        //违规冻结订单的账单本轮暂缓（不动状态，解冻后自然恢复）
        Set<String> blocked = foulOrderService.blockedBillIds(userBills);
        //更新余额
        for(MwUserBill userBill : userBills) {
            if(blocked.contains(userBill.getLinkId())) {
                log.debug("账单所属订单违规冻结，暂缓解锁 uid={} linkId={}",
                        userBill.getUid(), userBill.getLinkId());
                continue;
            }
            //设置为
            userBill.setUnlockStatus(0);
            userBillService.saveOrUpdate(userBill);
            userService.incMoney(userBill.getUid(), userBill.getNumber());
        }

    }

}
