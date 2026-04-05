package com.cqie.shortlink_admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.cqie.shortlink_admin.common.config.RBloomFilterConfiguration;
import com.cqie.shortlink_admin.entity.ShortLinkDO;
import com.cqie.shortlink_admin.mapper.ShortLinkMapper;
import com.cqie.shortlink_admin.service.RedirectService;
import com.cqie.shortlink_admin.util.RedisUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import static com.cqie.shortlink_admin.common.constant.RedisCacheConstant.CACHE_SHORT_LINK;
import static com.cqie.shortlink_admin.common.constant.RedisCacheConstant.LOCK_SHORT_LINK_REBUILD;

@Service
@RequiredArgsConstructor
public class RedirectServiceImpl implements RedirectService {

    private final ShortLinkMapper shortLinkMapper;

    private final RedisTemplate<String, String> redisTemplate;

    private final RedissonClient redissonClient;

    private final RBloomFilter<String> shortLinkCreateCachePenetrationBloomFilter;


    /**
     * 根据短链跳转
     * @param shortUrl 短链
     */
    @Override
    public void redirect(String shortUrl, HttpServletRequest request, HttpServletResponse response) {

        // 从缓存中获取短链对应的长链
        String link = redisTemplate.opsForValue().get(CACHE_SHORT_LINK + shortUrl);

        // 如果缓存中存在短链对应的长链，则进行302重定向
        if (StringUtils.isNotBlank(link)) {
            response.setStatus(HttpServletResponse.SC_FOUND);
            response.setHeader("Location", link);
            return;
        }

        //没有则查询布隆过滤器，判断数据库是否存在
        if (!shortLinkCreateCachePenetrationBloomFilter.contains(shortUrl)) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // 查询数据库
        RLock lock = redissonClient.getLock(LOCK_SHORT_LINK_REBUILD + shortUrl);

        lock.lock();
        try {
            //double-check
            String originUrl = redisTemplate.opsForValue().get(CACHE_SHORT_LINK + shortUrl);
            if (StringUtils.isNotBlank(originUrl)) {
                response.setStatus(HttpServletResponse.SC_FOUND);
                response.setHeader("Location", originUrl);
                return;
            }

            // 查询数据库
            ShortLinkDO shortLink = shortLinkMapper.selectOne(
                    new LambdaQueryWrapper<ShortLinkDO>()
                            .eq(ShortLinkDO::getShortUri, shortUrl)
                            .eq(ShortLinkDO::getEnableStatus, 0)
            );

            // 如果数据库中不存在该短链，则返回404
            if (shortLink == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                // 缓存短链不存在的信息，解决布隆过滤器误判的问题
                redisTemplate.opsForValue().set(CACHE_SHORT_LINK + shortUrl, "", 5 * 60 * 1000);
                return;
            }

            //重建缓存
            redisTemplate.opsForValue().set(CACHE_SHORT_LINK + shortUrl, shortLink.getOriginUrl());

            // 进行302重定向到原始链接
            response.setStatus(HttpServletResponse.SC_FOUND);
            response.setHeader("Location", shortLink.getOriginUrl());
        } finally {
            lock.unlock();
        }
    }
}
