package com.cqie.shortlink_project.common.constant;

public class RocketMQConstant {

    /**
     * 短链缓存一致性topic
     */
    public static final String SHORT_LINK_CACHE_EVICT_TOPIC = "short-link-cache-evict-topic";

    /**
     * 短链缓存一致性死信队列topic
     */
    public static final String SHORT_LINK_CACHE_EVICT_DEAD_LETTER_TOPIC = "%DLQ%short-link-cache-evict-topic";

    /**
     * 短链缓存一致性消费者组
     */
    public static final String SHORT_LINK_CACHE_EVICT_CONSUMER_GROUP = "short-link-cache-evict-consumer-group01";

    /**
     * 短链缓存一致性死信队列消费者组
     */
    public static final String SHORT_LINK_CACHE_EVICT_DEAD_LETTER_CONSUMER_GROUP = "short-link-cache-evict-dead-letter-consumer-group01";

    /**
     * 短链缓存一致性去重检查
     */
    public static final String SHORT_LINK_STATES_CHECK_IDEMPOTENT = "short-link-cache-evict-check-idempotent";


}
