package com.cqie.shortlink_project.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.cqie.shortlink_project.dto.request.ShortLinkCreateRequest;
import com.cqie.shortlink_project.dto.response.ShortLinkCreateResponse;
import com.cqie.shortlink_project.entity.LinkDO;

/**
* @author friendA
* @description 针对表【t_link(短链接表)】的数据库操作Service
* @createDate 2026-04-02 16:24:45
*/
public interface LinkService extends IService<LinkDO> {

    ShortLinkCreateResponse createShortLink(ShortLinkCreateRequest requestParma);

}
