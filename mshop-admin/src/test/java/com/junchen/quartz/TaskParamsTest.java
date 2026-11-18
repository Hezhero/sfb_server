package com.junchen.quartz;

import com.junchen.modules.quartz.utils.TaskParams;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * 定时任务参数解析契约测试。
 *
 * <p><b>这个 seam 解决什么</b>：38 个任务类此前各写各的参数解析，坏 JSON 有的抛、
 * 有的静默用默认值。静默那种最危险——配置写错了任务照跑，运维永远不知道。
 * 本模块统一成「坏 JSON 就抛」，因为 {@code ExecutionJob} 会记录失败并把任务暂停，
 * 配置错误立刻可见。</p>
 */
public class TaskParamsTest {

    /** 测试用参数对象 */
    public static class DemoParam {
        private Integer minutes = 10;
        private Integer limit = 20;

        public Integer getMinutes() { return minutes; }
        public void setMinutes(Integer minutes) { this.minutes = minutes; }
        public Integer getLimit() { return limit; }
        public void setLimit(Integer limit) { this.limit = limit; }
    }

    @Test
    public void emptyParamYieldsDefaultInstance() {
        DemoParam p = TaskParams.parse(null, DemoParam.class, "测试任务");
        assertNotNull(p);
        assertEquals(Integer.valueOf(10), p.getMinutes());
    }

    @Test
    public void blankParamYieldsDefaultInstance() {
        DemoParam p = TaskParams.parse("   ", DemoParam.class, "测试任务");
        assertNotNull(p);
        assertEquals(Integer.valueOf(10), p.getMinutes());
    }

    @Test
    public void emptyJsonObjectYieldsDefaultInstance() {
        DemoParam p = TaskParams.parse("{}", DemoParam.class, "测试任务");
        assertNotNull(p);
        assertEquals(Integer.valueOf(10), p.getMinutes());
    }

    @Test
    public void validJsonBindsFields() {
        DemoParam p = TaskParams.parse("{\"minutes\":30,\"limit\":5}", DemoParam.class, "测试任务");
        assertEquals(Integer.valueOf(30), p.getMinutes());
        assertEquals(Integer.valueOf(5), p.getLimit());
    }

    @Test
    public void malformedJsonThrowsWithTaskName() {
        try {
            TaskParams.parse("{not json", DemoParam.class, "订单采集任务");
            fail("坏 JSON 必须抛异常，让 ExecutionJob 记录失败并暂停任务");
        } catch (IllegalArgumentException e) {
            String msg = e.getMessage();
            assertNotNull(msg);
            // 报错要能让运维一眼定位是哪个任务配错了
            org.junit.Assert.assertTrue("异常信息应包含任务名，实际：" + msg, msg.contains("订单采集任务"));
            org.junit.Assert.assertTrue("异常信息应包含原始入参，实际：" + msg, msg.contains("{not json"));
        }
    }

    @Test
    public void jsonArrayIsRejectedAsMalformed() {
        // 参数必须是 JSON 对象，数组是配置错误
        try {
            TaskParams.parse("[1,2,3]", DemoParam.class, "测试任务");
            fail("JSON 数组不是合法任务参数，应当抛异常");
        } catch (IllegalArgumentException e) {
            assertNotNull(e.getMessage());
        }
    }

    @Test
    public void nonJsonTextIsRejectedWhenTaskRequiresObject() {
        // 纯数字串（如计费任务的 params="100"）不是 JSON 对象；
        // 这类任务应改用显式的 readInt，而不是让 parse 宽容接受
        try {
            TaskParams.parse("100", DemoParam.class, "测试任务");
            fail("非 JSON 对象应当抛异常");
        } catch (IllegalArgumentException e) {
            assertNotNull(e.getMessage());
        }
    }
}
