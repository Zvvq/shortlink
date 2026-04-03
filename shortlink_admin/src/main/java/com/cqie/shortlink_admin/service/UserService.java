package com.cqie.shortlink_admin.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.cqie.shortlink_admin.dto.request.UserLoginRequestDTO;
import com.cqie.shortlink_admin.dto.request.UserRegisterDTO;
import com.cqie.shortlink_admin.dto.response.UserResponseDTO;
import com.cqie.shortlink_admin.entity.UserDO;

/**
* @author friendA
* @description 针对表【t_user】的数据库操作Service
* @createDate 2026-03-23 20:07:52
*/

//@Service
public interface UserService extends IService<UserDO> {

    UserResponseDTO getUserByUsername(String username);

    String login(UserLoginRequestDTO requestParam);

    boolean checkUsernameExists(String username);

    void register(UserRegisterDTO requestParam);

    void logout();

}
