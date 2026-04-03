package com.cqie.shortlink_admin.controller;

import com.cqie.shortlink_admin.common.convention.result.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/shortlink/v1/group")
public class GroupController {

    @GetMapping("save/{groupName}")
    public Result<String> save(@PathVariable String groupName) {
        return Result.success("保存成功");
    }

}
