package com.cqie.shortlink_admin.dto.message;

import lombok.*;
import org.apache.rocketmq.common.message.Message;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StatsMessage implements Serializable {
    //短链
    private String shortUrl;
    //用户id
    private String uvId;
    //访问时间
    private LocalDateTime accessTime;
}
