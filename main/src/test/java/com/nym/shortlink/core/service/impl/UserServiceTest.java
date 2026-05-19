package com.nym.shortlink.core.service.impl;

import com.nym.shortlink.core.common.convention.exception.ClientException;
import com.nym.shortlink.core.dao.entity.UserDO;
import com.nym.shortlink.core.dao.mapper.UserMapper;
import com.nym.shortlink.core.dto.req.UserLoginReqDTO;
import com.nym.shortlink.core.dto.req.UserRegisterReqDTO;
import com.nym.shortlink.core.dto.resp.UserLoginRespDTO;
import com.nym.shortlink.core.service.GroupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("UserService - BCrypt 密码安全")
class UserServiceTest {

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10);

    private UserMapper userMapper;
    private RBloomFilter<String> bloomFilter;
    private RedissonClient redissonClient;
    private StringRedisTemplate stringRedisTemplate;
    private GroupService groupService;
    private UserServiceImpl userService;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        userMapper = mock(UserMapper.class);
        bloomFilter = mock(RBloomFilter.class);
        redissonClient = mock(RedissonClient.class);
        stringRedisTemplate = mock(StringRedisTemplate.class);
        groupService = mock(GroupService.class);

        userService = new UserServiceImpl(bloomFilter, redissonClient, stringRedisTemplate, groupService, passwordEncoder);
        ReflectionTestUtils.setField(userService, "baseMapper", userMapper);
    }

    @Test
    @DisplayName("注册时密码使用 BCrypt 哈希后存储")
    void registerHashesPasswordWithBcrypt() {
        when(bloomFilter.contains("newuser")).thenReturn(false);
        RLock mockLock = mock(RLock.class);
        when(mockLock.tryLock()).thenReturn(true);
        when(redissonClient.getLock(anyString())).thenReturn(mockLock);
        when(userMapper.insert(any(UserDO.class))).thenReturn(1);

        UserRegisterReqDTO req = new UserRegisterReqDTO();
        req.setUsername("newuser");
        req.setPassword("rawPassword123");

        userService.register(req);

        ArgumentCaptor<UserDO> captor = ArgumentCaptor.forClass(UserDO.class);
        verify(userMapper).insert(captor.capture());
        UserDO inserted = captor.getValue();

        // 存储的密码不是明文
        assertNotEquals("rawPassword123", inserted.getPassword());
        // 存储的密码是有效的 BCrypt 哈希
        assertTrue(inserted.getPassword().startsWith("$2a$10$"));
        // 验证 BCrypt 能匹配原始密码
        assertTrue(passwordEncoder.matches("rawPassword123", inserted.getPassword()));
    }

    @Test
    @DisplayName("登录时用 BCrypt matches 验证密码")
    void loginUsesBcryptMatches() throws Exception {
        String rawPassword = "correctPassword456";
        String hashedPassword = passwordEncoder.encode(rawPassword);

        UserDO mockUser = new UserDO();
        mockUser.setUsername("existingUser");
        mockUser.setPassword(hashedPassword);
        mockUser.setDelFlag(0);

        when(userMapper.selectOne(any())).thenReturn(mockUser);

        HashOperations<String, Object, Object> hashOps = mock(HashOperations.class);
        when(stringRedisTemplate.opsForHash()).thenReturn(hashOps);

        UserLoginReqDTO req = new UserLoginReqDTO();
        req.setUsername("existingUser");
        req.setPassword(rawPassword);

        UserLoginRespDTO result = userService.login(req);

        assertNotNull(result);
        assertNotNull(result.getToken());
    }

    @Test
    @DisplayName("错误密码登录抛出 ClientException")
    void loginWithWrongPasswordThrowsException() {
        String hashedPassword = passwordEncoder.encode("rightPassword");

        UserDO mockUser = new UserDO();
        mockUser.setUsername("testUser");
        mockUser.setPassword(hashedPassword);
        mockUser.setDelFlag(0);

        when(userMapper.selectOne(any())).thenReturn(mockUser);

        UserLoginReqDTO req = new UserLoginReqDTO();
        req.setUsername("testUser");
        req.setPassword("wrongPassword");

        ClientException ex = assertThrows(ClientException.class, () -> userService.login(req));
        assertEquals("用户不存在或密码错误", ex.getMessage());
    }

    @Test
    @DisplayName("用户不存在时登录抛出 ClientException")
    void loginWithNonexistentUserThrowsException() {
        when(userMapper.selectOne(any())).thenReturn(null);

        UserLoginReqDTO req = new UserLoginReqDTO();
        req.setUsername("nonexist");
        req.setPassword("somePassword");

        ClientException ex = assertThrows(ClientException.class, () -> userService.login(req));
        assertEquals("用户不存在或密码错误", ex.getMessage());
    }
}
