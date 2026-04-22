package com.cqie.shortlink_project.common.consumer;

import com.cqie.shortlink_project.common.constant.RedisCacheConstant;
import com.cqie.shortlink_project.common.constant.RocketMQConstant;
import com.cqie.shortlink_project.common.convention.exception.ClientException;
import com.cqie.shortlink_project.entity.CacheEvictMessage;
import com.cqie.shortlink_project.util.IdempotentUtil;
import com.cqie.shortlink_project.util.MessageUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
@RocketMQMessageListener(topic = RocketMQConstant.SHORT_LINK_CACHE_EVICT_DEAD_LETTER_TOPIC, consumerGroup = RocketMQConstant.SHORT_LINK_CACHE_EVICT_DEAD_LETTER_CONSUMER_GROUP)
public class CacheDLQConsumer implements RocketMQListener<MessageExt> {

    private final RedisTemplate<String, String> redisTemplate;
    private final IdempotentUtil idempotentUtil;
    private final MessageUtil messageUtil;


    @Override
    public void onMessage(MessageExt message) {
        log.error("接收到死信队列消息: {}", message);
        String msgId = message.getMsgId();

        boolean idempotent = idempotentUtil.checkIdempotent(RocketMQConstant.SHORT_LINK_STATES_CHECK_IDEMPOTENT, msgId);
        if (idempotent) {
            log.info("消息 {} 已经被消费过了，忽略重复消费", msgId);
            return;
        }

        CacheEvictMessage cacheEvictMessage = messageUtil.deserializeMessage(message.getBody(), CacheEvictMessage.class);
        //重试
        try {
            redisTemplate.delete(RedisCacheConstant.CACHE_SHORT_LINK + cacheEvictMessage.getShortUrl());
        } catch (Exception e) {
            log.error("死信队列删除缓存失败: {}", cacheEvictMessage.getShortUrl(), e);
            throw new ClientException("死信队列删除缓存失败: " + cacheEvictMessage.getShortUrl());
        }
    }
}
