package com.cqie.shortlink_admin.controller;

import com.cqie.shortlink_admin.common.convention.result.Result;
import com.cqie.shortlink_admin.dto.request.SortGroupRequestDTO;
import com.cqie.shortlink_admin.dto.request.UpdateGroupRequestDTO;
import com.cqie.shortlink_admin.dto.response.SaveGroupResponseDTO;
import com.cqie.shortlink_admin.dto.response.SelectGroupResponseDTO;
import com.cqie.shortlink_admin.service.GroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 分组管理控制层
 */
@RestController
@RequestMapping("/api/shortlink/v1/group")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;

    /**
     * 保存分组
     * @param groupName 分组名称
     * @return 保存结果
     */
    @PostMapping("/save")
    public Result<SaveGroupResponseDTO> save(@RequestParam String groupName) {
        return Result.success(groupService.saveGroup(groupName));
    }

    /**
     * 查询当前用户的分组列表
     * @return 分组列表
     */
    @GetMapping("/list")
    public Result<List<SelectGroupResponseDTO>> list() {
        return Result.success(groupService.selectGroup());
    }

    /**
     * 更新分组名称
     * @param requestDTO 更新请求参数
     * @return 更新结果
     */
    @PutMapping("/update")
    public Result<Void> update(@RequestBody UpdateGroupRequestDTO requestDTO) {
        groupService.updateGroup(requestDTO);
        return Result.success();
    }

    /**
     * 删除分组
     * @param gid 分组标识
     * @return 删除结果
     */
    @DeleteMapping("/delete/{gid}")
    public Result<Void> delete(@PathVariable String gid) {
        groupService.deleteGroup(gid);
        return Result.success();
    }

    /**
     * 分组排序
     * @param requestDTOList 排序请求参数列表
     * @return 排序结果
     */
    @PutMapping("/sort")
    public Result<Void> sort(@RequestBody List<SortGroupRequestDTO> requestDTOList) {
        groupService.sortGroup(requestDTOList);
        return Result.success();
    }

}
