package com.cqie.shortlink_project.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 分组及短链接数量响应DTO
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GroupLinkCountResponse {

    /**
     * 分组标识
     */
    private String gid;

    /**
     * 分组名称
     */
    private String name;

    /**
     * 排序序号
     */
    private Integer sortOrder;

    /**
     * 短链接数量
     */
    private Long linkCount;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;
}
