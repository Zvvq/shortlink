package com.cqie.shortlink_admin.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cqie.shortlink_admin.entity.LinkAccessStatsDO;
import com.cqie.shortlink_admin.mapper.LinkAccessStatsMapper;
import com.cqie.shortlink_admin.service.LinkAccessStatsService;
import org.springframework.stereotype.Service;


@Service
public class LinkAccessStatsServiceImpl
        extends ServiceImpl<LinkAccessStatsMapper, LinkAccessStatsDO>
        implements LinkAccessStatsService {

}
