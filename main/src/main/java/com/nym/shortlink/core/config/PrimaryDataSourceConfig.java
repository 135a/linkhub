package com.nym.shortlink.core.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.sql.DataSource;

/**
 * 主数据源 SqlSessionFactory 配置
 * <p>
 * 不手动创建 DataSource —— 直接复用 Spring Boot 从 spring.datasource.* 自动装配的
 * ShardingSphere DataSource。本类仅负责创建一个 @Primary 的 SqlSessionFactory，
 * 防止 ClickHouse 的 SqlSessionFactory 抢占 MybatisPlusAutoConfiguration 的默认行为。
 */
@Configuration
@MapperScan(
        basePackages = "com.nym.shortlink.core.dao.mapper",
        sqlSessionFactoryRef = "primarySqlSessionFactory"
)
public class PrimaryDataSourceConfig {

    @Primary
    @Bean(name = "primarySqlSessionFactory")
    public SqlSessionFactory primarySqlSessionFactory(
            @Qualifier("dataSource") DataSource dataSource,
            MyMetaObjectHandler myMetaObjectHandler) throws Exception {

        MybatisSqlSessionFactoryBean factoryBean = new MybatisSqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        factoryBean.setMapperLocations(
                new PathMatchingResourcePatternResolver()
                        .getResources("classpath:mapper/*.xml")
        );

        // 配置 MyBatis-Plus 全局属性 (自动填充 createTime 等)
        GlobalConfig globalConfig = new GlobalConfig();
        globalConfig.setMetaObjectHandler(myMetaObjectHandler);
        factoryBean.setGlobalConfig(globalConfig);

        // 配置分页插件
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        factoryBean.setPlugins(interceptor);

        // 性能优化：关闭 SQL 打印（与原本的 yaml 保持一致）
        MybatisConfiguration configuration = new MybatisConfiguration();
        configuration.setLogImpl(org.apache.ibatis.logging.nologging.NoLoggingImpl.class);
        factoryBean.setConfiguration(configuration);

        return factoryBean.getObject();
    }
}
