package com.cqie.shortlink_admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cqie.shortlink_admin.dto.response.SaveGroupResponseDTO;
import com.cqie.shortlink_admin.dto.response.SelectGroupResponseDTO;
import com.cqie.shortlink_admin.entity.ShortLinkGroupDO;

import java.util.List;

public interface GroupService extends IService<ShortLinkGroupDO> {
    SaveGroupResponseDTO saveGroup(String groupName);

    List<SelectGroupResponseDTO> selectGroup(String username);
}
