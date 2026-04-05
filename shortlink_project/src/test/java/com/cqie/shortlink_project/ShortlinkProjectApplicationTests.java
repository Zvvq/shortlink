package com.cqie.shortlink_project;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ShortlinkProjectApplicationTests {

    @Test
    void contextLoads() {

        boolean notBlank = StringUtils.isNotBlank("");
        System.out.println(notBlank);
    }

}
