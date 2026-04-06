package com.cqie.shortlink_project.common.constant;

public class RedisCacheConstant {

    // 用户注册锁
    public final static String LOCK_USER_REGISTER = "short-link:lock:user:register:";

    // 短链缓存
    public final static String CACHE_SHORT_LINK = "short-link:cache:short-link:";

    // 短链缓存重建锁
    public final static String LOCK_SHORT_LINK_REBUILD = "short-link:lock:short-link:rebuild:";

}
