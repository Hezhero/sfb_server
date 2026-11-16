package com.mailvor.dataoke;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.mailvor.modules.tk.service.DtkGoodsStaleFilter;
import com.mailvor.modules.tk.service.DtkGoodsSyncService;
import com.mailvor.modules.tk.vo.GoodsSearchDataVo;
import com.mailvor.modules.tk.vo.GoodsSearchVo;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * C 端商品查询失效过滤测试（工单 05）。
 *
 * <p><b>接缝</b>：{@link DtkGoodsStaleFilter} 的两个过滤方法。这是「前端不返回已失效商品」
 * 这条验收标准的落地点——过滤逻辑错了，用户就会点进已下架商品，
 * 正是本工单要消除的投诉来源。</p>
 */
public class DtkGoodsStaleFilterTest {

    private DtkGoodsSyncService syncService;
    private DtkGoodsStaleFilter filter;

    @Before
    public void setUp() {
        syncService = mock(DtkGoodsSyncService.class);
        filter = new DtkGoodsStaleFilter();
        ReflectionTestUtils.setField(filter, "goodsSyncService", syncService);
    }

    private void staleIds(Long... ids) {
        when(syncService.listStaleDtkIds(anyInt())).thenReturn(Arrays.asList(ids));
    }

    private static JSONObject goodsListResponse(String items) {
        return JSON.parseObject("{\"code\":0,\"msg\":\"成功\",\"data\":{\"list\":" + items
                + ",\"totalNum\":100,\"pageId\":\"next\"}}");
    }

    // ---------------------------------------------------------------
    // /tao/goods/list
    // ---------------------------------------------------------------

    @Test
    public void goodsListDropsStaleItemsAndAdjustsTotal() {
        staleIds(11L, 22L);
        JSONObject response = goodsListResponse(
                "[{\"id\":11,\"title\":\"已下架\"},{\"id\":33,\"title\":\"在架\"},{\"id\":22,\"title\":\"已下架\"}]");

        JSONObject filtered = filter.filterGoodsList(response);

        assertEquals(1, filtered.getJSONObject("data").getJSONArray("list").size());
        assertEquals(Long.valueOf(33L),
                filtered.getJSONObject("data").getJSONArray("list").getJSONObject(0).getLong("id"));
        // 剔除 2 条，总数同步下调，否则前端按原总数翻页会卡在空页
        assertEquals(Long.valueOf(98L), filtered.getJSONObject("data").getLong("totalNum"));
        // 游标必须原样保留，否则分页断掉
        assertEquals("next", filtered.getJSONObject("data").getString("pageId"));
    }

    @Test
    public void goodsListIsUntouchedWhenNothingIsStale() {
        when(syncService.listStaleDtkIds(anyInt())).thenReturn(Collections.emptyList());
        JSONObject response = goodsListResponse("[{\"id\":1},{\"id\":2}]");

        JSONObject filtered = filter.filterGoodsList(response);

        assertEquals(2, filtered.getJSONObject("data").getJSONArray("list").size());
        assertEquals(Long.valueOf(100L), filtered.getJSONObject("data").getLong("totalNum"));
    }

    @Test
    public void goodsListIgnoresStaleIdsNotPresentInThisPage() {
        staleIds(999L);
        JSONObject response = goodsListResponse("[{\"id\":1},{\"id\":2}]");

        JSONObject filtered = filter.filterGoodsList(response);
        assertEquals(2, filtered.getJSONObject("data").getJSONArray("list").size());
        assertEquals(Long.valueOf(100L), filtered.getJSONObject("data").getLong("totalNum"));
    }

    @Test
    public void goodsListToleratesOddShapesWithoutThrowing() {
        staleIds(1L);
        // 上游结构异常时不能抛异常：商品列表接口不能因为过滤逻辑 500
        for (JSONObject odd : new JSONObject[]{
                null,
                JSON.parseObject("{}"),
                JSON.parseObject("{\"code\":-1,\"msg\":\"错误\"}"),
                JSON.parseObject("{\"code\":0,\"data\":null}"),
                JSON.parseObject("{\"code\":0,\"data\":{\"list\":null}}"),
                JSON.parseObject("{\"code\":0,\"data\":{\"list\":[]}}")}) {
            try {
                filter.filterGoodsList(odd);
            } catch (RuntimeException e) {
                fail("不该对 " + odd + " 抛异常：" + e);
            }
        }
    }

    @Test
    public void goodsListFiltersOnDtkIdNotGoodsId() {
        // 失效库存的是大淘客商品 id；goodsId（淘宝商品 id）不能拿来匹配
        staleIds(11L);
        JSONObject response = goodsListResponse(
                "[{\"id\":11,\"goodsId\":\"tb-11\"},{\"id\":22,\"goodsId\":\"11\"}]");

        JSONObject filtered = filter.filterGoodsList(response);
        // 只有 id=11 被剔除；goodsId 恰好等于 "11" 的那条必须保留
        assertEquals(1, filtered.getJSONObject("data").getJSONArray("list").size());
        assertEquals(Long.valueOf(22L),
                filtered.getJSONObject("data").getJSONArray("list").getJSONObject(0).getLong("id"));
    }

    // ---------------------------------------------------------------
    // /tao/goods/search
    // ---------------------------------------------------------------

    private static GoodsSearchDataVo searchResponse(Long... ids) {
        List<GoodsSearchVo> list = new ArrayList<>();
        for (Long id : ids) {
            GoodsSearchVo vo = new GoodsSearchVo();
            vo.setId(id);
            list.add(vo);
        }
        GoodsSearchDataVo data = new GoodsSearchDataVo();
        data.setList(list);
        data.setTotalNum(ids.length);
        data.setPageId("page-next");
        return data;
    }

    @Test
    public void goodsSearchDropsStaleItemsAndAdjustsTotal() {
        staleIds(11L);
        GoodsSearchDataVo filtered = filter.filterGoodsSearch(searchResponse(11L, 22L, 33L));

        assertEquals(2, filtered.getList().size());
        assertEquals(Long.valueOf(22L), filtered.getList().get(0).getId());
        assertEquals(Integer.valueOf(2), filtered.getTotalNum());
        // 游标不能动
        assertEquals("page-next", filtered.getPageId());
    }

    @Test
    public void goodsSearchIsUntouchedWhenNothingIsStale() {
        when(syncService.listStaleDtkIds(anyInt())).thenReturn(Collections.emptyList());
        GoodsSearchDataVo filtered = filter.filterGoodsSearch(searchResponse(1L, 2L));

        assertEquals(2, filtered.getList().size());
        assertEquals(Integer.valueOf(2), filtered.getTotalNum());
    }

    @Test
    public void goodsSearchToleratesNulls() {
        staleIds(1L);
        assertNull(filter.filterGoodsSearch(null));

        GoodsSearchDataVo empty = new GoodsSearchDataVo();
        assertSame(empty, filter.filterGoodsSearch(empty));
    }

    // ---------------------------------------------------------------
    // 容错：失效库读取失败不能拖垮查询
    // ---------------------------------------------------------------

    @Test
    public void staleLookupFailureDegradesToNoFilteringInsteadOfBreakingTheRequest() {
        when(syncService.listStaleDtkIds(anyInt())).thenThrow(new RuntimeException("db down"));

        JSONObject response = goodsListResponse("[{\"id\":1},{\"id\":2}]");
        JSONObject filtered = filter.filterGoodsList(response);

        // 读不到失效列表就不过滤：宁可多展示一个，也不能让列表接口挂掉
        assertEquals(2, filtered.getJSONObject("data").getJSONArray("list").size());
        assertTrue(filter.currentStaleIds().isEmpty());
    }

    @Test
    public void isStaleMatchesOnDtkId() {
        staleIds(11L);
        assertTrue(filter.isStale(11L));
        assertFalse(filter.isStale(22L));
        assertFalse("null 不参与匹配", filter.isStale(null));
    }
}
