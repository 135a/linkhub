package com.nym.shortlink.core.common.biz.user;

import com.alibaba.ttl.TransmittableThreadLocal;

import java.util.Optional;

/**
 * 用户上下文
 * 该类用于管理当前线程中的用户信息，使用TransmittableThreadLocal实现线程间的用户信息传递
 */
public final class UserContext {

    /**
     * 使用TransmittableThreadLocal存储用户信息的线程局部变量
     * TransmittableThreadLocal支持线程池执行时传递父线程的变量值
     * <a href="https://github.com/alibaba/transmittable-thread-local" />
     */
    private static final ThreadLocal<UserInfoDTO> USER_THREAD_LOCAL = new TransmittableThreadLocal<>();

    /**
     * 设置用户至上下文
     * 将用户信息存储到当前线程的ThreadLocal中
     *
     * @param user 用户详情信息
     */
    public static void setUser(UserInfoDTO user) {
        USER_THREAD_LOCAL.set(user);
    }

    /**
     * 获取上下文中用户 ID
     * 从当前线程的ThreadLocal中获取用户信息，并返回用户ID

     *
     * @return 用户 ID，如果用户信息不存在则返回null
     */
    public static String getUserId() {
        UserInfoDTO userInfoDTO = USER_THREAD_LOCAL.get();
        return Optional.ofNullable(userInfoDTO).map(UserInfoDTO::getUserId).orElse(null);
    }

    /**
     * 获取上下文中用户名称
     * 从当前线程的ThreadLocal中获取用户信息，并返回用户名称

     *
     * @return 用户名称，如果用户信息不存在则返回null
     */
    public static String getUsername() {
        UserInfoDTO userInfoDTO = USER_THREAD_LOCAL.get();
        return Optional.ofNullable(userInfoDTO).map(UserInfoDTO::getUsername).orElse(null);
    }

    /**
     * 获取上下文中用户真实姓名
     * 从当前线程的ThreadLocal中获取用户信息，并返回用户真实姓名

     *
     * @return 用户真实姓名，如果用户信息不存在则返回null
     */
    public static String getRealName() {
        UserInfoDTO userInfoDTO = USER_THREAD_LOCAL.get();
        return Optional.ofNullable(userInfoDTO).map(UserInfoDTO::getRealName).orElse(null);
    }

    /**
     * 清理用户上下文
     * 移除当前线程中的用户信息，防止内存泄漏
     */
    public static void removeUser() {
        USER_THREAD_LOCAL.remove();
    }
}
