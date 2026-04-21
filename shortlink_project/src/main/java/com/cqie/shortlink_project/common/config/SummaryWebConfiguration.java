package com.cqie.shortlink_project.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "summary.web")
public class SummaryWebConfiguration {

    /**
     * 主机地址，默认为 localhost
     */
    private String host;

    /**
     * 上下文路径，默认为 /web/summary
     */
    private String contextPath;


    /**
     * 获取基础 URL
     * @return 基础 URL
     */
    public String getBaseUrl() {
        return host + contextPath;
    }
}
