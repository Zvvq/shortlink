package com.cqie.shortlink_admin.controller;

import com.cqie.shortlink_admin.dto.request.LinkAccessStatsRequestDTO;
import com.cqie.shortlink_admin.dto.response.LinkAccessStatsResponseDTO;
import com.cqie.shortlink_admin.service.LinkAccessStatsService;
import com.cqie.shortlink_common.common.convention.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 短链接访问统计控制层
 */
@RestController
@RequestMapping("/api/shortlink/v1/link/access-stats")
@RequiredArgsConstructor
public class LinkAccessStatsController {

    private final LinkAccessStatsService linkAccessStatsService;

    /**
     * 查询短链接指定日期和小时的点击统计
     *
     * @param requestDTO 访问统计查询请求参数
     * @return 指定日期全天点击数和指定小时点击数
     */
    @GetMapping("/query")
    public Result<LinkAccessStatsResponseDTO> query(LinkAccessStatsRequestDTO requestDTO) {
        return Result.success(linkAccessStatsService.queryLinkAccessStats(requestDTO));
    }
}
