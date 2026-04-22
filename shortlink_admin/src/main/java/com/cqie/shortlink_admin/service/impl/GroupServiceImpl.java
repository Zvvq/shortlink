package com.cqie.shortlink_admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cqie.shortlink_common.common.convention.exception.ClientException;
import com.cqie.shortlink_admin.dto.request.SortGroupRequestDTO;
import com.cqie.shortlink_admin.dto.request.UpdateGroupRequestDTO;
import com.cqie.shortlink_admin.dto.response.SaveGroupResponseDTO;
import com.cqie.shortlink_admin.dto.response.SelectGroupResponseDTO;
import com.cqie.shortlink_admin.entity.ShortLinkGroupDO;
import com.cqie.shortlink_admin.mapper.TGroupMapper;
import com.cqie.shortlink_admin.service.GroupService;
import com.cqie.shortlink_admin.util.Base62Util;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

import static com.cqie.shortlink_common.common.constant.GroupConstant.MAX_GROUP_COUNT;
import static com.cqie.shortlink_common.common.convention.errorcode.BaseErrorCode.*;

@Service
@RequiredArgsConstructor
public class GroupServiceImpl extends ServiceImpl<TGroupMapper, ShortLinkGroupDO>
        implements GroupService {


    /**
     * 保存分组
     * @param groupName 分组名称
     * @return 保存结果
     */
    @Override
    public SaveGroupResponseDTO saveGroup(String groupName) {
        String username = getCurrentUsername();

        // 检查分组数量限制
        checkGroupCount(username);

        // 先插入数据获取自增ID
        ShortLinkGroupDO groupDO = ShortLinkGroupDO.builder()
                .name(groupName)
                .username(username)
                .sortOrder(0)
                .build();

        int insert = baseMapper.insert(groupDO);

        if (insert < 1) {
            throw new ClientException(GROUP_SAVE_ERROR);
        }

        // 使用自增ID生成62进制的gid
        String gid = Base62Util.encode(groupDO.getId());

        // 更新gid字段
        groupDO.setGid(gid);
        baseMapper.updateById(groupDO);

        return SaveGroupResponseDTO.builder()
                .name(groupName)
                .sortOrder(getGroupCount(username))
                .build();
    }

    /**
     * 查询分组列表
     * @return 分组列表
     */
    @Override
    public List<SelectGroupResponseDTO> selectGroup() {
        String username = getCurrentUsername();

        List<ShortLinkGroupDO> groupList = baseMapper.selectList(
                new QueryWrapper<ShortLinkGroupDO>()
                        .eq("username", username)
                        .eq("del_flag", 0)
                        .orderByAsc("sort_order", "create_time")
        );

        return groupList.stream().map(group -> {
            SelectGroupResponseDTO dto = new SelectGroupResponseDTO();
            dto.setGid(group.getGid());
            dto.setName(group.getName());
            dto.setSortOrder(group.getSortOrder());
            return dto;
        }).collect(Collectors.toList());
    }


    /**
     * 更新分组
     * @param requestDTO 更新请求参数
     */
    @Override
    public void updateGroup(UpdateGroupRequestDTO requestDTO) {
        String username = getCurrentUsername();

        // 验证分组是否存在且属于当前用户
        ShortLinkGroupDO groupDO = baseMapper.selectOne(
                new QueryWrapper<ShortLinkGroupDO>()
                        .eq("gid", requestDTO.getGid())
                        .eq("username", username)
                        .eq("del_flag", 0)
        );

        if (groupDO == null) {
            throw new ClientException(GROUP_NOT_EXIST_ERROR);
        }

        // 更新分组名称
        groupDO.setName(requestDTO.getName());
        int update = baseMapper.updateById(groupDO);

        if (update < 1) {
            throw new ClientException(GROUP_UPDATE_ERROR);
        }
    }

    /**
     * 删除分组
     * @param gid 分组标识
     */
    @Override
    public void deleteGroup(String gid) {
        String username = getCurrentUsername();

        // 验证分组是否存在且属于当前用户
        ShortLinkGroupDO groupDO = baseMapper.selectOne(
                new QueryWrapper<ShortLinkGroupDO>()
                        .eq("gid", gid)
                        .eq("username", username)
                        .eq("del_flag", 0)
        );

        if (groupDO == null) {
            throw new ClientException(GROUP_NOT_EXIST_ERROR);
        }

        // 逻辑删除
        groupDO.setDelFlag(1);
        int update = baseMapper.updateById(groupDO);

        if (update < 1) {
            throw new ClientException(GROUP_DELETE_ERROR);
        }
    }

    /**
     * 分组排序
     * @param requestDTOList 排序请求参数列表
     */
    @Override
    public void sortGroup(List<SortGroupRequestDTO> requestDTOList) {
        String username = getCurrentUsername();

        for (SortGroupRequestDTO dto : requestDTOList) {
            // 验证分组是否存在且属于当前用户
            ShortLinkGroupDO groupDO = baseMapper.selectOne(
                    new QueryWrapper<ShortLinkGroupDO>()
                            .eq("gid", dto.getGid())
                            .eq("username", username)
                            .eq("del_flag", 0)
            );

            if (groupDO == null) {
                throw new ClientException(GROUP_NOT_EXIST_ERROR);
            }

            // 更新排序号
            groupDO.setSortOrder(dto.getSortOrder());
            int update = baseMapper.updateById(groupDO);

            if (update < 1) {
                throw new ClientException(GROUP_SORT_ERROR);
            }
        }
    }

    /**
     * 获取当前登录用户名
     * @return 用户名
     */
    private String getCurrentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString();
    }

    /**
     * 查询当前用户的分组数量
     * @param username 用户名
     * @return 分组数量
     */
    private int getGroupCount(String username) {
        return baseMapper.selectCount(
                new QueryWrapper<ShortLinkGroupDO>()
                        .eq("username", username)
                        .eq("del_flag", 0)
        ).intValue();
    }

    /**
     * 检查分组数量是否超出限制
     * @param username 用户名
     */
    private void checkGroupCount(String username) {
        Long count = baseMapper.selectCount(
                new QueryWrapper<ShortLinkGroupDO>()
                        .eq("username", username)
                        .eq("del_flag", 0)
        );

        if (count >= MAX_GROUP_COUNT) {
            throw new ClientException(GROUP_SAVE_COUNT_ERROR);
        }
    }
}
