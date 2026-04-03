package com.cqie.shortlink_admin;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@MapperScan("com.cqie.shortlink_admin.mapper")
public class ShortlinkAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShortlinkAdminApplication.class, args);
    }

}
