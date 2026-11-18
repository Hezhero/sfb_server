package com.junchen.dataoke;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.junchen.modules.tk.param.DtkScrollPageParam;
import com.junchen.modules.tk.param.GetNewestGoodsParam;
import com.junchen.modules.tk.param.PullGoodsByTimeParam;
import com.junchen.modules.tk.param.StaleGoodsParam;
import com.junchen.modules.tk.util.DataokeApi;
import org.junit.Test;

import java.util.TreeMap;

import static org.junit.Assert.*;

/**
 * 商品库同步三件套契约测试（工单 05）。
 *
 * <p><b>接缝</b>：请求参数组装（{@code toParamMap}）。上游 HTTP 走 hutool 静态方法，
 * 无法用 {@code MockRestServiceServer} 拦截，因此这里逐字段核对<b>官方参数表</b>，
 * 确保本地拼出的参数名/类型与 api-d.html?id=3 / id=11 / id=12 完全一致。
 * 参数名写错不会被编译发现，只会在线上表现为「上游返回参数错误」——所以必须钉住。</p>
 *
 * <p>响应解析在 {@link DtkGoodsContractTest} 里覆盖（那里也验证 scroll_id 分页串联）。</p>
 */
public class DtkGoodsParamContractTest {

    // ---------------------------------------------------------------
    // pageSize 白名单（三个接口共同的官方约束）
    // ---------------------------------------------------------------

    @Test
    public void pageSizeIsNormalizedIntoOfficialWhitelist() {
        // 官方：「仅支持输入 10,50,100,200，最大值 200，若小于 10 则按 10 条处理」
        assertEquals(10, DtkScrollPageParam.normalizePageSize(null));
        assertEquals(10, DtkScrollPageParam.normalizePageSize(0));
        assertEquals(10, DtkScrollPageParam.normalizePageSize(5));
        assertEquals(10, DtkScrollPageParam.normalizePageSize(10));
        assertEquals(50, DtkScrollPageParam.normalizePageSize(11));
        assertEquals(50, DtkScrollPageParam.normalizePageSize(30));
        assertEquals(50, DtkScrollPageParam.normalizePageSize(50));
        assertEquals(100, DtkScrollPageParam.normalizePageSize(80));
        assertEquals(200, DtkScrollPageParam.normalizePageSize(150));
        assertEquals(200, DtkScrollPageParam.normalizePageSize(200));
        assertEquals(200, DtkScrollPageParam.normalizePageSize(1000));
    }

    @Test
    public void pageIdDefaultsToOneAndBlankIsTreatedAsFirstPage() {
        GetNewestGoodsParam param = new GetNewestGoodsParam();
        assertEquals("1", param.effectivePageId());

        for (String blank : new String[]{null, "", "   "}) {
            param.setPageId(blank);
            assertEquals("首屏必须是 1", "1", param.effectivePageId());
            assertEquals("1", param.toParamMap().get("pageId"));
        }

        // 上游签发的游标必须原样透传（不能转数字、不能截断）
        param.setPageId(" bcc699f1fc4ecaf5 ");
        assertEquals("bcc699f1fc4ecaf5", param.effectivePageId());
        assertEquals("bcc699f1fc4ecaf5", param.toParamMap().get("pageId"));
    }

    // ---------------------------------------------------------------
    // get-newest-goods（id=3，v1.2.0）
    // ---------------------------------------------------------------

    @Test
    public void newestGoodsUsesOfficialParamNamesAndDefaults() {
        GetNewestGoodsParam param = new GetNewestGoodsParam();
        TreeMap<String, Object> map = param.toParamMap();

        // 官方必填两项，未指定时用接口默认值
        assertEquals("1", map.get("pageId"));
        assertEquals(GetNewestGoodsParam.DEFAULT_PAGE_SIZE, map.get("pageSize"));
        assertEquals("官方 pageSize 默认 10", 10, map.get("pageSize"));

        // 未设置的选填项不能出现在参数里：空白值会让签名出现 key=，上游判参数非法
        assertFalse(map.containsKey("startTime"));
        assertFalse(map.containsKey("endTime"));
        assertFalse(map.containsKey("cids"));
        assertFalse(map.containsKey("sort"));

        // 官方参数名逐项核对（改名会直接导致上游「参数错误」）
        param.setStartTime("2026-11-16 00:00:00");
        param.setEndTime("2026-11-16 23:59:59");
        param.setCids("1,2,3");
        param.setSubcid(86369);
        param.setSort("1");
        param.setHasCoupon(1);
        map = param.toParamMap();

        assertEquals("2026-11-16 00:00:00", map.get("startTime"));
        assertEquals("2026-11-16 23:59:59", map.get("endTime"));
        assertEquals("1,2,3", map.get("cids"));
        assertEquals(86369, map.get("subcid"));
        assertEquals("1", map.get("sort"));
        assertEquals(1, map.get("hasCoupon"));

        // 参数集合必须与官方表完全一致，多一个少一个都算契约破坏
        assertEquals(new java.util.TreeSet<>(java.util.Arrays.asList(
                        "pageId", "pageSize", "startTime", "endTime", "cids", "subcid",
                        "sort", "hasCoupon")),
                new java.util.TreeSet<>(map.keySet()));
    }

    @Test
    public void newestGoodsHonorsExplicitPageSizeWithinWhitelist() {
        GetNewestGoodsParam param = new GetNewestGoodsParam();
        param.setPageSize(100);
        assertEquals(100, param.toParamMap().get("pageSize"));

        // 非白名单值被归一，不会把非法值发给上游
        param.setPageSize(33);
        assertEquals(50, param.toParamMap().get("pageSize"));
    }

    // ---------------------------------------------------------------
    // get-stale-goods-by-time（id=11，v1.0.1）
    // ---------------------------------------------------------------

    @Test
    public void staleGoodsUsesOfficialParamNamesAndDefaultPageSize() {
        StaleGoodsParam param = new StaleGoodsParam();
        TreeMap<String, Object> map = param.toParamMap();

        assertEquals("1", map.get("pageId"));
        assertEquals("官方 pageSize 默认 100", 100, map.get("pageSize"));
        assertFalse(map.containsKey("startTime"));
        assertFalse(map.containsKey("endTime"));

        param.setStartTime("2026-11-15 20:00:00");
        param.setEndTime("2026-11-16 20:00:00");
        map = param.toParamMap();

        assertEquals("2026-11-15 20:00:00", map.get("startTime"));
        assertEquals("2026-11-16 20:00:00", map.get("endTime"));
        assertEquals(new java.util.TreeSet<>(java.util.Arrays.asList(
                        "pageId", "pageSize", "startTime", "endTime")),
                new java.util.TreeSet<>(map.keySet()));
    }

    @Test
    public void staleGoodsWindowIsDocumentedAsTwentyFourHours() {
        // 官方：「查询时间24小时以内」。这个常量是任务调度频率的依据，不能随手改。
        assertEquals(24L * 60 * 60 * 1000, StaleGoodsParam.MAX_WINDOW_MILLIS);
    }

    // ---------------------------------------------------------------
    // pull-goods-by-time（id=12，v1.2.3）
    // ---------------------------------------------------------------

    @Test
    public void pullGoodsUsesSingularCidAndFullOfficialParamSet() {
        PullGoodsByTimeParam param = new PullGoodsByTimeParam();
        TreeMap<String, Object> map = param.toParamMap();

        assertEquals("1", map.get("pageId"));
        assertEquals("官方 pageSize 默认 10", 10, map.get("pageSize"));

        param.setCid("6");
        param.setSubcid(86369);
        param.setPre(0);
        param.setSort("2");
        param.setStartTime("2026-11-16 00:00:00");
        param.setEndTime("2026-11-16 23:59:59");
        param.setFreeshipRemoteDistrict(1);
        param.setChoice(1);
        param.setHasCoupon(-1);
        map = param.toParamMap();

        // 注意是单数 cid（与 get-newest-goods 的 cids 不同）
        assertEquals("6", map.get("cid"));
        assertFalse("pull-goods-by-time 没有 cids 参数", map.containsKey("cids"));
        assertEquals(86369, map.get("subcid"));
        assertEquals(0, map.get("pre"));
        assertEquals("2", map.get("sort"));
        assertEquals("2026-11-16 00:00:00", map.get("startTime"));
        assertEquals("2026-11-16 23:59:59", map.get("endTime"));
        assertEquals(1, map.get("freeshipRemoteDistrict"));
        assertEquals(1, map.get("choice"));
        assertEquals(-1, map.get("hasCoupon"));

        assertEquals(new java.util.TreeSet<>(java.util.Arrays.asList(
                        "pageId", "pageSize", "cid", "subcid", "pre", "sort", "startTime",
                        "endTime", "freeshipRemoteDistrict", "choice", "hasCoupon")),
                new java.util.TreeSet<>(map.keySet()));
    }

    @Test
    public void pullGoodsHasNoNewestGoodsOnlyFields() throws Exception {
        // get-newest-goods 的 cids 是多值字符串；pull 用单值 cid。两者混用会导致上游报错。
        // 用反射断言字段层面就不存在，避免以后有人「为了统一」加回来。
        for (String newestOnly : new String[]{"cids"}) {
            try {
                GetNewestGoodsParam.class.getDeclaredField(newestOnly);
            } catch (NoSuchFieldException e) {
                fail("get-newest-goods 应当有 " + newestOnly + " 字段");
            }
            try {
                PullGoodsByTimeParam.class.getDeclaredField(newestOnly);
                fail("pull-goods-by-time 不应有 " + newestOnly + " 字段");
            } catch (NoSuchFieldException expected) {
                // 正确：pull 用单数 cid
            }
        }
        assertFalse(new PullGoodsByTimeParam().toParamMap().containsKey("cids"));
    }

    // ---------------------------------------------------------------
    // 枚举契约：URL 与 version 必须来自各自文档页
    // ---------------------------------------------------------------

    @Test
    public void enumUrlsAndVersionsMatchEachDocumentPage() {
        assertEquals("https://openapi.dataoke.com/api/goods/get-newest-goods",
                DataokeApi.GOODS_NEWEST.getUrl());
        assertEquals("id=3 的接口版本号 v1.2.0", "v1.2.0", DataokeApi.GOODS_NEWEST.getVersion());

        assertEquals("https://openapi.dataoke.com/api/goods/get-stale-goods-by-time",
                DataokeApi.GOODS_STALE.getUrl());
        assertEquals("id=11 的接口版本号 v1.0.1", "v1.0.1", DataokeApi.GOODS_STALE.getVersion());

        assertEquals("https://openapi.dataoke.com/api/goods/pull-goods-by-time",
                DataokeApi.GOODS_PULL_BY_TIME.getUrl());
        assertEquals("id=12 的接口版本号 v1.2.3", "v1.2.3", DataokeApi.GOODS_PULL_BY_TIME.getVersion());
    }

    @Test
    public void paramMapsAreTreeMapsSoSigningOrderIsStable() {
        // 大淘客签名依赖参数按 key 字典序拼接，用 HashMap 会导致验签随机失败
        assertTrue(new GetNewestGoodsParam().toParamMap() instanceof TreeMap);
        assertTrue(new StaleGoodsParam().toParamMap() instanceof TreeMap);
        assertTrue(new PullGoodsByTimeParam().toParamMap() instanceof TreeMap);
    }
}
