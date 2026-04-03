package com.cqie.shortlink_admin.dto.response;

import lombok.Data;

@Data
public class SelectGroupResponseDTO {
    private String gid;

    private String name;

    private int sortOrder;
}
