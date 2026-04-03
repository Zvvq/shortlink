package com.cqie.shortlink_admin.dto.response;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SaveGroupResponseDTO {

    private String name;


    private int sortOrder;
}
