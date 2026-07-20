package com.nym.shortlink.core.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 数据库持久层配置类
 */
@Configuration
public class DataBaseConfiguration {

    /**
     * 分页插件
     */
    @Bean
    @ConditionalOnMissingBean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
    /**
     * 主数据源（ShardingSphere）
     */
    @Bean
    @org.springframework.context.annotation.Primary
    public javax.sql.DataSource primaryDataSource(org.springframework.core.env.Environment env) {
        return org.springframework.boot.jdbc.DataSourceBuilder.create()
                .driverClassName(env.getProperty("spring.datasource.driver-class-name"))
                .url(env.getProperty("spring.datasource.url"))
                .build();
    }

    @Bean
    @org.springframework.context.annotation.Primary
    public org.apache.ibatis.session.SqlSessionFactory sqlSessionFactory(
            javax.sql.DataSource primaryDataSource,
            MybatisPlusInterceptor mybatisPlusInterceptor,
            org.springframework.beans.factory.ObjectProvider<com.baomidou.mybatisplus.core.handlers.MetaObjectHandler> metaObjectHandler) throws Exception {
        com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean factoryBean = new com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean();
        factoryBean.setDataSource(primaryDataSource);
        factoryBean.setMapperLocations(new org.springframework.core.io.support.PathMatchingResourcePatternResolver().getResources("classpath:mapper/*.xml"));
        
        com.baomidou.mybatisplus.core.MybatisConfiguration configuration = new com.baomidou.mybatisplus.core.MybatisConfiguration();
        configuration.setLogImpl(org.apache.ibatis.logging.stdout.StdOutImpl.class);
        factoryBean.setConfiguration(configuration);
        factoryBean.setPlugins(new org.apache.ibatis.plugin.Interceptor[]{mybatisPlusInterceptor});
        
        com.baomidou.mybatisplus.core.config.GlobalConfig globalConfig = new com.baomidou.mybatisplus.core.config.GlobalConfig();
        metaObjectHandler.ifAvailable(globalConfig::setMetaObjectHandler);
        factoryBean.setGlobalConfig(globalConfig);
        
        return factoryBean.getObject();
    }
}
