package com.mailvor.modules.quartz.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

/**
 * Quartz 任务参数解析的唯一入口。
 *
 * <p><b>为什么需要它</b>：本项目 38 个定时任务的参数此前各写各的——坏 JSON 有的抛异常、
 * 有的静默回退默认值。静默那种最危险：配置写错了任务照跑，线上表现为「用默认值采了很久
 * 才发现参数根本不生效」，且没有任何报错。</p>
 *
 * <p><b>统一策略</b>：参数为空 / 空白 / {@code "{}"} → 返回默认实例；其余必须能解析成
 * JSON <b>对象</b>，否则抛 {@link IllegalArgumentException}。</p>
 *
 * <p><b>为什么抛而不吞</b>：{@code ExecutionJob.executeInternal} 捕获异常后会写
 * {@code quartz_log}（含堆栈）并把该任务<strong>暂停</strong>（{@code updateIsPause}）。
 * 也就是说「抛」= 配置错误立刻可见 + 任务自动停下，正好避免带着错误配置持续跑。</p>
 *
 * <p><b>非 JSON 的数字串</b>（如计费任务的 {@code params="100"}）不适用于本方法，
 * 那类任务应使用自己的 {@code readInt} 风格的解析——它们语义上是单个数值，不是对象。</p>
 */
public final class TaskParams {

    private TaskParams() {
    }

    /**
     * 解析任务参数。
     *
     * @param paramStr 来自 {@code system_quartz_job.params} 的原始串，可为 null
     * @param type     目标参数类型
     * @param taskName 任务中文名，仅用于异常信息，便于运维定位
     * @return 解析结果；入参为空时返回 {@code type} 的默认实例
     * @throws IllegalArgumentException 入参非空但不是合法 JSON 对象
     */
    public static <T> T parse(String paramStr, Class<T> type, String taskName) {
        if (paramStr == null || paramStr.trim().isEmpty()) {
            return newInstance(type);
        }
        String trimmed = paramStr.trim();
        if ("{}".equals(trimmed)) {
            return newInstance(type);
        }
        // 必须是 JSON 对象：数组/裸值都是配置错误
        if (!trimmed.startsWith("{")) {
            throw new IllegalArgumentException(
                    "[定时任务参数错误] " + taskName + " 的 params 必须是 JSON 对象，实际为：" + trimmed);
        }
        try {
            T result = JSON.parseObject(trimmed, type);
            return result == null ? newInstance(type) : result;
        } catch (RuntimeException e) {
            throw new IllegalArgumentException(
                    "[定时任务参数错误] " + taskName + " 的 params 无法解析：" + trimmed + "，" + e.getMessage(), e);
        }
    }

    private static <T> T newInstance(Class<T> type) {
        try {
            return type.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(
                    "任务参数类型 " + type.getName() + " 必须有无参构造器", e);
        }
    }

    /**
     * 解析单值整数参数（计费类任务用：params 是裸数字串如 {@code "100"}）。
     *
     * @return 解析结果；入参为空或非法时返回 {@code fallback}
     */
    public static int parseInt(String paramStr, int fallback, String taskName) {
        if (paramStr == null || paramStr.trim().isEmpty()) {
            return fallback;
        }
        try {
            return Integer.parseInt(paramStr.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "[定时任务参数错误] " + taskName + " 的 params 必须是整数，实际为：" + paramStr, e);
        }
    }

    /**
     * 兼容旧写法：把 JSON 解析成 {@link JSONObject}，空入参返回空对象。
     */
    public static JSONObject parseObject(String paramStr, String taskName) {
        if (paramStr == null || paramStr.trim().isEmpty()) {
            return new JSONObject();
        }
        String trimmed = paramStr.trim();
        if (!trimmed.startsWith("{")) {
            throw new IllegalArgumentException(
                    "[定时任务参数错误] " + taskName + " 的 params 必须是 JSON 对象，实际为：" + trimmed);
        }
        try {
            JSONObject o = JSON.parseObject(trimmed);
            return o == null ? new JSONObject() : o;
        } catch (RuntimeException e) {
            throw new IllegalArgumentException(
                    "[定时任务参数错误] " + taskName + " 的 params 无法解析：" + trimmed + "，" + e.getMessage(), e);
        }
    }
}
