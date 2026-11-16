package com.mailvor.modules.tk.vo;

import com.alibaba.fastjson.annotation.JSONField;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 大淘客「违规订单查询」响应（官方 {@code get-foul-order-list}）。
 *
 * <p>官方文档 https://www.dataoke.com/pmc/api-d.html?id=86 的完整响应体：</p>
 *
 * <pre>
 * {
 *   "requestId": "bb0b061b1deddcfd722cf579b4d7672e",
 *   "time": 1642731609620,
 *   "code": 0,                       // 0 成功；1 参数错误；-1 服务器错误
 *   "msg": "成功",
 *   "data": {
 *     "page_no": 1,
 *     "page_size": 100,
 *     "total_count": 97,
 *     "results": [ ... ]             // 见 {@link FoulOrderVo}
 *   }
 * }
 * </pre>
 *
 * <p>分页是<b>页码式</b>（{@code page_no} 从 1 开始），不是游标式 —— 拉下一页只需
 * {@code pageNo + 1}，与商品同步那三个 scroll_id 接口不同。</p>
 *
 * <p><b>注意</b>：内部承载分页数据的类命名为 {@link Page} 而非 {@code Data}，
 * 因为 {@code Data} 会遮蔽 Lombok 的 {@code @Data} 注解，导致注解处理器报
 * 「无法转换为 java.lang.annotation.Annotation」，进而让整个编译单元的 Lombok
 * 生成全部失效（表现是几百个「找不到 setter」的级联错误）。</p>
 */
@Data
@ApiModel(value = "FoulOrderResVo", description = "违规订单查询响应")
public class FoulOrderResVo implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 上游 code，0 表示成功 */
    @ApiModelProperty(value = "上游返回码，0成功")
    private Integer code;

    @ApiModelProperty(value = "上游返回描述")
    private String msg;

    private Page data;

    /** 上游是否返回成功 */
    @JSONField(serialize = false, deserialize = false)
    public boolean isSuccess() {
        return code != null && code == 0;
    }

    /**
     * 结果列表，永不为 null。
     *
     * <p>上游在无数据时可能省略整个 {@code data} 节点，或返回空 {@code results}，
     * 调用方不应该到处判空。</p>
     */
    @JSONField(serialize = false, deserialize = false)
    public List<FoulOrderVo> getResults() {
        if (data == null || data.getResults() == null) {
            return Collections.emptyList();
        }
        return data.getResults();
    }

    @JSONField(serialize = false, deserialize = false)
    public Integer getPageNo() {
        return data == null ? null : data.getPageNo();
    }

    @JSONField(serialize = false, deserialize = false)
    public Integer getPageSize() {
        return data == null ? null : data.getPageSize();
    }

    @JSONField(serialize = false, deserialize = false)
    public Integer getTotalCount() {
        return data == null ? null : data.getTotalCount();
    }

    @Data
    @ApiModel(value = "FoulOrderResVo.Page", description = "违规订单分页数据")
    public static class Page implements Serializable {
        private static final long serialVersionUID = 1L;

        /** 当前页码，从 1 开始 */
        @JSONField(name = "page_no")
        @ApiModelProperty(value = "当前页码")
        private Integer pageNo;

        /** 每页条数 */
        @JSONField(name = "page_size")
        @ApiModelProperty(value = "每页条数")
        private Integer pageSize;

        /** 总条数 */
        @JSONField(name = "total_count")
        @ApiModelProperty(value = "总条数")
        private Integer totalCount;

        /** 违规订单列表；上游可能返回 null */
        @JSONField(name = "results")
        @ApiModelProperty(value = "违规订单列表")
        private ArrayList<FoulOrderVo> results;
    }
}
