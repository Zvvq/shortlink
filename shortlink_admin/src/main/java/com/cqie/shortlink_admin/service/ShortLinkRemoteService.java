package com.cqie.shortlink_admin.service;


import com.cqie.shortlink_admin.dto.request.ShortLinkCreateRequestDTO;
import com.cqie.shortlink_admin.dto.request.ShortLinkPageRequestDTO;
import com.cqie.shortlink_admin.dto.request.ShortLinkUpdateRequestDTO;
import com.cqie.shortlink_admin.dto.response.ShortLinkCreateResponseDTO;
import com.cqie.shortlink_admin.dto.response.ShortLinkPageResponseDTO;
import java.util.List;


/**
 * 短链接远程调用服务
 * 通过HTTP调用project模块的接口
 */
public interface ShortLinkRemoteService {

    /**
     * 创建短链接
     */
    ShortLinkCreateResponseDTO createShortLink(ShortLinkCreateRequestDTO requestDTO);

    /**
     * 更新短链接
     */
    void updateShortLink(ShortLinkUpdateRequestDTO requestDTO);

    /**
     * 删除短链接
     */
    void deleteShortLink(String fullShortUrl);

    /**
     * 分页查询短链接
     */
    List<ShortLinkPageResponseDTO> pageShortLink(ShortLinkPageRequestDTO requestDTO);
}
