package com.cqie.shortlink_admin.controller;

import cn.hutool.core.bean.BeanUtil;
import com.cqie.shortlink_common.common.convention.result.Result;
import com.cqie.shortlink_admin.dto.request.UserLoginRequestDTO;
import com.cqie.shortlink_admin.dto.request.UserRegisterDTO;
import com.cqie.shortlink_admin.dto.response.UserActualResponseDTO;
import com.cqie.shortlink_admin.dto.response.UserResponseDTO;
import com.cqie.shortlink_admin.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * 用户管理控制层
 */
@RestController
@RequestMapping("/api/shortlink/v1/user")
@AllArgsConstructor
public class UserController {

    private final UserService userService;


    /**
     * 获取根据用户名用户信息
     * @return 用户信息
     */
    @GetMapping("/info")
    public Result<UserResponseDTO> getUserInfo() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return  Result.success(userService.getUserByUsername(username));
    }

    /**
     * 获取实际用户信息
     * @return 用户信息
     */
    @GetMapping("/actual")
    public Result<UserActualResponseDTO> getActualUserInfo() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return Result.success(BeanUtil.toBean(userService.getUserByUsername(username), UserActualResponseDTO.class));
    }

    /**
     * 检查用户名是否存在
     * @param username 用户名
     * @return true存储，false不存在
     */

    @GetMapping("/exists/{username}")
    public Result<Boolean> checkUsernameExists(@PathVariable String username) {
        return Result.success(userService.checkUsernameExists(username));
    }

    /**
     * 用户注册
     * @param requestParam 注册信息
     * @return true注册成功，false注册失败
     */
    @PostMapping("/register")
    public Result<Boolean> register(@RequestBody UserRegisterDTO requestParam) {
        userService.register(requestParam);
        return Result.success();
    }

    /**
     * 用户登录
     * @param requestParam 登录信息
     * @return 登录成功，返回token
     */
    @PostMapping("/login")
    public Result<String> login(@RequestBody UserLoginRequestDTO requestParam) {
        return Result.success(userService.login(requestParam));
    }

    /**
     * 用户登出
     * @return true登出成功，false登出失败
     */
    @PostMapping("/logout")
    public Result<Boolean> logout() {
        userService.logout();
        return Result.success();
    }

}
