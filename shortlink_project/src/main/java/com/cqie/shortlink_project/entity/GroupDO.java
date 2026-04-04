package com.cqie.shortlink_project.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 短链接分组表（只读，对应admin模块的t_group表）
 * @TableName t_group
 */
@TableName(value = "t_group")
@Data
public class GroupDO {

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
    @TableField("sort_order")
    private Integer sortOrder;
}
