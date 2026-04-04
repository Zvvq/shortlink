package com.cqie.shortlink_admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cqie.shortlink_admin.common.convention.exception.ClientException;
import com.cqie.shortlink_admin.dto.request.UserLoginRequestDTO;
import com.cqie.shortlink_admin.dto.request.UserRegisterDTO;
import com.cqie.shortlink_admin.dto.response.UserResponseDTO;
import com.cqie.shortlink_admin.entity.UserDO;
import com.cqie.shortlink_admin.mapper.TUserMapper;
import com.cqie.shortlink_admin.service.UserService;
import com.cqie.shortlink_admin.util.BeanUtil;
import com.cqie.shortlink_admin.util.JwtUtil;
import com.cqie.shortlink_admin.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import static com.cqie.shortlink_admin.common.constant.RedisCacheConstant.LOCK_USER_REGISTER;
import static com.cqie.shortlink_admin.common.convention.errorcode.BaseErrorCode.*;

/**
* @author friendA
* @description 针对表【t_user】的数据库操作Service实现
* @createDate 2026-03-23 20:07:52
*/
@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl extends ServiceImpl<TUserMapper, UserDO>
    implements UserService {

    private final RBloomFilter<String> userRegisterCachePenetrationBloomFilter;
    private final RedissonClient redissonClient;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final RedisUtil redisUtil;



    /**
     * 根据用户名获取用户信息
     *
     * @param username 用户名
     * @return 用户响应 DTO，包含用户详细信息；如果用户不存在则返回 null
     */
    @Override
    public UserResponseDTO getUserByUsername(String username) {
        UserDO selectedOne = baseMapper.selectOne(new QueryWrapper<UserDO>().eq("username", username));

        if (selectedOne == null) {
            throw new ClientException(USER_NOT_EXIST_ERROR);
        }


        return BeanUtil.convert(selectedOne, UserResponseDTO.class);
    }

    /**
     * 用户登录，验证用户名和密码并生成 JWT 令牌
     *
     * @param requestParam 登录请求参数，包含用户名和密码
     * @return JWT 令牌字符串；如果用户名或密码错误或账号已删除则返回 null
     */
    @Override
    public String login(UserLoginRequestDTO requestParam) {

        // 使用authenticationManager进行认证
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                requestParam.getUsername(), requestParam.getPassword());
        try {
            Authentication authenticate = authenticationManager.authenticate(authenticationToken);

            //认证通过
            SecurityContextHolder.getContext().setAuthentication(authenticate);

            String token = JwtUtil.generateToken(requestParam.getUsername());
            redisUtil.setJti(JwtUtil.extractJti(token), requestParam.getUsername(), 7200L);

            return "Bearer " + token;
        } catch (BadCredentialsException e) {
            // 用户名或密码错误
            throw new ClientException(USER_LOGIN_ERROR);
        }

    }


    /**
     * 检查用户名是否已存在（通过布隆过滤器）
     *
     * @param username 待检查的用户名
     * @return true 表示用户名可能存在（需进一步数据库确认），false 表示用户名一定不存在
     */
    @Override
    public boolean checkUsernameExists(String username) {
        return userRegisterCachePenetrationBloomFilter.contains( username);
    }

    /**
     *  注册
     * @param requestParam 注册参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(UserRegisterDTO requestParam) {

        //校验用户是否存在
        if (checkUsernameExists(requestParam.getUsername())) {
            throw new ClientException(USER_EXIST_ERROR);
        }

        RLock lock = redissonClient.getLock(LOCK_USER_REGISTER + requestParam.getUsername());

        if (!lock.tryLock()) {
            throw new ClientException(USER_EXIST_ERROR);
        }

        try {
            requestParam.setPassword(passwordEncoder.encode(requestParam.getPassword()));//加密
            //插入用户
            int insert = baseMapper.insert(BeanUtil.convert(requestParam, UserDO.class));
            if (insert < 1) {
                throw new ClientException(USER_SAVE_ERROR);
            }

            //添加布隆过滤器
            userRegisterCachePenetrationBloomFilter.add(requestParam.getUsername());
        } catch (Exception e) {
            log.error("注册失败，username: {}", requestParam.getUsername(), e);
            throw new ClientException(CLIENT_ERROR);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 登出
     */
    @Override
    public void logout() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            String username = authentication.getName();
            redisUtil.deleteJti(username);
        }
        SecurityContextHolder.clearContext();
    }
}




