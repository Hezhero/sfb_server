/**
 * Copyright (C) 2018-2025
 * All rights reserved, Designed By www.mailvor.com
 */
package com.mailvor.modules.tk.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.mailvor.modules.tk.config.DataokeConfig;
import com.mailvor.modules.tk.config.JdConfig;
import com.mailvor.modules.tk.config.PddConfig;
import com.mailvor.modules.tk.param.*;
import com.mailvor.modules.tk.util.DataokeApi;
import com.mailvor.modules.tk.util.DataokeApiClient;
import com.mailvor.modules.tk.vo.*;
import com.mailvor.modules.tk.vo.jd.JdGoodsListVO;
import com.mailvor.modules.tk.vo.pdd.PddSearchDataVO;
import com.mailvor.modules.tk.vo.pdd.PddSearchListVO;
import com.mailvor.modules.tk.vo.vip.VipGoodsDetailDataVo;
import com.mailvor.modules.tk.vo.vip.VipGoodsDetailVO;
import com.mailvor.modules.tk.vo.vip.VipSearchListVO;
import com.mailvor.modules.tk.vo.vip.VipWordCodeVO;
import com.mailvor.modules.utils.TkUtil;
import com.mailvor.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.mailvor.modules.tk.util.DataokeApi.*;
import static com.mailvor.modules.utils.TkUtil.EXCLUDE_KEY_WROD_LIST;
import static com.mailvor.modules.utils.TkUtil.hasWord;

/**
 * @projectName:openapi
 * @author:
 * @createTime: 2019/04/24 14:55
 * @description:
 */
@Slf4j
@Component
public class DataokeService {
    private static TypeReference<DataokeResVo<GoodsListVo>> goodsListTypeRef = new TypeReference<DataokeResVo<GoodsListVo>>(GoodsListVo.class){};
    private static TypeReference<DataokeResVo<GoodsDetailVo>> goodsDetailTypeRef = new TypeReference<DataokeResVo<GoodsDetailVo>>(GoodsDetailVo.class){};
    private static TypeReference<DataokeResVo<GoodsWordVo>> goodsWordTypeRef = new TypeReference<DataokeResVo<GoodsWordVo>>(GoodsWordVo.class){};
    private static TypeReference<DataokeResVo<GoodsParseVo>> goodsParseTypeRef = new TypeReference<DataokeResVo<GoodsParseVo>>(GoodsParseVo.class){};
    private static TypeReference<DataokeResVo<GoodsSearchDataVo>> goodsSearchTypeRef = new TypeReference<DataokeResVo<GoodsSearchDataVo>>(GoodsSearchDataVo.class){};
    private static TypeReference<TreeMap<String, Object>> mapTypeReference = new TypeReference<TreeMap<String, Object>>() {};
    private static TypeReference<DataokeResVo<ParseContentVo>> parseContentTypeRef = new TypeReference<DataokeResVo<ParseContentVo>>(ParseContentVo.class){};

    private String regStr = "((http|https)://)([\\w-]+\\.)+[\\w$]+(\\/[\\w-?=&./]*)?";

    private Pattern pattern = Pattern.compile(regStr);

    @Resource
    private DataokeConfig config;

    @Resource
    private PddConfig pddConfig;

    @Resource
    private JdConfig jdConfig;

    @Resource
    private PddService pddService;

    public JSONObject goodsList(GoodsListParam param) {

        TreeMap<String, Object> paraMap = JSON.parseObject(JSON.toJSONString(param), mapTypeReference);
        String data = getData(DataokeApi.GOODS_LIST.getUrl(), DataokeApi.GOODS_LIST.getVersion(), paraMap);
        JSONObject jsonObject = JSON.parseObject(data);
        JSONArray resData = jsonObject.getJSONObject("data").getJSONArray("list");
        List list = resData.stream().filter(e->{
            JSONObject obj = (JSONObject) e;
            String title = obj.getString("dtitle");
            if(hasWord(title)){
                return false;
            }
            return true;
        }).collect(Collectors.toList());
        jsonObject.getJSONObject("data").put("list", list);
        return jsonObject;
    }
    public DataokeResVo<GoodsListVo> goodsVOS(GoodsListParam param) {

        TreeMap<String, Object> paraMap = JSON.parseObject(JSON.toJSONString(param), mapTypeReference);
        String data = getData(GOODS_LIST.getUrl(), GOODS_LIST.getVersion(), paraMap);
        try{
            return JSON.parseObject(data, goodsListTypeRef);
        }catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public DataokeResVo<GoodsSearchDataVo> goodsSearch(GoodsSearchParam param) {
        String keyWord = param.getKeyWords().toLowerCase();
        if(TkUtil.hasWord(keyWord) || EXCLUDE_KEY_WROD_LIST.contains(param.getKeyWords().toLowerCase())){
            DataokeResVo<GoodsSearchDataVo> res = new DataokeResVo<>();
            res.setCode(200);
            GoodsSearchDataVo data = new GoodsSearchDataVo();
            data.setList(new ArrayList<>(0));
            data.setBrandList(new ArrayList<>(0));
            res.setData(data);
            return res;
        }
        TreeMap<String, Object> paraMap = JSON.parseObject(JSON.toJSONString(param), mapTypeReference);
        String data = getData(DataokeApi.GOODS_SEARCH.getUrl(), DataokeApi.GOODS_SEARCH.getVersion(), paraMap);
        return JSON.parseObject(data, goodsSearchTypeRef);
    }
    public JSONObject goodsDetail(String goodsId) {

        TreeMap<String, Object> paraMap = new TreeMap<>();
        paraMap.put("goodsId", goodsId);
        String data = getData(DataokeApi.GOODS_DETAIL.getUrl(), DataokeApi.GOODS_DETAIL.getVersion(), paraMap);
        return JSON.parseObject(data);
    }

    public JSONObject goodsWord(String goodsId, String pid, String channelId) {

        TreeMap<String, Object> paraMap = new TreeMap<>();
        paraMap.put("goodsId", goodsId);
        if(StringUtils.isNotBlank(pid)) {
            paraMap.put("pid", pid);
        }
        if(StringUtils.isNotBlank(channelId)) {
            paraMap.put("channelId", channelId);
        }
        String data = getData(DataokeApi.GOODS_WORD.getUrl(), DataokeApi.GOODS_WORD.getVersion(), paraMap);
        return JSON.parseObject(data);
    }
    public DataokeResVo<GoodsParseVo> goodsParse(String content, String pid, String channelId) {

        TreeMap<String, Object> paraMap = new TreeMap<>();
        paraMap.put("content", content);
        if(org.apache.commons.lang3.StringUtils.isNotBlank(pid)) {
            paraMap.put("pid", pid);
        }
//         大淘客接口问题 这里传渠道id就无法解析
        if(org.apache.commons.lang3.StringUtils.isNotBlank(channelId)) {
            paraMap.put("channelId", channelId);
        }
        String data = getData(DataokeApi.GOODS_PARSE.getUrl(), DataokeApi.GOODS_PARSE.getVersion(), paraMap);
        return JSON.parseObject(data, goodsParseTypeRef);
    }
    public JSONObject getCategory() {
        TreeMap<String, Object> paraMap = new TreeMap<>();
        String data = getData(DataokeApi.GOODS_CATEGORY.getUrl(), DataokeApi.GOODS_CATEGORY.getVersion(), paraMap);
        return JSON.parseObject(data);
    }
    public JSONObject getCommentList(GoodsCommentParam param) {

        TreeMap<String, Object> paraMap = JSON.parseObject(JSON.toJSONString(param), mapTypeReference);
        String data = getData(DataokeApi.GOODS_COMMENT.getUrl(), DataokeApi.GOODS_COMMENT.getVersion(), paraMap);
        return JSON.parseObject(data);
    }
    public JSONObject getTopic() {
        TreeMap<String, Object> paraMap = new TreeMap<>();
        String data = getData(DataokeApi.TOPIC_LIST2.getUrl(), DataokeApi.TOPIC_LIST2.getVersion(), paraMap);
        return JSON.parseObject(data);
    }

    public JSONObject getBanner() {
        TreeMap<String, Object> paraMap = new TreeMap<>();
        String data = getData(DataokeApi.BANNER.getUrl(), DataokeApi.BANNER.getVersion(), paraMap);
        return JSON.parseObject(data);
    }
    public JSONObject getTbActivityList(TbActivityListParam param) {
        TreeMap<String, Object> paraMap = JSON.parseObject(JSON.toJSONString(param), mapTypeReference);
        String data = getData(DataokeApi.TB_ACTIVITY_LIST.getUrl(), DataokeApi.TB_ACTIVITY_LIST.getVersion(), paraMap);
        return JSON.parseObject(data);
    }
    public JSONObject parseTbActivity(TbActivityParseParam param) {
        TreeMap<String, Object> paraMap = JSON.parseObject(JSON.toJSONString(param), mapTypeReference);
        String data = getData(DataokeApi.TB_ACTIVITY_PARSE.getUrl(), DataokeApi.TB_ACTIVITY_PARSE.getVersion(), paraMap);
        return JSON.parseObject(data);
    }

    public JSONObject parseContent(ParseContentParam param) {
        if(param.getContent()!= null && param.getContent().contains("yangkeduo.com")) {
            try {
                //修复ios 拼多多查券无法识别的问题，goods_id必须放在?后面
                param.setContent(parsePddContent(param.getContent()));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        TreeMap<String, Object> paraMap = JSON.parseObject(JSON.toJSONString(param), mapTypeReference);
        String data = getData(DataokeApi.GOODS_PARSE_ALL.getUrl(), DataokeApi.GOODS_PARSE_ALL.getVersion(), paraMap);
        return JSON.parseObject(data);
    }

    protected String parsePddContent(String content) throws UnsupportedEncodingException {
        int goodsIdIndex = content.lastIndexOf("&amp;goods_id=");
        if(goodsIdIndex > 0) {
            content = URLDecoder.decode(content, "utf-8").replace("&amp;", "&");
            String goodsIdParam = content.substring(content.lastIndexOf("&goods_id=")).replace("&", "");
            content = content.replace(".html?", ".html?" + goodsIdParam + "&");
        }
        return content;
    }
    public static void main(String[] args) throws UnsupportedEncodingException {
        DataokeService service = new DataokeService();
        String content = service.parsePddContent(
//                "https://mobile.yangkeduo.com/goods.html?goods_id=410160347693&page_from=29&pxq_secret_key=33TRW3AEOYKNICYPDAMRKZYAIBORSSAIYRVGAPUEYLIBJYGXXFWQ&share_uin=OK3ATMDGM5DQLD75P3625MMMGY_GEXDA&refer_share_id=e7fa4c701d3545a096709154e1b3f17c&refer_share_uin=OK3ATMDGM5DQLD75P3625MMMGY_GEXDA&refer_share_channel=copy_link&refer_share_form=text"
        "https://mobile.yangkeduo.com/goods2.html?refer_share_id=7hRYk1RcMeDNOBGy1590mIysmB5AZ60n&refer_share_channel=copy_link&pxq_secret_key=RFY2XNUVPFJEMXA6PPGD6UXLEBZYQJYTBWKFKECUFP3BG444E5UQ&_wvx=10&_x_ddjb_act=%7B%22st%22%3A%221%22%7D&_x_ddjb_id=1784892_252288888%7CCC_230129_1784892_252288888_7b316846d52696aba7de6707cc9488b5&_x_ddjb_gs=%7B%22gs_src%22%3A%221%22%2C%22gs_scn%22%3A%223%22%7D&_wv=41729&share_uin=FEHOG3PNIIGI24R2DELMGM34YU_GEXDA&page_from=29&refer_share_uin=FEHOG3PNIIGI24R2DELMGM34YU_GEXDA&_x_customParameters=%7B%22uid%22%3A%226%22%7D&goods_id=410160347693"
        );
        System.out.println(content);
    }

    public JdGoodsListVO brandListJD(com.mailvor.modules.tk.param.jd.GoodsListJDParam param) {
        TreeMap<String, Object> params = JSON.parseObject(JSON.toJSONString(param), mapTypeReference);
        String data = getData(DataokeApi.JD_BRAND_LIST.getUrl(), DataokeApi.JD_BRAND_LIST.getVersion(), params);
        return JSON.parseObject(data, JdGoodsListVO.class);
    }

    public JdGoodsListVO ninesListJD(com.mailvor.modules.tk.param.jd.GoodsListJDParam param) {
        TreeMap<String, Object> params = JSON.parseObject(JSON.toJSONString(param), mapTypeReference);
        String data = getData(DataokeApi.JD_NINES_LIST.getUrl(), DataokeApi.JD_NINES_LIST.getVersion(), params);
        return JSON.parseObject(data, JdGoodsListVO.class);
    }

    protected String getData(String url, String version, TreeMap<String, Object> paraMap) {
        // 配置缺失时在发请求前就抛业务异常，避免空 key 签名/发请求或 JSON.parseObject 抛 JSONException
        config.requireConfigured();
        return DataokeApiClient.sendReq(
                url,
                config.getKey(),
                config.getSecret(),
                version,
                paraMap);
    }

    public JSONObject goodsDetailJD(String goodsId, String itemId) {

        TreeMap<String, Object> paraMap = new TreeMap<>();
        if(StringUtils.isNotBlank(goodsId) && !"0".equals(goodsId) && !goodsId.equals(itemId)) {
            paraMap.put("skuIds", goodsId);
        }
        if(StringUtils.isNotBlank(itemId)) {
            paraMap.put("itemIds", itemId);
        }
        String data = getData(DataokeApi.JD_GOODS_DETAIL.getUrl(), DataokeApi.JD_GOODS_DETAIL.getVersion(), paraMap);
        return JSON.parseObject(data);
    }
    public VipGoodsDetailDataVo goodsDetailVIP(String goodsId, String openId) {

        TreeMap<String, Object> paraMap = new TreeMap<>();
        paraMap.put("goodsIdList", "[\"" + goodsId + "\"]");
        JSONObject request = new JSONObject();

        request.put("openId", openId);
        request.put("realCall", "true");
        paraMap.put("request", request.toJSONString());
//        paraMap.put("chanTag", "default_pid");

        String data = getData(DataokeApi.VIP_GOODS_DETAIL.getUrl(), DataokeApi.VIP_GOODS_DETAIL.getVersion(), paraMap);
        VipGoodsDetailDataVo dataVo = JSON.parseObject(data, VipGoodsDetailDataVo.class);
        if(CollectionUtils.isNotEmpty(dataVo.getData())) {
            VipGoodsDetailVO detailVO = dataVo.getData().get(0);
            detailVO.setShopName(detailVO.getStoreName());
            dataVo.setGoods(detailVO);
            dataVo.setData(null);
        }

        return dataVo;
    }

    public JSONObject goodsWordJD(String itemUrl, String couponUrl, String pid) {

        TreeMap<String, Object> paraMap = new TreeMap<>();
        paraMap.put("materialId", itemUrl);
        if(StringUtils.isNotBlank(couponUrl)) {
            paraMap.put("couponUrl", couponUrl);
        }
        if(pid != null) {
            paraMap.put("positionId", pid);
        }
        paraMap.put("unionId", jdConfig.getUnionId());
        String data = getData(DataokeApi.JD_GOODS_WORD.getUrl(), DataokeApi.JD_GOODS_WORD.getVersion(), paraMap);
        return JSON.parseObject(data);
    }

    public JSONObject parseUrlJD(String itemUrl) {

        TreeMap<String, Object> paraMap = new TreeMap<>();
        paraMap.put("url", itemUrl);
        paraMap.put("unionId", jdConfig.getUnionId());
        String data = getData(DataokeApi.JD_PARSE_URL.getUrl(), DataokeApi.JD_PARSE_URL.getVersion(), paraMap);
        return JSON.parseObject(data);
    }
    public JSONObject goodsDetailPDD(String goodsSign) {

        TreeMap<String, Object> paraMap = new TreeMap<>();
        paraMap.put("goodsSign", goodsSign);
        String data = getData(DataokeApi.PDD_GOODS_DETAIL.getUrl(), DataokeApi.PDD_GOODS_DETAIL.getVersion(), paraMap);
        return JSON.parseObject(data);
    }
    public JSONObject goodsWordPDD(String goodsSign,Long uid) {

        TreeMap<String, Object> paraMap = new TreeMap<>();
        paraMap.put("pid", pddConfig.getPid());
        paraMap.put("goodsSign", goodsSign);
        if(uid != null) {
            int auth = pddService.authQuery(uid);
            if(auth == 1) {
                paraMap.put("customParameters", pddConfig.getParam(uid));
            }
        }
        String data = getData(DataokeApi.PDD_GOODS_WORD.getUrl(), DataokeApi.PDD_GOODS_WORD.getVersion(), paraMap);
        return JSON.parseObject(data);
    }

    public JSONObject goodsSimilarList(String id, String size) {
        TreeMap<String, Object> paraMap = new TreeMap<>();
        paraMap.put("id", id);
        paraMap.put("size", size);
        String data = getData(DataokeApi.GOODS_SIMILAR.getUrl(), DataokeApi.GOODS_SIMILAR.getVersion(), paraMap);
        return JSON.parseObject(data);
    }

    public JSONObject ddq(String roundTime) {
        TreeMap<String, Object> paraMap = new TreeMap<>();
        if(StringUtils.isNotBlank(roundTime)) {
            paraMap.put("roundTime", roundTime);
        }
        String data = getData(DataokeApi.TB_DDQ.getUrl(), DataokeApi.TB_DDQ.getVersion(), paraMap);
        return JSON.parseObject(data);
    }

    public JSONObject rankingList(RankingListParam param) {
        TreeMap<String, Object> paraMap = JSON.parseObject(JSON.toJSONString(param), mapTypeReference);
        String data = getData(DataokeApi.TB_RANK_LIST.getUrl(), DataokeApi.TB_RANK_LIST.getVersion(), paraMap);
        JSONObject jsonObject = JSON.parseObject(data);
        JSONArray resData = jsonObject.getJSONArray("data");
        List list = resData.stream().filter(e->{
            JSONObject obj = (JSONObject) e;
            String title = obj.getString("title");
            if(hasWord(title)){
                return false;
            }
            return true;
        }).collect(Collectors.toList());
        jsonObject.put("data", list);
        return jsonObject;
    }

    public JSONObject shopConvert(String shopId, String shopName, String pid, String channelId) {
        TreeMap<String, Object> paraMap = new TreeMap<>();
        paraMap.put("sellerId", shopId);
        paraMap.put("shopName", shopName);
        paraMap.put("appkey", config.getKey());
        if(StringUtils.isNotBlank(pid)) {
            paraMap.put("pid", pid);
        }
        if(StringUtils.isNotBlank(channelId)) {
            paraMap.put("relationId", channelId);
        }
        String data = getData(DataokeApi.TB_SHOP_CONVERT.getUrl(), DataokeApi.TB_SHOP_CONVERT.getVersion(), paraMap);
        return JSON.parseObject(data);
    }

    public JSONObject getBrandList(Integer cid, Integer pageId) {
        TreeMap<String, Object> paraMap = new TreeMap<>();
        paraMap.put("cid", cid.toString());
        paraMap.put("pageId", pageId.toString());
        paraMap.put("pageSize", "10");
        paraMap.put("appkey", config.getKey());
        String data = getData(DataokeApi.TB_BRAND_LIST.getUrl(), DataokeApi.TB_BRAND_LIST.getVersion(), paraMap);
        return JSON.parseObject(data);
    }
    public JSONObject getBrandGoodsList(String brandId, Integer pageId, Integer pageSize) {
        TreeMap<String, Object> paraMap = new TreeMap<>();
        paraMap.put("brandId", brandId);
        paraMap.put("pageId", pageId.toString());
        paraMap.put("pageSize", pageSize.toString());
        paraMap.put("appkey", config.getKey());
        String data = getData(DataokeApi.TB_BRAND_GOODS_LIST.getUrl(), DataokeApi.TB_BRAND_GOODS_LIST.getVersion(), paraMap);
        return JSON.parseObject(data);
    }
    public VipSearchListVO goodsListVip(GoodsListVipParam param) {
        //唯品会结构和统一结构相同，无需转换
        TreeMap<String, Object> paraMap = JSON.parseObject(JSON.toJSONString(param), mapTypeReference);
        String data = getData(DataokeApi.VIP_GOODS_LIST.getUrl(), DataokeApi.VIP_GOODS_LIST.getVersion(), paraMap);
        return JSON.parseObject(data,VipSearchListVO.class);
    }


    public VipWordCodeVO goodsWordVIP(String itemUrl, String statParam, JSONObject urlGenRequest) {

        TreeMap<String, Object> paraMap = new TreeMap<>();
        paraMap.put("urlList", "[\"" + itemUrl + "\"]");
        if(org.apache.commons.lang3.StringUtils.isNotBlank(statParam)) {
            paraMap.put("statParam", statParam);
        }
        paraMap.put("urlGenRequest", urlGenRequest.toJSONString());
        String data = getData(DataokeApi.VIP_GOODS_WORD.getUrl(), DataokeApi.VIP_GOODS_WORD.getVersion(), paraMap);
        VipWordCodeVO codeVO = JSON.parseObject(data, VipWordCodeVO.class);
        if(codeVO != null &&codeVO.getData() != null && CollectionUtils.isNotEmpty(codeVO.getData().getList())) {
            codeVO.setWord(codeVO.getData().getList().get(0));
            codeVO.getData().setList(null);
        }
        return codeVO;
    }


    public PddSearchListVO goodsListPdd(GoodsListPddParam param) {
        if(org.apache.commons.lang3.StringUtils.isNotBlank(param.getKeyword())) {
            String keyWord = param.getKeyword().toLowerCase();
            if(hasWord(keyWord) || EXCLUDE_KEY_WROD_LIST.contains(keyWord)){
                PddSearchListVO res = new PddSearchListVO();
                PddSearchDataVO data = new PddSearchDataVO();
                data.setList(new ArrayList<>(0));
                data.setTotal(0);
                res.setCode(0);
                res.setMsg("成功");
                res.setData(data);
                return res;
            }
        }

        TreeMap<String, Object> paraMap = JSON.parseObject(JSON.toJSONString(param), mapTypeReference);
        String data = getData(DataokeApi.PDD_GOODS_SEARCH.getUrl(), DataokeApi.PDD_GOODS_SEARCH.getVersion(), paraMap);
        return JSON.parseObject(data, PddSearchListVO.class);

    }
    public JSONObject goodsCatePdd(Integer parentId) {

        TreeMap<String, Object> paraMap = new TreeMap<>();
        paraMap.put("parentId", parentId.toString());
        String data = getData(DataokeApi.PDD_GOODS_CATE.getUrl(), DataokeApi.PDD_GOODS_CATE.getVersion(), paraMap);
        return JSON.parseObject(data);
    }
    public JSONObject vipGoodsSearch(GoodsSearchVipParam param) {
        if(StringUtils.isNotBlank(param.getKeyword())) {
            String keyWord = param.getKeyword().toLowerCase();
            if(TkUtil.hasWord(keyWord) || EXCLUDE_KEY_WROD_LIST.contains(keyWord)){
                JSONObject res = new JSONObject();
                JSONObject data = new JSONObject();
                data.put("goodsInfoList", new JSONArray());
                data.put("total", 0);
                res.put("code", 0);
                res.put("msg", "成功");
                res.put("data", data);
                return res;
            }
        }
        TreeMap<String, Object> paraMap = JSON.parseObject(JSON.toJSONString(param), mapTypeReference);
        String data = getData(DataokeApi.VIP_GOODS_SEARCH.getUrl(), DataokeApi.VIP_GOODS_SEARCH.getVersion(), paraMap);
        return JSON.parseObject(data);
    }

    public TBResVo queryTBList(QueryTBParam param) {
        TreeMap<String, Object> paraMap = JSON.parseObject(JSON.toJSONString(param), mapTypeReference);
        String data = getData(TB_QUERY_ORDER.getUrl(), TB_QUERY_ORDER.getVersion(), paraMap);
        log.warn("*淘宝订单："+ data);
        try{
            return JSON.parseObject(data, TBResVo.class);
        }catch (Exception e) {
            e.printStackTrace();
        }
        return null;

    }

    public JdResVo queryJdList(QueryJdParam param) {
        TreeMap<String, Object> paraMap = JSON.parseObject(JSON.toJSONString(param), mapTypeReference);
        String data = getData(JD_QUERY_ORDER.getUrl(), JD_QUERY_ORDER.getVersion(), paraMap);
        log.warn("*京东订单："+ data);
        if(StringUtils.isBlank(data)){
            return null;
        }
        try{
            return JSON.parseObject(data, JdResVo.class);
        }catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    public VipResVo queryVipList(QueryVipParam param) {
        TreeMap<String, Object> paraMap = JSON.parseObject(JSON.toJSONString(param), mapTypeReference);
        String data = getData(VIP_QUERY_ORDER.getUrl(), VIP_QUERY_ORDER.getVersion(), paraMap);
        log.warn("*唯品会订单："+ data);
        if(StringUtils.isBlank(data)){
            return null;
        }
        try{
            return JSON.parseObject(data, VipResVo.class);
        }catch (Exception e) {
            e.printStackTrace();
        }
        return null;

    }
    protected static String DY_ORDER_QUERY_URL = "%s?date_type=%s&size=%s&page=%s&order_type=%s&search_start_time=%s&search_end_time=%s&app_token=%s";
    public DyResVo queryDyList(QueryDyParam param) {
//        String data = "";
//        try {
//            String url = String.format(DY_ORDER_QUERY_URL, param.getUrl(),
//                    param.getDataType(), param.getSize(), param.getPage(),param.getOrderType(),
//                    URLEncoder.encode(param.getSearchStartTime(), "UTF-8"),
//                    URLEncoder.encode(param.getSearchEndTime(),"UTF-8"), param.getAppToken());
//
//
//            data = HttpUtil.httpGetRequest(url);
//        } catch (UnsupportedEncodingException e) {
//            e.printStackTrace();
//        }
        TreeMap<String, Object> paraMap = JSON.parseObject(JSON.toJSONString(param), mapTypeReference);
        String data = getData(DY_QUERY_ORDER.getUrl(), DY_QUERY_ORDER.getVersion(), paraMap);
        log.warn("*抖音订单："+ data);
        try{
            return JSON.parseObject(data, DyResVo.class);
        }catch (Exception e) {
            e.printStackTrace();
        }
        return null;

    }
    /**
     * 不可用 大淘客的订单采集有bug 使用 {@link PddService#queryPddOrderList(QueryPddParam)}
     *
     * @param param the param
     * @return the pdd res vo
     */
    public PddResVo queryPddList(QueryPddParam param) {
        TreeMap<String, Object> paraMap = JSON.parseObject(JSON.toJSONString(param), mapTypeReference);
        String data = getData(PDD_QUERY_ORDER.getUrl(), PDD_QUERY_ORDER.getVersion(), paraMap);
        log.warn("拼多多订单："+ data);
        return JSON.parseObject(data, PddResVo.class);
    }

    // ------------------------------------------------------------------
    // 会员备案（B2B2C 归因前提）
    // 官方文档：https://www.dataoke.com/api-d.html?id=135 / id=136
    // 主机 openapi.dataoke.com，路径前缀 /open-api/（与其它 /api/ 接口不同）。
    // ------------------------------------------------------------------

    /** externalType 渠道类型：0-其他（本平台统一按「其他」上报，不走微信/微博等渠道） */
    public static final int EXTERNAL_TYPE_OTHER = 0;

    /** opType 操作类型：1-新绑（首次备案） */
    public static final int OP_TYPE_BIND = 1;

    /** opType 操作类型：2-更新（已有备案记录时重新生成授权链接） */
    public static final int OP_TYPE_UPDATE = 2;

    /**
     * 获取会员备案授权链接（官方 {@code get-relation-record}）。
     *
     * <p>官方参数表（api-d.html?id=135）：</p>
     * <ul>
     *   <li>{@code externalId} 必填，私域用户自定义唯一标识 —— 本平台传推客 uid 的字符串</li>
     *   <li>{@code externalType} 必填，渠道类型 0其他 1微信 2微博 3抖音 4快手 5QQ —— 固定 0</li>
     *   <li>{@code opType} 必填，1新绑 2更新 —— 由调用方按「是否已有备案记录」决定</li>
     *   <li>{@code redirectUrl} 必填，授权完成后跳转实际转链后的商品短链</li>
     *   <li>{@code authId} / {@code ucrowdId} 选填，本平台不传（用默认授权）</li>
     * </ul>
     *
     * <p><b>注意</b>：历史实现里的 {@code relationApp} / {@code pid} 两个参数在官方文档中
     * 并不存在，已删除。</p>
     *
     * @param externalId   私域用户自定义唯一标识（推客 uid 的字符串形式）
     * @param externalType 渠道类型，见 {@link #EXTERNAL_TYPE_OTHER}
     * @param opType       操作类型：1新绑 2更新
     * @param redirectUrl  授权完成后跳转的商品短链
     * @return 上游响应；网络异常或返回无法解析时返回 null（由调用方转中文化提示）
     */
    public TbRelationRecordVo getRelationRecord(String externalId, int externalType,
                                                int opType, String redirectUrl) {
        TreeMap<String, Object> paraMap = new TreeMap<>();
        paraMap.put("externalId", externalId);
        paraMap.put("externalType", externalType);
        paraMap.put("opType", opType);
        paraMap.put("redirectUrl", redirectUrl);
        return requestFiling(DataokeApi.TB_RELATION_RECORD, paraMap, TbRelationRecordVo.class);
    }

    /**
     * 查询会员备案信息（官方 {@code get-publisher-info}）。
     *
     * <p>官方参数表（api-d.html?id=136）：</p>
     * <ul>
     *   <li>{@code externalId} 必填，私域用户自定义唯一标识 —— 与生成授权链接时保持一致</li>
     *   <li>{@code externalType} 必填，渠道类型 —— 与生成授权链接时保持一致（固定 0）</li>
     *   <li>{@code authId} 选填，本平台不传</li>
     * </ul>
     *
     * <p><b>注意</b>：历史实现只传 {@code pid}，那不是官方参数，已删除。</p>
     *
     * @param externalId   私域用户自定义唯一标识（推客 uid 的字符串形式）
     * @param externalType 渠道类型，见 {@link #EXTERNAL_TYPE_OTHER}
     * @return 上游响应；网络异常或返回无法解析时返回 null（由调用方转中文化提示）
     */
    public TbPublisherInfoVo getPublisherInfo(String externalId, int externalType) {
        TreeMap<String, Object> paraMap = new TreeMap<>();
        paraMap.put("externalId", externalId);
        paraMap.put("externalType", externalType);
        return requestFiling(DataokeApi.TB_PUBLISHER_INFO, paraMap, TbPublisherInfoVo.class);
    }

    /**
     * 会员备案类接口的统一请求与解析。
     *
     * <p>与其它方法的区别：上游异常/脏报文统一收敛成 {@code null}，不向上抛异常，
     * 也不把 {@code DataokeApiClient} 返回的裸中文字符串当正常报文透传——
     * 备案是结算前置条件，任何"看起来成功"的误判都会直接导致资损。</p>
     */
    private <T> T requestFiling(DataokeApi api, TreeMap<String, Object> paraMap, Class<T> clazz) {
        String data;
        try {
            data = getData(api.getUrl(), api.getVersion(), paraMap);
        } catch (RuntimeException e) {
            log.error("大淘客会员备案接口调用异常 api={} err={}", api.getDesc(), e.getMessage());
            return null;
        }
        if (StringUtils.isBlank(data) || !data.trim().startsWith("{")) {
            log.error("大淘客会员备案接口返回非 JSON api={} resp={}", api.getDesc(), data);
            return null;
        }
        try {
            return JSON.parseObject(data, clazz);
        } catch (RuntimeException e) {
            log.error("大淘客会员备案接口响应解析失败 api={} resp={}", api.getDesc(), data);
            return null;
        }
    }

    // ------------------------------------------------------------------
    // 商品库同步三件套（工单 05）
    // 官方文档：api-d.html?id=3（商品更新）、id=11（失效商品）、id=12（定时拉取）。
    // 三个接口共用 scroll_id 分页协议，翻页循环统一交给 DtkScrollPager。
    // ------------------------------------------------------------------

    /**
     * 拉取一页「商品更新」（官方 {@code get-newest-goods}，v1.2.0）。
     *
     * <p>官方参数表（id=3）：{@code pageId} 必填（scroll_id 分页，首次传 1）、
     * {@code pageSize} 必填（仅 10/50/100/200，默认 10），其余可选。</p>
     *
     * <p><b>注意</b>：本接口官方响应<b>不含</b> title / mainPic / shopName，
     * 只有价格与分类字段。需要完整字段请用 {@link #pullGoodsByTime}。</p>
     *
     * @param param 分页与筛选参数，{@code pageId} 应为本次要请求的游标
     * @return 上游原始 JSON；网络异常或报文不可解析时返回 {@code null}
     */
    public JSONObject newestGoods(GetNewestGoodsParam param) {
        TreeMap<String, Object> paraMap = param.toParamMap();
        String data = getData(DataokeApi.GOODS_NEWEST.getUrl(),
                DataokeApi.GOODS_NEWEST.getVersion(), paraMap);
        return parseGoodsPage(DataokeApi.GOODS_NEWEST, paraMap, data);
    }

    /**
     * 拉取一页「失效商品」（官方 {@code get-stale-goods-by-time}，v1.0.1）。
     *
     * <p>官方参数表（id=11）：{@code pageId} 必填、{@code pageSize} 可选（默认 100）、
     * {@code startTime} / {@code endTime} 可选。</p>
     *
     * <p><b>查询窗口仅限 24 小时</b>：{@code startTime} 只能追溯到最近 24 小时内的下架记录。
     * 窗口收敛由调用方负责（见 {@code DtkGoodsSyncService}）。</p>
     *
     * @param param 分页与时间窗口参数
     * @return 上游原始 JSON；网络异常或报文不可解析时返回 {@code null}
     */
    public JSONObject staleGoods(StaleGoodsParam param) {
        TreeMap<String, Object> paraMap = param.toParamMap();
        String data = getData(DataokeApi.GOODS_STALE.getUrl(),
                DataokeApi.GOODS_STALE.getVersion(), paraMap);
        return parseGoodsPage(DataokeApi.GOODS_STALE, paraMap, data);
    }

    /**
     * 拉取一页「定时拉取商品」（官方 {@code pull-goods-by-time}，v1.2.3）。
     *
     * <p>官方参数表（id=12）：{@code pageId} 必填、其余可选。响应字段最全（约 50 个），
     * 是本地商品库字段的唯一完整来源。</p>
     *
     * @param param 分页与筛选参数
     * @return 上游原始 JSON；网络异常或报文不可解析时返回 {@code null}
     */
    public JSONObject pullGoodsByTime(PullGoodsByTimeParam param) {
        TreeMap<String, Object> paraMap = param.toParamMap();
        String data = getData(DataokeApi.GOODS_PULL_BY_TIME.getUrl(),
                DataokeApi.GOODS_PULL_BY_TIME.getVersion(), paraMap);
        return parseGoodsPage(DataokeApi.GOODS_PULL_BY_TIME, paraMap, data);
    }

    /**
     * 商品库类接口的统一响应处理。
     *
     * <p>与 {@link #requestFiling} 的区别：商品同步是<b>批量定时任务</b>，失败不应抛异常
     * 中断整轮任务，但要能区分「上游返回了错误码」和「报文根本没解析出来」——
     * 前者记 warn（可能是参数/权限问题），后者记 error（网络或上游故障）。
     * 两者都返回 {@code null}，由调用方的翻页器按「本页没有数据」终止。</p>
     */
    private JSONObject parseGoodsPage(DataokeApi api, TreeMap<String, Object> paraMap, String data) {
        if (StringUtils.isBlank(data) || !data.trim().startsWith("{")) {
            // DataokeApiClient 在参数/密钥缺失时返回裸中文字符串，这里必须当失败处理
            log.error("大淘客商品库接口返回非 JSON api={} 参数={} resp={}",
                    api.getDesc(), paraMap, data);
            return null;
        }
        JSONObject res;
        try {
            res = JSON.parseObject(data);
        } catch (RuntimeException e) {
            log.error("大淘客商品库接口响应解析失败 api={} resp={}", api.getDesc(), data);
            return null;
        }
        if (res == null) {
            return null;
        }
        Integer code = res.getInteger("code");
        if (code == null || code != 0) {
            // 官方错误码表：-1 服务器错误、1 参数错误
            log.warn("大淘客商品库接口返回错误码 api={} code={} msg={} 参数={}",
                    api.getDesc(), code, res.getString("msg"), paraMap);
        }
        return res;
    }

    // ------------------------------------------------------------------
    // 违规订单（工单 04）
    // 官方文档：https://www.dataoke.com/pmc/api-d.html?id=86
    // 路径前缀是 /api/（与备案接口的 /open-api/ 不同），主机 openapi.dataoke.com。
    //
    // 业务背景：淘宝联盟判定「店铺淘宝客」「订单虚假交易」后会冻结或扣除佣金。
    // 本地若不查，结算给推客的金额会系统性地高于实际到账 —— 这是直接资损。
    // ------------------------------------------------------------------

    /** 官方限制：查询时间跨度不超过 30 天。超过会被上游拒掉，因此在组装参数时就夹住。 */
    public static final int FOUL_ORDER_MAX_SPAN_DAYS = 30;

    /** 官方默认每页 10 条；本平台统一取 100，减少往返次数。 */
    public static final int FOUL_ORDER_PAGE_SIZE = 100;

    /**
     * 组装「违规订单查询」{@code get-foul-order-list} 的请求参数。
     *
     * <p>参数名严格照官方 {@code request_tree0}，一个不多一个不少：</p>
     * <ul>
     *   <li>{@code startTime} 必填，查询开始时间（以淘客订单创建时间起算），格式 {@code yyyy-MM-dd HH:mm:ss}</li>
     *   <li>{@code span} 查询时间跨度，单位天，<b>不超过 30</b></li>
     *   <li>{@code pageNo} 页码，从 1 开始</li>
     *   <li>{@code pageSize} 每页条数，官方默认 10</li>
     *   <li>{@code tbTradeId} 选填，按子订单号精确查</li>
     * </ul>
     *
     * <p><b>为什么 span 总是下发</b>：它是时间窗口上界而非普通分页参数。不传的话上游
     * 按自己的默认跨度处理，可能直接越过 30 天限制报错；因此这里不省略，而是夹到
     * {@code [1, 30]} 后下发。其余选填项为 null 时<b>不放进 map</b> ——
     * {@code SignMD5Util} 会把 null 拼成 {@code pageNo=null}，直接验签失败。</p>
     *
     * @param startTime 查询开始时间，null/空白时按「今天 00:00:00」兜底
     * @param span      查询跨度（天），null 或超出范围时夹到 [1, 30]
     * @param pageNo    页码，null 时用 1
     * @param pageSize  每页条数，null 时用 {@link #FOUL_ORDER_PAGE_SIZE}
     * @param tbTradeId 子订单号，可为 null
     * @return 按 key 字典序排好的参数字典（签名依赖字典序，必须是 TreeMap）
     */
    public TreeMap<String, Object> foulOrderParams(String startTime, Integer span,
                                                   Integer pageNo, Integer pageSize,
                                                   String tbTradeId) {
        TreeMap<String, Object> paraMap = new TreeMap<>();
        paraMap.put("startTime", normalizeFoulStartTime(startTime));
        paraMap.put("span", clampFoulSpan(span));
        if (pageNo != null && pageNo > 0) {
            paraMap.put("pageNo", pageNo);
        }
        if (pageSize != null && pageSize > 0) {
            paraMap.put("pageSize", pageSize);
        }
        if (StringUtils.isNotBlank(tbTradeId)) {
            // 官方类型标 Number，但子订单号是 19 位，超出 int 精度。
            // 按字符串传上游同样受理（官方示例值本身就是字符串形态），且能避免精度丢失。
            paraMap.put("tbTradeId", tbTradeId.trim());
        }
        return paraMap;
    }

    /**
     * 校验并收敛 {@code span} 到官方允许的 [1, 30] 天。
     *
     * @param span 调用方期望的跨度，可为 null
     * @return 合法跨度；null 或越界时给出 {@link #FOUL_ORDER_MAX_SPAN_DAYS}
     */
    public static int clampFoulSpan(Integer span) {
        if (span == null || span < 1 || span > FOUL_ORDER_MAX_SPAN_DAYS) {
            return FOUL_ORDER_MAX_SPAN_DAYS;
        }
        return span;
    }

    /**
     * 把 {@code startTime} 收敛成官方要求的 {@code yyyy-MM-dd HH:mm:ss}。
     *
     * <p>允许调用方只传日期（官方调用示例给的就是 {@code "2021-12-10"}），
     * 此时补足 {@code 00:00:00}，避免上游因格式不符返回「参数错误」。</p>
     */
    public static String normalizeFoulStartTime(String startTime) {
        if (StringUtils.isBlank(startTime)) {
            return LocalDate.now().atStartOfDay()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }
        String v = startTime.trim();
        if (v.length() == 10 && v.charAt(4) == '-' && v.charAt(7) == '-') {
            return v + " 00:00:00";
        }
        return v;
    }

    /**
     * 拉取一页违规订单（官方 {@code get-foul-order-list}）。
     *
     * <p>与 {@link #queryTBList} 等方法的区别：上游失败时不吞异常、不返回半成品，
     * 而是统一返回 {@code null} 并记日志，由调用方的翻页器按「本页没数据」收尾 ——
     * 违规订单关系到结算，宁可这轮不更新，也不能把残缺报文当成功。</p>
     *
     * @param startTime 查询开始时间
     * @param span      查询跨度（天），内部会夹到 30
     * @param pageNo    页码，从 1 开始
     * @param pageSize  每页条数
     * @param tbTradeId 子订单号，可为 null
     * @return 解析后的响应；上游出错或报文无法解析时返回 null
     */
    public FoulOrderResVo queryFoulOrderList(String startTime, Integer span,
                                             Integer pageNo, Integer pageSize,
                                             String tbTradeId) {
        TreeMap<String, Object> paraMap = foulOrderParams(startTime, span, pageNo, pageSize, tbTradeId);
        String data;
        try {
            data = getData(TB_FOUL_ORDER.getUrl(), TB_FOUL_ORDER.getVersion(), paraMap);
        } catch (RuntimeException e) {
            log.error("大淘客违规订单接口调用异常 参数={} err={}", paraMap, e.getMessage());
            return null;
        }
        if (StringUtils.isBlank(data) || !data.trim().startsWith("{")) {
            // DataokeApiClient 在参数/密钥缺失时返回的是裸中文字符串而不是 JSON
            log.error("大淘客违规订单接口返回非 JSON 参数={} resp={}", paraMap, data);
            return null;
        }
        FoulOrderResVo res;
        try {
            res = JSON.parseObject(data, FoulOrderResVo.class);
        } catch (RuntimeException e) {
            log.error("大淘客违规订单接口响应解析失败 参数={} resp={}", paraMap, data);
            return null;
        }
        if (res == null) {
            return null;
        }
        if (!res.isSuccess()) {
            // 官方错误码表：-1 服务器错误、1 参数错误
            log.warn("大淘客违规订单接口返回错误码 code={} msg={} 参数={}",
                    res.getCode(), res.getMsg(), paraMap);
        }
        return res;
    }
}
