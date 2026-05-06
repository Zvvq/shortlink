package com.cqie.shortlink_project.dto.response;

import lombok.Data;

/**
 * Hourly click statistics for a short link.
 */
@Data
public class LinkClickStatsResponse {

    /**
     * Value from t_link_access_stats.full_short_url. Historical data may use shortUri.
     */
    private String linkKey;

    /**
     * Sum of hourly PV.
     */
    private Long clickNum;
}
