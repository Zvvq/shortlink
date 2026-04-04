package com.cqie.shortlink_admin.dto.request;

import lombok.Data;

/**
 * 更新分组请求DTO
 */
@Data
public class UpdateGroupRequestDTO {

    /**
     * 分组标识
     */
    private String gid;

    /**
     * 分组名称
     */
    private String name;
}
