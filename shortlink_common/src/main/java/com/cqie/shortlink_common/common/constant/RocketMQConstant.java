package com.cqie.shortlink_common.common.constant;

public final class RocketMQConstant {

    private RocketMQConstant() {
    }

    public static final String SHORT_LINK_STATES_TOPIC = "short-link-access-stats-topic";
    public static final String SHORT_LINK_STATES_CONSUMER_GROUP = "short-link-access-stats-consumer-group-01";
    public static final String SHORT_LINK_STATES_CHECK_IDEMPOTENT = "short-link-access-stats-check-idempotent";

    public static final String SHORT_LINK_CACHE_EVICT_TOPIC = "short-link-cache-evict-topic";
    public static final String SHORT_LINK_CACHE_EVICT_DEAD_LETTER_TOPIC = "%DLQ%short-link-cache-evict-topic";
    public static final String SHORT_LINK_CACHE_EVICT_CONSUMER_GROUP = "short-link-cache-evict-consumer-group01";
    public static final String SHORT_LINK_CACHE_EVICT_DEAD_LETTER_CONSUMER_GROUP =
            "short-link-cache-evict-dead-letter-consumer-group01";
}
