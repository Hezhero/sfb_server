package com.mailvor.modules.quartz.task;

import com.alibaba.fastjson.JSON;
import com.mailvor.modules.tk.param.PullGoodsByTimeParam;
import com.mailvor.modules.tk.service.DtkGoodsSyncService;
import com.mailvor.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 定时拉取商品任务 —— 工单 05 链路 3/3（{@code pull-goods-by-time}）。
 *
 * <p><b>为什么低频</b>：该接口响应字段最全（约 50 个），单页数据量大，是「价格保鲜」
 * 的主力链路但也是最重的调用。放凌晨执行（业务低峰）有两个好处：一是不和订单采集
 * 抢上游配额，二是当天上午 C 端看到的价格是刚同步过的。</p>
 *
 * <p><b>与增量任务的分工</b>：增量（{@code get-newest-goods}）负责「发现新商品」，
 * 全量（本任务）负责「刷新老商品的价格/佣金/优惠券」。二者缺一：
 * 只有增量则老商品价格永不更新，只有全量则新品要等到第二天才入库。</p>
 *
 * <p><b>参数</b>（Quartz {@code params}，JSON，全部可选）：</p>
 * <pre>
 * {"hours":24,"pageSize":200,"cid":"6","pre":0,"hasCoupon":1}
 * </pre>
 * 不写就是「最近 24 小时上架 + 每页 200 + 有券」。{@code hours} 是任务级调度参数
 * （决定回看多久上架的商品），不属于官方请求参数。
 */
@Slf4j
@Component
public class DtkGoodsPullTask {

    private static final DateTimeFormatter FF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 默认回看小时数：覆盖一天，与「每天一次」的注册频率匹配 */
    private static final int DEFAULT_HOURS = 24;

    /** 默认每页条数（官方白名单上限） */
    private static final int DEFAULT_PAGE_SIZE = 200;

    @Resource
    private DtkGoodsSyncService goodsSyncService;

    /** Quartz 反射调用入口（有 params 版本） */
    public void run(String paramStr) {
        PullGoodsByTimeParam param;
        if (StringUtils.isBlank(paramStr)) {
            param = new PullGoodsByTimeParam();
        } else {
            param = JSON.parseObject(paramStr, PullGoodsByTimeParam.class);
        }
        if (param == null) {
            param = new PullGoodsByTimeParam();
        }

        Integer hours = readInt(paramStr, "hours");
        if (StringUtils.isBlank(param.getStartTime()) || StringUtils.isBlank(param.getEndTime())) {
            LocalDateTime end = LocalDateTime.now();
            LocalDateTime start = end.minusHours(hours == null ? DEFAULT_HOURS : hours);
            param.setStartTime(start.format(FF));
            param.setEndTime(end.format(FF));
        }
        if (param.getHasCoupon() == null) {
            // 官方默认 1（有券）；显式写上，避免上游默认值变化时本地行为漂移
            param.setHasCoupon(1);
        }

        int pageSize = param.effectivePageSize(DEFAULT_PAGE_SIZE);

        log.info("定时拉取商品任务开始 start={} end={} pageSize={}",
                param.getStartTime(), param.getEndTime(), pageSize);
        try {
            DtkGoodsSyncService.SyncResult result = goodsSyncService.syncPullGoods(param, pageSize);
            log.info("定时拉取商品任务结束 pages={} fetched={} saved={} skipped={}",
                    result.getPages(), result.getFetched(), result.getSaved(), result.getSkipped());
        } catch (RuntimeException e) {
            log.error("定时拉取商品任务异常", e);
        }
    }

    /** 无参入口：Quartz 的 params 为 NULL 时走这里 */
    public void run() {
        run(null);
    }

    /** 读任务级整型参数（hours / pageSize 等），缺失或脏值返回 null */
    private static Integer readInt(String paramStr, String key) {
        if (StringUtils.isBlank(paramStr) || !paramStr.trim().startsWith("{")) {
            return null;
        }
        try {
            return JSON.parseObject(paramStr).getInteger(key);
        } catch (RuntimeException e) {
            return null;
        }
    }
}
