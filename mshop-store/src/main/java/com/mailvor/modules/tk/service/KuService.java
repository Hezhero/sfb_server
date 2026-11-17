/**
 * Copyright (C) 2018-2025
 * All rights reserved, Designed By www.mailvor.com
 */
package com.mailvor.modules.tk.service;


import com.mailvor.modules.tk.config.KuCmsAddress;
import cn.hutool.http.HttpRequest;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.mailvor.modules.shop.service.MwSystemConfigService;
import com.mailvor.modules.tk.constants.TkConstants;
import com.mailvor.modules.tk.param.GoodsListDyParam;
import com.mailvor.modules.tk.param.GoodsListKsParam;
import com.mailvor.modules.tk.param.ParseContentParam;
import com.mailvor.modules.tk.param.QueryDyKuParam;
import com.mailvor.modules.tk.param.QueryEleKuParam;
import com.mailvor.modules.tk.param.QueryKsOrderParam;
import com.mailvor.modules.tk.param.jd.GoodsListJDParam;
import com.mailvor.modules.tk.service.dto.DyLifeCityDto;
import com.mailvor.modules.tk.util.HttpUtils;
import com.mailvor.modules.tk.util.SignMD5Util;
import com.mailvor.modules.tk.vo.*;
import com.mailvor.modules.tk.vo.jd.JdKuCommonGoodsDetailDataVo;
import com.mailvor.modules.tk.vo.jd.JdKuCommonGoodsDetailVO;
import com.mailvor.modules.tk.vo.jd.JdKuSearchListVO;
import com.mailvor.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @projectName:openapi
 * @author:
 * @createTime: 2023/01/24 14:55
 * @description:
 */
@Slf4j
@Component
public class KuService {
    /**
     * 签名日期格式。DateTimeFormatter 是不可变且线程安全的，替换了原来的静态
     * SimpleDateFormat（并发 format 会产生字段串扰，导致签名错误）。
     */
    private static final DateTimeFormatter SIGN_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 将 Date 格式化为签名所需的日期字符串。线程安全，供 clipboard/shortLink 共用。
     */
    public static String formatDate(Date date) {
        return SIGN_DATE_FORMATTER.format(
                LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault()));
    }

    /**
     * HTTP POST 接缝：便于测试观察请求体，也便于统一失败处理。
     */
    protected String postJson(String url, String json) {
        return HttpUtils.doPost(url, json);
    }

    /** 上游失败时返回的结构化错误（JSON），调用方据此显式处理，不再出现空串 → NPE。 */
    private static String errorJson(String message) {
        JSONObject error = new JSONObject();
        error.put("code", 500);
        error.put("msg", message);
        return error.toJSONString();
    }

    public static final String KU_API_BASE = "https://v3.api.haodanku.com";


    public static final String KU_API_V3 = KU_API_BASE + "/rest";

    public static final String LOCAL_LIFE_API = KU_API_BASE + "/hv_ratesurl";
    public static final String LOCAL_LIFE_ORDER_API = KU_API_BASE + "/hv_order_list";


    public static final String MEITUAN_ACTIVITY_LIST_API = KU_API_BASE + "/meituan_activity_list";
    public static final String MEITUAN_ACTIVITY_WORD_API = KU_API_BASE + "/meituan_ratesurl";

    public static final String ELE_ACTIVITY_LIST_API = KU_API_BASE + "/elm_activity_list";
    public static final String ELE_ACTIVITY_WORD_API = KU_API_BASE + "/elm_activity_ratesurl";
    public static final String ELE_ACTIVITY_ORDER_API = KU_API_BASE + "/elm_order_list";

    public static final String COUDAN_API = "http://v2.api.haodanku.com/make_order";

    // 快手（好单库普通接口，apikey 鉴权，无需申请权限）
    public static final String KS_ITEM_LIST_API = KU_API_BASE + "/ks_item_list";
    public static final String KS_ITEM_DETAIL_API = KU_API_BASE + "/ks_item_detail";
    public static final String KS_ITEM_RATESURL_API = KU_API_BASE + "/ks_item_ratesurl";
    public static final String KS_ANALYZE_CODE_API = KU_API_BASE + "/ks_analyze_code";
    public static final String KS_ORDER_LIST_API = KU_API_BASE + "/ks_order_list";


    /**
     *  抖音本地生活分类
     *  参照这个链接做了精简 KU_API_BASE + "/dy_life_category
     *  因为有些分类下没有商品
     * */
    private static String DY_LIFE_CATE_STR = "{\"code\":200,\"msg\":\"success\",\"data\":{\"list\":[{\"title\":\"餐饮美食\",\"val\":1000000,\"children\":[{\"title\":\"饮品\",\"val\":1015000},{\"title\":\"地方菜\",\"val\":1001000},{\"title\":\"火锅\",\"val\":1003000},{\"title\":\"烧烤\",\"val\":1004000},{\"title\":\"烤肉\",\"val\":1005000},{\"title\":\"自助餐\",\"val\":1009000},{\"title\":\"东南亚菜\",\"val\":1010000},{\"title\":\"日韩料理\",\"val\":1011000},{\"title\":\"西餐\",\"val\":1012000},{\"title\":\"其他美食\",\"val\":1013000},{\"title\":\"面包甜点\",\"val\":1014000},{\"title\":\"音乐餐厅\",\"val\":1016000},{\"title\":\"快餐小吃\",\"val\":1017000},{\"title\":\"热门特色菜\",\"val\":1018000},{\"title\":\"生鲜果蔬\",\"val\":5004000}]},{\"title\":\"休闲娱乐\",\"val\":4000000,\"children\":[{\"title\":\"洗浴按摩\",\"val\":4001000},{\"title\":\"酒吧\",\"val\":4003000},{\"title\":\"DIY手工坊\",\"val\":4009000},{\"title\":\"其他休闲娱乐\",\"val\":4013000},{\"title\":\"电影演出\",\"val\":4014000},{\"title\":\"休闲运动\",\"val\":4015000},{\"title\":\"新奇体验\",\"val\":4017000},{\"title\":\"推理桌游\",\"val\":4019000},{\"title\":\"传统娱乐\",\"val\":4020000},{\"title\":\"户外玩乐\",\"val\":4021000},{\"title\":\"图书馆\",\"val\":4022000},{\"title\":\"茶馆\",\"val\":4024000},{\"title\":\"演出\",\"val\":4025000}]},{\"title\":\"商城购物\",\"val\":5000000,\"children\":[{\"title\":\"花鸟鱼虫市场\",\"val\":5001000},{\"title\":\"综合商场\",\"val\":5002000},{\"title\":\"日用商超\",\"val\":5003000},{\"title\":\"眼镜店\",\"val\":5005000},{\"title\":\"免税店\",\"val\":5006000},{\"title\":\"商业街步行街\",\"val\":5007000},{\"title\":\"家居建材\",\"val\":5008000},{\"title\":\"配饰\",\"val\":5009000},{\"title\":\"服饰鞋帽\",\"val\":5010000},{\"title\":\"数码家电\",\"val\":5011000},{\"title\":\"美妆个护\",\"val\":5012000},{\"title\":\"交通工具\",\"val\":5013000},{\"title\":\"运动户外\",\"val\":5014000}]},{\"title\":\"亲子乐园\",\"val\":21000000,\"children\":[{\"title\":\"儿童乐园\",\"val\":4016000},{\"title\":\"儿童才艺\",\"val\":7006000},{\"title\":\"STEAM\",\"val\":7007000},{\"title\":\"婴幼服务\",\"val\":21001000},{\"title\":\"儿童运动\",\"val\":21002000},{\"title\":\"亲子活动\",\"val\":21003000},{\"title\":\"孕婴童摄影\",\"val\":21004000},{\"title\":\"早教\",\"val\":21005000},{\"title\":\"母婴购物\",\"val\":21006000}]},{\"title\":\"生活服务\",\"val\":6000000,\"children\":[{\"title\":\"印刷摄影\",\"val\":6001000},{\"title\":\"房产家政\",\"val\":6003000},{\"title\":\"维修服务\",\"val\":6004000},{\"title\":\"洗涤护理\",\"val\":6005000},{\"title\":\"旅行社\",\"val\":6006000},{\"title\":\"孕产服务\",\"val\":6007000},{\"title\":\"搬家运输\",\"val\":6009000},{\"title\":\"法律财会\",\"val\":6010000},{\"title\":\"房产服务\",\"val\":6020000},{\"title\":\"养老服务\",\"val\":6021000},{\"title\":\"汽车服务\",\"val\":19005000}]},{\"title\":\"运动健身\",\"val\":3000000,\"children\":[{\"title\":\"游泳馆\",\"val\":3001000},{\"title\":\"综合体育馆\",\"val\":3002000},{\"title\":\"瑜伽\",\"val\":3003000},{\"title\":\"舞蹈\",\"val\":3004000},{\"title\":\"健身房\",\"val\":3005000},{\"title\":\"武术搏击\",\"val\":3006000},{\"title\":\"球类运动\",\"val\":3007000},{\"title\":\"其他运动健身\",\"val\":3008000}]},{\"title\":\"丽人美发\",\"val\":17000000,\"children\":[{\"title\":\"美发\",\"val\":17001000},{\"title\":\"美甲美睫\",\"val\":17002000},{\"title\":\"美容美体\",\"val\":17003000},{\"title\":\"其他丽人\",\"val\":17005000},{\"title\":\"医疗美容\",\"val\":17006000},{\"title\":\"纹眉纹绣\",\"val\":17007000}]},{\"title\":\"礼仪婚庆\",\"val\":22000000,\"children\":[{\"title\":\"结婚旅拍\",\"val\":22001000},{\"title\":\"婚宴\",\"val\":22002000},{\"title\":\"婚庆策划\",\"val\":22003000},{\"title\":\"婚纱礼服\",\"val\":22004000},{\"title\":\"彩妆造型\",\"val\":22005000},{\"title\":\"婚车租赁\",\"val\":22006000},{\"title\":\"婚礼跟拍\",\"val\":22007000},{\"title\":\"司仪主持\",\"val\":22008000},{\"title\":\"其他婚礼服务\",\"val\":22009000},{\"title\":\"婚纱摄影\",\"val\":22010000},{\"title\":\"婚礼喜品\",\"val\":22011000},{\"title\":\"珠宝首饰\",\"val\":22012000}]}]}}";

    @Resource
    private RestTemplate restTemplate;

    @Value("${haodanku.key}")
    private String key;

    @Value("${haodanku.waimaiKey}")
    private String waimaiKey;

    @Value(("${haodanku.vip.appId}"))
    private String appId;
    @Value(("${haodanku.vip.appSecret}"))
    private String appSecret;


    @Resource
    private MwSystemConfigService systemConfigService;

    /**
     * 搜索京东商品
     *
     * @param param the param
     * @return the jd ku goods detail data vo
     */
    public JdKuSearchListVO searchJD(GoodsListJDParam param) {
        String sortName = param.getSortName();
        String sort = param.getSort();
        Integer sortKu = null;
        if(org.apache.commons.lang3.StringUtils.isNotBlank(sortName) && org.apache.commons.lang3.StringUtils.isNotBlank(sort)) {
            if("inOrderCount30Days".equals(sortName)) {
                sortKu = 2;
            } else if ("price".equals(sortName) && "asc".equals(sort)) {
                sortKu = 4;
            } else if ("price".equals(sortName) && "desc".equals(sort)) {
                sortKu = 5;
            }
        }
        JdKuSearchListVO listVO = searchBaseJD(param, sortKu);
        return listVO;
    }

    public TkParseCodeVO clipboard(ParseContentParam daParam) {
        /**
         * 获取时间
         */
        Date currentDate = new Date();
        String date = formatDate(currentDate);

        /**
         * 生成签名
         */
        StringBuilder key = new StringBuilder();
        Map<String, String> map = new TreeMap<>(Comparator.naturalOrder());
        map.put("app_id", appId);
        map.put("date", date);
        map.put("method", "analyze.clipboard");
        map.put("content", daParam.getContent());

        for (Map.Entry<String, String> entry : map.entrySet()) {
            key.append(entry.getKey()).append(entry.getValue());
        }
        key.append(appSecret);
        String sign = SignMD5Util.MD5(key.toString()).toUpperCase();

        map.put("sign",sign);
        JSONObject jsonObject = new JSONObject();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            jsonObject.put(entry.getKey(), entry.getValue());
        }
        String jsonStr = jsonObject.toJSONString();
        log.info("提交的JSOn字符串:"+jsonStr);
        String response;
        try {
            response = convertUnicodeToCh(postJson(KU_API_V3, jsonStr));
        } catch (Exception e) {
            log.error("好单库 clipboard 请求失败", e);
            TkParseCodeVO error = new TkParseCodeVO();
            error.setCode(500);
            error.setMsg("好单库请求失败: " + e.getMessage());
            return error;
        }
        if (response == null || response.trim().isEmpty()) {
            log.error("好单库 clipboard 返回空响应");
            TkParseCodeVO error = new TkParseCodeVO();
            error.setCode(500);
            error.setMsg("好单库返回空响应");
            return error;
        }
        TkParseCodeVO codeVO = JSON.parseObject(response, TkParseCodeVO.class);
        if (codeVO == null) {
            log.error("好单库 clipboard 响应无法解析: {}", response);
            TkParseCodeVO error = new TkParseCodeVO();
            error.setCode(500);
            error.setMsg("好单库响应解析失败");
            return error;
        }
        // 好单库官方约定 code=1 或 200 均为成功（原实现只认 200，会把 code=1 误判为失败）
        Integer upstreamCode = codeVO.getCode();
        if(upstreamCode != null && (upstreamCode == 200 || upstreamCode == 1)) {
            codeVO.setCode(0);
            //京东goodsId为空，需要使用skuId替换
            // platType 可能为 null（上游返回空 data 或字段缺失），拆箱比较会 NPE
            TkParseVO parseVO = codeVO.getData();
            Integer platType = parseVO == null ? null : parseVO.getPlatType();
            if(platType != null && platType == 2 && org.apache.commons.lang3.StringUtils.isBlank(parseVO.getGoodsId())) {
                parseVO.setGoodsId(parseVO.getSkuId());
            }
        }
        return codeVO;

    }

    public JSONObject shortLink(String link) throws UnsupportedEncodingException {
        /**
         * 获取时间
         */
        Date currentDate = new Date();
        String date = formatDate(currentDate);
        /**
         * 生成签名
         */
        StringBuilder key = new StringBuilder();
        Map<String, String> map = new TreeMap<>(Comparator.naturalOrder());
        map.put("app_id", appId);
        map.put("date", date);
        map.put("method", "short.link");
        map.put("link", link);
        for (Map.Entry<String, String> entry : map.entrySet()) {
            key.append(entry.getKey()).append(entry.getValue());
        }
        key.append(appSecret);
        String sign = SignMD5Util.MD5(key.toString()).toUpperCase();

        map.put("sign",sign);
        JSONObject jsonObject = new JSONObject();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            jsonObject.put(entry.getKey(), entry.getValue());
        }
        String jsonStr = jsonObject.toJSONString();
        log.info("转短链 提交的jsonn字符串:"+jsonStr);

        String response;
        try {
            response = convertUnicodeToCh(postJson(KU_API_V3, jsonStr));
        } catch (Exception e) {
            log.error("好单库 shortLink 请求失败", e);
            return JSON.parseObject(errorJson("好单库短链请求失败: " + e.getMessage()));
        }
        if (response == null || response.trim().isEmpty()) {
            log.error("好单库 shortLink 返回空响应");
            return JSON.parseObject(errorJson("好单库短链返回空响应"));
        }
        JSONObject result = JSON.parseObject(response);
        if (result == null) {
            log.error("好单库 shortLink 响应无法解析: {}", response);
            return JSON.parseObject(errorJson("好单库短链响应解析失败"));
        }
        JSONObject data = result.getJSONObject("data");
        if (data == null) {
            // 上游返回错误码时没有 data（例如 HttpUtils 失败返回 {"code":500,...}），
            // 直接返回 null 会让调用方 NPE。这里原样回传上游错误体，保证返回非 null。
            log.warn("好单库 shortLink 响应无 data，原样回传: {}", response);
            return result;
        }
        return data;

    }
    /**
     * 将unicode转换为中文
     *
     * @param str
     * @return
     */
    private static String convertUnicodeToCh(String str) {
        Pattern pattern = Pattern.compile("(\\\\u(\\w{4}))");
        Matcher matcher = pattern.matcher(str);
        while (matcher.find()) {
            String unicodeFull = matcher.group(1);
            String unicodeNum = matcher.group(2);
            char singleChar = (char) Integer.parseInt(unicodeNum, 16);
            str = str.replace(unicodeFull, singleChar + "");
        }
        return str;
    }



    public JSONObject localLife(String platform, String channel) {

        StringBuilder sb = new StringBuilder();
        sb.append("apikey=");
        sb.append(key);
        sb.append("&platform=");
        sb.append(platform);
        sb.append("&channel=");
        sb.append(channel);
        HttpHeaders headers = new HttpHeaders();
        // 以表单的方式提交
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<String> requestParam = new HttpEntity<>(sb.toString(),headers);
        ResponseEntity<String> re = restTemplate.postForEntity(
                LOCAL_LIFE_API,
                requestParam,
                String.class);
        return JSON.parseObject(re.getBody());
    }


    public JSONObject localLifeOrder(int page, int size, long startTime, long endTime, int platform) {

        StringBuilder sb = new StringBuilder();
        sb.append("apikey=");
        sb.append(key);
        sb.append("&page=");
        sb.append(page);
        sb.append("&size=");
        sb.append(size);
        sb.append("&startTime=");
        sb.append(startTime);
        sb.append("&endTime=");
        sb.append(endTime);
        sb.append("&platform=");
        sb.append(platform);
        HttpHeaders headers = new HttpHeaders();
        // 以表单的方式提交
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<String> requestParam = new HttpEntity<>(sb.toString(),headers);
        ResponseEntity<String> re = restTemplate.postForEntity(
                LOCAL_LIFE_ORDER_API,
                requestParam,
                String.class);
        return JSON.parseObject(re.getBody());
    }



    public JSONObject meiTuanActivityList(int page, int size) {

        StringBuilder sb = new StringBuilder();
        sb.append("apikey=");
        sb.append(waimaiKey);
        sb.append("&min_id=");
        sb.append(page);
        sb.append("&back=");
        sb.append(size);
        HttpHeaders headers = new HttpHeaders();
        // 以表单的方式提交
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
//        HttpEntity<String> requestParam = new HttpEntity<>(sb.toString(),headers);
        ResponseEntity<String> re = restTemplate.getForEntity(
                MEITUAN_ACTIVITY_LIST_API + "?" + sb,
                String.class);
        return JSON.parseObject(re.getBody());
    }


    public JSONObject meiTuanWord(String activityId) {

        StringBuilder sb = new StringBuilder();
        sb.append("apikey=");
        sb.append(waimaiKey);
        sb.append("&activity_id=");
        sb.append(activityId);
        sb.append("&link_type=");
        sb.append(4);
        HttpHeaders headers = new HttpHeaders();
        // 以表单的方式提交
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<String> requestParam = new HttpEntity<>(sb.toString(),headers);
        ResponseEntity<String> re = restTemplate.postForEntity(
                MEITUAN_ACTIVITY_WORD_API,
                requestParam,
                String.class);
        return JSON.parseObject(re.getBody());
    }


    public JSONObject eleActivityList(int page, int size) {

        StringBuilder sb = new StringBuilder();
        sb.append("apikey=");
        sb.append(waimaiKey);
        sb.append("&min_id=");
        sb.append(page);
        sb.append("&back=");
        sb.append(size);
        ResponseEntity<String> re = restTemplate.getForEntity(
                ELE_ACTIVITY_LIST_API + "?" + sb,
                String.class);
        return JSON.parseObject(re.getBody());
    }


    public JSONObject eleWord(String activityId, Long uid) {

        StringBuilder sb = new StringBuilder();
        sb.append("apikey=");
        sb.append(waimaiKey);
        sb.append("&activity_id=");
        sb.append(activityId);
        sb.append("&sid=");
        sb.append(uid);
        HttpHeaders headers = new HttpHeaders();
        // 以表单的方式提交
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<String> requestParam = new HttpEntity<>(sb.toString(),headers);
        ResponseEntity<String> re = restTemplate.postForEntity(
                ELE_ACTIVITY_WORD_API,
                requestParam,
                String.class);
        return JSON.parseObject(re.getBody());
    }


    public EleKuResVo eleOrder(QueryEleKuParam param) {

        StringBuilder sb = new StringBuilder();
        sb.append("apikey=");
        sb.append(waimaiKey);
        sb.append("&min_id=");
        sb.append(param.getPage());
        sb.append("&back=");
        sb.append(param.getSize());
        sb.append("&start_date=");
        sb.append(param.getStart());
        sb.append("&end_date=");
        sb.append(param.getEnd());
        HttpHeaders headers = new HttpHeaders();
        // 以表单的方式提交
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<String> requestParam = new HttpEntity<>(sb.toString(),headers);
        ResponseEntity<String> re = restTemplate.postForEntity(
                ELE_ACTIVITY_ORDER_API,
                requestParam,
                String.class);
        return JSON.parseObject(re.getBody(), EleKuResVo.class);
    }
    /**
     * Dy life list json object.
     *
     * @param page       the page
     * @param size       the size
     * @param cityCode   the city code
     * @param categoryId the category id
     * @param sort       排序：1销量降序，2销量升序，3距离升序，4距离降序，5价格升序，6价格降序
     * @param keyword    the keyword
     * @param longitude  经度
     * @param latitude   纬度
     * @return the json object
     */
    public JSONObject dyLifeList(int page, int size, String cityCode, String categoryId, Integer sort, String keyword, Double longitude, Double latitude) {
        StringBuilder sb = new StringBuilder();
        sb.append(KU_API_BASE);
        sb.append("/dy_life_list?");
        sb.append("apikey=");
        sb.append(key);
        sb.append("&min_id=");
        sb.append(page);
        sb.append("&back=");
        sb.append(size);
        if(StringUtils.isNotBlank(cityCode)) {
            sb.append("&city_code=");
            sb.append(cityCode);
        }
        if(StringUtils.isNotBlank(categoryId)) {
            sb.append("&category_id=");
            sb.append(categoryId);
        }
        if(sort!=null) {
            sb.append("&sort=");
            sb.append(sort);
        }
        if(StringUtils.isNotBlank(keyword)) {
            sb.append("&keyword=");
            sb.append(keyword);
        }
        if(longitude!=null) {
            sb.append("&longitude=");
            sb.append(longitude);
        }
        if(latitude!=null) {
            sb.append("&latitude=");
            sb.append(latitude);
        }
        ResponseEntity<JSONObject> re = restTemplate.getForEntity(
                sb.toString(),
                JSONObject.class);
        return re.getBody();
    }


    public JSONObject dyLifeWord(String id, String channel) {

        StringBuilder sb = new StringBuilder();
        sb.append("apikey=");
        sb.append(key);
        sb.append("&id=");
        sb.append(id);
        sb.append("&channel=");
        sb.append(channel);
        HttpHeaders headers = new HttpHeaders();
        // 以表单的方式提交
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<String> requestParam = new HttpEntity<>(sb.toString(),headers);
        ResponseEntity<JSONObject> re = restTemplate.postForEntity(
                KU_API_BASE+"/dy_life_share",
                requestParam,
                JSONObject.class);
        return re.getBody();
    }

    public DyLifeCityDto dyLifeCityList() {
        String data = HttpRequest
                .get(KU_API_BASE + "/dy_life_city")
                .execute().body();
        return JSON.parseObject(data, DyLifeCityDto.class);
    }

    public DyLifeCityDto dyLifeCategoryList() {
        return JSON.parseObject(DY_LIFE_CATE_STR, DyLifeCityDto.class);
//        String data = HttpRequest
//                .get(KU_API_BASE + "/dy_life_category")
//                .execute().body();
//        return JSON.parseObject(data, DyLifeCityDto.class);
    }


    public DyKuResVo dyLifeOrder(QueryDyKuParam param) {

        StringBuilder sb = new StringBuilder();
        sb.append("apikey=");
        sb.append(key);
        sb.append("&min_id=");
        sb.append(param.getPage());
        sb.append("&back=");
        sb.append(param.getSize());
        sb.append("&start_date=");
        sb.append(param.getStart().getTime()/1000);
        sb.append("&end_date=");
        sb.append(param.getEnd().getTime()/1000);
        sb.append("&media_type=");
        sb.append(param.getMediaType());
        HttpHeaders headers = new HttpHeaders();
        // 以表单的方式提交
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<String> requestParam = new HttpEntity<>(sb.toString(),headers);
        ResponseEntity<String> re = restTemplate.postForEntity(
                KU_API_BASE+"/dy_order_list",
                requestParam,
                String.class);
        return JSON.parseObject(re.getBody(), DyKuResVo.class);
    }

    public DySearchListVO dyProductList(GoodsListDyParam param) {
        org.springframework.web.util.UriComponentsBuilder url = org.springframework.web.util.UriComponentsBuilder
                .fromHttpUrl("https://api.cmspro.haodanku.com/douyin/item/getListsByDy")
                .queryParam("min_id", param.getPageId())
                .queryParam("back", param.getPageSize())
                .queryParam("cid", getKuCid());
        if (StringUtils.isNotBlank(param.getKeyword())) url.queryParam("keyword", "{keyword}");
        if (param.getCateId() != null) url.queryParam("cate_id", param.getCateId());
        if (param.getSort() != null) url.queryParam("sort", param.getSort());
        java.net.URI uri = url.encode().buildAndExpand(Collections.singletonMap("keyword", param.getKeyword())).toUri();
        ResponseEntity<String> re = restTemplate.getForEntity(uri, String.class);
        DyCmsListVO cmsListVO = JSON.parseObject(re.getBody(), DyCmsListVO.class);
        DySearchListVO searchListVO = new DySearchListVO();
        searchListVO.setCode(cmsListVO.getCode());
        searchListVO.setMsg(cmsListVO.getMsg());
        if(cmsListVO.getData() != null) {
            searchListVO.setMinId(cmsListVO.getData().getMinId());
            searchListVO.setData(cmsListVO.getData().getProducts());
        }
        return searchListVO;
    }

    public DyGoodsDetailDataVo dyProductDetail(String itemId) {

        String url = String.format("https://api.cmspro.haodanku.com/douyin/item/detail?product_ids=%s&cid=%s",
                itemId, getKuCid());
        ResponseEntity<String> re = restTemplate.getForEntity(url, String.class);

        DyCmsGoodsDetailDataVo dataVo = JSON.parseObject(re.getBody(), DyCmsGoodsDetailDataVo.class);
        DyGoodsDetailDataVo res = new DyGoodsDetailDataVo();
        res.setCode(dataVo.getCode());
        res.setDetail(dataVo.getData());
        res.setMsg(dataVo.getMsg());
        return res;
    }

    public DyWordGoodsDataVO dyProductWord(String itemId, String channel) {

        KuCmsAddress cmsAddress = getKuCmsAddress();
        StringBuilder sb = new StringBuilder();
        sb.append("domain=").append(cmsAddress.getDomain());
        sb.append("&type=1");
        sb.append("&product_id=").append(itemId);
        sb.append("&use_coupon=true");
        sb.append("&need_share_link=true");
        sb.append("&cid=").append(cmsAddress.getCid());
        HttpHeaders headers = new HttpHeaders();
        // 以表单的方式提交
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<String> requestParam = new HttpEntity<>(sb.toString(),headers);
        ResponseEntity<String> re = restTemplate.postForEntity(
                "https://api.cmspro.haodanku.com/douyin/douyin/getItemsLink",
                requestParam,
                String.class);
        return JSON.parseObject(re.getBody(), DyWordGoodsDataVO.class);
    }

    public DyCateDataVO dyProductCateList() {
        // Read JSON as text so Spring/Jackson cannot leave nested LinkedHashMaps
        // inside a Fastjson JSONObject and break category-item casts.
        ResponseEntity<String> response = restTemplate.getForEntity(
                "https://api.cmspro.haodanku.com/duohaodan/index/category?type=9&cid=" + getKuCid(), String.class);
        JSONObject body = JSON.parseObject(response.getBody());
        if (body == null || (!Integer.valueOf(0).equals(body.getInteger("code")) &&
                !Integer.valueOf(1).equals(body.getInteger("code")) &&
                !Integer.valueOf(200).equals(body.getInteger("code")))) {
            throw new IllegalStateException(body == null ? "抖音分类查询失败" : body.getString("msg"));
        }
        JSONObject payload = body.getJSONObject("data");
        JSONArray categories = payload == null ? null : payload.getJSONArray("category");
        if (categories == null) throw new IllegalStateException("抖音分类响应缺少分类列表");
        List<DyCateVO> data = new ArrayList<>(categories.size());
        for (int i = 0; i < categories.size(); i++) {
            JSONObject category = categories.getJSONObject(i);
            if (category == null || category.getInteger("id") == null ||
                    StringUtils.isBlank(category.getString("name"))) continue;
            DyCateVO item = new DyCateVO();
            item.setCid(category.getInteger("id"));
            item.setTitle(category.getString("name"));
            data.add(item);
        }
        DyCateDataVO result = new DyCateDataVO();
        result.setCode(0);
        result.setData(data);
        return result;
    }

    public JSONObject getCateList(Integer type, Integer labelId) {
        StringBuilder sb = new StringBuilder();
        sb.append(KU_API_BASE);
        sb.append("/category_list?");
        sb.append("apikey=");
        sb.append(key);
        sb.append("&type=");
        sb.append(type);
        if(labelId != null) {
            sb.append("&label_id=");
            sb.append(labelId);
        }

        ResponseEntity<JSONObject> re = restTemplate.getForEntity(
                sb.toString(),
                JSONObject.class);
        return re.getBody();
    }

    public String getKuCid() {
        return getKuCmsAddress().getCid();
    }

    public String getKuHost() {
        return getKuCmsAddress().getDomain();
    }

    private KuCmsAddress getKuCmsAddress() {
        String url = TkConstants.kuCmsUrl;
        if (StringUtils.isBlank(url)) {
            url = systemConfigService.getAppDataConfig().getKuCid();
            KuCmsAddress address = KuCmsAddress.parse(url);
            TkConstants.kuCmsUrl = url;
            return address;
        }
        return KuCmsAddress.parse(url);
    }

    public JSONObject coudanList(Integer page, Integer size, String search) {

        StringBuilder sb = new StringBuilder();
        sb.append(COUDAN_API);
        sb.append("?apikey=");
        sb.append(key);
        sb.append("&min_id=");
        sb.append(page);
        sb.append("&min_size=");
        sb.append(size);
        if(StringUtils.isNotBlank(search)) {
            sb.append("&search_items=");
            sb.append(search);
        }

        ResponseEntity<JSONObject> re = restTemplate.getForEntity(
                sb.toString(),
                JSONObject.class);
        return re.getBody();
    }


    /**
     * 京东详情页使用搜索接口搜索加密id得到
     *
     * @param param the param
     * @return the jd ku goods detail data vo
     */
    public JdKuCommonGoodsDetailDataVo detailJD(GoodsListJDParam param) {
        JdKuSearchListVO listVO = searchBaseJD(param, null);
        JdKuCommonGoodsDetailDataVo commonSearchListVO = new JdKuCommonGoodsDetailDataVo();
        if(CollectionUtils.isNotEmpty(listVO.getData())) {
            commonSearchListVO.setData(JdKuCommonGoodsDetailVO.convert(listVO.getData().get(0)));
        }
        commonSearchListVO.setCode(listVO.getCode());
        commonSearchListVO.setMsg(listVO.getMsg());
        return commonSearchListVO;
    }
    /**
     * 搜索京东商品
     *
     * @param param the param
     * @return the jd ku goods detail data vo
     */
    private JdKuSearchListVO searchBaseJD(GoodsListJDParam param, Integer sortKu) {
        StringBuilder sb = new StringBuilder();
        sb.append("apikey=");
        sb.append(key);
        sb.append("&min_id=");
        sb.append(param.getPageId());
        if(param.getPageSize() != null) {
            sb.append("&back=");
            sb.append(param.getPageSize());
        }
        //这里可以传京东商品加密id
        if(org.apache.commons.lang3.StringUtils.isNotBlank(param.getKeyword())) {
            sb.append("&keyword=");
            sb.append(param.getKeyword());
        }
        if(sortKu != null) {
            sb.append("&sort=");
            sb.append(sortKu);
        }
        ResponseEntity<String> re = restTemplate.getForEntity("http://v3.api.haodanku.com/jd_goods_search?" + sb, String.class);

        JdKuSearchListVO listVO = JSON.parseObject(re.getBody(), JdKuSearchListVO.class);
        return listVO;
    }

    // ==================== 快手全链路（工单 06）====================

    /** 好单库统一成功码：官方约定 code=1 或 200 均为成功（code=0 表示失败）。 */
    public static boolean isKsSuccess(Integer code) {
        return Integer.valueOf(1).equals(code) || Integer.valueOf(200).equals(code);
    }

    /** 快手商品列表/搜索（GET，apikey 即可）。 */
    public KsGoodsListVo itemList(GoodsListKsParam param) {
        StringBuilder sb = new StringBuilder();
        sb.append("apikey=").append(key);
        if (StringUtils.isNotBlank(param.getKeyword())) {
            sb.append("&keyword=").append(urlEncode(param.getKeyword()));
        }
        if (param.getMinId() != null) {
            sb.append("&min_id=").append(param.getMinId());
        }
        if (param.getBack() != null) {
            sb.append("&back=").append(param.getBack());
        }
        if (param.getSortType() != null) {
            sb.append("&sort_type=").append(param.getSortType());
        }
        if (param.getMinPrice() != null) {
            sb.append("&min_price=").append(param.getMinPrice());
        }
        if (param.getMaxPrice() != null) {
            sb.append("&max_price=").append(param.getMaxPrice());
        }
        if (param.getMinSale() != null) {
            sb.append("&min_sale=").append(param.getMinSale());
        }
        if (param.getMaxSale() != null) {
            sb.append("&max_sale=").append(param.getMaxSale());
        }
        ResponseEntity<String> re = restTemplate.getForEntity(
                java.net.URI.create(KS_ITEM_LIST_API + "?" + sb), String.class);
        return parseKs(re == null ? null : re.getBody(), KsGoodsListVo.class);
    }

    /** 快手商品详情（GET，apikey + product_id）。 */
    public KsGoodsDetailVo itemDetail(String productId) {
        ResponseEntity<String> re = restTemplate.getForEntity(
                java.net.URI.create(KS_ITEM_DETAIL_API + "?apikey=" + key + "&product_id=" + urlEncode(productId)),
                String.class);
        return parseKs(re == null ? null : re.getBody(), KsGoodsDetailVo.class);
    }

    /** 快手商品转链（POST form-data，channel 为推客标识）。 */
    public KsItemRatesUrlVo itemRatesUrl(String productId, String channel) {
        StringBuilder sb = new StringBuilder();
        sb.append("apikey=").append(key);
        sb.append("&product_id=").append(urlEncode(productId));
        if (StringUtils.isNotBlank(channel)) {
            sb.append("&channel=").append(urlEncode(channel));
        }
        return postFormKs(KS_ITEM_RATESURL_API, sb.toString(), KsItemRatesUrlVo.class);
    }

    /** 快手口令解析（POST form-data，content 为快手口令）。 */
    public KsAnalyzeCodeVo analyzeCode(String content) {
        String body = "apikey=" + key + "&content=" + urlEncode(content);
        return postFormKs(KS_ANALYZE_CODE_API, body, KsAnalyzeCodeVo.class);
    }

    /**
     * 快手订单拉取（GET，start_date/end_date 为 Unix 秒，min_id 为游标）。
     * 成功码兼容 code=1 与 200，交给调用方以统一语义判断。
     */
    public KsOrderResVo orderList(QueryKsOrderParam param) {
        StringBuilder sb = new StringBuilder();
        sb.append("apikey=").append(key);
        sb.append("&min_id=").append(param.getMinId() == null ? 0 : param.getMinId());
        sb.append("&back=").append(param.getBack() == null ? 100 : param.getBack());
        sb.append("&start_date=").append(param.getStart() == null ? 0L : param.getStart());
        sb.append("&end_date=").append(param.getEnd() == null ? 0L : param.getEnd());
        sb.append("&date_type=").append(param.getDateType() == null ? 1 : param.getDateType());
        sb.append("&state=").append(param.getState() == null ? 0 : param.getState());
        ResponseEntity<String> re = restTemplate.getForEntity(KS_ORDER_LIST_API + "?" + sb, String.class);
        return parseKs(re == null ? null : re.getBody(), KsOrderResVo.class);
    }

    private <T> T postFormKs(String url, String body, Class<T> type) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        ResponseEntity<String> re = restTemplate.postForEntity(url, new HttpEntity<>(body, headers), String.class);
        return parseKs(re == null ? null : re.getBody(), type);
    }

    private <T> T parseKs(String body, Class<T> type) {
        if (body == null || body.trim().isEmpty()) {
            return null;
        }
        try {
            return JSON.parseObject(body, type);
        } catch (Exception e) {
            log.error("快手接口响应解析失败: {}", body, e);
            throw new IllegalStateException("快手服务响应异常，请稍后重试");
        }
    }

    private static String urlEncode(String value) {
        try {
            return java.net.URLEncoder.encode(value, "UTF-8");
        } catch (Exception e) {
            return value;
        }
    }
}
