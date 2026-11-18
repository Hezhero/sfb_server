package com.junchen.quartz;

import com.junchen.modules.quartz.task.FoulOrderCollectionTask;
import com.junchen.modules.tk.service.DataokeService;
import com.junchen.modules.tk.service.FoulOrderService;
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
    public void malformedJsonFailsLoudlyInsteadOfSilentlyUsingDefaults() {
        // 2026-11-17 决策变更：坏 JSON 不再静默回退默认值。
        // 理由：ExecutionJob 捕获异常后会写 quartz_log（含堆栈）并把任务暂停，
        // 配置错误立刻可见；而静默回退会让错误配置长期潜伏、用默认值跑下去。
        try {
            task.run("{not json");
            fail("坏 JSON 参数必须抛异常，让 ExecutionJob 记录失败并暂停任务");
        } catch (IllegalArgumentException e) {
            assertNotNull("异常信息不能为空", e.getMessage());
            assertTrue("异常信息应能定位是哪个任务配错了，实际：" + e.getMessage(),
                    e.getMessage().contains("违规订单采集任务"));
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
    public void unknownParamKeyIsIgnored() {
        // span 参数已删除。上游传了也不会报错（未知字段被忽略），
        // 但要确认它不会影响实际行为：day 仍按传入值生效。
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
        assertEquals("默认每页 100 条", Integer.valueOf(DataokeService.FOUL_ORDER_PAGE_SIZE), p.getPageSize());
        assertTrue("day 也必须在官方限制内", p.getDay() <= DataokeService.FOUL_ORDER_MAX_SPAN_DAYS);
        // span 参数已删除：官方跨度上限固定 30 天，由 syncFoulOrdersFromDaysAgo 内部固定使用，
        // 旧实现解析了 span 却从不传给下游（假参数）。
    }
}
