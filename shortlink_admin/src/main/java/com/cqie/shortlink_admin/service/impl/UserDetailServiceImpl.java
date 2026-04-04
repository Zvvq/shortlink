package com.cqie.shortlink_admin.service.impl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cqie.shortlink_admin.common.convention.exception.ClientException;
import com.cqie.shortlink_admin.entity.UserDO;
import com.cqie.shortlink_admin.mapper.TUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import static com.cqie.shortlink_admin.common.convention.errorcode.BaseErrorCode.USERNAME_NULL_ERROR;


@Service
@Slf4j
@RequiredArgsConstructor
public class UserDetailServiceImpl implements UserDetailsService{

    private final TUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;


    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        if (username == null || username.trim().isEmpty()) {
            throw new ClientException(USERNAME_NULL_ERROR);
        }

        // 查询用户
        UserDO userDO = userMapper.selectOne(new LambdaQueryWrapper<UserDO>()
                .eq(UserDO::getUsername, username)
                .eq(UserDO::getDelFlag, 0)
        );

        if (userDO == null) {
            throw new UsernameNotFoundException("用户不存在");
        }

        // 返回用户信息
        return User.builder()
                .username(userDO.getUsername())
                .password(userDO.getPassword())
                .build();
    }
}
