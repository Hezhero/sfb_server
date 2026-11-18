package com.junchen.modules.tk.param;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.junchen.modules.tk.util.DtkScrollPager;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;

/**
 * 大淘客 scroll_id 分页类接口的公共参数基类（工单 05）。
 *
 * <p><b>为什么不用现成的 {@link BaseParam}</b>：{@code BaseParam} 的 {@code pageId} 是
 * {@code Integer}，而官方 scroll_id 分页下 {@code pageId} 是上游签发的不透明字符串
 * （形如 {@code bcc699f1fc4ecaf5}），一旦按 Integer 传递，第二页起就会解析失败。
 * 字段名相同但语义不同，必须分开定义。</p>
 *
 * <p><b>pageSize 白名单</b>：官方三个接口一致声明「每页条数仅支持输入 10,50,100,200，
 * 最大值 200，若小于 10 则按 10 条处理」。上游对非白名单值的行为未在文档中承诺，
 * 因此本地先归一到白名单值再发请求，避免因参数非法让整批任务失败。</p>
 */
@Data
@ApiModel
public abstract class DtkScrollPageParam implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 官方允许的每页条数，升序 */
    public static final List<Integer> ALLOWED_PAGE_SIZES =
            Collections.unmodifiableList(Arrays.asList(10, 50, 100, 200));

    /** 官方上限 */
    public static final int MAX_PAGE_SIZE = 200;

    /** 官方下限 */
    public static final int MIN_PAGE_SIZE = 10;

    @ApiModelProperty(value = "分页id，首次传1，后续传上次响应的pageId（scroll_id 分页）")
    private String pageId = DtkScrollPager.FIRST_PAGE_ID;

    @ApiModelProperty(value = "每页条数，仅支持 10/50/100/200")
    private Integer pageSize;

    /**
     * 把 pageSize 归一到官方白名单。
     *
     * <p>规则：小于等于 10 → 10；大于等于 200 → 200；其余取「不小于请求值的最小白名单值」
     * （30→50、80→100、150→200）。这样既不会因为非法值被上游拒绝，也不会比调用方
     * 预期的每页条数更小而多翻几页。</p>
     */
    public static int normalizePageSize(Integer requested) {
        if (requested == null || requested <= MIN_PAGE_SIZE) {
            return MIN_PAGE_SIZE;
        }
        if (requested >= MAX_PAGE_SIZE) {
            return MAX_PAGE_SIZE;
        }
        for (Integer allowed : ALLOWED_PAGE_SIZES) {
            if (allowed >= requested) {
                return allowed;
            }
        }
        return MAX_PAGE_SIZE;
    }

    /** 归一后的每页条数；未指定时用调用方给的默认值 */
    public int effectivePageSize(int defaultSize) {
        return normalizePageSize(pageSize == null ? defaultSize : pageSize);
    }

    /** 归一后的游标；null/空白一律按首屏 "1" 处理，避免签名里出现空值 */
    public String effectivePageId() {
        if (pageId == null || pageId.trim().isEmpty()) {
            return DtkScrollPager.FIRST_PAGE_ID;
        }
        return pageId.trim();
    }

    /**
     * 转成上游请求参数（有序，签名依赖字典序）。
     *
     * <p>沿用本类库既有做法：字段名即上游参数名，用 fastjson 直接转，
     * 避免每加一个接口就手写一遍 put。转完再覆盖分页两项——分页参数必须始终生效，
     * 不能被默认值或空白值带偏。</p>
     *
     * @param defaultSize 未显式指定 pageSize 时该接口的官方默认值
     */
    public TreeMap<String, Object> toParamMap(int defaultSize) {
        // 先拷一份并把分页参数改写成归一值，再序列化，保证两个字段一定出现在参数里
        TreeMap<String, Object> map;
        try {
            map = JSON.parseObject(JSON.toJSONString(this),
                    new TypeReference<TreeMap<String, Object>>() {
                    });
        } catch (RuntimeException e) {
            map = new TreeMap<>();
        }
        if (map == null) {
            map = new TreeMap<>();
        }
        map.put("pageId", effectivePageId());
        map.put("pageSize", effectivePageSize(defaultSize));
        return map;
    }
}
