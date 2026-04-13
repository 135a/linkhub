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
package com.nym.shortlink.admin.config;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 布隆过滤器配置
 */
@Slf4j
@Configuration(value = "rBloomFilterConfigurationByAdmin")
public class RBloomFilterConfiguration {

    /**
     * 防止用户注册查询数据库的布隆过滤器
     * 如果检测到配置冲突，自动删除旧数据并重新初始化
     */
    @Bean
    public RBloomFilter<String> userRegisterCachePenetrationBloomFilter(RedissonClient redissonClient) {
        return createBloomFilterWithAutoRecovery(
                redissonClient, 
                "userRegisterCachePenetrationBloomFilter", 
                100000000L, 
                0.001
        );
    }

    /**
     * 防止分组标识注册查询数据库的布隆过滤器
     * 如果检测到配置冲突，自动删除旧数据并重新初始化
     */
    @Bean
    public RBloomFilter<String> gidRegisterCachePenetrationBloomFilter(RedissonClient redissonClient) {
        return createBloomFilterWithAutoRecovery(
                redissonClient, 
                "gidRegisterCachePenetrationBloomFilter", 
                200000000L, 
                0.001
        );
    }

    /**
     * 创建带自动恢复功能的布隆过滤器
     * 当检测到配置冲突时，自动删除旧数据并重新初始化
     *
     * @param redissonClient Redisson客户端
     * @param filterName 布隆过滤器名称
     * @param expectedInsertions 预期插入量
     * @param falseProbability 误判率
     * @return 布隆过滤器实例
     */
    private RBloomFilter<String> createBloomFilterWithAutoRecovery(
            RedissonClient redissonClient,
            String filterName,
            long expectedInsertions,
            double falseProbability) {
        
        RBloomFilter<String> bloomFilter = redissonClient.getBloomFilter(filterName);
        
        try {
            // 尝试初始化布隆过滤器
            if (!bloomFilter.isExists()) {
                boolean initResult = bloomFilter.tryInit(expectedInsertions, falseProbability);
                if (initResult) {
                    log.info("布隆过滤器 [{}] 初始化成功，预期插入量: {}, 误判率: {}", 
                            filterName, expectedInsertions, falseProbability);
                } else {
                    log.warn("布隆过滤器 [{}] 已存在，跳过初始化", filterName);
                }
            } else {
                log.info("布隆过滤器 [{}] 已存在，使用现有配置", filterName);
            }
        } catch (Exception e) {
            // 捕获配置冲突异常，删除旧数据并重新初始化
            if (e.getMessage() != null && e.getMessage().contains("Bloom filter config has been changed")) {
                log.error("布隆过滤器 [{}] 配置冲突，正在删除旧数据并重新初始化...", filterName, e);
                deleteAndRecreateBloomFilter(bloomFilter, redissonClient, filterName, expectedInsertions, falseProbability);
            } else {
                log.error("布隆过滤器 [{}] 初始化失败", filterName, e);
                throw e;
            }
        }
        
        return bloomFilter;
    }

    /**
     * 删除旧的布隆过滤器并重新创建
     */
    private void deleteAndRecreateBloomFilter(
            RBloomFilter<String> bloomFilter,
            RedissonClient redissonClient,
            String filterName,
            long expectedInsertions,
            double falseProbability) {
        try {
            // 删除旧的布隆过滤器数据
            boolean deleted = bloomFilter.delete();
            log.info("旧布隆过滤器 [{}] 删除结果: {}", filterName, deleted);
            
            // 重新获取布隆过滤器实例
            RBloomFilter<String> newBloomFilter = redissonClient.getBloomFilter(filterName);
            
            // 重新初始化
            boolean initResult = newBloomFilter.tryInit(expectedInsertions, falseProbability);
            if (initResult) {
                log.info("布隆过滤器 [{}] 重新初始化成功，预期插入量: {}, 误判率: {}", 
                        filterName, expectedInsertions, falseProbability);
            } else {
                log.error("布隆过滤器 [{}] 重新初始化失败", filterName);
                throw new RuntimeException("布隆过滤器重新初始化失败");
            }
        } catch (Exception ex) {
            log.error("布隆过滤器 [{}] 重建失败", filterName, ex);
            throw new RuntimeException("布隆过滤器重建失败: " + ex.getMessage(), ex);
        }
    }
}
