package com.junchen.modules.pay.rest;

import com.junchen.modules.pay.domain.MwPayChannel;
import com.junchen.modules.pay.domain.MwPayCompany;
import com.junchen.modules.pay.enums.PayChannelEnum;
import com.junchen.modules.pay.service.MwPayChannelService;
import com.junchen.modules.pay.service.MwPayCompanyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 支付通道选型与扣减额度的集成测试。
 *
 * <p>测试数据隔离：通道与主体主键一律交给数据库自增分配，不再写死 1/2/3…，
 * 避免与库中已有通道（含历史软删除行）发生主键冲突；整个用例在事务中执行，
 * 结束后回滚，不残留数据。
 */
@SpringBootTest
@Transactional
class PayControllerTest {
    @Resource
    private MwPayChannelService payChannelService;

    @Resource
    private MwPayCompanyService payCompanyService;

    private final List<Long> channelIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        // 主体主键由数据库分配
        MwPayCompany payCompany1 = MwPayCompany.builder()
                .company("苏州xxx公司")
                .phone("ccc")
                .sealUrl("bbb")
                .licenseUrl("aaa")
                .build();
        payCompanyService.save(payCompany1);

        MwPayCompany payCompany2 = MwPayCompany.builder()
                .company("苏州yyy公司")
                .phone("ccc")
                .sealUrl("bbb")
                .licenseUrl("aaa")
                .build();
        payCompanyService.save(payCompany2);

        // 生成10条通道 每条额度10万 5条支付宝 5条微信
        // 通道主键同样交给数据库分配，避免与既有数据主键碰撞
        for (long i = 0; i < 5; i++) {
            MwPayChannel payChannel = MwPayChannel.builder()
                    .channelName("支付宝" + i)
                    .channelKey(PayChannelEnum.ADAPAY.getKey())
                    .notifyUrl("abc")
                    .maxAmount(100000d)
                    .amount(100000d)
                    .status(8)
                    .companyId(payCompany1.getId())
                    .type(2)
                    .build();
            payChannelService.save(payChannel);
            channelIds.add(payChannel.getId());
        }
        for (long i = 0; i < 5; i++) {
            MwPayChannel payChannel = MwPayChannel.builder()
                    .channelKey(PayChannelEnum.ADAPAY.getKey())
                    .channelName("微信" + i)
                    .notifyUrl("abc")
                    .maxAmount(100000d)
                    .amount(100000d)
                    .status(8)
                    .companyId(payCompany1.getId())
                    .type(7)
                    .build();
            payChannelService.save(payChannel);
            channelIds.add(payChannel.getId());
        }
    }

    @Test
    void aliPay() {
        // 通道选择：按支付类型挑选可用通道
        MwPayChannel selected = payChannelService.channel(1L, 2);
        org.junit.jupiter.api.Assertions.assertNotNull(selected, "应能选出一条可用的支付通道");

        // 扣减额度：扣减后剩余额度应相应减少
        BigDecimal price = BigDecimal.valueOf(100);
        MwPayChannel updated = payChannelService.decPrice(price, selected.getId());
        org.junit.jupiter.api.Assertions.assertNotNull(updated, "扣减额度后应返回通道信息");
        org.junit.jupiter.api.Assertions.assertEquals(
                selected.getAmount() - price.doubleValue(), updated.getAmount(), 0.001,
                "扣减后剩余额度应减少对应金额");
    }
}
