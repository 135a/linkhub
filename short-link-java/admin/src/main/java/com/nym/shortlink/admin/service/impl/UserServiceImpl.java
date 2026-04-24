/*
 * Copyright © 2026 NageOffer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.nym.shortlink.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.UUID;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nym.shortlink.admin.common.biz.user.UserContext;
import com.nym.shortlink.admin.common.convention.exception.ClientException;
import com.nym.shortlink.admin.common.convention.exception.ServiceException;
import com.nym.shortlink.admin.common.enums.UserErrorCodeEnum;
import com.nym.shortlink.admin.dao.entity.UserDO;
import com.nym.shortlink.admin.dao.mapper.UserMapper;
import com.nym.shortlink.admin.dto.req.UserLoginReqDTO;
import com.nym.shortlink.admin.dto.req.UserRegisterReqDTO;
import com.nym.shortlink.admin.dto.req.UserUpdateReqDTO;
import com.nym.shortlink.admin.dto.resp.UserLoginRespDTO;
import com.nym.shortlink.admin.dto.resp.UserRespDTO;
import com.nym.shortlink.admin.service.GroupService;
import com.nym.shortlink.admin.service.UserService;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static com.nym.shortlink.admin.common.constant.RedisCacheConstant.LOCK_USER_REGISTER_KEY;
import static com.nym.shortlink.admin.common.constant.RedisCacheConstant.USER_LOGIN_ABS_KEY;
import static com.nym.shortlink.admin.common.constant.RedisCacheConstant.USER_LOGIN_KEY;
import static com.nym.shortlink.admin.common.enums.UserErrorCodeEnum.USER_EXIST;
import static com.nym.shortlink.admin.common.enums.UserErrorCodeEnum.USER_NAME_EXIST;
import static com.nym.shortlink.admin.common.enums.UserErrorCodeEnum.USER_SAVE_ERROR;

/**
 * 用户接口实现层
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, UserDO> implements UserService {

    /**
     * Sliding window for an active session. A successful token check extends
     * the login hash by this duration; idle past it and the session dies.
     */
    private static final long SESSION_SLIDING_TTL_SECONDS = TimeUnit.MINUTES.toSeconds(30);

    /**
     * Hard upper bound for any single login. Once exceeded, the sliding TTL
     * cannot resurrect the session - the user must log in again.
     */
    private static final long SESSION_ABSOLUTE_TTL_SECONDS = TimeUnit.HOURS.toSeconds(24);

    /**
     * Lua-backed atomic "check token + slide TTL" implemented once and shared
     * with the Go gateway (same source under gateway /internal/middleware/scripts).
     */
    private static final RedisScript<Long> AUTH_CHECK_SCRIPT = buildAuthCheckScript();

    private final RBloomFilter<String> userRegisterCachePenetrationBloomFilter;
    private final RedissonClient redissonClient;
    private final StringRedisTemplate stringRedisTemplate;
    private final GroupService groupService;

    private static RedisScript<Long> buildAuthCheckScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("scripts/short-link-auth-check.lua"));
        script.setResultType(Long.class);
        return script;
    }

    @Override
    public UserRespDTO getUserByUsername(String username) {
        LambdaQueryWrapper<UserDO> queryWrapper = Wrappers.lambdaQuery(UserDO.class)
                .eq(UserDO::getUsername, username);
        UserDO userDO = baseMapper.selectOne(queryWrapper);
        if (userDO == null) {
            throw new ServiceException(UserErrorCodeEnum.USER_NULL);
        }
        UserRespDTO result = new UserRespDTO();
        BeanUtils.copyProperties(userDO, result);
        return result;
    }

    @Override
    public Boolean hasUsername(String username) {
        return !userRegisterCachePenetrationBloomFilter.contains(username);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void register(UserRegisterReqDTO requestParam) {
        if (!hasUsername(requestParam.getUsername())) {
            throw new ClientException(USER_NAME_EXIST);
        }
        RLock lock = redissonClient.getLock(LOCK_USER_REGISTER_KEY + requestParam.getUsername());
        if (!lock.tryLock()) {
            throw new ClientException(USER_NAME_EXIST);
        }
        try {
            int inserted = baseMapper.insert(BeanUtil.toBean(requestParam, UserDO.class));
            if (inserted < 1) {
                throw new ClientException(USER_SAVE_ERROR);
            }
            groupService.saveGroup(requestParam.getUsername(), "默认分组");
            userRegisterCachePenetrationBloomFilter.add(requestParam.getUsername());
        } catch (DuplicateKeyException ex) {
            throw new ClientException(USER_EXIST);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    public void update(UserUpdateReqDTO requestParam) {
        if (!Objects.equals(requestParam.getUsername(), UserContext.getUsername())) {
            throw new ClientException("当前登录用户修改请求异常");
        }
        LambdaUpdateWrapper<UserDO> updateWrapper = Wrappers.lambdaUpdate(UserDO.class)
                .eq(UserDO::getUsername, requestParam.getUsername());
        baseMapper.update(BeanUtil.toBean(requestParam, UserDO.class), updateWrapper);
    }

    @Override
    public UserLoginRespDTO login(UserLoginReqDTO requestParam) {
        LambdaQueryWrapper<UserDO> queryWrapper = Wrappers.lambdaQuery(UserDO.class)
                .eq(UserDO::getUsername, requestParam.getUsername())
                .eq(UserDO::getPassword, requestParam.getPassword())
                .eq(UserDO::getDelFlag, 0);
        UserDO userDO = baseMapper.selectOne(queryWrapper);
        if (userDO == null) {
            throw new ClientException("用户不存在");
        }
        String slidingKey = USER_LOGIN_KEY + requestParam.getUsername();
        String absoluteKey = USER_LOGIN_ABS_KEY + requestParam.getUsername();
        // Single-device login: a fresh login invalidates any prior session of the
        // same user (both sliding and absolute keys) before writing the new token.
        stringRedisTemplate.delete(Arrays.asList(slidingKey, absoluteKey));
        String uuid = UUID.randomUUID().toString();
        stringRedisTemplate.opsForHash().put(slidingKey, uuid, JSON.toJSONString(userDO));
        stringRedisTemplate.expire(slidingKey, SESSION_SLIDING_TTL_SECONDS, TimeUnit.SECONDS);
        // Absolute sentinel - never touched by sliding renewals so it enforces
        // the 24h upper bound regardless of ongoing activity.
        stringRedisTemplate.opsForValue().set(absoluteKey, uuid, SESSION_ABSOLUTE_TTL_SECONDS, TimeUnit.SECONDS);
        return new UserLoginRespDTO(uuid);
    }

    @Override
    public Boolean checkLogin(String username, String token) {
        if (username == null || token == null) {
            return Boolean.FALSE;
        }
        Long result = stringRedisTemplate.execute(
                AUTH_CHECK_SCRIPT,
                Arrays.asList(USER_LOGIN_KEY + username, USER_LOGIN_ABS_KEY + username),
                token,
                String.valueOf(SESSION_SLIDING_TTL_SECONDS)
        );
        return result != null && result == 1L;
    }

    @Override
    public void logout(String username, String token) {
        // Logout is idempotent: deleting a missing key is a no-op, and double
        // logouts (or logouts after natural expiry) must not raise a 5xx.
        stringRedisTemplate.delete(Arrays.asList(
                USER_LOGIN_KEY + username,
                USER_LOGIN_ABS_KEY + username
        ));
    }
}
