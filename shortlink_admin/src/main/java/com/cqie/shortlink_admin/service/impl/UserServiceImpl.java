package com.cqie.shortlink_admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cqie.shortlink_admin.dto.request.UserLoginRequestDTO;
import com.cqie.shortlink_admin.dto.request.UserRegisterDTO;
import com.cqie.shortlink_admin.dto.response.UserResponseDTO;
import com.cqie.shortlink_admin.entity.ShortLinkGroupDO;
import com.cqie.shortlink_admin.entity.UserDO;
import com.cqie.shortlink_admin.mapper.TGroupMapper;
import com.cqie.shortlink_admin.mapper.TUserMapper;
import com.cqie.shortlink_admin.service.UserService;
import com.cqie.shortlink_admin.util.Base62Util;
import com.cqie.shortlink_admin.util.BeanUtil;
import com.cqie.shortlink_admin.util.JwtUtil;
import com.cqie.shortlink_admin.util.RedisUtil;
import com.cqie.shortlink_common.common.convention.exception.ClientException;
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

import static com.cqie.shortlink_common.common.constant.GroupConstant.DEFAULT_GROUP_NAME;
import static com.cqie.shortlink_common.common.constant.RedisCacheConstant.LOCK_USER_REGISTER;
import static com.cqie.shortlink_common.common.convention.errorcode.BaseErrorCode.*;

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
    private final TGroupMapper tGroupMapper;

    /**
     * 根据用户名获取用户信息
     * @param username 用户名
     * @return
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
     * 用户登录
     * @param requestParam 登录参数
     * @return 登录令牌
     */
    @Override
    public String login(UserLoginRequestDTO requestParam) {
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                requestParam.getUsername(), requestParam.getPassword());
        try {
            Authentication authenticate = authenticationManager.authenticate(authenticationToken);
            SecurityContextHolder.getContext().setAuthentication(authenticate);

            String token = JwtUtil.generateToken(requestParam.getUsername());
            redisUtil.setJti(JwtUtil.extractJti(token), requestParam.getUsername(), 7200L);
            return "Bearer " + token;
        } catch (BadCredentialsException e) {
            throw new ClientException(USER_LOGIN_ERROR);
        }
    }

    /**
     * 检查用户名是否存在
     * @param username 用户名
     * @return true存在，false不存在
     */
    @Override
    public boolean checkUsernameExists(String username) {
        return userRegisterCachePenetrationBloomFilter.contains(username);
    }

    /**
     * 用户注册
     * @param requestParam 注册参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(UserRegisterDTO requestParam) {
        if (checkUsernameExists(requestParam.getUsername())) {
            throw new ClientException(USER_EXIST_ERROR);
        }

        RLock lock = redissonClient.getLock(LOCK_USER_REGISTER + requestParam.getUsername());
        if (!lock.tryLock()) {
            throw new ClientException(USER_EXIST_ERROR);
        }

        try {
            requestParam.setPassword(passwordEncoder.encode(requestParam.getPassword()));
            int insert = baseMapper.insert(BeanUtil.convert(requestParam, UserDO.class));
            if (insert < 1) {
                throw new ClientException(USER_SAVE_ERROR);
            }

            createDefaultGroupIfAbsent(requestParam.getUsername());
            userRegisterCachePenetrationBloomFilter.add(requestParam.getUsername());
        } catch (ClientException e) {
            throw e;
        } catch (Exception e) {
            log.error("注册失败，username: {}", requestParam.getUsername(), e);
            throw new ClientException(CLIENT_ERROR);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 用户登出
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

    /**
     * 创建默认分组
     * @param username 用户名
     */
    private void createDefaultGroupIfAbsent(String username) {
        ShortLinkGroupDO existedGroup = tGroupMapper.selectOne(
                new QueryWrapper<ShortLinkGroupDO>()
                        .eq("username", username)
                        .eq("name", DEFAULT_GROUP_NAME)
                        .eq("del_flag", 0)
                        .last("limit 1")
        );
        if (existedGroup != null) {
            return;
        }

        ShortLinkGroupDO groupDO = ShortLinkGroupDO.builder()
                .name(DEFAULT_GROUP_NAME)
                .username(username)
                .sortOrder(0)
                .build();
        int insert = tGroupMapper.insert(groupDO);
        if (insert < 1 || groupDO.getId() == null) {
            throw new ClientException(GROUP_SAVE_ERROR);
        }

        groupDO.setGid(Base62Util.encode(groupDO.getId()));
        if (tGroupMapper.updateById(groupDO) < 1) {
            throw new ClientException(GROUP_SAVE_ERROR);
        }
    }
}
