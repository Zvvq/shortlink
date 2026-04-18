package com.cqie.shortlink_admin.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cqie.shortlink_admin.entity.ScheduledDO;
import com.cqie.shortlink_admin.mapper.ScheduledMapper;
import com.cqie.shortlink_admin.service.ScheduledService;
import org.springframework.stereotype.Service;

@Service
public class ScheduledServiceImpl extends ServiceImpl<ScheduledMapper, ScheduledDO>
        implements ScheduledService {

}
