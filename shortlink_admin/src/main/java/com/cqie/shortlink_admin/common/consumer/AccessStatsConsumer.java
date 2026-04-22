package com.cqie.shortlink_admin.common.consumer;


import com.cqie.shortlink_common.common.constant.RocketMQConstant;
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

        // 提取日期和小时
        String dayKey = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String hour = now.format(DateTimeFormatter.ofPattern("HH"));   // 只取小时

        // 构建新格式的 Key
        String pvDayKey = "short-link:pv:" + dayKey + ":" + shortUrl;
        String pvHourKey = "short-link:pv:" + dayKey + ":" + hour + ":" + shortUrl;
        String uvDayKey = "short-link:uv:" + dayKey + ":" + shortUrl;
        String uvHourKey = "short-link:uv:" + dayKey + ":" + hour + ":" + shortUrl;

        // 执行写入操作
        redisTemplate.opsForHyperLogLog().add(uvHourKey, uvId);
        redisTemplate.opsForValue().increment(pvHourKey);
        redisTemplate.opsForHyperLogLog().add(uvDayKey, uvId);
        redisTemplate.opsForValue().increment(pvDayKey);
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
