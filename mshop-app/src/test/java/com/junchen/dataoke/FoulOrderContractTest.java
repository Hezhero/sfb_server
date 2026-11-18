package com.junchen.dataoke;

import com.alibaba.fastjson.JSON;
import com.junchen.modules.tk.domain.JunchenTbOrder;
import com.junchen.modules.tk.service.FoulOrderService;
import com.junchen.modules.tk.vo.FoulOrderResVo;
import com.junchen.modules.tk.vo.FoulOrderVo;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

/**
 * 违规订单的解析与结算排除契约测试。
 *
 * <p>不依赖数据库，只验证外部报文解析与纯函数判定——这是资金相关逻辑中
 * 最容易写错、也最容易用单测锁住的部分。</p>
 */
public class FoulOrderContractTest {

    @Test
    public void parsesOfficialResponseFields() {
        String json = "{\"code\":0,\"msg\":\"成功\",\"data\":{\"page_no\":1,\"page_size\":100,"
                + "\"total_count\":1,\"results\":[{"
                + "\"relation_id\":2688182037,\"settle_month\":\"202112\","
                + "\"punish_status\":\"0\",\"violation_type\":\"1\","
                + "\"tb_trade_id\":\"2345504148926622436\","
                + "\"tk_trade_create_time\":\"2021-12-19 10:09:03\","
                + "\"tk_adzone_id\":\"110710450164\",\"tk_site_id\":\"1949600008\","
                + "\"tk_pub_id\":\"314500175\",\"day\":\"11\"}]}}";
        FoulOrderResVo res = JSON.parseObject(json, FoulOrderResVo.class);
        assertNotNull(res);
        assertEquals(Integer.valueOf(0), res.getCode());
        assertNotNull(res.getData());
        List<FoulOrderVo> results = res.getData().getResults();
        assertEquals(1, results.size());
        FoulOrderVo vo = results.get(0);
        assertEquals("2345504148926622436", vo.getTbTradeId());
        assertEquals(Long.valueOf(2688182037L), vo.getRelationId());
        assertEquals("202112", vo.getSettleMonth());
        assertTrue(vo.isFrozen());
        assertFalse(vo.isReleased());
        assertEquals(Integer.valueOf(FoulOrderVo.VIOLATION_SHOP_TBK), vo.getViolationTypeCode());
    }

    @Test
    public void violationTypeAcceptsChineseAndNumericForms() {
        FoulOrderVo numeric = new FoulOrderVo();
        numeric.setViolationType("2");
        assertEquals(Integer.valueOf(FoulOrderVo.VIOLATION_FAKE_TRADE), numeric.getViolationTypeCode());

        FoulOrderVo chinese = new FoulOrderVo();
        chinese.setViolationType("店铺淘宝客");
        assertEquals(Integer.valueOf(FoulOrderVo.VIOLATION_SHOP_TBK), chinese.getViolationTypeCode());

        FoulOrderVo unknown = new FoulOrderVo();
        unknown.setViolationType("不明原因");
        assertNull(unknown.getViolationTypeCode());
        assertEquals("未知违规类型", unknown.getViolationTypeText());
    }

    @Test
    public void missingPunishStatusIsTreatedAsFrozen() {
        // 结算方向：宁可晚几天恢复，也不能把平台已扣的佣金发出去
        FoulOrderVo vo = new FoulOrderVo();
        assertTrue(vo.isFrozen());
        assertFalse(vo.isReleased());

        vo.setPunishStatus("1");
        assertTrue(vo.isReleased());
        assertFalse(vo.isFrozen());
    }

    @Test
    public void releasedOrderIsNotBlockedFromSettlement() {
        JunchenTbOrder normal = new JunchenTbOrder();
        assertFalse(FoulOrderService.isFoulBlocked(normal));

        JunchenTbOrder frozen = new JunchenTbOrder();
        frozen.setFoulStatus(FoulOrderService.FOUL_STATUS_FROZEN);
        assertTrue(FoulOrderService.isFoulBlocked(frozen));

        JunchenTbOrder released = new JunchenTbOrder();
        released.setFoulStatus(FoulOrderService.FOUL_STATUS_NORMAL);
        assertFalse(FoulOrderService.isFoulBlocked(released));
    }

    @Test
    public void nullSafeNormalizesNullList() {
        assertNotNull(FoulOrderService.nullSafe(null));
        assertTrue(FoulOrderService.nullSafe(null).isEmpty());
        List<String> items = Arrays.asList("a", "b");
        assertEquals(items, FoulOrderService.nullSafe(items));
    }
}
