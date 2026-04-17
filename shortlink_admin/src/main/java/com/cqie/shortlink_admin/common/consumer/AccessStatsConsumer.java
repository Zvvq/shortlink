package com.cqie.shortlink_admin.common.consumer;


import com.cqie.shortlink_admin.common.constant.RocketMQConstant;
import com.cqie.shortlink_admin.dto.message.StatsMessage;
import com.cqie.shortlink_admin.util.IdempotentUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
@Slf4j
@RequiredArgsConstructor
@RocketMQMessageListener(topic = RocketMQConstant.SHORT_LINK_STATES_TOPIC, consumerGroup = RocketMQConstant.SHORT_LINK_STATES_CONSUMER_GROUP)
public class AccessStatsConsumer implements RocketMQListener<MessageExt> {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final IdempotentUtil  idempotentUtil;


    @Override
    public void onMessage(MessageExt message) {
        // 处理接收到的消息，例如记录访问统计数据

        StatsMessage statesMessage = deserializeMessage(message.getBody());

        if (statesMessage == null) {
            return;
        }

        //检查消息是否被消费（幂等性设计）
        if (idempotentUtil.checkIdempotent(RocketMQConstant.SHORT_LINK_STATES_CHECK_IDEMPOTENT, message.getMsgId())) {
            log.info("消息 {} 已经被消费过了，忽略重复消费", message.getMsgId());
            return;
        }


        String shortUrl = statesMessage.getShortUrl();
        String uvId = statesMessage.getUvId();
        LocalDateTime now = statesMessage.getAccessTime();

        String hourKey = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH"));
        String dayKey = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        redisTemplate.opsForHyperLogLog().add("short-link:uv:" + shortUrl + ":" + hourKey, uvId);
        redisTemplate.opsForValue().increment("short-link:pv:" + shortUrl + ":" + hourKey);
        redisTemplate.opsForHyperLogLog().add("short-link:uv:" + shortUrl + ":" + dayKey, uvId);
        redisTemplate.opsForValue().increment("short-link:pv:" + shortUrl + ":" + dayKey);
    }

    private StatsMessage deserializeMessage(byte[] body) {
        try {
            return objectMapper.readValue(body, StatsMessage.class);
        } catch (Exception e) {
            log.error("反序列化 StatsMessage 失败", e);
            return null;
        }
    }
}
