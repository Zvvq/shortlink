package com.cqie.shortlink_admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cqie.shortlink_admin.common.convention.exception.ClientException;
import com.cqie.shortlink_admin.dto.response.SaveGroupResponseDTO;
import com.cqie.shortlink_admin.dto.response.SelectGroupResponseDTO;
import com.cqie.shortlink_admin.entity.ShortLinkGroupDO;
import com.cqie.shortlink_admin.mapper.TGroupMapper;
import com.cqie.shortlink_admin.service.GroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import javax.xml.crypto.Data;
import java.util.List;
import java.util.UUID;

import static com.cqie.shortlink_admin.common.constant.GroupConstant.MAX_GROUP_COUNT;
import static com.cqie.shortlink_admin.common.convention.errorcode.BaseErrorCode.GROUP_SAVE_COUNT_ERROR;
import static com.cqie.shortlink_admin.common.convention.errorcode.BaseErrorCode.GROUP_SAVE_ERROR;

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

        String username = SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString();

        int insert = baseMapper.insert(ShortLinkGroupDO.builder()
                .gid(generateGid())
                .name(groupName)
                .username(username)
                .build());

        if (insert < 1) {
            throw new ClientException(GROUP_SAVE_ERROR);
        }

        return SaveGroupResponseDTO.builder()
                .name(groupName)
                .sortOrder(getGroupCount(username))
                .build();
    }

    @Override
    public List<SelectGroupResponseDTO> selectGroup(String groupName) {
        String username = SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString();
        baseMapper.selectList(
                new QueryWrapper<ShortLinkGroupDO>()
                        .eq("username", username)
                        .like(groupName != null, "name", groupName));
        return List.of();
    }


    /**
     * 生成GID
     * @return GID
     */
    private String generateGid() {
        String gid;
        do {
            gid = System.currentTimeMillis() +  UUID.randomUUID().toString().substring(0, 8);
            if (checkGroupIDExists(gid))
                return generateGid();
        } while (true);
    }

    /**
     * 检查GroupID是否存在
     * @param gid groupID
     * @return true 存在，false 不存在
     */
    private boolean checkGroupIDExists(String gid) {
        ShortLinkGroupDO checkGid = baseMapper.selectOne(new QueryWrapper<ShortLinkGroupDO>().eq("gid", gid));
        return checkGid != null;
    }

    /**
     * 查询当前用户的分组数量
     * @param username 用户名
      * @return 分组数量
     */
    private int getGroupCount(String username) {
        Long count = baseMapper.selectCount(
                new QueryWrapper<ShortLinkGroupDO>().eq("username", username)
        );

        if (count > MAX_GROUP_COUNT) {
            throw new ClientException(GROUP_SAVE_COUNT_ERROR);
        }

        return count.intValue();
    }
}
