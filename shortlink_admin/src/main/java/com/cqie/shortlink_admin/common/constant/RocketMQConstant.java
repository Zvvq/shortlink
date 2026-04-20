package com.cqie.shortlink_admin.common.constant;

public class RocketMQConstant {

    /**
     * 短链访问统计topic
     */
    public static final String SHORT_LINK_STATES_TOPIC = "short-link-access-stats-topic";

    /**
     * 短链访问统计消费者组
     */
    public static final String SHORT_LINK_STATES_CONSUMER_GROUP = "short-link-access-stats-consumer-group-01";

    /**
     * 短链访问统计去重检查
     */
    public static final String SHORT_LINK_STATES_CHECK_IDEMPOTENT = "short-link-access-stats-check-idempotent";
}
