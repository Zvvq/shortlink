package com.cqie.shortlink_admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cqie.shortlink_admin.dto.request.LinkAccessStatsRequestDTO;
import com.cqie.shortlink_admin.dto.response.LinkAccessStatsResponseDTO;
import com.cqie.shortlink_admin.entity.LinkAccessStatsDO;
import com.cqie.shortlink_admin.mapper.LinkAccessStatsMapper;
import com.cqie.shortlink_admin.service.LinkAccessStatsService;
import com.cqie.shortlink_common.common.convention.exception.ClientException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;


/**
 * 短链接访问统计服务实现层
 */
@Service
public class LinkAccessStatsServiceImpl
        extends ServiceImpl<LinkAccessStatsMapper, LinkAccessStatsDO>
        implements LinkAccessStatsService {

    /**
     * 查询短链接指定日期和小时的点击统计
     *
     * @param requestDTO 访问统计查询请求参数
     * @return 指定日期全天点击数和指定小时点击数
     */
    @Override
    public LinkAccessStatsResponseDTO queryLinkAccessStats(LinkAccessStatsRequestDTO requestDTO) {
        validateRequest(requestDTO);

        Set<String> linkKeys = buildLinkKeys(requestDTO);
        List<LinkAccessStatsDO> statsList = list(new LambdaQueryWrapper<LinkAccessStatsDO>()
                .in(LinkAccessStatsDO::getFullShortUrl, linkKeys)
                .apply("`date` = {0}", requestDTO.getDate())
                .between(LinkAccessStatsDO::getHour, 0, 23));

        long dayClickNum = statsList.stream()
                .mapToLong(this::getPv)
                .sum();
        long hourClickNum = statsList.stream()
                .filter(each -> requestDTO.getHour().equals(each.getHour()))
                .mapToLong(this::getPv)
                .sum();

        return LinkAccessStatsResponseDTO.builder()
                .fullShortUrl(trimToNull(requestDTO.getFullShortUrl()))
                .shortUri(resolveShortUri(requestDTO))
                .date(requestDTO.getDate())
                .hour(requestDTO.getHour())
                .dayClickNum(dayClickNum)
                .hourClickNum(hourClickNum)
                .build();
    }

    /**
     * 校验访问统计查询参数
     *
     * @param requestDTO 访问统计查询请求参数
     */
    private void validateRequest(LinkAccessStatsRequestDTO requestDTO) {
        if (requestDTO == null) {
            throw new ClientException("request cannot be null");
        }
        if (!StringUtils.hasText(requestDTO.getFullShortUrl()) && !StringUtils.hasText(requestDTO.getShortUri())) {
            throw new ClientException("fullShortUrl or shortUri is required");
        }
        if (requestDTO.getDate() == null) {
            throw new ClientException("date is required");
        }
        if (requestDTO.getHour() == null || requestDTO.getHour() < 0 || requestDTO.getHour() > 23) {
            throw new ClientException("hour must be between 0 and 23");
        }
    }

    /**
     * 构建短链接统计查询 key，兼容历史数据中仅保存 shortUri 的情况
     *
     * @param requestDTO 访问统计查询请求参数
     * @return 短链接统计查询 key 集合
     */
    private Set<String> buildLinkKeys(LinkAccessStatsRequestDTO requestDTO) {
        Set<String> linkKeys = new LinkedHashSet<>();
        addIfHasText(linkKeys, requestDTO.getFullShortUrl());
        addIfHasText(linkKeys, requestDTO.getShortUri());

        String fullShortUrl = trimToNull(requestDTO.getFullShortUrl());
        if (fullShortUrl != null) {
            int lastSlashIndex = fullShortUrl.lastIndexOf('/');
            if (lastSlashIndex >= 0 && lastSlashIndex < fullShortUrl.length() - 1) {
                linkKeys.add(fullShortUrl.substring(lastSlashIndex + 1));
            }
        }
        return linkKeys;
    }

    /**
     * 添加非空字符串
     *
     * @param values 字符串集合
     * @param value 待添加字符串
     */
    private void addIfHasText(Set<String> values, String value) {
        String trimValue = trimToNull(value);
        if (trimValue != null) {
            values.add(trimValue);
        }
    }

    /**
     * 解析短链接后缀
     *
     * @param requestDTO 访问统计查询请求参数
     * @return 短链接后缀
     */
    private String resolveShortUri(LinkAccessStatsRequestDTO requestDTO) {
        String shortUri = trimToNull(requestDTO.getShortUri());
        if (shortUri != null) {
            return shortUri;
        }

        String fullShortUrl = trimToNull(requestDTO.getFullShortUrl());
        if (fullShortUrl == null) {
            return null;
        }

        int lastSlashIndex = fullShortUrl.lastIndexOf('/');
        if (lastSlashIndex >= 0 && lastSlashIndex < fullShortUrl.length() - 1) {
            return fullShortUrl.substring(lastSlashIndex + 1);
        }
        return null;
    }

    /**
     * 字符串去空格，空字符串返回 null
     *
     * @param value 原始字符串
     * @return 处理后的字符串
     */
    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    /**
     * 获取 PV 点击数
     *
     * @param statsDO 访问统计实体
     * @return PV 点击数
     */
    private long getPv(LinkAccessStatsDO statsDO) {
        return statsDO.getPv() == null ? 0L : statsDO.getPv();
    }
}
