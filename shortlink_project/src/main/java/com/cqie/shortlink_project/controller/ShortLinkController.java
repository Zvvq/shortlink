package com.cqie.shortlink_project.controller;

import com.cqie.shortlink_project.common.convention.result.Result;
import com.cqie.shortlink_project.dto.request.ShortLinkCreateRequest;
import com.cqie.shortlink_project.dto.response.ShortLinkCreateResponse;
import com.cqie.shortlink_project.service.LinkService;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/shortlink/v1/link")
@RequiredArgsConstructor
public class ShortLinkController {

    private final LinkService LinkService;

    /**
     * 创建短链
     * @param requestParma 创建短链请求参数
     * @return 创建短链响应参数
     */
    @PostMapping("/create")
    public Result<ShortLinkCreateResponse> createShortLink(@RequestBody ShortLinkCreateRequest requestParma) {
        return Result.success(LinkService.createShortLink(requestParma));
    }
}
