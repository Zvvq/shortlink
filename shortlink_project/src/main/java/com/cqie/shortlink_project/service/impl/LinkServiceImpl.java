package com.cqie.shortlink_project.service.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cqie.shortlink_project.dto.request.ShortLinkCreateRequest;
import com.cqie.shortlink_project.dto.response.ShortLinkCreateResponse;
import com.cqie.shortlink_project.entity.LinkDO;
import com.cqie.shortlink_project.mapper.LinkMapper;
import com.cqie.shortlink_project.service.LinkService;
import com.cqie.shortlink_project.util.BeanUtil;
import com.cqie.shortlink_project.util.ShortLinkUtil;
import org.springframework.stereotype.Service;

/**
* @author friendA
* @description 针对表【t_link(短链接表)】的数据库操作Service实现
* @createDate 2026-04-02 16:24:45
*/
@Service
public class LinkServiceImpl extends ServiceImpl<LinkMapper, LinkDO>
    implements LinkService {


    /**
     * 创建短链
     * @param requestParma 创建短链请求参数
     * @return 创建短链响应参数
     */
    @Override
    public ShortLinkCreateResponse createShortLink(ShortLinkCreateRequest requestParma) {
        //获取并生成短链接
        String originUrl = requestParma.getOriginUrl();
        String shortLink = ShortLinkUtil.generateShortCode(originUrl);

        //将请求参数转换为实体类
        LinkDO linkDO = BeanUtil.convert(requestParma, LinkDO.class);
        linkDO.setShortUri(shortLink);
        linkDO.setFullShortUrl(requestParma.getDomain()+ "/" + shortLink);

        //保存短链接信息到数据库
        baseMapper.insert(linkDO);

        //构建返回信息
        ShortLinkCreateResponse shortLinkCreateResponse = BeanUtil.convert(linkDO, ShortLinkCreateResponse.class);
        shortLinkCreateResponse.setClickNum(0);
        shortLinkCreateResponse.setEnableStatus(1);

        return shortLinkCreateResponse;

    }
}




