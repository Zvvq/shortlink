package com.cqie.shortlink_project.common.convention.database;

import lombok.Data;

import java.util.Date;

@Data
public class BaseDO {

    private Date createTime;
    private Date updateTime;
    private Integer delFlag;

}
