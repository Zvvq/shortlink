package com.cqie.shortlink_admin.common.convention.database;

import lombok.Data;

import java.util.Date;

@Data
public class BaseDO {

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 删除标志 0：未删除 1：已删除
     */
    private Integer delFlag;

}
