package com.cqie.shortlink_project.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cqie.shortlink_project.common.convention.result.Result;
import com.cqie.shortlink_project.dto.request.ShortLinkCreateRequest;
import com.cqie.shortlink_project.dto.request.ShortLinkPageRequest;
import com.cqie.shortlink_project.dto.request.ShortLinkUpdateRequest;
import com.cqie.shortlink_project.dto.response.GenerateDescriptionResponse;
import com.cqie.shortlink_project.dto.response.GroupLinkCountResponse;
import com.cqie.shortlink_project.dto.response.ShortLinkCreateResponse;
import com.cqie.shortlink_project.dto.response.ShortLinkPageResponse;
import com.cqie.shortlink_project.service.LinkService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 短链接控制层
 */
@RestController
@RequestMapping("/api/shortlink/v1/link")
@RequiredArgsConstructor
@Slf4j
public class ShortLinkController {

    private final LinkService linkService;

    /**
     * 创建短链
     * @param requestParam 创建短链请求参数
     * @return 创建短链响应参数
     */
    @PostMapping("/create")
    public Result<ShortLinkCreateResponse> createShortLink(@RequestBody ShortLinkCreateRequest requestParam) {
        return Result.success(linkService.createShortLink(requestParam));
    }

    /**
     * 更新短链
     * @param requestParam 更新短链请求参数
     * @return 更新结果
     */
    @PutMapping("/update")
    public Result<Void> updateShortLink(@RequestBody ShortLinkUpdateRequest requestParam) {
        linkService.updateShortLink(requestParam);
        return Result.success();
    }

    /**
     * 删除短链
     * @param fullShortUrl 完整短链接
     * @return 删除结果
     */
    @DeleteMapping("/delete")
    public Result<Void> deleteShortLink(@RequestParam String fullShortUrl) {
        linkService.deleteShortLink(fullShortUrl);
        return Result.success();
    }

    /**
     * 分页查询短链
     * @param requestParam 分页查询请求参数
     * @return 分页查询结果
     */
    @GetMapping("/page")
    public Result<IPage<ShortLinkPageResponse>> pageShortLink(ShortLinkPageRequest requestParam) {
        return Result.success(linkService.pageShortLink(requestParam));
    }

    /**
     * 查询当前用户的所有分组及短链接数量
     * @param username 用户名
     * @return 分组及短链接数量列表
     */
    @GetMapping("/group/list")
    public Result<List<GroupLinkCountResponse>> listGroupLinkCount(@RequestParam String username) {
        return Result.success(linkService.listGroupLinkCount(username));
    }

    /**
     * 根据原始链接总结网页内容
     */
    @GetMapping("/description")
    public Result<GenerateDescriptionResponse> generateDescription(@RequestParam String originalUrl) {
        log.info("generateDescription originalUrl: {}", originalUrl);
        return Result.success(linkService.generateDescription(originalUrl));
    }
}
