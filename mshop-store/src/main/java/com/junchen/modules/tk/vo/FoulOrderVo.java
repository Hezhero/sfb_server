package com.junchen.modules.tk.vo;

import com.alibaba.fastjson.annotation.JSONField;
import com.junchen.utils.StringUtils;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;

/**
 * 大淘客「违规订单查询」单条记录（官方 {@code get-foul-order-list} 的 {@code data.results[]}）。
 *
 * <p>官方文档 https://www.dataoke.com/pmc/api-d.html?id=86 的响应字段：</p>
 *
 * <pre>
 * {
 *   "relation_id": 2688182037,             // 渠道关系 id
 *   "settle_month": "202112",              // 结算月份
 *   "punish_status": "0",                  // 处罚状态：0 冻结 / 1 解冻
 *   "violation_type": "店铺淘宝客",         // 处罚类型：1 店铺淘宝客 / 2 订单虚假交易
 *   "tk_trade_create_time": "2021-12-19 10:09:03",
 *   "tb_trade_id": "2345504148926622436",  // 子订单号 ← 匹配本地订单用这个
 *   "tk_adzone_id": "110710450164",
 *   "tk_site_id": "1949600008",
 *   "tk_pub_id": "314500175",
 *   "day": "11"                            // 下单距今多少天
 * }
 * </pre>
 *
 * <p><b>两个类型陷阱</b>（官方文档自己就不一致，这里都做了兼容）：</p>
 * <ul>
 *   <li>{@code violation_type} 字段表里标 String、示例取值写 {@code "1"/"2"}，
 *       但同一份文档的「返回示例」给的是中文 {@code "店铺淘宝客"}。两种形态都要能认，
 *       否则一半的违规类型会记成 null。</li>
 *   <li>{@code punish_status} 是字符串 {@code "0"/"1"}，不是数字也不是布尔；
 *       取值来自字符串，不能按数字反序列化后比较。</li>
 * </ul>
 */
@lombok.Data
@ApiModel(value = "FoulOrderVo", description = "违规订单单条记录")
public class FoulOrderVo implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 处罚状态：冻结 */
    public static final String PUNISH_FROZEN = "0";
    /** 处罚状态：解冻 */
    public static final String PUNISH_RELEASED = "1";

    /** 处罚类型编码：店铺淘宝客 */
    public static final int VIOLATION_SHOP_TBK = 1;
    /** 处罚类型编码：订单虚假交易 */
    public static final int VIOLATION_FAKE_TRADE = 2;

    /** 子订单号，用于匹配本地订单（对应 mw_order_tb.trade_parent_id，见 FoulOrderService 注释） */
    @JSONField(name = "tb_trade_id")
    @ApiModelProperty(value = "子订单号")
    private String tbTradeId;

    /** 渠道关系 id */
    @JSONField(name = "relation_id")
    @ApiModelProperty(value = "渠道关系id")
    private Long relationId;

    /** 处罚类型原始值（可能是 "1"/"2"，也可能是中文） */
    @JSONField(name = "violation_type")
    @ApiModelProperty(value = "处罚类型原始值")
    private String violationType;

    /** 处罚状态："0" 冻结 / "1" 解冻 */
    @JSONField(name = "punish_status")
    @ApiModelProperty(value = "处罚状态：0冻结 1解冻")
    private String punishStatus;

    /** 结算月份，形如 202112 */
    @JSONField(name = "settle_month")
    @ApiModelProperty(value = "结算月份")
    private String settleMonth;

    /** 淘客订单创建时间，形如 2021-12-19 10:09:03 */
    @JSONField(name = "tk_trade_create_time")
    @ApiModelProperty(value = "淘客订单创建时间")
    private String tkTradeCreateTime;

    /** pid 里的 adzoneid */
    @JSONField(name = "tk_adzone_id")
    @ApiModelProperty(value = "pid里的adzoneid")
    private String tkAdzoneId;

    /** pid 里的 siteid */
    @JSONField(name = "tk_site_id")
    @ApiModelProperty(value = "pid里的siteid")
    private String tkSiteId;

    /** pid 里的 pubid */
    @JSONField(name = "tk_pub_id")
    @ApiModelProperty(value = "pid里的pubid")
    private String tkPubId;

    /** 下单距今多少天 */
    @JSONField(name = "day")
    @ApiModelProperty(value = "下单距今多少天")
    private String day;

    /**
     * 是否已解冻。
     *
     * <p>只有上游明确给出 {@code "1"} 才算解冻。其余一切情况（缺失、空串、无法识别的值）
     * 都返回 false，也就是按冻结处理 —— 结算方向决定取舍：漏判解冻只是晚几天恢复结算，
     * 误判解冻会直接把平台已扣的佣金发出去。</p>
     */
    @JSONField(serialize = false, deserialize = false)
    public boolean isReleased() {
        return punishStatus != null && PUNISH_RELEASED.equals(punishStatus.trim());
    }

    /**
     * 是否被冻结（处罚生效中）。
     *
     * <p>与 {@link #isReleased()} 严格互斥：上游没给状态时按冻结处理。</p>
     */
    @JSONField(serialize = false, deserialize = false)
    public boolean isFrozen() {
        return !isReleased();
    }

    /**
     * 处罚类型编码：1 店铺淘宝客 / 2 订单虚假交易；无法识别时返回 null（不猜测）。
     *
     * <p>兼容上游两种形态：数字串 {@code "1"/"2"} 与 中文 {@code "店铺淘宝客"/"订单虚假交易"}。</p>
     */
    @JSONField(serialize = false, deserialize = false)
    public Integer getViolationTypeCode() {
        if (StringUtils.isBlank(violationType)) {
            return null;
        }
        String v = violationType.trim();
        if ("1".equals(v) || "店铺淘宝客".equals(v)) {
            return VIOLATION_SHOP_TBK;
        }
        if ("2".equals(v) || "订单虚假交易".equals(v)) {
            return VIOLATION_FAKE_TRADE;
        }
        return null;
    }

    /** 处罚类型中文名，用于落库备注与后台展示；无法识别时给出「未知违规类型」 */
    @JSONField(serialize = false, deserialize = false)
    public String getViolationTypeText() {
        Integer code = getViolationTypeCode();
        if (code == null) {
            return "未知违规类型";
        }
        return code == VIOLATION_SHOP_TBK ? "店铺淘宝客" : "订单虚假交易";
    }
}
