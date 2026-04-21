package com.cqie.shortlink_project.dto.response;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GenerateDescriptionResponse {

    /**
     * 摘要
     */
    private String summary;
    /**
     * 是否安全，true 表示安全，false 表示不安全
     */
    @JSONField(name = "is_safe")
    private Boolean isSafe;
}
