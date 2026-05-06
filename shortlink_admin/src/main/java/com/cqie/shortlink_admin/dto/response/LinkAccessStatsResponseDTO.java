package com.cqie.shortlink_admin.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

/**
 * 短链接访问统计响应 DTO
 */
@Data
@Builder
public class LinkAccessStatsResponseDTO {

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
    private LocalDate date;

    /**
     * 统计小时
     */
    private Integer hour;

    /**
     * 当天点击数
     */
    private Long dayClickNum;

    /**
     * 指定小时点击数
     */
    private Long hourClickNum;
}
