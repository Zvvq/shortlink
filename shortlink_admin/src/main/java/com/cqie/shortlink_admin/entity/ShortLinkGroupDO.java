package com.cqie.shortlink_admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cqie.shortlink_admin.common.convention.database.BaseDO;
import lombok.*;

@EqualsAndHashCode(callSuper = true)
@TableName("t_group")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ShortLinkGroupDO extends BaseDO {

    /**
     * ID，自增主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 分组标识，62进制编码
     */
    private String gid;

    /**
     * 分组名称
     */
    private String name;

    /**
     * 创建分组的用户名
     */
    private String username;

    /**
     * 分组排序序号
     */
    private Integer sortOrder;
}
