package com.cqie.shortlink_admin.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
@TableName("scheduled") // 对应数据库表名
public class ScheduledDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 定时器ID（主键）
     */
    @TableId
    private String cronId;

    /**
     * 定时器名称
     */
    private String cronName;

    /**
     * cron 表达式
     */
    private String cron;
}
