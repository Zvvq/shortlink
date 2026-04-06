package com.cqie.shortlink_admin.service;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.cqie.shortlink_admin.dto.request.ShortLinkCreateRequestDTO;
import com.cqie.shortlink_admin.dto.request.ShortLinkPageRequestDTO;
import com.cqie.shortlink_admin.dto.request.ShortLinkUpdateRequestDTO;
import com.cqie.shortlink_admin.dto.response.ShortLinkCreateResponseDTO;
import com.cqie.shortlink_admin.dto.response.ShortLinkPageResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 短链接远程调用服务
 * 通过HTTP调用project模块的接口
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShortLinkRemoteService {

    @Value("${shortlink.project.url:http://localhost:8081}")
    private String projectUrl;

    private final RBloomFilter<String> shortLinkCreateCachePenetrationBloomFilter;

    /**
     * 创建短链接
     * @param requestDTO 创建请求参数
     * @return 创建结果
     */
    public ShortLinkCreateResponseDTO createShortLink(ShortLinkCreateRequestDTO requestDTO) {

        String url = projectUrl + "/api/shortlink/v1/link/create";
        
        // 发送POST请求，使用JSON格式
        String jsonBody = JSONUtil.toJsonStr(requestDTO);
        String result = HttpRequest.post(url)
                .header("Content-Type", "application/json")
                .body(jsonBody)
                .execute()
                .body();
        
        // 解析响应
        ShortLinkCreateResponseDTO responseDTO = parseCreateResponse(result);
        
        // 创建成功，添加到布隆过滤器
        if (responseDTO != null && responseDTO.getFullShortUrl() != null) {
            shortLinkCreateCachePenetrationBloomFilter.add(responseDTO.getFullShortUrl());
            log.info("短链接创建成功，已添加到布隆过滤器: {}", responseDTO.getFullShortUrl());
        }
        
        return responseDTO;
    }

    /**
     * 更新短链接
     * @param requestDTO 更新请求参数
     */
    public void updateShortLink(ShortLinkUpdateRequestDTO requestDTO) {
        String url = projectUrl + "/api/shortlink/v1/link/update";
        
        // 将DTO转换为Map
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("fullShortUrl", requestDTO.getFullShortUrl());
        paramMap.put("originUrl", requestDTO.getOriginUrl());
        paramMap.put("gid", requestDTO.getGid());
        paramMap.put("validDateType", requestDTO.getValidDateType());
        paramMap.put("validDate", requestDTO.getValidDate());
        paramMap.put("describe", requestDTO.getDescribe());
        
        // 发送PUT请求
        HttpResponse execute = HttpRequest.put(url)
                .body(JSONUtil.toJsonStr(paramMap))
                .execute();
    }

    /**
     * 删除短链接
     * @param fullShortUrl 完整短链接
     */
    public void deleteShortLink(String fullShortUrl) {
        String url = projectUrl + "/api/shortlink/v1/link/delete?fullShortUrl=" + fullShortUrl;
        
        // 发送DELETE请求
        HttpRequest.delete(url).execute();
    }

    /**
     * 分页查询短链接
     * @param requestDTO 分页查询请求参数
     * @return 分页查询结果
     */
    public List<ShortLinkPageResponseDTO> pageShortLink(ShortLinkPageRequestDTO requestDTO) {
        String url = projectUrl + "/api/shortlink/v1/link/page";
        
        // 构建查询参数
        Map<String, Object> paramMap = new HashMap<>();
        if (requestDTO.getGid() != null) {
            paramMap.put("gid", requestDTO.getGid());
        }
        if (requestDTO.getCurrent() != null) {
            paramMap.put("current", requestDTO.getCurrent());
        }
        if (requestDTO.getSize() != null) {
            paramMap.put("size", requestDTO.getSize());
        }
        
        // 发送GET请求
        log.info("分页查询请求URL: {}, 参数: {}", url, paramMap);
        HttpResponse response = HttpRequest.get(url)
                .form(paramMap)
                .timeout(5000)
                .execute();
        
        log.info("响应状态码: {}", response.getStatus());
        String result = response.body();
        log.info("分页查询短链接响应: {}", result);
        
        if (result == null || result.trim().isEmpty()) {
            log.error("project模块返回空响应，请检查: 1.project模块是否启动 2.端口是否正确");
            return new ArrayList<>();
        }
        
        // 解析响应
        return parsePageResponse(result);
    }

    /**
     * 解析创建响应结果
     * @param response HTTP响应字符串
     * @return 短链接创建响应DTO
     */
    private ShortLinkCreateResponseDTO parseCreateResponse(String response) {
        JSONObject jsonObject = JSONUtil.parseObj(response);
        JSONObject data = jsonObject.getJSONObject("data");
        
        if (data == null) {
            return null;
        }
        
        return ShortLinkCreateResponseDTO.builder()
                .domain(data.getStr("domain"))
                .shortUri(data.getStr("shortUri"))
                .fullShortUrl(data.getStr("fullShortUrl"))
                .originUrl(data.getStr("originUrl"))
                .clickNum(data.getInt("clickNum"))
                .gid(data.getStr("gid"))
                .enableStatus(data.getInt("enableStatus"))
                .validDateType(data.getInt("validDateType"))
                .validDate(data.getDate("validDate"))
                .describe(data.getStr("describe"))
                .build();
    }

    /**
     * 解析分页响应结果
     * @param response HTTP响应字符串
     * @return 短链接分页列表
     */
    private List<ShortLinkPageResponseDTO> parsePageResponse(String response) {
        JSONObject jsonObject = JSONUtil.parseObj(response);
        JSONObject data = jsonObject.getJSONObject("data");
        
        if (data == null) {
            return new ArrayList<>();
        }
        
        JSONArray records = data.getJSONArray("records");
        if (records == null) {
            return new ArrayList<>();
        }
        
        List<ShortLinkPageResponseDTO> result = new ArrayList<>();
        for (int i = 0; i < records.size(); i++) {
            JSONObject item = records.getJSONObject(i);
            result.add(ShortLinkPageResponseDTO.builder()
                    .id(item.getLong("id"))
                    .domain(item.getStr("domain"))
                    .shortUri(item.getStr("shortUri"))
                    .fullShortUrl(item.getStr("fullShortUrl"))
                    .originUrl(item.getStr("originUrl"))
                    .clickNum(item.getInt("clickNum"))
                    .gid(item.getStr("gid"))
                    .enableStatus(item.getInt("enableStatus"))
                    .createdType(item.getInt("createdType"))
                    .validDateType(item.getInt("validDateType"))
                    .validDate(item.getDate("validDate"))
                    .describe(item.getStr("describe"))
                    .createTime(item.getDate("createTime"))
                    .updateTime(item.getDate("updateTime"))
                    .build());
        }
        
        return result;
    }
}
