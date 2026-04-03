package com.cqie.shortlink_admin.entity;

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

    private String gid;

    private String name;

    private String username;

    private int sortOrder;
}
