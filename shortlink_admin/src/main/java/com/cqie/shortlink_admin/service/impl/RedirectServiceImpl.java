package com.cqie.shortlink_admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.cqie.shortlink_admin.common.constant.RocketMQConstant;

import com.cqie.shortlink_admin.dto.message.StatsMessage;
import com.cqie.shortlink_admin.entity.ShortLinkDO;
import com.cqie.shortlink_admin.mapper.ShortLinkMapper;
import com.cqie.shortlink_admin.service.RedirectService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
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
    private final RocketMQTemplate rocketMQTemplate;




    /**
     * 根据短链跳转
     * @param shortUrl 短链
     */
    @Override
    public void redirect(String shortUrl, HttpServletRequest request, HttpServletResponse response) {

        // 从缓存中获取短链对应的长链
        if (checkShortLinkCache(shortUrl, request, response)) {
            log.debug("缓存命中: {}", shortUrl);
            return;
        }

        // 查询布隆过滤器
        if (!shortLinkCreateCachePenetrationBloomFilter.contains(shortUrl)) {
            log.warn("布隆过滤器拦截: {}", shortUrl);
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // 获取分布式锁
        RLock lock = redissonClient.getLock(LOCK_SHORT_LINK_REBUILD + shortUrl);
        lock.lock();
        try {
            // double-check
            if (checkShortLinkCache(shortUrl, request, response)) {
                return;
            }

            // 查询数据库
            ShortLinkDO shortLink = shortLinkMapper.selectOne(
                    new LambdaQueryWrapper<ShortLinkDO>()
                            .eq(ShortLinkDO::getShortUri, shortUrl)
                            .eq(ShortLinkDO::getEnableStatus, 1)
                            .gt(ShortLinkDO::getValidDate, LocalDateTime.now())
            );
            if (shortLink == null) {
                log.warn("短链不存在: {}", shortUrl);
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                redisTemplate.opsForValue().set(CACHE_SHORT_LINK + shortUrl, "", 60, TimeUnit.SECONDS);//设置空值缓存，防止缓存穿透
                return;
            }

            // 重建缓存并重定向
            redisTemplate.opsForValue().set(CACHE_SHORT_LINK + shortUrl, shortLink.getOriginUrl(), 7 * 24 * 60 * 60, TimeUnit.SECONDS);//设置过期时间为七天

            // 统计访问量，涉及到cookie操作，放在锁内，避免并发导致的统计数据不准确问题
            //cookie操作放在重定向之前
            statsLinkAccess(shortUrl, request, response);
            response.setStatus(HttpServletResponse.SC_FOUND);
            response.setHeader("Location", shortLink.getOriginUrl());


            log.info("重定向成功: {} -> {}", shortUrl, shortLink.getOriginUrl());
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
    private boolean checkShortLinkCache(String shortUrl, HttpServletRequest request, HttpServletResponse response) {
        String originUrl = redisTemplate.opsForValue().get(CACHE_SHORT_LINK + shortUrl);
        if (StringUtils.isNotBlank(originUrl)) {
            //统计
            statsLinkAccess(shortUrl, request, response);
            response.setStatus(HttpServletResponse.SC_FOUND);
            response.setHeader("Location", originUrl);

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
    private void statsLinkAccess(String shortUrl, HttpServletRequest request, HttpServletResponse response) {

        // 统计uv，先从cookie中获取uvId，如果没有则生成一个新的uvId并设置cookie
        Cookie[] cookies = request.getCookies();
        String uvId = null;
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("_uv_id_".equals(cookie.getName())) {
                    uvId = cookie.getValue();
                    break;
                }
            }
        }

        // 没有cookie，说明是新用户，生成一个新的uvId并设置cookie
        if (uvId == null) {
            uvId = generateUvId();
            Cookie cookie = new Cookie("_uv_id_", uvId);
            cookie.setPath("/");// 设置cookie的路径为根路径
            cookie.setMaxAge(365 * 24 * 3600);// 设置cookie的过期时间为一年
            cookie.setHttpOnly(true);// 设置cookie只能通过http访问，不能通过js访问
            // 根据情况选择是否设置Secure属性,HTTPS环境下建议设置为true
//            cookie.setSecure(request.isSecure());
            response.addCookie(cookie);
        }

        LocalDateTime now = LocalDateTime.now();
        // 构建访问统计消息
        StatsMessage message = StatsMessage.builder()
                .shortUrl(shortUrl)
                .uvId(uvId)
                .accessTime(now)
                .build();

        rocketMQTemplate.asyncSend(RocketMQConstant.SHORT_LINK_STATES_TOPIC, message,
                new SendCallback() {
                    @Override
                    public void onSuccess(SendResult sendResult) {
                    }

                    @Override
                    public void onException(Throwable throwable) {
                        log.error("发送访问统计消息失败: {}", throwable.getMessage(), throwable);
                    }
                }


        );
    }

    /**
     * 生成uvId
     * @return uvId
     */
    private String generateUvId() {
        return UUID.randomUUID().toString();
    }
}
