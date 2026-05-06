package com.cqie.shortlink_project.common.consumer;


import com.cqie.shortlink_common.common.constant.RedisCacheConstant;
import com.cqie.shortlink_common.common.constant.RocketMQConstant;
import com.cqie.shortlink_project.entity.CacheEvictMessage;
import com.cqie.shortlink_project.util.IdempotentUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
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
@RocketMQMessageListener(topic = RocketMQConstant.SHORT_LINK_CACHE_EVICT_TOPIC, consumerGroup = RocketMQConstant.SHORT_LINK_CACHE_EVICT_CONSUMER_GROUP)
public class CacheEvictConsumer implements RocketMQListener<MessageExt> {

    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public void onMessage(MessageExt messageExt) {
        // 处理接收到的消息，例如记录访问统计数据
        String msgId = messageExt.getMsgId();

        CacheEvictMessage cacheEvictMessage = deserializeMessage(messageExt.getBody());
        if (cacheEvictMessage == null || cacheEvictMessage.getShortUrl() == null) {
            log.error("消息反序列化失败或shortUrl为空: msgId={}", msgId);
            return;
        }
        
        String cacheEvictShortUrl = cacheEvictMessage.getShortUrl();
        String shortUrl = cacheEvictShortUrl.substring(cacheEvictShortUrl.length() - 6);
        log.info("接收到消息: msgId={}, body={}", msgId, shortUrl);

        // 构建完整的缓存key
        String cacheKey = RedisCacheConstant.CACHE_SHORT_LINK + shortUrl;
        if (Boolean.TRUE.equals(redisTemplate.delete(cacheKey))) {
            log.info("成功删除缓存: {}", cacheKey);
        } else {
            log.info("缓存 {} 不存在", cacheKey);
        }

    }



    private CacheEvictMessage deserializeMessage(byte[] body) {
        try {
            return objectMapper.readValue(body, CacheEvictMessage.class);
        } catch (Exception e) {
            log.error("反序列化 CacheEvictMessage 失败", e);
            return null;
        }
    }
}
