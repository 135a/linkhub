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
package com.nym.shortlink.core.config;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 布隆过滤器配置
 */
@Slf4j
@Configuration
public class RBloomFilterConfiguration {

    private static final String BLOOM_FILTER_NAME = "shortUriCreateCachePenetrationBloomFilter";
    private static final long EXPECTED_INSERTIONS = 100000000L;
    private static final double FALSE_PROBABILITY = 0.001;

    /**
     * 防止短链接创建查询数据库的布隆过滤器
     * 如果检测到配置冲突，自动删除旧数据并重新初始化
     */
    @Bean
    public RBloomFilter<String> shortUriCreateCachePenetrationBloomFilter(RedissonClient redissonClient) {
        RBloomFilter<String> cachePenetrationBloomFilter = redissonClient.getBloomFilter(BLOOM_FILTER_NAME);
        
        try {
            // 尝试初始化布隆过滤器
            if (!cachePenetrationBloomFilter.isExists()) {
                boolean initResult = cachePenetrationBloomFilter.tryInit(EXPECTED_INSERTIONS, FALSE_PROBABILITY);
                if (initResult) {
                    log.info("布隆过滤器 [{}] 初始化成功，预期插入量: {}, 误判率: {}", 
                            BLOOM_FILTER_NAME, EXPECTED_INSERTIONS, FALSE_PROBABILITY);
                } else {
                    log.warn("布隆过滤器 [{}] 已存在，跳过初始化", BLOOM_FILTER_NAME);
                }
            } else {
                log.info("布隆过滤器 [{}] 已存在，使用现有配置", BLOOM_FILTER_NAME);
            }
        } catch (Exception e) {
            // 捕获配置冲突异常，删除旧数据并重新初始化
            if (e.getMessage() != null && e.getMessage().contains("Bloom filter config has been changed")) {
                log.error("布隆过滤器 [{}] 配置冲突，正在删除旧数据并重新初始化...", BLOOM_FILTER_NAME, e);
                deleteAndRecreateBloomFilter(cachePenetrationBloomFilter, redissonClient);
            } else {
                log.error("布隆过滤器 [{}] 初始化失败", BLOOM_FILTER_NAME, e);
                throw e;
            }
        }
        
        return cachePenetrationBloomFilter;
    }

    /**
     * 删除旧的布隆过滤器并重新创建
     */
    private void deleteAndRecreateBloomFilter(RBloomFilter<String> bloomFilter, RedissonClient redissonClient) {
        try {
            // 删除旧的布隆过滤器数据
            boolean deleted = bloomFilter.delete();
            log.info("旧布隆过滤器 [{}] 删除结果: {}", BLOOM_FILTER_NAME, deleted);
            
            // 重新获取布隆过滤器实例
            RBloomFilter<String> newBloomFilter = redissonClient.getBloomFilter(BLOOM_FILTER_NAME);
            
            // 重新初始化
            boolean initResult = newBloomFilter.tryInit(EXPECTED_INSERTIONS, FALSE_PROBABILITY);
            if (initResult) {
                log.info("布隆过滤器 [{}] 重新初始化成功，预期插入量: {}, 误判率: {}", 
                        BLOOM_FILTER_NAME, EXPECTED_INSERTIONS, FALSE_PROBABILITY);
            } else {
                log.error("布隆过滤器 [{}] 重新初始化失败", BLOOM_FILTER_NAME);
                throw new RuntimeException("布隆过滤器重新初始化失败");
            }
        } catch (Exception ex) {
            log.error("布隆过滤器 [{}] 重建失败", BLOOM_FILTER_NAME, ex);
            throw new RuntimeException("布隆过滤器重建失败: " + ex.getMessage(), ex);
        }
    }
}
