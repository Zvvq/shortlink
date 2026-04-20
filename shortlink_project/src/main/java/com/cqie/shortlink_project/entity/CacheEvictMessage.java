package com.cqie.shortlink_project.entity;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CacheEvictMessage {

    /**
     * 短链
     */
    private String shortUrl;

}
