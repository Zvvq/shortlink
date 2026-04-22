package com.cqie.shortlink_admin.controller;

import com.cqie.shortlink_common.common.convention.result.Result;
import com.cqie.shortlink_admin.dto.request.ShortLinkCreateRequestDTO;
import com.cqie.shortlink_admin.dto.request.ShortLinkPageRequestDTO;
import com.cqie.shortlink_admin.dto.request.ShortLinkUpdateRequestDTO;
import com.cqie.shortlink_admin.dto.response.ShortLinkCreateResponseDTO;
import com.cqie.shortlink_admin.dto.response.ShortLinkPageResponseDTO;
import com.cqie.shortlink_admin.service.ShortLinkRemoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 短链接控制层（Admin模块）
 * 通过HTTP调用Project模块的接口
 */
@RestController
@RequestMapping("/api/shortlink/v1/link")
@RequiredArgsConstructor
public class ShortLinkController {

    private final ShortLinkRemoteService shortLinkRemoteService;

    /**
     * 创建短链接
     * @param requestDTO 创建请求参数
     * @return 创建结果
     */
    @PostMapping("/create")
    public Result<ShortLinkCreateResponseDTO> createShortLink(@RequestBody ShortLinkCreateRequestDTO requestDTO) {
        // 从SecurityContext获取当前用户名
        String username = SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString();
        requestDTO.setUsername(username);
        
        // 设置创建类型为控制台
        requestDTO.setCreatedType(0);
        
        ShortLinkCreateResponseDTO response = shortLinkRemoteService.createShortLink(requestDTO);
        return Result.success(response);
    }

    /**
     * 更新短链接
     * @param requestDTO 更新请求参数
     * @return 更新结果
     */
    @PutMapping("/update")
    public Result<Void> updateShortLink(@RequestBody ShortLinkUpdateRequestDTO requestDTO) {
        shortLinkRemoteService.updateShortLink(requestDTO);
        return Result.success();
    }

    /**
     * 删除短链接
     * @param fullShortUrl 完整短链接
     * @return 删除结果
     */
    @DeleteMapping("/delete")
    public Result<Void> deleteShortLink(@RequestParam String fullShortUrl) {
        shortLinkRemoteService.deleteShortLink(fullShortUrl);
        return Result.success();
    }

    /**
     * 分页查询短链接
     * @param requestDTO 分页查询请求参数
     * @return 分页查询结果
     */
    @GetMapping("/page")
    public Result<List<ShortLinkPageResponseDTO>> pageShortLink(ShortLinkPageRequestDTO requestDTO) {
        List<ShortLinkPageResponseDTO> response = shortLinkRemoteService.pageShortLink(requestDTO);
        return Result.success(response);
    }
}
