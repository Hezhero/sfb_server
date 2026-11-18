package com.junchen.modules.order.service;

import com.junchen.modules.tk.domain.TkOrder;
import com.junchen.modules.tk.service.JunchenDyOrderService;
import com.junchen.modules.tk.service.JunchenEleOrderService;
import com.junchen.modules.tk.service.JunchenJdOrderService;
import com.junchen.modules.tk.service.JunchenMtOrderService;
import com.junchen.modules.tk.service.JunchenPddOrderService;
import com.junchen.modules.tk.service.JunchenTbOrderService;
import com.junchen.modules.tk.service.JunchenVipOrderService;
import org.springframework.stereotype.Component;

import java.util.function.BiConsumer;

import java.util.List;
import java.util.function.Function;

/**
 * 平台订单注册表：把「哪些平台参与退款/风控/计费」从每个文件逐字段枚举，
 * 收成一份可遍历的 adapter 清单。
 *
 * <p><b>为什么需要</b>：此前 7 个聚合文件各自写死 5~6 个 {@code Junchen*OrderService}
 * 字段，每个文件漏的平台还不一样（Ele 全漏、Mt 部分漏），漏接不报错、不测试失败，
 * 只是那部分订单永远不参与退款冲正/计费。把清单收成一处后，新增平台只需加一个
 * adapter，漏接在多处会立刻显现。</p>
 *
 * <p>本类只提供 seam 与 adapter 定义；具体聚合点（退款/风控/计费）遍历
 * {@link #refundableSources(...)} 即可，不再逐字段注入。</p>
 */
@Component
public class PlatformOrderRegistry {

    /**
     * 一个平台在「可退款订单」这个 seam 上的 adapter。
     * 把各平台不同的 Service 类型与 domain 类型收进统一接口。
     */
    public static final class OrderSource {
        private final String platform;
        private final Class<? extends TkOrder> domainType;
        private final Function<Integer, List<? extends TkOrder>> finder;
        private final BiConsumer<SuStoreOrderService, TkOrder> refundAction;

        private OrderSource(String platform, Class<? extends TkOrder> domainType,
                            Function<Integer, List<? extends TkOrder>> finder,
                            BiConsumer<SuStoreOrderService, TkOrder> refundAction) {
            this.platform = platform;
            this.domainType = domainType;
            this.finder = finder;
            this.refundAction = refundAction;
        }

        public String platform() { return platform; }
        public Class<? extends TkOrder> domainType() { return domainType; }

        public List<? extends TkOrder> findRefundable(int prior) {
            return finder.apply(prior);
        }

        /** 调用对应平台类型的 decHbAndRefundOrder 重载 */
        public void refund(SuStoreOrderService svc, TkOrder order) {
            refundAction.accept(svc, order);
        }

        public static OrderSource tb(String name, JunchenTbOrderService svc) {
            return new OrderSource(name, com.junchen.modules.tk.domain.JunchenTbOrder.class,
                    svc::getRefundAndBindOrderList,
                    (s, o) -> s.decHbAndRefundOrder(o.getUid(), (com.junchen.modules.tk.domain.JunchenTbOrder) o));
        }
        public static OrderSource jd(String name, JunchenJdOrderService svc) {
            return new OrderSource(name, com.junchen.modules.tk.domain.JunchenJdOrder.class,
                    svc::getRefundAndBindOrderList,
                    (s, o) -> s.decHbAndRefundOrder(o.getUid(), (com.junchen.modules.tk.domain.JunchenJdOrder) o));
        }
        public static OrderSource pdd(String name, JunchenPddOrderService svc) {
            return new OrderSource(name, com.junchen.modules.tk.domain.JunchenPddOrder.class,
                    svc::getRefundAndBindOrderList,
                    (s, o) -> s.decHbAndRefundOrder(o.getUid(), (com.junchen.modules.tk.domain.JunchenPddOrder) o));
        }
        public static OrderSource vip(String name, JunchenVipOrderService svc) {
            return new OrderSource(name, com.junchen.modules.tk.domain.JunchenVipOrder.class,
                    svc::getRefundAndBindOrderList,
                    (s, o) -> s.decHbAndRefundOrder(o.getUid(), (com.junchen.modules.tk.domain.JunchenVipOrder) o));
        }
        public static OrderSource dy(String name, JunchenDyOrderService svc) {
            return new OrderSource(name, com.junchen.modules.tk.domain.JunchenDyOrder.class,
                    svc::getRefundAndBindOrderList,
                    (s, o) -> s.decHbAndRefundOrder(o.getUid(), (com.junchen.modules.tk.domain.JunchenDyOrder) o));
        }
        public static OrderSource mt(String name, JunchenMtOrderService svc) {
            return new OrderSource(name, com.junchen.modules.tk.domain.JunchenMtOrder.class,
                    svc::getRefundAndBindOrderList,
                    (s, o) -> s.decHbAndRefundOrder(o.getUid(), (com.junchen.modules.tk.domain.JunchenMtOrder) o));
        }
        public static OrderSource ele(String name, JunchenEleOrderService svc) {
            // Ele 没有 decHbAndRefundOrder 重载（历史上从未参与退款冲正）。
            // 注册表保留它以便「查询退款单」的聚合点覆盖全平台，但退款动作为空操作——
            // 若将来要支持 Ele 退款，需先在 SuStoreOrderService 补对应重载。
            return new OrderSource(name, com.junchen.modules.tk.domain.JunchenEleOrder.class,
                    svc::getRefundAndBindOrderList,
                    (s, o) -> org.slf4j.LoggerFactory.getLogger(PlatformOrderRegistry.class)
                            .warn("饿了么订单退款冲正尚未实现，平台={}", name));
        }
    }

    /** 所有可退款平台的 adapter。新增平台只需在此加一行。 */
    public static List<OrderSource> refundableSources(
            JunchenTbOrderService tb, JunchenJdOrderService jd, JunchenPddOrderService pdd,
            JunchenVipOrderService vip, JunchenDyOrderService dy, JunchenMtOrderService mt,
            JunchenEleOrderService ele) {
        return List.of(
                OrderSource.tb("淘宝", tb),
                OrderSource.jd("京东", jd),
                OrderSource.pdd("拼多多", pdd),
                OrderSource.vip("唯品会", vip),
                OrderSource.dy("抖音", dy),
                OrderSource.mt("美团", mt),
                OrderSource.ele("饿了么", ele)
        );
    }
}
