package com.junchen.dataoke;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.junchen.modules.tk.domain.MwDtkGoods;
import com.junchen.modules.tk.service.DataokeService;
import com.junchen.modules.tk.service.DtkGoodsSyncService;
import com.junchen.modules.tk.service.mapper.MwDtkGoodsMapper;
import com.junchen.modules.tk.util.DtkScrollPager;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 商品库同步服务测试（工单 05）。
 *
 * <p><b>接缝</b>：{@link DtkGoodsSyncService} 的公开方法。上游 HTTP 由
 * {@link DataokeService} 承担（hutool 静态调用，无法用 MockRestServiceServer 拦截），
 * 因此这里 mock 平台层与 Mapper，验证业务编排：</p>
 * <ul>
 *   <li>scroll_id 分页正确串联（首次 1 → 上游游标 → 空列表终止）</li>
 *   <li>失效商品按大淘客商品 id（官方字段 {@code id}）匹配本地</li>
 *   <li>24 小时窗口收敛</li>
 *   <li>价格变更能更新本地已存字段</li>
 * </ul>
 */
public class DtkGoodsSyncServiceTest {

    private DataokeService dataoke;
    private MwDtkGoodsMapper goodsMapper;
    private DtkGoodsSyncService service;

    @Before
    public void setUp() {
        dataoke = mock(DataokeService.class);
        goodsMapper = mock(MwDtkGoodsMapper.class);
        service = new DtkGoodsSyncService();
        ReflectionTestUtils.setField(service, "dataokeService", dataoke);
        ReflectionTestUtils.setField(service, "goodsMapper", goodsMapper);
    }

    /** 造一页上游响应（结构见官方「返回示例」，三个接口一致） */
    private static JSONObject page(String listJson, String nextPageId) {
        return JSON.parseObject("{\"time\":1554711897417,\"code\":0,\"msg\":\"成功\",\"data\":{"
                + "\"list\":" + listJson + ",\"totalNum\":16499,\"pageId\":"
                + (nextPageId == null ? "null" : "\"" + nextPageId + "\"") + "}}");
    }

    /** 捕获批量 upsert 收到的行，并按 MySQL 语义返回「影响行数」 */
    private List<MwDtkGoods> captureUpserts() {
        final List<MwDtkGoods> captured = new ArrayList<>();
        when(goodsMapper.mysqlInsertOrUpdateBath(any())).thenAnswer(inv -> {
            List<MwDtkGoods> rows = inv.getArgument(0);
            captured.addAll(rows);
            return rows.size();
        });
        return captured;
    }

    // ---------------------------------------------------------------
    // scroll_id 分页：串起上游游标
    // ---------------------------------------------------------------

    @Test
    public void newestSyncFollowsUpstreamCursorAndStopsOnEmptyList() {
        when(dataoke.newestGoods(any())).thenReturn(
                page("[{\"id\":1,\"goodsId\":\"a\"},{\"id\":2,\"goodsId\":\"b\"}]", "cur-2"),
                page("[{\"id\":3,\"goodsId\":\"c\"}]", "cur-3"),
                page("[]", "cur-4"));

        List<MwDtkGoods> saved = captureUpserts();

        DtkGoodsSyncService.SyncResult result = service.syncNewestGoods(null, 100);

        assertEquals(3, result.getFetched());
        assertEquals(3, result.getSaved());
        assertEquals(3, saved.size());
        assertEquals(Arrays.asList(1L, 2L, 3L),
                Arrays.asList(saved.get(0).getDtkId(), saved.get(1).getDtkId(), saved.get(2).getDtkId()));
        // 每一条都标记为正常在架
        for (MwDtkGoods goods : saved) {
            assertEquals(Integer.valueOf(MwDtkGoods.STATUS_NORMAL), goods.getGoodsStatus());
            assertEquals(MwDtkGoods.SOURCE_NEWEST, goods.getSource());
        }
    }

    @Test
    public void newestSyncRequestsFirstPageWithCursorOneThenUpstreamCursor() {
        List<String> pageIds = new ArrayList<>();
        when(dataoke.newestGoods(any())).thenAnswer(inv -> {
            pageIds.add(inv.<com.junchen.modules.tk.param.GetNewestGoodsParam>getArgument(0).getPageId());
            return pageIds.size() == 1 ? page("[{\"id\":1}]", "cur-2") : page("[]", null);
        });
        captureUpserts();

        service.syncNewestGoods(null, 100);
        assertEquals(Arrays.asList("1", "cur-2"), pageIds);
    }

    @Test
    public void upstreamFailureOnFirstPageProducesNoWritesInsteadOfCrashing() {
        when(dataoke.newestGoods(any())).thenReturn(null);

        DtkGoodsSyncService.SyncResult result = service.syncNewestGoods(null, 100);
        assertEquals(0, result.getFetched());
        assertEquals(0, result.getSaved());
        verify(goodsMapper, never()).mysqlInsertOrUpdateBath(any());
    }

    @Test
    public void upstreamErrorCodeIsNotTreatedAsData() {
        // code != 0 是上游错误（官方错误码表：-1 服务器错误 / 1 参数错误）
        when(dataoke.newestGoods(any())).thenReturn(
                JSON.parseObject("{\"code\":-1,\"msg\":\"服务器错误\"}"));

        DtkGoodsSyncService.SyncResult result = service.syncNewestGoods(null, 100);
        assertEquals(0, result.getFetched());
        verify(goodsMapper, never()).mysqlInsertOrUpdateBath(any());
    }

    // ---------------------------------------------------------------
    // 失效商品：按大淘客商品 id 匹配
    // ---------------------------------------------------------------

    @Test
    public void staleSyncMarksLocalGoodsByDtkId() {
        when(dataoke.staleGoods(any())).thenReturn(
                page("[{\"id\":11,\"goodsId\":\"x\"},{\"id\":22,\"goodsId\":\"y\"}]", null));
        when(goodsMapper.update(any(), any())).thenReturn(2);

        DtkGoodsSyncService.SyncResult result = service.syncStaleGoods(100);

        assertEquals(2, result.getFetched());
        // 必须真的去更新本地记录，而不是只统计
        verify(goodsMapper, atLeastOnce()).update(any(), any());
    }

    @Test
    public void staleSyncMatchesOnDtkIdColumnNotGoodsIdOrGoodsSign() {
        when(dataoke.staleGoods(any())).thenReturn(
                page("[{\"id\":11,\"goodsId\":\"x\",\"goodsSign\":\"s-1\"}]", null));
        when(goodsMapper.update(any(), any())).thenReturn(1);

        service.syncStaleGoods(100);

        org.mockito.ArgumentCaptor<com.baomidou.mybatisplus.core.conditions.Wrapper> captor =
                org.mockito.ArgumentCaptor.forClass(
                        com.baomidou.mybatisplus.core.conditions.Wrapper.class);
        verify(goodsMapper, atLeastOnce()).update(any(), captor.capture());

        // UpdateWrapper 的 SQL 片段里必须出现 dtk_id，且不能出现 goods_id / goods_sign
        String sql = captor.getValue().getSqlSegment();
        assertTrue("失效匹配必须按 dtk_id 列，实际：" + sql, sql.contains("dtk_id"));
        assertFalse("不能用 goods_id 匹配，实际：" + sql, sql.contains("goods_id"));
        assertFalse("不能用 goods_sign 匹配，实际：" + sql, sql.contains("goods_sign"));
    }

    @Test
    public void rowsMissingDtkIdAreSkippedBecauseTheyCanNeverBeMatched() {
        when(dataoke.staleGoods(any())).thenReturn(page("[{\"goodsId\":\"no-id\"},{\"id\":22}]", null));
        when(goodsMapper.update(any(), any())).thenReturn(1);

        DtkGoodsSyncService.SyncResult result = service.syncStaleGoods(100);

        assertEquals("两条都拉到了", 2, result.getFetched());
        assertEquals("缺 id 的那条被跳过", 1, result.getSkipped());
    }

    @Test
    public void staleSyncKeepsStaleIdsAvailableForFrontendFiltering() {
        // 失效 id 集合的来源必须稳定：前端过滤要能拿到
        MwDtkGoods stale = MwDtkGoods.builder().dtkId(11L).goodsStatus(MwDtkGoods.STATUS_STALE).build();
        when(goodsMapper.selectList(any())).thenReturn(Collections.singletonList(stale));

        List<Long> ids = service.listStaleDtkIds(1000);
        assertEquals(Collections.singletonList(11L), ids);
    }

    @Test
    public void staleIdQueryFiltersOnStatusColumn() {
        when(goodsMapper.selectList(any())).thenReturn(Collections.emptyList());
        service.listStaleDtkIds(100);

        org.mockito.ArgumentCaptor<com.baomidou.mybatisplus.core.conditions.Wrapper> captor =
                org.mockito.ArgumentCaptor.forClass(
                        com.baomidou.mybatisplus.core.conditions.Wrapper.class);
        verify(goodsMapper).selectList(captor.capture());
        String sql = captor.getValue().getSqlSegment();
        assertTrue("必须按 goods_status 过滤失效，实际：" + sql, sql.contains("goods_status"));
        assertEquals("只取 dtk_id 一列", "dtk_id", captor.getValue().getSqlSelect());
    }

    @Test
    public void staleSyncWindowNeverExceedsTwentyFourHours() {
        when(dataoke.staleGoods(any())).thenAnswer(inv -> {
            com.junchen.modules.tk.param.StaleGoodsParam param = inv.getArgument(0);
            long start = parse(param.getStartTime());
            long end = parse(param.getEndTime());
            assertTrue("窗口不能超过 24 小时，实际 " + (end - start),
                    end - start <= com.junchen.modules.tk.param.StaleGoodsParam.MAX_WINDOW_MILLIS);
            return page("[]", null);
        });

        service.syncStaleGoods(100);
        verify(dataoke).staleGoods(any());
    }

    @Test
    public void staleWindowStartsExactlyTwentyFourHoursBeforeNow() {
        // 窗口必须是「满 24 小时」而不是更窄：任务每天只跑一次时，窄窗口会漏数据
        java.util.Date now = new java.util.Date(1_700_000_000_000L);
        com.junchen.modules.tk.param.StaleGoodsParam param = DtkGoodsSyncService.buildStaleWindow(now);

        long end = parse(param.getEndTime());
        long start = parse(param.getStartTime());
        assertEquals(1_700_000_000_000L, end);
        assertEquals(com.junchen.modules.tk.param.StaleGoodsParam.MAX_WINDOW_MILLIS, end - start);
        // 格式必须是官方要求的 yyyy-MM-dd HH:mm:ss
        assertEquals("2026-11-16 20:00:00".length(), param.getStartTime().length());
    }

    private static long parse(String text) {
        try {
            return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(text).getTime();
        } catch (Exception e) {
            throw new AssertionError("时间格式应为 yyyy-MM-dd HH:mm:ss，实际 " + text, e);
        }
    }

    // ---------------------------------------------------------------
    // 定时拉取：字段最全，价格变更要能更新
    // ---------------------------------------------------------------

    @Test
    public void pullSyncPersistsFullFieldsAndRefreshesChangedPrice() {
        when(dataoke.pullGoodsByTime(any())).thenReturn(page("[{"
                + "\"id\":21631667,\"goodsId\":\"599287662104\","
                + "\"title\":\"月饼礼盒\",\"dtitle\":\"9味月饼\","
                + "\"mainPic\":\"https://img.example/1.png\",\"shopName\":\"某旗舰店\","
                + "\"originalPrice\":24.9,\"actualPrice\":14.9,\"couponPrice\":10.0,"
                + "\"commissionRate\":30.0,\"monthSales\":229734,"
                + "\"sellerId\":\"2935830269\",\"cid\":6,\"createTime\":\"2019-08-15 00:08:03\","
                + "\"itemLink\":\"https://detail.tmall.com/item.htm?id=599287662104\"}]", null));

        List<MwDtkGoods> saved = captureUpserts();

        DtkGoodsSyncService.SyncResult result = service.syncPullGoods(null, 10);

        assertEquals(1, result.getSaved());
        MwDtkGoods goods = saved.get(0);
        assertEquals(Long.valueOf(21631667L), goods.getDtkId());
        assertEquals("599287662104", goods.getGoodsId());
        assertEquals("月饼礼盒", goods.getTitle());
        assertEquals("9味月饼", goods.getDtitle());
        assertEquals("https://img.example/1.png", goods.getMainPic());
        assertEquals("某旗舰店", goods.getShopName());
        assertEquals(Double.valueOf(24.9), goods.getOriginalPrice());
        // 券后价是最容易变价的字段，必须每次同步都覆盖
        assertEquals(Double.valueOf(14.9), goods.getActualPrice());
        assertEquals(Double.valueOf(10.0), goods.getCouponPrice());
        assertEquals(Double.valueOf(30.0), goods.getCommissionRate());
        assertEquals(Long.valueOf(229734L), goods.getMonthSales());
        assertEquals("2935830269", goods.getSellerId());
        assertEquals(Long.valueOf(6L), goods.getCid());
        assertEquals(MwDtkGoods.SOURCE_PULL, goods.getSource());
        assertEquals(Integer.valueOf(MwDtkGoods.STATUS_NORMAL), goods.getGoodsStatus());
        assertNotNull(goods.getGoodsCreateTime());
        assertNotNull(goods.getLastSyncTime());
    }

    @Test
    public void pullSyncSkipsRowsWithoutDtkIdBecauseTheyCannotBeMatchedLater() {
        when(dataoke.pullGoodsByTime(any())).thenReturn(page(
                "[{\"goodsId\":\"no-id\"},{\"id\":5,\"goodsId\":\"ok\"}]", null));

        List<MwDtkGoods> saved = captureUpserts();

        DtkGoodsSyncService.SyncResult result = service.syncPullGoods(null, 10);

        assertEquals("两条都拉到了", 2, result.getFetched());
        assertEquals("只有带 dtkId 的那条能入库", 1, result.getSaved());
        assertEquals(Long.valueOf(5L), saved.get(0).getDtkId());
    }

    // ---------------------------------------------------------------
    // 分页上限兜底
    // ---------------------------------------------------------------

    @Test
    public void runawayUpstreamCursorIsCappedInsteadOfLoopingForever() {
        // 上游每次都返回新游标 + 一条数据：只能靠 maxPages 兜底
        when(dataoke.newestGoods(any())).thenAnswer(inv -> page("[{\"id\":1}]", "cur-" + System.nanoTime()));
        captureUpserts();

        DtkGoodsSyncService.SyncResult result = service.syncNewestGoods(null, 1);
        assertTrue("必须被页数上限截断", result.getPages() <= DtkScrollPager.DEFAULT_MAX_PAGES);
        assertTrue(result.getFetched() <= DtkScrollPager.DEFAULT_MAX_PAGES);
    }
}
