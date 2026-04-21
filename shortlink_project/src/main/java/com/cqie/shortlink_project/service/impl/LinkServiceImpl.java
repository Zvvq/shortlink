package com.cqie.shortlink_project.service.impl;


import cn.hutool.http.HttpRequest;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cqie.shortlink_project.common.config.SummaryWebConfiguration;
import com.cqie.shortlink_project.common.constant.RocketMQConstant;
import com.cqie.shortlink_project.common.convention.exception.ClientException;
import com.cqie.shortlink_project.dto.request.ShortLinkCreateRequest;
import com.cqie.shortlink_project.dto.request.ShortLinkPageRequest;
import com.cqie.shortlink_project.dto.request.ShortLinkUpdateRequest;
import com.cqie.shortlink_project.dto.response.GroupLinkCountResponse;
import com.cqie.shortlink_project.dto.response.ShortLinkCreateResponse;
import com.cqie.shortlink_project.dto.response.ShortLinkPageResponse;
import com.cqie.shortlink_project.entity.CacheEvictMessage;
import com.cqie.shortlink_project.entity.GroupDO;
import com.cqie.shortlink_project.entity.LinkDO;
import com.cqie.shortlink_project.mapper.GroupMapper;
import com.cqie.shortlink_project.mapper.LinkMapper;
import com.cqie.shortlink_project.service.LinkService;
import com.cqie.shortlink_project.util.BeanUtil;
import com.cqie.shortlink_project.util.ShortLinkUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.cqie.shortlink_project.common.convention.errorcode.BaseErrorCode.*;

/**
* @author friendA
* @description 针对表【t_link(短链接表)】的数据库操作Service实现
* @createDate 2026-04-02 16:24:45
*/
@Service
@RequiredArgsConstructor
@Slf4j
public class LinkServiceImpl extends ServiceImpl<LinkMapper, LinkDO>
    implements LinkService {

    private final GroupMapper groupMapper;
    private final RocketMQTemplate rocketMQTemplate;
    private final SummaryWebConfiguration summaryWebConfiguration;


    /**
     * 创建短链
     * @param requestParam 创建短链请求参数
     * @return 创建短链响应参数
     */
    @Override
    public ShortLinkCreateResponse createShortLink(ShortLinkCreateRequest requestParam) {
        //获取并生成短链接
        String originUrl = requestParam.getOriginUrl();
        String shortLink = ShortLinkUtil.generateShortCode(originUrl);

        //将请求参数转换为实体类
        LinkDO linkDO = BeanUtil.convert(requestParam, LinkDO.class);
        linkDO.setShortUri(shortLink);
        linkDO.setFullShortUrl(requestParam.getDomain()+ "/" + shortLink);
        linkDO.setClickNum(0L);
        linkDO.setEnableStatus(1);

        //保存短链接信息到数据库
        baseMapper.insert(linkDO);

        //构建返回信息
        ShortLinkCreateResponse shortLinkCreateResponse = BeanUtil.convert(linkDO, ShortLinkCreateResponse.class);
        shortLinkCreateResponse.setClickNum(0);
        shortLinkCreateResponse.setEnableStatus(1);

        return shortLinkCreateResponse;
    }

    /**
     * 更新短链
     * @param requestParam 更新短链请求参数
     */
    @Override
    public void updateShortLink(ShortLinkUpdateRequest requestParam) {
        // 查询短链接是否存在
        LinkDO linkDO = baseMapper.selectOne(
                new QueryWrapper<LinkDO>()
                        .eq("full_short_url", requestParam.getFullShortUrl())
                        .eq("gid", requestParam.getGid())
                        .eq("del_flag", 0)
        );

        if (linkDO == null) {
            throw new ClientException(SHORT_LINK_NOT_EXIST_ERROR);
        }

        // 更新字段
        linkDO.setOriginUrl(requestParam.getOriginUrl());
        linkDO.setValidDateType(requestParam.getValidDateType());
        linkDO.setValidDate(requestParam.getValidDate());
        linkDO.setDescribe(requestParam.getDescribe());

        int update = baseMapper.updateById(linkDO);
        if (update < 1) {
            throw new ClientException(SHORT_LINK_UPDATE_ERROR);
        }

        // 构建缓存清除消息
        CacheEvictMessage message = CacheEvictMessage
                .builder()
                .shortUrl(linkDO.getFullShortUrl())
                .build();

        // 发送缓存清除消息
        rocketMQTemplate.asyncSend(RocketMQConstant.SHORT_LINK_CACHE_EVICT_TOPIC, message,
                new SendCallback() {
                    @Override
                    public void onSuccess(SendResult sendResult) {
                        // 处理发送成功
                        log.info("发送短链缓存清除消息成功: {}", sendResult);
                    }

                    @Override
                    public void onException(Throwable e) {
                        // 处理发送异常
                        log.error("发送短链缓存清除消息失败: {}", e);
                    }
                });

    }

    /**
     * 删除短链
     * @param fullShortUrl 完整短链接
     */
    @Override
    public void deleteShortLink(String fullShortUrl) {
        // 查询短链接是否存在
        LinkDO linkDO = baseMapper.selectOne(
                new QueryWrapper<LinkDO>()
                        .eq("full_short_url", fullShortUrl)
                        .eq("del_flag", 0)
        );

        if (linkDO == null) {
            throw new ClientException(SHORT_LINK_NOT_EXIST_ERROR);
        }

        // 逻辑删除
        linkDO.setDelFlag(1);
        int update = baseMapper.updateById(linkDO);

        if (update < 1) {
            throw new ClientException(SHORT_LINK_DELETE_ERROR);
        }
    }

    /**
     * 分页查询短链
     * @param requestParam 分页查询请求参数
     * @return 分页查询结果
     */
    @Override
    public IPage<ShortLinkPageResponse> pageShortLink(ShortLinkPageRequest requestParam) {
        // 设置默认分页参数
        long current = requestParam.getCurrent() != null ? requestParam.getCurrent() : 1;
        long size = requestParam.getSize() != null ? requestParam.getSize() : 10;

        Page<LinkDO> page = new Page<>(current, size);

        // 构建查询条件
        QueryWrapper<LinkDO> queryWrapper = new QueryWrapper<LinkDO>()
                .eq("del_flag", 0)
                .orderByDesc("create_time");

        // 如果指定了分组标识，添加分组筛选
        if (requestParam.getGid() != null && !requestParam.getGid().isEmpty()) {
            queryWrapper.eq("gid", requestParam.getGid());
        }

        IPage<LinkDO> linkPage = baseMapper.selectPage(page, queryWrapper);

        // 转换结果
        List<ShortLinkPageResponse> records = linkPage.getRecords().stream().map(linkDO -> {
            ShortLinkPageResponse response = new ShortLinkPageResponse();
            response.setId(linkDO.getId());
            response.setDomain(linkDO.getDomain());
            response.setShortUri(linkDO.getShortUri());
            response.setFullShortUrl(linkDO.getFullShortUrl());
            response.setOriginUrl(linkDO.getOriginUrl());
            response.setClickNum(linkDO.getClickNum());
            response.setGid(linkDO.getGid());
            response.setEnableStatus(linkDO.getEnableStatus());
            response.setCreatedType(linkDO.getCreatedType());
            response.setValidDateType(linkDO.getValidDateType());
            response.setValidDate(linkDO.getValidDate());
            response.setDescribe(linkDO.getDescribe());
            response.setCreateTime(linkDO.getCreateTime());
            response.setUpdateTime(linkDO.getUpdateTime());
            return response;
        }).collect(Collectors.toList());

        Page<ShortLinkPageResponse> resultPage = new Page<>(linkPage.getCurrent(), linkPage.getSize(), linkPage.getTotal());
        resultPage.setRecords(records);

        return resultPage;
    }

    /**
     * 查询当前用户的所有分组及短链接数量
     * @param username 用户名
     * @return 分组及短链接数量列表
     */
    @Override
    public List<GroupLinkCountResponse> listGroupLinkCount(String username) {
        // 1. 使用MyBatis-Plus查询当前用户的所有分组
        List<GroupDO> groupList = groupMapper.selectList(
                new QueryWrapper<GroupDO>()
                        .eq("username", username)
                        .eq("del_flag", 0)
                        .orderByAsc("sort_order")
        );

        if (groupList == null || groupList.isEmpty()) {
            return List.of();
        }

        // 2. 获取所有gid
        List<String> gidList = groupList.stream()
                .map(GroupDO::getGid)
                .collect(Collectors.toList());

        // 3. 使用MyBatis-Plus统计每个gid下的短链接数量
        List<Map<String, Object>> countList = baseMapper.selectMaps(
                new QueryWrapper<LinkDO>()
                        .select("gid", "count(*) as link_count")
                        .in("gid", gidList)
                        .eq("del_flag", 0)
                        .groupBy("gid")
        );

        // 4. 转换为Map<gid, count>
        Map<String, Long> countMap = countList.stream()
                .collect(Collectors.toMap(
                        m -> (String) m.get("gid"),
                        m -> ((Number) m.get("link_count")).longValue(),
                        (v1, v2) -> v1
                ));

        // 5. 组装结果
        return groupList.stream().map(group -> {
            GroupLinkCountResponse response = new GroupLinkCountResponse();
            response.setGid(group.getGid());
            response.setName(group.getName());
            response.setSortOrder(group.getSortOrder());
            response.setLinkCount(countMap.getOrDefault(group.getGid(), 0L));
            return response;
        }).collect(Collectors.toList());
    }

    @Override
    public String generateDescription(String originalUrl) {
        String baseUrl = summaryWebConfiguration.getBaseUrl();
        String responseBody = HttpRequest.post(baseUrl)
                .form("url", originalUrl)          // 表单参数
                .execute()
                .body();


        // 解析响应
        JSONObject response = JSONObject.parseObject(responseBody);
        if (!response.getJSONObject("code").equals("200") || response.getJSONObject("data") == null) {
            throw new ClientException(SUMMARY_GENERATION_ERROR);
        }
        return response.getJSONObject("data").getString("summary");
    }

}
