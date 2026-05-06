package com.cqie.shortlink_admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cqie.shortlink_admin.dto.request.LinkAccessStatsRequestDTO;
import com.cqie.shortlink_admin.dto.response.LinkAccessStatsResponseDTO;
import com.cqie.shortlink_admin.entity.LinkAccessStatsDO;


/**
 * 短链接访问统计服务层
 */
public interface LinkAccessStatsService extends IService<LinkAccessStatsDO> {

    /**
     * 查询短链接指定日期和小时的点击统计
     *
     * @param requestDTO 访问统计查询请求参数
     * @return 指定日期全天点击数和指定小时点击数
     */
    LinkAccessStatsResponseDTO queryLinkAccessStats(LinkAccessStatsRequestDTO requestDTO);
}
