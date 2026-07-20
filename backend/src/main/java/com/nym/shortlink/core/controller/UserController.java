package com.nym.shortlink.core.controller;

import cn.hutool.core.bean.BeanUtil;
import com.nym.shortlink.core.common.biz.ratelimit.RateLimit;
import com.nym.shortlink.core.common.convention.result.Result;
import com.nym.shortlink.core.common.convention.result.Results;
import com.nym.shortlink.core.dto.req.UserLoginReqDTO;
import com.nym.shortlink.core.dto.req.UserRegisterReqDTO;
import com.nym.shortlink.core.dto.req.UserUpdateReqDTO;
import com.nym.shortlink.core.dto.resp.UserActualRespDTO;
import com.nym.shortlink.core.dto.resp.UserLoginRespDTO;
import com.nym.shortlink.core.dto.resp.UserRespDTO;
import com.nym.shortlink.core.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

/**
 * 用户管理控制层
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    /**
     * 根据用户名查询用户信息
     */
    @RateLimit(resource = "get_user", qps = 20)
    @GetMapping("/api/short-link/admin/v1/user/{username}")
    public Result<UserRespDTO> getUserByUsername(@PathVariable("username") String username) {
        Result<UserRespDTO> result = Results.success(userService.getUserByUsername(username));
        return result;
    }

    /**
     * 根据用户名查询无脱敏用户信息
     */
    @RateLimit(resource = "get_actual_user", qps = 20)
    @GetMapping("/api/short-link/admin/v1/actual/user/{username}")
    public Result<UserActualRespDTO> getActualUserByUsername(@PathVariable("username") String username) {
        Result<UserActualRespDTO> result = Results.success(BeanUtil.toBean(userService.getUserByUsername(username), UserActualRespDTO.class));
        return result;
    }

    /**
     * 查询用户名是否存在
     */
    @RateLimit(resource = "check_username", qps = 20)
    @GetMapping("/api/short-link/admin/v1/user/has-username")
    public Result<Boolean> hasUsername(@RequestParam("username") String username) {
        Result<Boolean> result = Results.success(userService.hasUsername(username));
        return result;
    }

    /**
     * 注册用户
     */
    @RateLimit(resource = "user_register", qps = 1, message = "操作过于频繁，请稍后再试")
    @PostMapping("/api/short-link/admin/v1/user")
    public Result<Void> register(@RequestBody UserRegisterReqDTO requestParam) {
        userService.register(requestParam);
        return Results.success();
    }

    /**
     * 修改用户
     */
    @RateLimit(resource = "update_user", qps = 5)
    @PutMapping("/api/short-link/admin/v1/user")
    public Result<Void> update(@RequestBody UserUpdateReqDTO requestParam) {
        userService.update(requestParam);
        return Results.success();
    }

    /**
     * 用户登录
     */
    @RateLimit(resource = "user_login", qps = 5, message = "登录过于频繁，请稍后再试")
    @PostMapping("/api/short-link/admin/v1/user/login")
    public Result<UserLoginRespDTO> login(@RequestBody UserLoginReqDTO requestParam) {
        Result<UserLoginRespDTO> result = Results.success(userService.login(requestParam));
        return result;
    }

    /**
     * 检查用户是否登录
     */
    @GetMapping("/api/short-link/admin/v1/user/check-login")
    public Result<Boolean> checkLogin(@RequestParam("username") String username, @RequestParam("token") String token) {
        Result<Boolean> result = Results.success(userService.checkLogin(username, token));
        return result;
    }

    /**
     * 用户退出登录
     */
    @RateLimit(resource = "user_logout", qps = 10)
    @DeleteMapping("/api/short-link/admin/v1/user/logout")
    public Result<Void> logout(@RequestParam("username") String username, @RequestParam("token") String token) {
        userService.logout(username, token);
        return Results.success();
    }
}
