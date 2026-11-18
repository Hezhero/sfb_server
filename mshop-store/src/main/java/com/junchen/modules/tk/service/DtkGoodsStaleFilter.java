package com.junchen.modules.tk.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.junchen.modules.tk.vo.DtkScrollPageDataVo;
import com.junchen.modules.tk.vo.GoodsSearchDataVo;
import com.junchen.modules.tk.vo.GoodsSearchVo;
import com.junchen.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * C 端商品查询的失效过滤（工单 05）。
 *
 * <p><b>为什么要过滤</b>：{@code /tao/goods/list} 与 {@code /tao/goods/search} 是
 * C 端商品列表的直接来源，它们实时把上游结果透传给 App。上游对这些查询本身
 * 只返回在架商品，但存在两个时间差会让已下架商品混进来：</p>
 * <ol>
 *   <li>上游结果里可能有「刚下架但尚未从索引剔除」的商品</li>
 *   <li>本地 app 会缓存过上游响应（首页卡片缓存 24h），缓存里可能带着已下架商品</li>
 * </ol>
 * <p>因此过滤必须放在响应出口处按本地失效库剔除，而不是指望上游。</p>
 *
 * <p><b>为什么用「本地失效 id 集合」而不是改上游请求</b>：上游没有「排除指定 id」
 * 这类参数，过滤只能本地做。失效商品占整体比例很低（通常千分之几），
 * 拉一次全量失效 id 到内存做差集，比逐条查库便宜得多。</p>
 *
 * <p><b>匹配键</b>：与本工单的失效检测一致，一律用大淘客商品 id 匹配
 * （官方在失效接口的 {@code id} 字段上写明「用该ID进行匹配」）。</p>
 */
@Slf4j
@Service
public class DtkGoodsStaleFilter {

    /**
     * 单次加载失效 id 的上限。
     *
     * <p>失效商品是少数派，正常情况下远小于这个量级；设上限是防止某次上游异常
     * 把整库都标成失效时，把一大坨 id 拉进内存拖垮接口。</p>
     */
    private static final int STALE_ID_LIMIT = 20000;

    @Resource
    private DtkGoodsSyncService goodsSyncService;

    /**
     * 读当前失效商品 id 集合。
     *
     * <p>任何异常都返回空集合：<b>过滤失败不能连带查询失败</b>——
     * 宁可多展示一个可能已下架的商品，也不能让整个商品列表接口 500。</p>
     */
    public Set<Long> currentStaleIds() {
        try {
            List<Long> ids = goodsSyncService.listStaleDtkIds(STALE_ID_LIMIT);
            if (ids == null || ids.isEmpty()) {
                return Collections.emptySet();
            }
            return new HashSet<>(ids);
        } catch (RuntimeException e) {
            log.warn("读取失效商品 id 失败，本轮跳过过滤", e);
            return Collections.emptySet();
        }
    }

    /**
     * 过滤 {@code /tao/goods/list} 的响应。
     *
     * <p>该接口直接返回上游原始 JSON（{@code DataokeService.goodsList}），
     * 结构为 {@code {"code":0,"data":{"list":[...],"totalNum":N,"pageId":"..."}}}。
     * 过滤后同步下调 {@code totalNum}，否则前端翻页会以为后面还有被剔掉的那几条。</p>
     *
     * @param response 上游原始响应，允许为 null
     * @return 过滤后的响应；入参为 null 时返回 null
     */
    public JSONObject filterGoodsList(JSONObject response) {
        if (response == null) {
            return null;
        }
        Set<Long> stale = currentStaleIds();
        if (stale.isEmpty()) {
            return response;
        }
        JSONObject data = response.getJSONObject("data");
        if (data == null) {
            return response;
        }
        JSONArray list = data.getJSONArray("list");
        if (list == null || list.isEmpty()) {
            return response;
        }

        JSONArray kept = new JSONArray(list.size());
        int removed = 0;
        for (int i = 0; i < list.size(); i++) {
            JSONObject item = list.getJSONObject(i);
            if (item == null) {
                continue;
            }
            Long dtkId = item.getLong("id");
            if (dtkId != null && stale.contains(dtkId)) {
                removed++;
                continue;
            }
            kept.add(item);
        }
        if (removed == 0) {
            return response;
        }

        data.put("list", kept);
        // 总数同步下调：本页剔除 N 条，总数也少 N 条，避免前端按原总数翻页
        Long total = data.getLong("totalNum");
        if (total != null) {
            data.put("totalNum", Math.max(0L, total - removed));
        }
        log.debug("商品列表已过滤失效商品 removed={} remain={}", removed, kept.size());
        return response;
    }

    /**
     * 过滤 {@code /tao/goods/search} 的强类型响应。
     *
     * <p>该接口返回 {@code DataokeResVo<GoodsSearchDataVo>}，与 goods/list 的区别是
     * 强类型包装。同样下调 {@code totalNum}。</p>
     *
     * @param response 上游响应，允许为 null
     * @return 过滤后的响应；入参为 null 时返回 null
     */
    public GoodsSearchDataVo filterGoodsSearch(GoodsSearchDataVo response) {
        if (response == null) {
            return null;
        }
        Set<Long> stale = currentStaleIds();
        if (stale.isEmpty() || response.getList() == null || response.getList().isEmpty()) {
            return response;
        }

        List<GoodsSearchVo> kept = new ArrayList<>(response.getList().size());
        int removed = 0;
        for (GoodsSearchVo item : response.getList()) {
            if (item == null) {
                continue;
            }
            if (item.getId() != null && stale.contains(item.getId())) {
                removed++;
                continue;
            }
            kept.add(item);
        }
        if (removed == 0) {
            return response;
        }

        response.setList(kept);
        Integer total = response.getTotalNum();
        if (total != null) {
            response.setTotalNum(Math.max(0, total - removed));
        }
        log.debug("商品搜索已过滤失效商品 removed={} remain={}", removed, kept.size());
        return response;
    }

    /** 判断单个大淘客商品 id 是否已失效（商品详情页可用） */
    public boolean isStale(Long dtkId) {
        if (dtkId == null) {
            return false;
        }
        return currentStaleIds().contains(dtkId);
    }
}
