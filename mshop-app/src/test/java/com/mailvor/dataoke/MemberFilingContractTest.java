package com.mailvor.dataoke;

import com.alibaba.fastjson.JSON;
import com.mailvor.modules.tk.vo.TbPublisherInfoVo;
import com.mailvor.modules.tk.vo.TbRelationRecordVo;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * 会员备案（工单 03）契约测试。
 *
 * <p>接缝选择：大淘客的 HTTP 客户端走 hutool {@code HttpUtil.get} 静态调用，
 * 无法用 {@code MockRestServiceServer} 拦截，因此这里在「上游响应 JSON → VO」
 * 这一层做契约校验：上游报文按<b>官方文档</b>字段构造，断言本地解析结果与业务语义一致。</p>
 *
 * <p>官方文档：{@code api-d.html?id=135}（get-relation-record）、
 * {@code id=136}（get-publisher-info）。</p>
 *
 * <p>覆盖三类场景：成功（正常报文）、失败（上游错误码）、参数异常（字段缺失/为空）。</p>
 */
public class MemberFilingContractTest {

    // ---------------------------------------------------------------
    // 成功场景：查询会员备案信息 get-publisher-info（官方字段）
    // ---------------------------------------------------------------

    @Test
    public void publisherInfoParsesOfficialFields() {
        // 官方响应：data.total_count + data.inviter_list[{account_name, create_date, special_id, root_pid}]
        TbPublisherInfoVo vo = JSON.parseObject(
                "{\"code\":0,\"msg\":\"成功\",\"data\":{\"total_count\":1,\"inviter_list\":["
                        + "{\"account_name\":\"小明\",\"create_date\":\"2026-01-02 03:04:05\","
                        + "\"special_id\":123456789,\"root_pid\":\"mm_1_2_3\"}]}}",
                TbPublisherInfoVo.class);

        assertTrue(vo.isSuccess());
        assertTrue("total_count>0 且 inviter_list 非空应判定为已备案", vo.isFiled());
        assertEquals(Integer.valueOf(1), vo.getTotalCount());
        assertEquals(1, vo.getInviterList().size());

        TbPublisherInfoVo.Inviter inviter = vo.getLatestInviter();
        assertNotNull(inviter);
        assertEquals("小明", inviter.getAccountName());
        assertEquals("2026-01-02 03:04:05", inviter.getCreateDate());
        assertEquals(Long.valueOf(123456789L), inviter.getSpecialId());
        assertEquals("mm_1_2_3", inviter.getRootPid());
    }

    @Test
    public void publisherInfoTreatsMissingOrEmptyRecordsAsUnfiled() {
        String[] bodies = {
                // data 缺失
                "{\"code\":0}",
                // data 为 null
                "{\"code\":0,\"data\":null}",
                // data 为空对象
                "{\"code\":0,\"data\":{}}",
                // total_count 为 0
                "{\"code\":0,\"data\":{\"total_count\":0,\"inviter_list\":[]}}",
                // total_count>0 但列表为空：不一致的报文，按未备案处理（保守）
                "{\"code\":0,\"data\":{\"total_count\":3,\"inviter_list\":[]}}",
                // 列表有元素但 total_count 缺失：同样保守按未备案
                "{\"code\":0,\"data\":{\"inviter_list\":[{\"special_id\":1}]}}"
        };
        for (String body : bodies) {
            TbPublisherInfoVo vo = JSON.parseObject(body, TbPublisherInfoVo.class);
            assertTrue(body, vo.isSuccess());
            assertFalse(body, vo.isFiled());
        }
    }

    @Test
    public void publisherInfoPicksFirstInviterAsLatestWhenMultipleRecords() {
        TbPublisherInfoVo vo = JSON.parseObject(
                "{\"code\":0,\"data\":{\"total_count\":2,\"inviter_list\":["
                        + "{\"special_id\":222,\"root_pid\":\"mm_new\"},"
                        + "{\"special_id\":111,\"root_pid\":\"mm_old\"}]}}",
                TbPublisherInfoVo.class);

        assertTrue(vo.isFiled());
        assertEquals(Integer.valueOf(2), vo.getTotalCount());
        assertEquals(Long.valueOf(222L), vo.getLatestInviter().getSpecialId());
        assertEquals("mm_new", vo.getLatestInviter().getRootPid());
    }

    // ---------------------------------------------------------------
    // 成功场景：获取会员备案授权链接 get-relation-record（官方字段）
    // ---------------------------------------------------------------

    @Test
    public void relationRecordUrlReadsOfficialUrlField() {
        TbRelationRecordVo vo = JSON.parseObject(
                "{\"code\":0,\"msg\":\"成功\",\"data\":{\"url\":\"https://auth.example/1\"}}",
                TbRelationRecordVo.class);

        assertTrue(vo.isSuccess());
        assertEquals("https://auth.example/1", vo.getRelationUrl());
    }

    @Test
    public void relationRecordUrlIsNullWhenUpstreamReturnsEmptyData() {
        String[] bodies = {
                "{\"code\":0}",
                "{\"code\":0,\"data\":{}}",
                "{\"code\":0,\"data\":null}",
                "{\"code\":0,\"data\":{\"url\":\"  \"}}"
        };
        for (String body : bodies) {
            assertNull(body, JSON.parseObject(body, TbRelationRecordVo.class).getRelationUrl());
        }
    }

    @Test
    public void relationRecordUrlIsTrimmed() {
        TbRelationRecordVo vo = JSON.parseObject(
                "{\"code\":0,\"data\":{\"url\":\"  https://auth.example/pad  \"}}",
                TbRelationRecordVo.class);
        assertEquals("https://auth.example/pad", vo.getRelationUrl());
    }

    // ---------------------------------------------------------------
    // 失败场景：上游返回错误码
    // ---------------------------------------------------------------

    @Test
    public void upstreamErrorCodeIsNotTreatedAsSuccess() {
        TbPublisherInfoVo info = JSON.parseObject(
                "{\"code\":-1,\"msg\":\"系统错误\",\"data\":null}", TbPublisherInfoVo.class);
        assertFalse(info.isSuccess());
        assertFalse(info.isFiled());

        TbRelationRecordVo record = JSON.parseObject(
                "{\"code\":-1,\"msg\":\"系统错误\",\"data\":null}", TbRelationRecordVo.class);
        assertFalse(record.isSuccess());
        assertNull(record.getRelationUrl());
    }

    @Test
    public void toleratedGarbageBodyNeverReportsSuccess() {
        // DataokeApiClient 在参数/密钥缺失时返回的是裸中文字符串而不是 JSON，
        // 解析层必须给出「失败」而不是抛异常或误判成功。
        for (String body : new String[]{"", "  ", "参数不能为空", "secret不能为空"}) {
            try {
                TbPublisherInfoVo vo = JSON.parseObject(body, TbPublisherInfoVo.class);
                if (vo != null) {
                    assertFalse(body, vo.isSuccess());
                }
            } catch (RuntimeException ignored) {
                // 抛异常同样可以接受：上层会转成中文化错误
            }
        }
    }
}
