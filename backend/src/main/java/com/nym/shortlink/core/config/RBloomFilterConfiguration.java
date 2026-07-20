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
     * 防止短链接创建查询数据库的布隆过滤器
     * 如果检测到配置冲突，自动删除旧数据并重新初始化
     */
    @Bean
    public RBloomFilter<String> shortUriCreateCachePenetrationBloomFilter(RedissonClient redissonClient) {
        return createBloomFilterWithAutoRecovery(
                redissonClient, 
                "shortUriCreateCachePenetrationBloomFilter", 
                100000000L, 
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
        
        // 计算预期的 bit size 和 hash iterations
        long expectedSize = (long) (-expectedInsertions * Math.log(falseProbability) / (Math.log(2) * Math.log(2)));
        int expectedIterations = (int) Math.max(1, Math.round((double) expectedSize / expectedInsertions * Math.log(2)));

        try {
            if (!bloomFilter.isExists()) {
                bloomFilter.tryInit(expectedInsertions, falseProbability);
                log.info("布隆过滤器 [{}] 初始化成功，预期插入量: {}, 误判率: {}", 
                        filterName, expectedInsertions, falseProbability);
            } else {
                // 如果已存在，校验现有配置是否匹配
                long actualSize = bloomFilter.getSize();
                int actualIterations = bloomFilter.getHashIterations();
                
                if (actualSize != expectedSize || actualIterations != expectedIterations) {
                    log.error("布隆过滤器 [{}] 配置冲突！预期: (size={}, iterations={}), 实际: (size={}, iterations={})。正在重建...", 
                            filterName, expectedSize, expectedIterations, actualSize, actualIterations);
                    deleteAndRecreateBloomFilter(bloomFilter, redissonClient, filterName, expectedInsertions, falseProbability);
                } else {
                    log.info("布隆过滤器 [{}] 已存在且配置匹配，使用现有实例", filterName);
                }
            }
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("Bloom filter config has been changed")) {
                log.error("布隆过滤器 [{}] 检测到配置变更异常，正在强制重建...", filterName);
                deleteAndRecreateBloomFilter(bloomFilter, redissonClient, filterName, expectedInsertions, falseProbability);
            } else {
                log.error("布隆过滤器 [{}] 初始化过程中出现未知错误", filterName, e);
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
