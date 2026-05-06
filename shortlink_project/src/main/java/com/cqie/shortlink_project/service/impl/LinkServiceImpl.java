package com.cqie.shortlink_project.service.impl;

import cn.hutool.http.HttpRequest;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cqie.shortlink_common.common.constant.RocketMQConstant;
import com.cqie.shortlink_common.common.convention.exception.ClientException;
import com.cqie.shortlink_project.common.config.SummaryWebConfiguration;
import com.cqie.shortlink_project.dto.request.ShortLinkCreateRequest;
import com.cqie.shortlink_project.dto.request.ShortLinkPageRequest;
import com.cqie.shortlink_project.dto.request.ShortLinkUpdateRequest;
import com.cqie.shortlink_project.dto.response.GenerateDescriptionResponse;
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
import org.springframework.util.StringUtils;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.cqie.shortlink_common.common.constant.GroupConstant.DEFAULT_GROUP_NAME;
import static com.cqie.shortlink_common.common.convention.errorcode.BaseErrorCode.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class LinkServiceImpl extends ServiceImpl<LinkMapper, LinkDO>
        implements LinkService {

    private static final int PERMANENT_VALID_DATE_TYPE = 0;
    private static final LocalDateTime PERMANENT_VALID_DATE = LocalDateTime.of(2099, 1, 1, 0, 0);

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
        String shortLink = ShortLinkUtil.generateShortCode(requestParam.getOriginUrl());
        if (!StringUtils.hasText(requestParam.getGid())) {
            requestParam.setGid(resolveDefaultGroupId(requestParam.getUsername()));
        }

        LinkDO linkDO = BeanUtil.convert(requestParam, LinkDO.class);
        linkDO.setShortUri(shortLink);
        linkDO.setFullShortUrl(requestParam.getDomain() + "/" + shortLink);
        linkDO.setValidDate(resolveValidDate(requestParam.getValidDateType(), requestParam.getValidDate()));
        linkDO.setClickNum(0L);
        linkDO.setEnableStatus(1);
        baseMapper.insert(linkDO);

        ShortLinkCreateResponse shortLinkCreateResponse = BeanUtil.convert(linkDO, ShortLinkCreateResponse.class);
        shortLinkCreateResponse.setClickNum(0);
        shortLinkCreateResponse.setEnableStatus(1);
        return shortLinkCreateResponse;
    }

    /**
     * 更新短链信息
     * @param requestParam 更新短链请求参数
     */
    @Override
    public void updateShortLink(ShortLinkUpdateRequest requestParam) {
        LinkDO linkDO = baseMapper.selectOne(
                new QueryWrapper<LinkDO>()
                        .eq("full_short_url", requestParam.getFullShortUrl())
                        .eq("gid", requestParam.getGid())
                        .eq("del_flag", 0)
        );
        if (linkDO == null) {
            throw new ClientException(SHORT_LINK_NOT_EXIST_ERROR);
        }

        linkDO.setOriginUrl(requestParam.getOriginUrl());
        linkDO.setValidDateType(requestParam.getValidDateType());
        linkDO.setValidDate(resolveValidDate(requestParam.getValidDateType(), requestParam.getValidDate()));
        linkDO.setDescribe(requestParam.getDescribe());

        int update = baseMapper.updateById(linkDO);
        if (update < 1) {
            throw new ClientException(SHORT_LINK_UPDATE_ERROR);
        }

        CacheEvictMessage message = CacheEvictMessage.builder()
                .shortUrl(linkDO.getFullShortUrl())
                .build();
        rocketMQTemplate.asyncSend(RocketMQConstant.SHORT_LINK_CACHE_EVICT_TOPIC, message,
                new SendCallback() {
                    @Override
                    public void onSuccess(SendResult sendResult) {
                        log.info("发送短链缓存清除消息成功 {}", sendResult);
                    }

                    @Override
                    public void onException(Throwable e) {
                        log.error("发送短链缓存清除消息失败 {}", e.getMessage(), e);
                    }
                });
    }

    /**
     * 根据完整短链接删除短链接
     * @param fullShortUrl 完整短链接
     */
    @Override
    public void deleteShortLink(String fullShortUrl) {
        LinkDO linkDO = baseMapper.selectOne(
                new QueryWrapper<LinkDO>()
                        .eq("full_short_url", fullShortUrl)
                        .eq("del_flag", 0)
        );
        if (linkDO == null) {
            throw new ClientException(SHORT_LINK_NOT_EXIST_ERROR);
        }

        linkDO.setDelFlag(1);
        int update = baseMapper.updateById(linkDO);
        if (update < 1) {
            throw new ClientException(SHORT_LINK_DELETE_ERROR);
        }
    }

    /**
     * 分页查询短链信息
     * @param requestParam 分页查询请求参数
     * @return 分页查询结果
     */
    @Override
    public IPage<ShortLinkPageResponse> pageShortLink(ShortLinkPageRequest requestParam) {
        long current = requestParam.getCurrent() != null ? requestParam.getCurrent() : 1;
        long size = requestParam.getSize() != null ? requestParam.getSize() : 10;

        Page<LinkDO> page = new Page<>(current, size);
        QueryWrapper<LinkDO> queryWrapper = new QueryWrapper<LinkDO>()
                .eq("del_flag", 0)
                .orderByDesc("create_time");
        if (StringUtils.hasText(requestParam.getGid())) {
            queryWrapper.eq("gid", requestParam.getGid());
        }

        IPage<LinkDO> linkPage = baseMapper.selectPage(page, queryWrapper);
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
     * 根据用户分组短链数量
     * @param username 用户名
     * @return 分组短链数量列表
     */
    @Override
    public List<GroupLinkCountResponse> listGroupLinkCount(String username) {
        List<GroupDO> groupList = groupMapper.selectList(
                new QueryWrapper<GroupDO>()
                        .eq("username", username)
                        .eq("del_flag", 0)
                        .orderByAsc("sort_order")
        );
        if (groupList == null || groupList.isEmpty()) {
            return List.of();
        }

        List<String> gidList = groupList.stream()
                .map(GroupDO::getGid)
                .collect(Collectors.toList());
        List<Map<String, Object>> countList = baseMapper.selectMaps(
                new QueryWrapper<LinkDO>()
                        .select("gid", "count(*) as link_count")
                        .in("gid", gidList)
                        .eq("del_flag", 0)
                        .groupBy("gid")
        );

        Map<String, Long> countMap = countList.stream()
                .collect(Collectors.toMap(
                        m -> (String) m.get("gid"),
                        m -> ((Number) m.get("link_count")).longValue(),
                        (v1, v2) -> v1
                ));

        return groupList.stream().map(group -> {
            GroupLinkCountResponse response = new GroupLinkCountResponse();
            response.setGid(group.getGid());
            response.setName(group.getName());
            response.setSortOrder(group.getSortOrder());
            response.setLinkCount(countMap.getOrDefault(group.getGid(), 0L));
            return response;
        }).collect(Collectors.toList());
    }

    /**
     * 根据原始链接生成描述信息
     * @param originalUrl 原始链接
     * @return 描述信息
     */
    @Override
    public GenerateDescriptionResponse generateDescription(String originalUrl) {
        String baseUrl = summaryWebConfiguration.getBaseUrl();

        Map<String, String> body = new HashMap<>();
        body.put("url", originalUrl);

        String responseBody = HttpRequest.post(baseUrl)
                .header("Content-Type", "application/json")
                .body(JSONObject.toJSONString(body))
                .execute()
                .body();

        JSONObject response = JSONObject.parseObject(responseBody);
        GenerateDescriptionResponse data = response.getJSONObject("data").to(GenerateDescriptionResponse.class);

        if (!"200".equals(response.getString("code")) || data == null) {
            throw new ClientException(SUMMARY_GENERATION_ERROR);
        }
        log.info("generateDescription response: {}", response);

        // 验证描述信息是否安全，如果不安全则抛出异常
        if (!data.getIsSafe()) {
            throw new ClientException(SUMMARY_GENERATION_ERROR);
        }

        return response.getJSONObject("data").to(GenerateDescriptionResponse.class);
    }

    /**
     * 解析默认分组ID，如果用户没有指定分组ID，则将短链放在默认分组下
     * @param username 用户名
     * @return 默认分组ID
     */
    private String resolveDefaultGroupId(String username) {
        GroupDO defaultGroup = groupMapper.selectOne(
                new QueryWrapper<GroupDO>()
                        .eq("username", username)
                        .eq("name", DEFAULT_GROUP_NAME)
                        .eq("del_flag", 0)
                        .last("limit 1")
        );
        if (defaultGroup == null || !StringUtils.hasText(defaultGroup.getGid())) {
            throw new ClientException(GROUP_NOT_EXIST_ERROR);
        }
        return defaultGroup.getGid();
    }

    private Date resolveValidDate(Integer validDateType, Date validDate) {
        if (Objects.equals(validDateType, PERMANENT_VALID_DATE_TYPE)) {
            return Timestamp.valueOf(PERMANENT_VALID_DATE);
        }
        return validDate;
    }
}
