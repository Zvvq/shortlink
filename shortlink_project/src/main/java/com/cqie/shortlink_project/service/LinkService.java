package com.cqie.shortlink_project.service;


import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cqie.shortlink_project.dto.request.ShortLinkCreateRequest;
import com.cqie.shortlink_project.dto.request.ShortLinkPageRequest;
import com.cqie.shortlink_project.dto.request.ShortLinkUpdateRequest;
import com.cqie.shortlink_project.dto.response.GenerateDescriptionResponse;
import com.cqie.shortlink_project.dto.response.GroupLinkCountResponse;
import com.cqie.shortlink_project.dto.response.ShortLinkCreateResponse;
import com.cqie.shortlink_project.dto.response.ShortLinkPageResponse;
import com.cqie.shortlink_project.entity.LinkDO;

import java.util.List;

/**
* @author friendA
* @description 针对表【t_link(短链接表)】的数据库操作Service
* @createDate 2026-04-02 16:24:45
*/
public interface LinkService extends IService<LinkDO> {

    /**
     * 创建短链
     * @param requestParam 创建短链请求参数
     * @return 创建短链响应参数
     */
    ShortLinkCreateResponse createShortLink(ShortLinkCreateRequest requestParam);

    /**
     * 更新短链
     * @param requestParam 更新短链请求参数
     */
    void updateShortLink(ShortLinkUpdateRequest requestParam);

    /**
     * 删除短链
     * @param fullShortUrl 完整短链接
     */
    void deleteShortLink(String fullShortUrl);

    /**
     * 分页查询短链
     * @param requestParam 分页查询请求参数
     * @return 分页查询结果
     */
    IPage<ShortLinkPageResponse> pageShortLink(ShortLinkPageRequest requestParam);

    /**
     * 查询当前用户的所有分组及短链接数量
     * @param username 用户名
     * @return 分组及短链接数量列表
     */
    List<GroupLinkCountResponse> listGroupLinkCount(String username);


    /**
     * 根据原始链接生成描述
     * @param originalUrl 原始链接
     * @return 描述信息
     */
    GenerateDescriptionResponse generateDescription(String originalUrl);
}
