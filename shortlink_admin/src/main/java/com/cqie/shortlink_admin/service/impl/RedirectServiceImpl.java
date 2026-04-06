package com.cqie.shortlink_admin.service.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.cqie.shortlink_admin.entity.LinkAccessStatsDO;
import com.cqie.shortlink_admin.entity.ShortLinkDO;
import com.cqie.shortlink_admin.mapper.LinkAccessStatsMapper;
import com.cqie.shortlink_admin.mapper.ShortLinkMapper;
import com.cqie.shortlink_admin.service.RedirectService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.xml.crypto.Data;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import static com.cqie.shortlink_admin.common.constant.RedisCacheConstant.CACHE_SHORT_LINK;
import static com.cqie.shortlink_admin.common.constant.RedisCacheConstant.LOCK_SHORT_LINK_REBUILD;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedirectServiceImpl implements RedirectService {

    private final ShortLinkMapper shortLinkMapper;

    private final RedisTemplate<String, String> redisTemplate;

    private final RedissonClient redissonClient;

    private final RBloomFilter<String> shortLinkCreateCachePenetrationBloomFilter;

    private final LinkAccessStatsMapper linkAccessStatsMapper;

    // 并发统计
    private final AtomicLong totalRequestCount = new AtomicLong(0);
    private final AtomicLong cacheHitCount = new AtomicLong(0);
    private final AtomicLong bloomFilterMissCount = new AtomicLong(0);
    private final AtomicLong dbQueryCount = new AtomicLong(0);
    private final AtomicLong lockWaitCount = new AtomicLong(0);


    /**
     * 根据短链跳转
     * @param shortUrl 短链
     */
    @Override
    public void redirect(String shortUrl, HttpServletRequest request, HttpServletResponse response) {
        long requestNum = totalRequestCount.incrementAndGet();

        // 从缓存中获取短链对应的长链
        if (checkShortLinkCache(shortUrl, response)) {
            log.debug("[请求:{}] 缓存命中: {}", requestNum, shortUrl);
            return;
        }

        // 查询布隆过滤器
        if (!shortLinkCreateCachePenetrationBloomFilter.contains(shortUrl)) {
            bloomFilterMissCount.incrementAndGet();
            log.warn("[请求:{}] 布隆过滤器拦截: {}", requestNum, shortUrl);
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // 获取分布式锁
        RLock lock = redissonClient.getLock(LOCK_SHORT_LINK_REBUILD + shortUrl);
        lockWaitCount.incrementAndGet();
        lock.lock();
        try {
            // double-check
            if (checkShortLinkCache(shortUrl, response)) {
                return;
            }

            // 查询数据库
            ShortLinkDO shortLink = shortLinkMapper.selectOne(
                    new LambdaQueryWrapper<ShortLinkDO>()
                            .eq(ShortLinkDO::getShortUri, shortUrl)
                            .eq(ShortLinkDO::getEnableStatus, 1)
                            .gt(ShortLinkDO::getValidDate, LocalDateTime.now())
            );
            dbQueryCount.incrementAndGet();

            if (shortLink == null) {
                log.warn("[请求:{}] 短链不存在: {}", requestNum, shortUrl);
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                redisTemplate.opsForValue().set(CACHE_SHORT_LINK + shortUrl, "", 5 * 60 * 1000);
                return;
            }

            // 重建缓存并重定向
            redisTemplate.opsForValue().set(CACHE_SHORT_LINK + shortUrl, shortLink.getOriginUrl());
            response.setStatus(HttpServletResponse.SC_FOUND);
            response.setHeader("Location", shortLink.getOriginUrl());
            statsLinkAccess(shortUrl);

            log.info("[请求:{}] 重定向成功: {} -> {}", requestNum, shortUrl, shortLink.getOriginUrl());
        } finally {
            lock.unlock();
        }
    }

    /**
     * 检查短链缓存
     * @param shortUrl 短链
     * @param response 响应
     * @return 是否命中缓存
     */
    private boolean checkShortLinkCache(String shortUrl, HttpServletResponse response) {
        String originUrl = redisTemplate.opsForValue().get(CACHE_SHORT_LINK + shortUrl);
        if (StringUtils.isNotBlank(originUrl)) {
            cacheHitCount.incrementAndGet();
            response.setStatus(HttpServletResponse.SC_FOUND);
            response.setHeader("Location", originUrl);
            //统计
            statsLinkAccess(shortUrl);
            return true;
        }

        if (Objects.equals(originUrl, "")) {
            // 空值缓存，表示该短链不存在
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return true;
        }
        return false;
    }


    /**
     * 统计短链访问量
     * @param shortUrl 短链
     */
    private void statsLinkAccess(String shortUrl) {
        // 统计访问量
        Date now = DateUtil.date();
        LinkAccessStatsDO linkAccessStats = LinkAccessStatsDO.builder()
                .gid("default")
                .pv(1)
                .uv(1)
                .uip(1)
                .date(now)
                .fullShortUrl(shortUrl)
                .hour(DateUtil.hour(now, true))
                .weekday(DateUtil.dayOfWeekEnum(now).getIso8601Value())
                .build();

        linkAccessStatsMapper.statsLinkAccess(linkAccessStats);
    }
}
