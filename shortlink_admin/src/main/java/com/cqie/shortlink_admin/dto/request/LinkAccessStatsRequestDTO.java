package com.cqie.shortlink_admin.dto.request;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * 短链接访问统计查询请求 DTO
 */
@Data
public class LinkAccessStatsRequestDTO {

    /**
     * 完整短链接
     */
    private String fullShortUrl;

    /**
     * 短链接后缀
     */
    private String shortUri;

    /**
     * 统计日期
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;

    /**
     * 统计小时，取值范围 0-23
     */
    private Integer hour;
}
