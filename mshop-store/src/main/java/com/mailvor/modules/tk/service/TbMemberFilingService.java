package com.mailvor.modules.tk.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mailvor.api.MshopException;
import com.mailvor.modules.tk.config.TbConfig;
import com.mailvor.modules.tk.domain.MwTbFiling;
import com.mailvor.modules.tk.domain.MwTbFilingLog;
import com.mailvor.modules.tk.service.mapper.MwTbFilingLogMapper;
import com.mailvor.modules.tk.service.mapper.MwTbFilingMapper;
import com.mailvor.modules.tk.vo.TbFilingResultVo;
import com.mailvor.modules.tk.vo.TbPublisherInfoVo;
import com.mailvor.modules.tk.vo.TbRelationRecordVo;
import com.mailvor.utils.StringUtils;
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
 * <p><b>上游两个接口</b>（主机 {@code openapiv2.dataoke.com}，前缀 {@code /open-api/}）：</p>
 * <ul>
 *   <li>{@code get-relation-record} —— 生成推客专属的备案授权链接</li>
 *   <li>{@code get-publisher-info} —— 查询推客当前的备案状态</li>
 * </ul>
 *
 * <p><b>错误处理原则</b>：上游错误码一律翻译成中文可读提示，不把原始 code / 原文
 * 抛给前端；同时把「无法确认」按「未备案」处理，宁可多提示一次，也不让未备案的
 * 推广跑出去产生无法归因的订单。</p>
 */
@Slf4j
@Service
public class TbMemberFilingService {

    /** 备案类型：3-推客（官方 relationApp 取值 1邀请人 / 2渠道 / 3推客） */
    private static final int RELATION_APP_PUBLISHER = 3;

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
     * @param uid 推客 uid
     * @param pid 调用方传入的推广位（**不采信**，仅用于兼容旧前端；实际使用服务端配置的渠道 pid）
     * @return 含授权链接与当前备案状态的结果
     */
    @Transactional(rollbackFor = Exception.class)
    public TbFilingResultVo getRelationUrl(Long uid, String pid) {
        if (uid == null || uid <= 0) {
            throw new MshopException("请先登录后再获取备案链接");
        }
        // 推客自报 pid 不可信：用服务端配置的渠道推广位，避免把备案关系绑到他人名下
        String channelPid = tbConfig == null ? null : tbConfig.getChannelPid();
        if (StringUtils.isBlank(channelPid)) {
            throw new MshopException("淘宝渠道推广位未配置，暂无法生成备案授权链接");
        }
        String usePid = channelPid.trim();

        TbRelationRecordVo response = dataokeService.getRelationRecord(RELATION_APP_PUBLISHER, usePid);
        if (response == null || !response.isSuccess()) {
            throw new MshopException("会员备案授权链接获取失败，请稍后重试");
        }
        String url = response.getRelationUrl();
        if (StringUtils.isBlank(url)) {
            throw new MshopException("会员备案授权链接获取失败，请稍后重试");
        }

        MwTbFiling filing = getOrInit(uid, usePid);
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
     * 主动查询上游备案状态并落库，返回最新状态。
     *
     * <p>状态发生变更时写一条 {@link MwTbFilingLog}，作为结算争议的追溯依据；
     * 状态没变不写，避免推客每次刷新都产生噪音。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public TbFilingResultVo refreshStatus(Long uid) {
        if (uid == null || uid <= 0) {
            throw new MshopException("请先登录后再查询备案状态");
        }
        String channelPid = tbConfig == null ? null : tbConfig.getChannelPid();
        if (StringUtils.isBlank(channelPid)) {
            throw new MshopException("淘宝渠道推广位未配置，暂无法查询备案状态");
        }
        String usePid = channelPid.trim();

        TbPublisherInfoVo response = dataokeService.getPublisherInfo(usePid);
        if (response == null || !response.isSuccess()) {
            throw new MshopException("会员备案信息查询失败，请稍后重试");
        }

        MwTbFiling filing = getOrInit(uid, usePid);
        Integer fromStatus = filing.getRelationStatus();
        int newStatus = response.getRelationStatus() != null && response.getRelationStatus() == STATUS_FILED
                ? STATUS_FILED : STATUS_UNFILED;

        filing.setRelationStatus(newStatus);
        filing.setUpdateTime(new Date());
        if (newStatus == STATUS_FILED && filing.getFiledTime() == null) {
            filing.setFiledTime(new Date());
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
                    .tbPid(usePid)
                    .fromStatus(fromStatus)
                    .relationStatus(newStatus)
                    .remark(newStatus == STATUS_FILED ? "备案完成" : "备案状态变更为未备案")
                    .build();
            statusLog.setCreateTime(new Date());
            statusLog.setUpdateTime(new Date());
            filingLogMapper.insert(statusLog);
        }

        TbFilingResultVo result = toResult(filing);
        result.setSpecialPid(StringUtils.isBlank(response.getSpecialPid()) ? null : response.getSpecialPid().trim());
        return result;
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
        MwTbFiling filing = filingMapper.selectOne((Wrappers.<MwTbFiling>lambdaQuery()
                .eq(MwTbFiling::getUid, uid)
                .last("limit 1")));
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

    /** 读取本地备案记录，没有则构造一条新记录（尚未入库） */
    private MwTbFiling getOrInit(Long uid, String pid) {
        MwTbFiling filing = filingMapper.selectOne((Wrappers.<MwTbFiling>lambdaQuery()
                .eq(MwTbFiling::getUid, uid)
                .last("limit 1")));
        if (filing != null) {
            filing.setTbPid(pid);
            return filing;
        }
        filing = MwTbFiling.builder()
                .uid(uid)
                .tbPid(pid)
                .relationStatus(STATUS_UNFILED)
                .build();
        filing.setCreateTime(new Date());
        filing.setUpdateTime(new Date());
        return filing;
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
        result.setFiledTime(filing.getFiledTime());
        result.setHint(filed ? null : HINT_UNFILED);
        return result;
    }
}
