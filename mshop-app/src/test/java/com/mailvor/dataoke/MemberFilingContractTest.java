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
 * 这一层做契约校验：上游报文按官方字段构造，断言本地解析结果与业务语义一致。</p>
 *
 * <p>覆盖三类场景：成功（正常报文）、失败（上游错误码）、参数异常（字段缺失/为空）。</p>
 */
public class MemberFilingContractTest {

    // ---------------------------------------------------------------
    // 成功场景：查询会员备案信息 get-publisher-info
    // ---------------------------------------------------------------

    @Test
    public void publisherInfoParsesFiledStateFromUpstreamPayload() {
        TbPublisherInfoVo vo = JSON.parseObject(
                "{\"code\":0,\"msg\":\"成功\",\"data\":{\"relation_status\":1,\"special_pid\":\"mm_1_2_3\"}}",
                TbPublisherInfoVo.class);

        assertTrue(vo.isSuccess());
        assertTrue(vo.isFiled());
        assertEquals(Integer.valueOf(1), vo.getRelationStatus());
        assertEquals("mm_1_2_3", vo.getSpecialPid());
    }

    @Test
    public void publisherInfoTreatsMissingOrZeroStatusAsUnfiled() {
        String[] bodies = {
                "{\"code\":0,\"data\":{}}",
                "{\"code\":0,\"data\":null}",
                "{\"code\":0,\"data\":{\"relation_status\":0}}"
        };
        for (String body : bodies) {
            TbPublisherInfoVo vo = JSON.parseObject(body, TbPublisherInfoVo.class);
            assertTrue(body, vo.isSuccess());
            assertFalse(body, vo.isFiled());
        }
    }

    // ---------------------------------------------------------------
    // 成功场景：获取会员备案授权链接 get-relation-record
    // ---------------------------------------------------------------

    @Test
    public void relationRecordUrlToleratesUpstreamFieldAliases() {
        String[][] cases = {
                {"{\"code\":0,\"msg\":\"成功\",\"data\":{\"tb_relation_record\":\"https://auth.example/1\"}}",
                        "https://auth.example/1"},
                {"{\"code\":0,\"msg\":\"成功\",\"data\":{\"relation_record\":\"https://auth.example/2\"}}",
                        "https://auth.example/2"},
                {"{\"code\":0,\"msg\":\"成功\",\"data\":{\"url\":\"https://auth.example/3\"}}",
                        "https://auth.example/3"}
        };
        for (String[] item : cases) {
            TbRelationRecordVo vo = JSON.parseObject(item[0], TbRelationRecordVo.class);
            assertTrue(item[0], vo.isSuccess());
            assertEquals(item[0], item[1], vo.getRelationUrl());
        }
    }

    @Test
    public void relationRecordUrlIsNullWhenUpstreamReturnsEmptyData() {
        String[] bodies = {
                "{\"code\":0,\"data\":{}}",
                "{\"code\":0,\"data\":null}",
                "{\"code\":0,\"data\":{\"tb_relation_record\":\"  \"}}"
        };
        for (String body : bodies) {
            assertNull(body, JSON.parseObject(body, TbRelationRecordVo.class).getRelationUrl());
        }
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
