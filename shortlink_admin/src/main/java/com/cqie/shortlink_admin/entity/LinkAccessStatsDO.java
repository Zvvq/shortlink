package com.cqie.shortlink_admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cqie.shortlink_admin.common.convention.database.BaseDO;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Date;

/**
 * 短链访问统计 DO
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("t_link_access_stats")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LinkAccessStatsDO extends BaseDO {


    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 分组标识
     */
    private String gid;

    /**
     * 完整短链接
     */
    private String fullShortUrl;

    /**
     * 日期
     */
    private LocalDateTime date;

    /**
     * 访问量
     */
    private Integer pv;

    /**
     * 独立访问数
     */
    private Integer uv;

    /**
     * 独立IP数
     */
    private Integer uip;

    /**
     * 小时
     */
    private Integer hour;

    /**
     * 星期
     */
    private Integer weekday;

}
