package com.cqie.shortlink_admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;

import com.cqie.shortlink_admin.common.convention.database.BaseDO;
import lombok.Data;

/**
 * 短链接表
 * @TableName t_link
 */
@TableName(value ="t_link")
@Data
public class ShortLinkDO extends BaseDO {
    /**
     * ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 域名
     */
    private String domain;

    /**
     * 短链接
     */
    private String shortUri;

    /**
     * 完整短链接
     */
    private String fullShortUrl;

    /**
     * 原始链接
     */
    private String originUrl;

    /**
     * 点击量
     */
    private Integer clickNum;

    /**
     * 分组标识
     */
    private String gid;

    /**
     * 启用标识 0：未启用 1：已启用
     */
    private Integer enableStatus;

    /**
     * 创建类型 0：控制台 1：接口
     */
    private Integer createdType;

    /**
     * 有效期类型 0：永久有效 1：用户自定义
     */
    private Integer validDateType;

    /**
     * 有效期
     */
    private Date validDate;

    /**
     * 描述
     */
    //字段名
    @TableField(value = "`describe`")
    private String describe;


    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getDomain() == null) ? 0 : getDomain().hashCode());
        result = prime * result + ((getShortUri() == null) ? 0 : getShortUri().hashCode());
        result = prime * result + ((getFullShortUrl() == null) ? 0 : getFullShortUrl().hashCode());
        result = prime * result + ((getOriginUrl() == null) ? 0 : getOriginUrl().hashCode());
        result = prime * result + ((getClickNum() == null) ? 0 : getClickNum().hashCode());
        result = prime * result + ((getGid() == null) ? 0 : getGid().hashCode());
        result = prime * result + ((getEnableStatus() == null) ? 0 : getEnableStatus().hashCode());
        result = prime * result + ((getCreatedType() == null) ? 0 : getCreatedType().hashCode());
        result = prime * result + ((getValidDateType() == null) ? 0 : getValidDateType().hashCode());
        result = prime * result + ((getValidDate() == null) ? 0 : getValidDate().hashCode());
        result = prime * result + ((getDescribe() == null) ? 0 : getDescribe().hashCode());
        result = prime * result + ((getCreateTime() == null) ? 0 : getCreateTime().hashCode());
        result = prime * result + ((getUpdateTime() == null) ? 0 : getUpdateTime().hashCode());
        result = prime * result + ((getDelFlag() == null) ? 0 : getDelFlag().hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", id=").append(id);
        sb.append(", domain=").append(domain);
        sb.append(", shortUri=").append(shortUri);
        sb.append(", fullShortUrl=").append(fullShortUrl);
        sb.append(", originUrl=").append(originUrl);
        sb.append(", clickNum=").append(clickNum);
        sb.append(", gid=").append(gid);
        sb.append(", enableStatus=").append(enableStatus);
        sb.append(", createdType=").append(createdType);
        sb.append(", validDateType=").append(validDateType);
        sb.append(", validDate=").append(validDate);
        sb.append(", describe=").append(describe);
        sb.append("]");
        return sb.toString();
    }
}
