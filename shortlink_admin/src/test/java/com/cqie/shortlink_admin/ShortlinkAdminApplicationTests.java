package com.cqie.shortlink_admin;

import com.cqie.shortlink_admin.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Random;

@SpringBootTest
class ShortlinkAdminApplicationTests {

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

}
