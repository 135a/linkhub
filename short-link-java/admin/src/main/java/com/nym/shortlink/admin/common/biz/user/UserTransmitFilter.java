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
package com.nym.shortlink.admin.common.biz.user;

import com.nym.shortlink.admin.common.constant.UserConstant;
import com.nym.shortlink.admin.service.UserService;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URLDecoder;

import static java.nio.charset.StandardCharsets.UTF_8;

@RequiredArgsConstructor
public class UserTransmitFilter implements Filter {

    private final UserService userService;

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
        // 尝试从不同的请求头格式中获取用户信息
        String userId = httpServletRequest.getHeader(UserConstant.USER_ID_KEY);
        if (!StringUtils.hasText(userId)) {
            userId = httpServletRequest.getHeader("user-id");
        }
        String userName = httpServletRequest.getHeader(UserConstant.USER_NAME_KEY);
        if (!StringUtils.hasText(userName)) {
            userName = httpServletRequest.getHeader("user-name");
        }
        String realName = httpServletRequest.getHeader(UserConstant.REAL_NAME_KEY);
        if (!StringUtils.hasText(realName)) {
            realName = httpServletRequest.getHeader("real-name");
        }
        String token = httpServletRequest.getHeader("token");
        if (!StringUtils.hasText(token)) {
            token = httpServletRequest.getHeader("Authorization");
        }
        // 解码用户名和真实姓名
        if (StringUtils.hasText(userName)) {
            userName = URLDecoder.decode(userName, UTF_8);
        }
        if (StringUtils.hasText(realName)) {
            realName = URLDecoder.decode(realName, UTF_8);
        }
        // Defense-in-depth model:
        //   - When the request carries a userId header we trust the Go edge
        //     gateway has already run the Lua token check, so we skip the
        //     Redis round-trip and just populate the request-scoped context.
        //     This removes one Redis RTT from every admin API call.
        //   - When userId is missing (e.g. someone is hitting the admin
        //     container directly, bypassing the edge), we still call
        //     checkLogin to enforce token validity server-side.
        if (StringUtils.hasText(userName) && StringUtils.hasText(token)) {
            boolean trustedByEdge = StringUtils.hasText(userId);
            if (trustedByEdge || userService.checkLogin(userName, token)) {
                UserInfoDTO userInfoDTO = UserInfoDTO.builder()
                        .userId(userId)
                        .username(userName)
                        .realName(realName)
                        .token(token)
                        .build();
                UserContext.setUser(userInfoDTO);
            }
        }
        try {
            filterChain.doFilter(servletRequest, servletResponse);
        } finally {
            UserContext.removeUser();
        }
    }
}
