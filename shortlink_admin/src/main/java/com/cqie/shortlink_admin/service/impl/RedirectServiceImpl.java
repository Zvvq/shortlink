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
        String threadName = Thread.currentThread().getName();
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));

        log.info("[请求:{}] [{}] 线程:{} 开始处理短链: {}", requestNum, time, threadName, shortUrl);

        // 从缓存中获取短链对应的长链
        if (checkShortLinkCache(shortUrl, response)) {
            log.info("[请求:{}] [{}] 线程:{} 缓存命中,直接返回", requestNum, time, threadName);
            return;
        }

        //没有则查询布隆过滤器，判断数据库是否存在
        if (!shortLinkCreateCachePenetrationBloomFilter.contains(shortUrl)) {
            bloomFilterMissCount.incrementAndGet();
            log.warn("[请求:{}] [{}] 线程:{} 布隆过滤器判定不存在: {}", requestNum, time, threadName, shortUrl);
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // 查询数据库
        log.info("[请求:{}] [{}] 线程:{} 尝试获取锁...", requestNum, time, threadName);
        RLock lock = redissonClient.getLock(LOCK_SHORT_LINK_REBUILD + shortUrl);
        lockWaitCount.incrementAndGet();
        lock.lock();
        try {
            log.info("[请求:{}] [{}] 线程:{} 获取锁成功,执行double-check", requestNum, time, threadName);
            //double-check
            if (checkShortLinkCache(shortUrl, response)) {
                log.info("[请求:{}] [{}] 线程:{} double-check缓存命中", requestNum, time, threadName);
                return;
            }

            // 查询数据库
            log.info("[请求:{}] [{}] 线程:{} 查询数据库...", requestNum, time, threadName);
            ShortLinkDO shortLink = shortLinkMapper.selectOne(
                    new LambdaQueryWrapper<ShortLinkDO>()
                            .eq(ShortLinkDO::getShortUri, shortUrl)
                            .eq(ShortLinkDO::getEnableStatus, 1)// 判断是否启用
                            .gt(ShortLinkDO::getValidDate, LocalDateTime.now())// 判断是否过期

            );
            dbQueryCount.incrementAndGet();

            // 如果数据库中不存在该短链，则返回404
            if (shortLink == null) {
                log.warn("[请求:{}] [{}] 线程:{} 数据库中不存在该短链: {}", requestNum, time, threadName, shortUrl);
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                // 缓存短链不存在的信息，解决布隆过滤器误判的问题
                redisTemplate.opsForValue().set(CACHE_SHORT_LINK + shortUrl, "", 5 * 60 * 1000);
                return;
            }
            //重建缓存
            log.info("[请求:{}] [{}] 线程:{} 重建缓存,原始链接: {}", requestNum, time, threadName, shortLink.getOriginUrl());
            redisTemplate.opsForValue().set(CACHE_SHORT_LINK + shortUrl, shortLink.getOriginUrl());

            // 进行302重定向到原始链接
            response.setStatus(HttpServletResponse.SC_FOUND);
            response.setHeader("Location", shortLink.getOriginUrl());

            //统计
            statsLinkAccess(shortUrl);
            log.info("[请求:{}] [{}] 线程:{} 重定向成功", requestNum, time, threadName);
        } finally {
            lock.unlock();
            log.info("[请求:{}] [{}] 线程:{} 释放锁", requestNum, time, threadName);
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
