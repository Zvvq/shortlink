package com.cqie.shortlink_admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cqie.shortlink_admin.dto.request.SortGroupRequestDTO;
import com.cqie.shortlink_admin.dto.request.UpdateGroupRequestDTO;
import com.cqie.shortlink_admin.dto.response.SaveGroupResponseDTO;
import com.cqie.shortlink_admin.dto.response.SelectGroupResponseDTO;
import com.cqie.shortlink_admin.entity.ShortLinkGroupDO;

import java.util.List;

public interface GroupService extends IService<ShortLinkGroupDO> {

    /**
     * 保存分组
     * @param groupName 分组名称
     * @return 保存结果
     */
    SaveGroupResponseDTO saveGroup(String groupName);

    /**
     * 查询当前用户的分组列表
     * @return 分组列表
     */
    List<SelectGroupResponseDTO> selectGroup();

    /**
     * 更新分组名称
     * @param requestDTO 更新请求参数
     */
    void updateGroup(UpdateGroupRequestDTO requestDTO);

    /**
     * 删除分组
     * @param gid 分组标识
     */
    void deleteGroup(String gid);

    /**
     * 分组排序
     * @param requestDTOList 排序请求参数列表
     */
    void sortGroup(List<SortGroupRequestDTO> requestDTOList);
}
