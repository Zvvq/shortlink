package com.cqie.shortlink_admin;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.cqie.shortlink_admin.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Random;

@SpringBootTest
class ShortlinkAdminApplicationTests {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

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
    void testStringUtils() {
        String string = redisTemplate.opsForValue().get("132");
        System.out.println(string);

    }

}
