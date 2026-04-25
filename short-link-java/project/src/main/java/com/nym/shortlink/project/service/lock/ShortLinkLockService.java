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

package com.nym.shortlink.project.service.lock;

import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import static com.nym.shortlink.project.common.constant.RedisKeyConstant.LOCK_GID_UPDATE_KEY;
import static com.nym.shortlink.project.common.constant.RedisKeyConstant.LOCK_GOTO_SHORT_LINK_KEY;

/**
 * 短链接分布式锁服务
 */
@Service
@RequiredArgsConstructor
public class ShortLinkLockService {

    private final RedissonClient redissonClient;

    /**
     * 获取 GID 更新读锁
     *
     * @param fullShortUrl 完整短链接
     * @return 读锁
     */
    public RLock gidUpdateReadLock(String fullShortUrl) {
        return redissonClient.getReadWriteLock(String.format(LOCK_GID_UPDATE_KEY, fullShortUrl)).readLock();
    }

    /**
     * 获取 GID 更新写锁
     *
     * @param fullShortUrl 完整短链接
     * @return 写锁
     */
    public RLock gidUpdateWriteLock(String fullShortUrl) {
        return redissonClient.getReadWriteLock(String.format(LOCK_GID_UPDATE_KEY, fullShortUrl)).writeLock();
    }

    /**
     * 获取短链接跳转读锁
     *
     * @param fullShortUrl 完整短链接
     * @return 读锁
     */
    public RLock gotoReadLock(String fullShortUrl) {
        return redissonClient.getReadWriteLock(String.format(LOCK_GOTO_SHORT_LINK_KEY, fullShortUrl)).readLock();
    }

    /**
     * 获取短链接跳转写锁
     *
     * @param fullShortUrl 完整短链接
     * @return 写锁
     */
    public RLock gotoWriteLock(String fullShortUrl) {
        return redissonClient.getReadWriteLock(String.format(LOCK_GOTO_SHORT_LINK_KEY, fullShortUrl)).writeLock();
    }
}
