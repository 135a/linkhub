package com.nym.shortlink.core.common.biz.user;

import com.nym.shortlink.core.common.constant.UserConstant;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URLDecoder;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * 用户信息传输过滤器，用于在请求处理过程中提取和设置用户信息
 * 该类实现了Filter接口，是一个典型的Servlet过滤器
 */

/**
 * 用户传输过滤器，用于处理和传递用户信息
 * 该过滤器从请求头中提取用户相关信息，并将其存储到用户上下文中
 */
public class UserTransmitFilter implements Filter {

    /**
     * 执行过滤操作
     * @param servletRequest 请求对象
     * @param servletResponse 响应对象
     * @param filterChain 过滤器链
     * @throws IOException 可能抛出的IO异常
     * @throws ServletException 可能抛出的Servlet异常
     */
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        // 将请求对象转换为HttpServletRequest，以便获取请求头信息
        HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
        // 从请求头中获取用户名
        String userName = httpServletRequest.getHeader(UserConstant.USER_NAME_KEY);
        // 检查用户名是否存在
        if (StringUtils.hasText(userName)) {
            // 从请求头中获取用户ID和真实姓名
            String userId = httpServletRequest.getHeader(UserConstant.USER_ID_KEY);
            String realName = httpServletRequest.getHeader(UserConstant.REAL_NAME_KEY);
            // 如果用户名存在，则进行URL解码
            if (StringUtils.hasText(userName)) {
                userName = URLDecoder.decode(userName, UTF_8);
            }
            if (StringUtils.hasText(realName)) {
                realName = URLDecoder.decode(realName, UTF_8);
            }
            String token = httpServletRequest.getHeader(UserConstant.USER_TOKEN_KEY);
            UserInfoDTO userInfoDTO = UserInfoDTO.builder()
                    .userId(userId)
                    .username(userName)
                    .realName(realName)
                    .token(token)
                    .build();
            UserContext.setUser(userInfoDTO);
        }
        try {
            filterChain.doFilter(servletRequest, servletResponse);
        } finally {
            UserContext.removeUser();
        }
    }
}
