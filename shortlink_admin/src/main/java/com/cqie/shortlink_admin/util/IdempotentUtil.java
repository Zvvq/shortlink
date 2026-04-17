package com.cqie.shortlink_admin.util;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class IdempotentUtil {

    private final RedisTemplate<String, String> redisTemplate;


    /**
     * 检查消息消费是否重复
     * @param businessKey 业务键
     * @param msgId 消息ID
     * @return true 表示消息消费不重复，false 表示消息消费重复
     */
    public boolean checkIdempotent(String businessKey, String msgId) {
        // 原子 SETNX + 过期时间
        return Boolean.TRUE.equals(
                redisTemplate.opsForValue().setIfAbsent(businessKey+ ":" + msgId, "1", 24, TimeUnit.HOURS)
        );
    }
}
