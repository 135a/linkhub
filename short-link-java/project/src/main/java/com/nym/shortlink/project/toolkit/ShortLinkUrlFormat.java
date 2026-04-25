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

package com.nym.shortlink.project.toolkit;

/**
 * 短链接 URL 格式化工具
 */
public class ShortLinkUrlFormat {

    /**
     * 获取短链接原始前缀
     *
     * @param scheme 协议（http/https）
     * @return 前缀字符串（如 "http://"）
     */
    public static String originPrefix(String scheme) {
        return scheme + "://";
    }
}
