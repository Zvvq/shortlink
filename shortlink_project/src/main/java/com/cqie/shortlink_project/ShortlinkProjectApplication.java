package com.cqie.shortlink_project;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.cqie.shortlink_project.mapper")
public class ShortlinkProjectApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShortlinkProjectApplication.class, args);
    }

}
