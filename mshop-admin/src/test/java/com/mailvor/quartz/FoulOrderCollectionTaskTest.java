package com.mailvor.quartz;

import com.mailvor.modules.quartz.task.FoulOrderCollectionTask;
import com.mailvor.modules.tk.service.DataokeService;
import com.mailvor.modules.tk.service.FoulOrderService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

/**
 * 违规订单采集任务（工单 04）单元测试。
 *
 * <p><b>接缝</b>：{@link FoulOrderCollectionTask#run(String)} —— 这是 Quartz
 * 通过反射调用的唯一入口。上游与落库由 {@link FoulOrderService} 承担，这里 mock 掉它，
 * 只校验任务层职责：参数解析、默认值、以及**时间窗口不超过官方 30 天限制**这条硬约束。</p>
 *
 * <p><b>为什么专门测窗口</b>：官方 {@code get-foul-order-list} 的 {@code span}
 * 参数「不超过 30 天」。超限时上游只返回一句「参数错误」，不指出是哪个参数，
 * 排查成本很高。所以窗口夹取必须在任务层就兜住，且要有测试锁住这个行为——
 * 否则某天有人在 {@code system_quartz_job.params} 里填个 90，任务会静默失效。</p>
 */
public class FoulOrderCollectionTaskTest {

    private FoulOrderService foulOrderService;
    private FoulOrderCollectionTask task;

    @Before
    public void setUp() {
        foulOrderService = mock(FoulOrderService.class);
        task = new FoulOrderCollectionTask();
        ReflectionTestUtils.setField(task, "foulOrderService", foulOrderService);
    }

    private String capturedDay() {
        ArgumentCaptor<Integer> day = ArgumentCaptor.forClass(Integer.class);
        verify(foulOrderService).syncFoulOrdersFromDaysAgo(day.capture(), any());
        return String.valueOf(day.getValue());
    }

    // ---------------------------------------------------------------
    // 参数解析与默认值
    // ---------------------------------------------------------------

    @Test
    public void nullOrEmptyParamsUseDefaults() {
        task.run(null);
        assertEquals("默认回溯窗口 30 天", "30", capturedDay());

        reset(foulOrderService);
        task.run("");
        assertEquals("30", capturedDay());

        reset(foulOrderService);
        task.run("{}");
        assertEquals("30", capturedDay());
    }

    @Test
    public void parsesJsonParamsAndPassesDayThrough() {
        task.run("{\"day\":7,\"span\":7,\"pageSize\":50}");
        assertEquals("7", capturedDay());
        verify(foulOrderService).syncFoulOrdersFromDaysAgo(eq(7), eq(50));
    }

    @Test
    public void malformedJsonFallsBackToDefaultsInsteadOfCrashing() {
        // Quartz 会把 params 原样传进来，写错一个引号不该让整轮任务消失
        try {
            task.run("{not json");
        } catch (RuntimeException e) {
            fail("参数解析失败不应抛出，实际：" + e);
        }
    }

    @Test
    public void invalidNumbersFallBackToDefaults() {
        task.run("{\"day\":0}");
        assertEquals("day=0 视为未配置", "30", capturedDay());

        reset(foulOrderService);
        task.run("{\"day\":-5}");
        assertEquals("30", capturedDay());
    }

    @Test
    public void nullFieldValuesFallBackToDefaults() {
        task.run("{\"day\":null,\"pageSize\":null}");
        assertEquals("30", capturedDay());
    }

    // ---------------------------------------------------------------
    // 官方 30 天硬约束
    // ---------------------------------------------------------------

    @Test
    public void dayBeyondThirtyIsClampedToThirty() {
        task.run("{\"day\":90}");
        assertEquals("官方限制：查询跨度不超过 30 天，day 也必须夹住", "30", capturedDay());

        reset(foulOrderService);
        task.run("{\"day\":31}");
        assertEquals("30", capturedDay());
    }

    @Test
    public void dayAtExactlyThirtyIsAllowed() {
        task.run("{\"day\":30}");
        assertEquals("30", capturedDay());
    }

    @Test
    public void spanBeyondThirtyIsClampedToo() {
        // span 在 FoulOrderService 内部还会再夹一次（双保险），这里验证任务层不会
        // 把非法值直接透传出去造成上游参数错误。
        task.run("{\"day\":30,\"span\":90}");
        verify(foulOrderService).syncFoulOrdersFromDaysAgo(eq(30), any());
    }

    @Test
    public void normalizeClampsToMaxAndFallsBackWhenInvalid() {
        assertEquals(30, FoulOrderCollectionTask.normalize(null, 30, 30));
        assertEquals(30, FoulOrderCollectionTask.normalize(0, 30, 30));
        assertEquals(30, FoulOrderCollectionTask.normalize(-1, 30, 30));
        assertEquals(30, FoulOrderCollectionTask.normalize(31, 30, 30));
        assertEquals(30, FoulOrderCollectionTask.normalize(90, 30, 30));
        assertEquals(7, FoulOrderCollectionTask.normalize(7, 30, 30));
        assertEquals(30, FoulOrderCollectionTask.normalize(30, 30, 30));
    }

    // ---------------------------------------------------------------
    // 上游异常行为（明确约定，不留模糊）
    // ---------------------------------------------------------------

    @Test
    public void upstreamFailurePropagatesSoQuartzRecordsIt() {
        // 约定：上游异常向上抛。Quartz 的 ExecutionJob 会把它记进 QuartzLog，
        // 运营在后台能看到失败；若在这里吞掉，任务会「看似成功但什么都没做」。
        when(foulOrderService.syncFoulOrdersFromDaysAgo(anyInt(), any()))
                .thenThrow(new IllegalStateException("upstream down"));
        try {
            task.run("{\"day\":7}");
            fail("上游异常应当上抛，交给 Quartz 记录执行失败");
        } catch (IllegalStateException expected) {
            assertEquals("upstream down", expected.getMessage());
        }
    }

    // ---------------------------------------------------------------
    // 参数默认值本身
    // ---------------------------------------------------------------

    @Test
    public void paramDefaultsRespectOfficialLimits() {
        FoulOrderCollectionTask.FoulOrderParam p = new FoulOrderCollectionTask.FoulOrderParam();
        assertEquals("默认回溯 30 天（官方上限）", Integer.valueOf(30), p.getDay());
        assertEquals("默认跨度 7 天，远低于官方上限", Integer.valueOf(7), p.getSpan());
        assertEquals("默认每页 100 条", Integer.valueOf(DataokeService.FOUL_ORDER_PAGE_SIZE), p.getPageSize());
        assertTrue("默认值必须在官方限制内", p.getSpan() <= DataokeService.FOUL_ORDER_MAX_SPAN_DAYS);
        assertTrue("day 也必须在官方限制内", p.getDay() <= DataokeService.FOUL_ORDER_MAX_SPAN_DAYS);
    }
}
