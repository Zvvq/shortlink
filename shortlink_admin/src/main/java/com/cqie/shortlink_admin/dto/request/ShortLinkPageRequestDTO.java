package com.cqie.shortlink_admin.dto.request;

import lombok.Data;

/**
 * 短链接分页查询请求DTO
 */
@Data
public class ShortLinkPageRequestDTO {

    /**
     * 分组标识
     */
    private String gid;

    /**
     * 当前页
     */
    private Long current;

    /**
     * 每页显示条数
     */
    private Long size;
}
