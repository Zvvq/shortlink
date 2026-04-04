package com.cqie.shortlink_admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cqie.shortlink_admin.entity.ShortLinkDO;
import com.cqie.shortlink_admin.mapper.ShortLinkMapper;
import com.cqie.shortlink_admin.service.RedirectService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RedirectServiceImpl implements RedirectService {

    private final ShortLinkMapper shortLinkMapper;


    /**
     * 根据短链跳转
     * @param shortUrl 短链
     */
    @Override
    public void redirect(String shortUrl, HttpServletRequest request, HttpServletResponse response) {

        //TODO 添加缓存
        ShortLinkDO shortLink = shortLinkMapper.selectOne(
                new LambdaQueryWrapper<ShortLinkDO>()
                        .eq(ShortLinkDO::getShortUri, shortUrl)
                        .eq(ShortLinkDO::getEnableStatus, 0)
        );
            
        if (shortLink == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
            
        // 进行302重定向到原始链接
        response.setStatus(HttpServletResponse.SC_FOUND);
        response.setHeader("Location", shortLink.getOriginUrl());
    }
}
