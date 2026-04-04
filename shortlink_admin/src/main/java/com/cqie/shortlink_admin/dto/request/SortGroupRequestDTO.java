package com.cqie.shortlink_admin.dto.request;

import lombok.Data;

/**
 * 分组排序请求DTO
 */
@Data
public class SortGroupRequestDTO {

    /**
     * 分组标识
     */
    private String gid;

    /**
     * 排序序号
     */
    private Integer sortOrder;
}
