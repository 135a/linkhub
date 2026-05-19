package com.nym.shortlink.core.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.UUID;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nym.shortlink.core.common.biz.user.UserContext;
import com.nym.shortlink.core.common.convention.exception.ClientException;
import com.nym.shortlink.core.common.convention.exception.ServiceException;
import com.nym.shortlink.core.common.enums.UserErrorCodeEnum;
import com.nym.shortlink.core.dao.entity.UserDO;
import com.nym.shortlink.core.dao.mapper.UserMapper;
import com.nym.shortlink.core.dto.req.UserLoginReqDTO;
import com.nym.shortlink.core.dto.req.UserRegisterReqDTO;
import com.nym.shortlink.core.dto.req.UserUpdateReqDTO;
import com.nym.shortlink.core.dto.resp.UserLoginRespDTO;
import com.nym.shortlink.core.dto.resp.UserRespDTO;
import com.nym.shortlink.core.service.GroupService;
import com.nym.shortlink.core.service.UserService;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static com.nym.shortlink.core.common.constant.RedisCacheConstant.LOCK_USER_REGISTER_KEY;
import static com.nym.shortlink.core.common.constant.RedisCacheConstant.USER_LOGIN_KEY;
import static com.nym.shortlink.core.common.enums.UserErrorCodeEnum.USER_EXIST;
import static com.nym.shortlink.core.common.enums.UserErrorCodeEnum.USER_NAME_EXIST;
import static com.nym.shortlink.core.common.enums.UserErrorCodeEnum.USER_SAVE_ERROR;

/**
 * 用户接口实现层
 * 实现了用户相关的业务逻辑，包括用户注册、登录、信息更新等功能
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, UserDO> implements UserService {

    // 布隆过滤器，用于防止缓存穿透
    private final RBloomFilter<String> userRegisterCachePenetrationBloomFilter;
    // Redisson客户端，用于分布式锁
    private final RedissonClient redissonClient;
    // Redis字符串操作模板
    private final StringRedisTemplate stringRedisTemplate;
    // 分组服务
    private final GroupService groupService;
    // 密码编码器
    private final PasswordEncoder passwordEncoder;

    /**
     * 根据用户名获取用户信息
     * @param username 用户名
     * @return UserRespDTO 用户信息响应对象
     * @throws ServiceException 当用户不存在时抛出
     */
    @Override
    public UserRespDTO getUserByUsername(String username) {
        // 构建查询条件
        LambdaQueryWrapper<UserDO> queryWrapper = Wrappers.lambdaQuery(UserDO.class)
                .eq(UserDO::getUsername, username);
        // 执行查询
        UserDO userDO = baseMapper.selectOne(queryWrapper);
        if (userDO == null) {
            throw new ServiceException(UserErrorCodeEnum.USER_NULL);
        }
        // 复制属性到响应对象
        UserRespDTO result = new UserRespDTO();
        BeanUtils.copyProperties(userDO, result);
        return result;
    }

    /**
     * 检查用户名是否可用
     * @param username 用户名
     * @return Boolean 如果用户名可用返回true，否则返回false
     */
    @Override
    public Boolean hasUsername(String username) {
        return !userRegisterCachePenetrationBloomFilter.contains(username);
    }

    /**
     * 用户注册
     * @param requestParam 注册请求参数
     * @throws ClientException 当用户名已存在或保存失败时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void register(UserRegisterReqDTO requestParam) {
        // 检查用户名是否已存在
        if (!hasUsername(requestParam.getUsername())) {
            throw new ClientException(USER_NAME_EXIST);
        }
        // 获取分布式锁
        RLock lock = redissonClient.getLock(LOCK_USER_REGISTER_KEY + requestParam.getUsername());
        // 尝试获取锁
        if (!lock.tryLock()) {
            throw new ClientException(USER_NAME_EXIST);
        }
        try {
            // 加密密码
            requestParam.setPassword(passwordEncoder.encode(requestParam.getPassword()));
            // 保存用户信息
            int inserted = baseMapper.insert(BeanUtil.toBean(requestParam, UserDO.class));
            if (inserted < 1) {
                throw new ClientException(USER_SAVE_ERROR);
            }
            // 创建默认分组
            groupService.saveGroup(requestParam.getUsername(), "默认分组");
            // 将用户名添加到布隆过滤器
            userRegisterCachePenetrationBloomFilter.add(requestParam.getUsername());
        } catch (DuplicateKeyException ex) {
            throw new ClientException(USER_EXIST);
        } finally {
            // 释放锁
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 更新用户信息
     * @param requestParam 更新请求参数
     * @throws ClientException 当当前登录用户与请求用户不匹配时抛出
     */
    @Override
    public void update(UserUpdateReqDTO requestParam) {
        // 验证当前登录用户
        if (!Objects.equals(requestParam.getUsername(), UserContext.getUsername())) {
            throw new ClientException("当前登录用户修改请求异常");
        }
        // 构建更新条件
        LambdaUpdateWrapper<UserDO> updateWrapper = Wrappers.lambdaUpdate(UserDO.class)
                .eq(UserDO::getUsername, requestParam.getUsername());
        // 执行更新
        baseMapper.update(BeanUtil.toBean(requestParam, UserDO.class), updateWrapper);
    }

    /**
     * 用户登录
     * @param requestParam 登录请求参数
     * @return UserLoginRespDTO 登录响应对象，包含token
     * @throws ClientException 当用户不存在或密码错误时抛出
     */
    @Override
    public UserLoginRespDTO login(UserLoginReqDTO requestParam) {
        // 构建查询条件
        LambdaQueryWrapper<UserDO> queryWrapper = Wrappers.lambdaQuery(UserDO.class)
                .eq(UserDO::getUsername, requestParam.getUsername())
                .eq(UserDO::getDelFlag, 0);
        // 执行查询
        UserDO userDO = baseMapper.selectOne(queryWrapper);
        // 验证用户和密码
        if (userDO == null || !passwordEncoder.matches(requestParam.getPassword(), userDO.getPassword())) {
            throw new ClientException("用户不存在或密码错误");
        }
        /**
         * Hash存储结构
         * Key：login_用户名
         * Value：
         *  Key：token标识
         *  Val：JSON 字符串（用户信息）
         */
        // 生成token
        String uuid = UUID.randomUUID().toString();
        // 将用户信息存入Redis
        stringRedisTemplate.opsForHash().put(USER_LOGIN_KEY + requestParam.getUsername(), uuid, JSON.toJSONString(userDO));
        // 设置过期时间
        stringRedisTemplate.expire(USER_LOGIN_KEY + requestParam.getUsername(), 30L, TimeUnit.MINUTES);
        return new UserLoginRespDTO(uuid);
    }

    /**
     * 检查用户登录状态
     * @param username 用户名
     * @param token token
     * @return Boolean 如果用户已登录返回true，否则返回false
     */
    @Override
    public Boolean checkLogin(String username, String token) {
        return stringRedisTemplate.opsForHash().get(USER_LOGIN_KEY + username, token) != null;
    }

    /**
     * 用户登出
     * @param username 用户名
     * @param token token
     * @throws ClientException 当token不存在或用户未登录时抛出
     */
    @Override
    public void logout(String username, String token) {
        if (checkLogin(username, token)) {
            // 删除登录信息
            stringRedisTemplate.delete(USER_LOGIN_KEY + username);
            return;
        }
        throw new ClientException("用户Token不存在或用户未登录");
    }
}
