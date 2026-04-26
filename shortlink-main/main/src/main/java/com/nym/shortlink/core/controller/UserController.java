package com.nym.shortlink.core.controller;

import cn.hutool.core.bean.BeanUtil;
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
    @GetMapping("/api/short-link/admin/v1/user/{username}")
    public Result<UserRespDTO> getUserByUsername(@PathVariable("username") String username) {
        log.info("进入接口: getUserByUsername");
        Result<UserRespDTO> result = Results.success(userService.getUserByUsername(username));
        log.info("接口处理完毕: getUserByUsername");
        return result;
    }

    /**
     * 根据用户名查询无脱敏用户信息
     */
    @GetMapping("/api/short-link/admin/v1/actual/user/{username}")
    public Result<UserActualRespDTO> getActualUserByUsername(@PathVariable("username") String username) {
        log.info("进入接口: getActualUserByUsername");
        Result<UserActualRespDTO> result = Results.success(BeanUtil.toBean(userService.getUserByUsername(username), UserActualRespDTO.class));
        log.info("接口处理完毕: getActualUserByUsername");
        return result;
    }

    /**
     * 查询用户名是否存在
     */
    @GetMapping("/api/short-link/admin/v1/user/has-username")
    public Result<Boolean> hasUsername(@RequestParam("username") String username) {
        log.info("进入接口: hasUsername");
        Result<Boolean> result = Results.success(userService.hasUsername(username));
        log.info("接口处理完毕: hasUsername");
        return result;
    }

    /**
     * 注册用户
     */
    @PostMapping("/api/short-link/admin/v1/user")
    public Result<Void> register(@RequestBody UserRegisterReqDTO requestParam) {
        log.info("进入接口: register");
        userService.register(requestParam);
        log.info("接口处理完毕: register");
        return Results.success();
    }

    /**
     * 修改用户
     */
    @PutMapping("/api/short-link/admin/v1/user")
    public Result<Void> update(@RequestBody UserUpdateReqDTO requestParam) {
        log.info("进入接口: update");
        userService.update(requestParam);
        log.info("接口处理完毕: update");
        return Results.success();
    }

    /**
     * 用户登录
     */
    @PostMapping("/api/short-link/admin/v1/user/login")
    public Result<UserLoginRespDTO> login(@RequestBody UserLoginReqDTO requestParam) {
        log.info("进入接口: login");
        Result<UserLoginRespDTO> result = Results.success(userService.login(requestParam));
        log.info("接口处理完毕: login");
        return result;
    }

    /**
     * 检查用户是否登录
     */
    @GetMapping("/api/short-link/admin/v1/user/check-login")
    public Result<Boolean> checkLogin(@RequestParam("username") String username, @RequestParam("token") String token) {
        log.info("进入接口: checkLogin");
        Result<Boolean> result = Results.success(userService.checkLogin(username, token));
        log.info("接口处理完毕: checkLogin");
        return result;
    }

    /**
     * 用户退出登录
     */
    @DeleteMapping("/api/short-link/admin/v1/user/logout")
    public Result<Void> logout(@RequestParam("username") String username, @RequestParam("token") String token) {
        log.info("进入接口: logout");
        userService.logout(username, token);
        log.info("接口处理完毕: logout");
        return Results.success();
    }
}
