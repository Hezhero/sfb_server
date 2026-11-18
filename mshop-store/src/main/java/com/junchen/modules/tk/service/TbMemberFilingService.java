package com.junchen.modules.tk.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.junchen.api.MshopException;
import com.junchen.modules.tk.config.TbConfig;
import com.junchen.modules.tk.domain.MwTbFiling;
import com.junchen.modules.tk.domain.MwTbFilingLog;
import com.junchen.modules.tk.service.mapper.MwTbFilingLogMapper;
import com.junchen.modules.tk.service.mapper.MwTbFilingMapper;
import com.junchen.modules.tk.vo.TbFilingResultVo;
import com.junchen.modules.tk.vo.TbPublisherInfoVo;
import com.junchen.modules.tk.vo.TbRelationRecordVo;
import com.junchen.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Date;
import java.util.Objects;

/**
 * 推客淘宝渠道备案服务（工单 03）。
 *
 * <p><b>业务背景</b>：淘宝联盟要求推广者与渠道方建立备案关系，未备案的推广行为
 * 无法归因到本平台，佣金从源头就断。因此备案是 B2B2C 模式的合法性前提，
 * 也是转链与佣金结算的前置校验项。</p>
 *
 * <p><b>上游两个接口</b>（官方文档，主机 {@code openapi.dataoke.com}，前缀 {@code /open-api/}）：</p>
 * <ul>
 *   <li>{@code get-relation-record}（api-d.html?id=135）—— 生成推客专属的备案授权链接</li>
 *   <li>{@code get-publisher-info}（api-d.html?id=136）—— 查询推客当前的备案记录</li>
 * </ul>
 *
 * <p><b>身份映射</b>：两个接口都按 {@code externalId}（私域用户自定义唯一标识）+
 * {@code externalType}（渠道类型）识别用户，本平台传推客 uid 的字符串形式，
 * 渠道类型固定 {@link DataokeService#EXTERNAL_TYPE_OTHER}（0=其他）。
 * 官方参数里没有 pid，本地 {@code tb_pid} 只作为追溯用的渠道标识保留。</p>
 *
 * <p><b>错误处理原则</b>：上游错误码一律翻译成中文可读提示，不把原始 code / 原文
 * 抛给前端；同时把「无法确认」按「未备案」处理，宁可多提示一次，也不让未备案的
 * 推广跑出去产生无法归因的订单。</p>
 */
@Slf4j
@Service
public class TbMemberFilingService {

    /** 渠道类型固定为 0（其他），语义见官方文档 externalType */
    private static final int EXTERNAL_TYPE = DataokeService.EXTERNAL_TYPE_OTHER;

    /** 备案状态：1-已备案 */
    private static final int STATUS_FILED = 1;
    /** 备案状态：0-未备案 */
    private static final int STATUS_UNFILED = 0;

    private static final String HINT_UNFILED =
            "您尚未完成淘宝渠道备案，推广订单无法归集佣金，请先获取备案链接完成备案后再推广";

    @Resource
    private DataokeService dataokeService;

    @Resource
    private TbConfig tbConfig;

    @Resource
    private MwTbFilingMapper filingMapper;

    @Resource
    private MwTbFilingLogMapper filingLogMapper;

    /**
     * 获取推客的备案授权链接，并把链接落库。
     *
     * <p>官方 {@code get-relation-record} 的四个必填参数在此补齐：</p>
     * <ul>
     *   <li>{@code externalId} = 推客 uid 字符串</li>
     *   <li>{@code externalType} = 0（其他）</li>
     *   <li>{@code opType} = 1 新绑 / 2 更新，判定规则见 {@link #decideOpType(MwTbFiling)}</li>
     *   <li>{@code redirectUrl} = 调用方传入的商品短链</li>
     * </ul>
     *
     * @param uid         推客 uid
     * @param redirectUrl 备案完成后跳转的淘宝商品短链（官方必填）
     * @return 含授权链接与当前备案状态的结果
     */
    @Transactional(rollbackFor = Exception.class)
    public TbFilingResultVo getRelationUrl(Long uid, String redirectUrl) {
        if (uid == null || uid <= 0) {
            throw new MshopException("请先登录后再获取备案链接");
        }
        String useRedirectUrl = normalizeRedirectUrl(redirectUrl);
        String externalId = externalId(uid);
        // 只查一次本地记录：既决定 opType，也作为落库目标，避免重复查询
        MwTbFiling filing = getOrInit(uid);
        int opType = decideOpType(filing);

        TbRelationRecordVo response = dataokeService.getRelationRecord(
                externalId, EXTERNAL_TYPE, opType, useRedirectUrl);
        if (response == null || !response.isSuccess()) {
            throw new MshopException("会员备案授权链接获取失败，请稍后重试");
        }
        String url = response.getRelationUrl();
        if (StringUtils.isBlank(url)) {
            throw new MshopException("会员备案授权链接获取失败，请稍后重试");
        }

        filing.setRelationUrl(url);
        filing.setUpdateTime(new Date());
        if (filing.getId() == null) {
            filing.setCreateTime(new Date());
            filingMapper.insert(filing);
        } else {
            filingMapper.updateById(filing);
        }

        TbFilingResultVo result = toResult(filing);
        result.setRelationUrl(url);
        return result;
    }

    /**
     * 主动查询上游备案记录并落库，返回最新状态。
     *
     * <p>状态发生变更时写一条 {@link MwTbFilingLog}，作为结算争议的追溯依据；
     * 状态没变不写，避免推客每次刷新都产生噪音。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public TbFilingResultVo refreshStatus(Long uid) {
        if (uid == null || uid <= 0) {
            throw new MshopException("请先登录后再查询备案状态");
        }

        TbPublisherInfoVo response = dataokeService.getPublisherInfo(externalId(uid), EXTERNAL_TYPE);
        if (response == null || !response.isSuccess()) {
            throw new MshopException("会员备案信息查询失败，请稍后重试");
        }

        MwTbFiling filing = getOrInit(uid);
        Integer fromStatus = filing.getRelationStatus();
        // 官方不返回状态字段，只返回 total_count + inviter_list，
        // 因此 isFiled() 是「total_count > 0 且 inviter_list 非空」的推断。
        int newStatus = response.isFiled() ? STATUS_FILED : STATUS_UNFILED;

        filing.setRelationStatus(newStatus);
        filing.setTotalCount(response.getTotalCount());
        filing.setUpdateTime(new Date());
        if (newStatus == STATUS_FILED && filing.getFiledTime() == null) {
            filing.setFiledTime(new Date());
        }

        // inviter_list 可能有多条：只落最新一条（列表首条）的明细 + 记录总数。
        TbPublisherInfoVo.Inviter latest = response.getLatestInviter();
        if (latest != null) {
            filing.setSpecialId(latest.getSpecialId());
            filing.setRootPid(trimToNull(latest.getRootPid()));
            filing.setAccountName(trimToNull(latest.getAccountName()));
            // root_pid 就是备案时实际使用的推广位，落库便于对账
            if (StringUtils.isNotBlank(latest.getRootPid())) {
                filing.setTbPid(trimToNull(latest.getRootPid()));
            }
        }

        if (filing.getId() == null) {
            filing.setCreateTime(new Date());
            filingMapper.insert(filing);
        } else {
            filingMapper.updateById(filing);
        }

        if (!Objects.equals(fromStatus, newStatus)) {
            MwTbFilingLog statusLog = MwTbFilingLog.builder()
                    .uid(uid)
                    .tbPid(filing.getTbPid())
                    .specialId(filing.getSpecialId())
                    .fromStatus(fromStatus)
                    .relationStatus(newStatus)
                    .remark(newStatus == STATUS_FILED ? "备案完成" : "备案状态变更为未备案")
                    .build();
            statusLog.setCreateTime(new Date());
            statusLog.setUpdateTime(new Date());
            filingLogMapper.insert(statusLog);
        }

        return toResult(filing);
    }

    /**
     * 转链 / 查看佣金前的前置校验：未备案直接给出中文化提示。
     *
     * <p>匿名用户（uid 为 null 或 0）不是推客，不参与备案校验，
     * 否则会误伤 C 端下单与领奖链路。</p>
     */
    public void assertFiled(Long uid) {
        if (uid == null || uid <= 0) {
            return;
        }
        if (!isFiled(uid)) {
            throw new MshopException(HINT_UNFILED);
        }
    }

    /**
     * 推客是否已完成备案。
     *
     * <p>匿名用户（uid 为 null 或 0）不是推客，视为「无需备案」返回 true，
     * 避免 C 端下单/领奖链路被误伤。本地无记录按未备案处理。</p>
     */
    public boolean isFiled(Long uid) {
        if (uid == null || uid <= 0) {
            return true;
        }
        MwTbFiling filing = findFiling(uid);
        return filing != null && filing.isFiled();
    }

    /**
     * 未备案时给前端的中文引导提示；已备案或匿名用户返回 null。
     *
     * <p>用于「提示但不阻断」的场景（如转链）：推客在淘宝其实已完成备案、
     * 只是本地还没查过状态时，硬拦会白丢单，所以先让链路跑通、同时把提示带给 App。</p>
     */
    public String hintFor(Long uid) {
        if (uid == null || uid <= 0) {
            return null;
        }
        return isFiled(uid) ? null : HINT_UNFILED;
    }

    /**
     * 判定 {@code opType}：1 新绑 / 2 更新。
     *
     * <p><b>官方文档只给了取值含义，没给判定规则</b>，这里按「本地是否已有备案记录」推断：
     * 本地无记录（首次备案）→ 1 新绑；本地已有记录且曾经拿到过授权链接或已备案 → 2 更新。</p>
     *
     * <p>之所以不额外调一次 {@code get-publisher-info} 来判定：那会给「取链接」这个
     * 高频轻接口叠加一次上游往返与失败面，而 opType 传错的后果只是上游对已存在关系
     * 重新发一次绑定请求（幂等），代价远小于多一次跨网调用。</p>
     */
    private int decideOpType(MwTbFiling filing) {
        boolean hasLocalRecord = filing != null
                && (filing.isFiled() || StringUtils.isNotBlank(filing.getRelationUrl()));
        return hasLocalRecord ? DataokeService.OP_TYPE_UPDATE : DataokeService.OP_TYPE_BIND;
    }

    /**
     * 校验并归一化 {@code redirectUrl}（官方必填）。
     *
     * <p><b>为什么不给默认值</b>：官方语义是「授权完成后跳转实际转链后的商品短链」，
     * 也就是用户完成备案后落在哪个商品上，只有调用方（App 转链上下文）知道。
     * 服务端随便填一个首页短链会让用户在完成备案后跳到无关页面，
     * 而备案是不可撤销的一次性动作——这种体验损失远大于让调用方显式传参的成本。
     * 因此缺参数时直接返回中文化错误，暴露调用方 bug，而不是静默兜底。</p>
     */
    private String normalizeRedirectUrl(String redirectUrl) {
        if (StringUtils.isBlank(redirectUrl)) {
            throw new MshopException("缺少备案完成后的跳转商品短链，暂无法生成备案链接");
        }
        String url = redirectUrl.trim();
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            throw new MshopException("备案完成后的跳转地址格式不正确");
        }
        return url;
    }

    /**
     * 推客身份映射到上游 {@code externalId}。
     *
     * <p>官方定义是「私域用户自定义唯一标识」，本平台用 uid 保证唯一且稳定；
     * 一旦变更，历史备案关系会全部失效，因此不要改成昵称/手机号等可变字段。</p>
     */
    private String externalId(Long uid) {
        return String.valueOf(uid);
    }

    /** 读取本地备案记录，没有则构造一条新记录（尚未入库） */
    private MwTbFiling getOrInit(Long uid) {
        MwTbFiling filing = findFiling(uid);
        if (filing != null) {
            return filing;
        }
        filing = MwTbFiling.builder()
                .uid(uid)
                .tbPid(channelPidOrNull())
                .relationStatus(STATUS_UNFILED)
                .build();
        filing.setCreateTime(new Date());
        filing.setUpdateTime(new Date());
        return filing;
    }

    private MwTbFiling findFiling(Long uid) {
        return filingMapper.selectOne((Wrappers.<MwTbFiling>lambdaQuery()
                .eq(MwTbFiling::getUid, uid)
                .last("limit 1")));
    }

    /** 服务端配置的渠道推广位，仅用于追溯（官方备案参数里没有 pid），未配置时返回 null */
    private String channelPidOrNull() {
        String pid = tbConfig == null ? null : tbConfig.getChannelPid();
        return StringUtils.isBlank(pid) ? null : pid.trim();
    }

    private static String trimToNull(String value) {
        return StringUtils.isBlank(value) ? null : value.trim();
    }

    private TbFilingResultVo toResult(MwTbFiling filing) {
        boolean filed = filing.isFiled();
        TbFilingResultVo result = new TbFilingResultVo();
        result.setUid(filing.getUid());
        result.setTbPid(filing.getTbPid());
        result.setFiled(filed);
        result.setRelationStatus(filing.getRelationStatus() == null ? STATUS_UNFILED : filing.getRelationStatus());
        result.setStatusText(filed ? "已备案" : "未备案");
        result.setRelationUrl(filing.getRelationUrl());
        result.setSpecialId(filing.getSpecialId());
        result.setRootPid(filing.getRootPid());
        result.setAccountName(filing.getAccountName());
        result.setTotalCount(filing.getTotalCount());
        result.setFiledTime(filing.getFiledTime());
        result.setHint(filed ? null : HINT_UNFILED);
        return result;
    }
}
