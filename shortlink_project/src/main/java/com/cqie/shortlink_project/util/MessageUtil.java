package com.cqie.shortlink_project.util;

import com.cqie.shortlink_project.entity.CacheEvictMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class MessageUtil {

    private final ObjectMapper objectMapper;

    /**
     * 反序列化消息成CacheEvictMessage
     * @param body 消息体
     * @return CacheEvictMessage
     */
    public <T> T deserializeMessage(byte[] body, Class<T> clazz) {
        try {
            return objectMapper.readValue(body, clazz);
        } catch (Exception e) {
            log.error("反序列化 CacheEvictMessage 失败", e);
            return null;
        }
    }
}
