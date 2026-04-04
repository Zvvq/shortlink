package com.cqie.shortlink_project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cqie.shortlink_project.entity.GroupDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 短链接分组表数据库操作Mapper
 */
@Mapper
public interface GroupMapper extends BaseMapper<GroupDO> {
}
