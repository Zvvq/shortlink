package com.cqie.shortlink_admin;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.cqie.shortlink_admin.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.redisson.api.RBloomFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.IntStream;

@SpringBootTest
class ShortlinkAdminApplicationTests {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private RBloomFilter<String> shortLinkCreateCachePenetrationBloomFilter;


    @Test
    void contextLoads() {
    }

    @Test
    void testJwt() {
        JwtUtil jwtUtil = new JwtUtil();
        String token = jwtUtil.generateToken("admin");
        System.out.println(token);
    }

    @Test
    void testJwt2() {
        System.out.println("用户" + (10000 + new Random().nextInt(90000)));
    }

    @Test
    void insertPvUvTestData() {
        String date = "2026-04-16";

        // ---------- 辅助方法：填充 HyperLogLog 到目标近似基数 ----------
        // 注意：HyperLogLog 是概率数据结构，基数较小时误差较大，此处通过添加足够多的不同元素来接近目标值
        // 为简单起见，直接添加 count 个不同元素，对于小基数误差可接受

        // ---------- 短码 PemwYn ----------
        // PV 仍为字符串数值
        redisTemplate.opsForValue().set("short-link:pv:" + date + ":PemwYn", "6");
        redisTemplate.opsForValue().set("short-link:pv:" + date + ":16:PemwYn", "2");
        redisTemplate.opsForValue().set("short-link:pv:" + date + ":17:PemwYn", "1");
        redisTemplate.opsForValue().set("short-link:pv:" + date + ":18:PemwYn", "1");
        redisTemplate.opsForValue().set("short-link:pv:" + date + ":15:PemwYn", "2");

        // UV 使用 HyperLogLog：为每个小时和全天添加模拟用户 ID
        addUvElements("short-link:uv:" + date + ":PemwYn", 4);            // 全天 UV 目标 4
        addUvElements("short-link:uv:" + date + ":16:PemwYn", 2);
        addUvElements("short-link:uv:" + date + ":17:PemwYn", 1);
        addUvElements("short-link:uv:" + date + ":18:PemwYn", 1);
        addUvElements("short-link:uv:" + date + ":15:PemwYn", 1);

        // ---------- 短码 AqBIQj ----------
        redisTemplate.opsForValue().set("short-link:pv:" + date + ":AqBIQj", "2");
        redisTemplate.opsForValue().set("short-link:pv:" + date + ":15:AqBIQj", "2");

        addUvElements("short-link:uv:" + date + ":AqBIQj", 2);
        addUvElements("short-link:uv:" + date + ":15:AqBIQj", 2);

        // ---------- 2026-04-17 数据（截图中有）----------
        redisTemplate.opsForValue().set("short-link:pv:2026-04-17:15:PemwYn", "1");
        addUvElements("short-link:uv:2026-04-17:15:PemwYn", 1);
        redisTemplate.opsForValue().set("short-link:pv:2026-04-17:PemwYn", "1");
        addUvElements("short-link:uv:2026-04-17:PemwYn", 1);

        System.out.println("✅ PV/UV 测试数据插入完成（UV 已用 HyperLogLog 填充）");
    }

    /**
     * 向指定 HyperLogLog Key 中添加模拟用户 ID，使其基数接近 targetCount
     */
    private void addUvElements(String key, int targetCount) {
        // 删除旧 Key（确保从零开始）
        redisTemplate.delete(key);
        // 生成 targetCount 个不同的用户 ID（例如 "user:1", "user:2" ...）
        String[] users = IntStream.rangeClosed(1, targetCount)
                .mapToObj(i -> "user:" + i)
                .toArray(String[]::new);
        redisTemplate.opsForHyperLogLog().add(key, users);
    }

    @Test
    void testHyperLogLog() {
        Long size = redisTemplate.opsForHyperLogLog().size("short-link:uv:2026-04-18:PemwYn");
        String string = redisTemplate.opsForValue().get("short-link:pv:2026-04-18:PemwYn");
        System.out.println("UV 数量：" + size);
        System.out.println("PV 数量：" + string);
    }


    @Test
    void addRBFilter() {
        shortLinkCreateCachePenetrationBloomFilter.add("PemwYn");
    }

}

