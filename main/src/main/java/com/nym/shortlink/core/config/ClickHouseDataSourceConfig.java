package com.nym.shortlink.core.config;

import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/**
 * ClickHouse 数据源配置
 * <p>
 * 关键设计：不将 ClickHouse DataSource 暴露为 Spring Bean，
 * 而是在 SqlSessionFactory 内部内联创建。
 * 这样就不会干扰 Spring Boot 的 DataSourceAutoConfiguration，
 * 让主数据源（ShardingSphere）的自动装配正常运行。
 */
@Configuration
@MapperScan(
        basePackages = "com.nym.shortlink.core.dao.clickhouse",
        sqlSessionFactoryRef = "clickHouseSqlSessionFactory"
)
public class ClickHouseDataSourceConfig {

    @Bean(name = "clickHouseSqlSessionFactory")
    public SqlSessionFactory clickHouseSqlSessionFactory(Environment env) throws Exception {
        // 内联创建 DataSource，不注册为 Spring Bean，避免干扰自动装配
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setDriverClassName("com.clickhouse.jdbc.ClickHouseDriver");
        dataSource.setJdbcUrl(env.getProperty("clickhouse.datasource.jdbc-url"));
        dataSource.setUsername(env.getProperty("clickhouse.datasource.username"));
        dataSource.setPassword(env.getProperty("clickhouse.datasource.password"));
        dataSource.setMaximumPoolSize(
                env.getProperty("clickhouse.datasource.maximum-pool-size", Integer.class, 100));
        dataSource.setMinimumIdle(
                env.getProperty("clickhouse.datasource.minimum-idle", Integer.class, 10));
        dataSource.setConnectionTimeout(
                env.getProperty("clickhouse.datasource.connection-timeout", Long.class, 30000L));
        dataSource.setIdleTimeout(
                env.getProperty("clickhouse.datasource.idle-timeout", Long.class, 600000L));
        dataSource.setMaxLifetime(
                env.getProperty("clickhouse.datasource.max-lifetime", Long.class, 1800000L));
        dataSource.setPoolName(
                env.getProperty("clickhouse.datasource.pool-name", "ClickHouseHikariPool"));

        MybatisSqlSessionFactoryBean factoryBean = new MybatisSqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        factoryBean.setMapperLocations(
                new PathMatchingResourcePatternResolver()
                        .getResources("classpath:mapper/clickhouse/*.xml")
        );
        return factoryBean.getObject();
    }
}
