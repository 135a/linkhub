package com.nym.shortlink.core.config;

import com.clickhouse.jdbc.ClickHouseDataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.sql.DataSource;
import java.util.Properties;

/**
 * ClickHouse 数据源配置
 * <p>
 * 与 MySQL 主库数据源相互独立，通过 @Qualifier 区分
 */
@Configuration
@MapperScan(
        basePackages = "com.nym.shortlink.core.dao.mapper.clickhouse",
        sqlSessionFactoryRef = "clickHouseSqlSessionFactory"
)
public class ClickHouseDataSourceConfig {

    @Value("${clickhouse.datasource.url:jdbc:clickhouse://localhost:8123/shortlink_stats}")
    private String url;

    @Value("${clickhouse.datasource.username:admin}")
    private String username;

    @Value("${clickhouse.datasource.password:admin}")
    private String password;

    @Bean(name = "clickHouseDataSource")
    public DataSource clickHouseDataSource() throws Exception {
        com.zaxxer.hikari.HikariConfig hikariConfig = new com.zaxxer.hikari.HikariConfig();
        hikariConfig.setDriverClassName("com.clickhouse.jdbc.ClickHouseDriver");
        hikariConfig.setJdbcUrl(url);
        hikariConfig.setUsername(username);
        if (password != null && !password.isEmpty()) {
            hikariConfig.setPassword(password);
        }
        hikariConfig.setMaximumPoolSize(10);
        hikariConfig.setMinimumIdle(2);
        hikariConfig.setConnectionTimeout(30000);
        hikariConfig.setIdleTimeout(600000);
        hikariConfig.setMaxLifetime(1800000);
        hikariConfig.setPoolName("ClickHouseHikariPool");
        return new com.zaxxer.hikari.HikariDataSource(hikariConfig);
    }

    @Bean(name = "clickHouseSqlSessionFactory")
    public SqlSessionFactory clickHouseSqlSessionFactory(
            @org.springframework.beans.factory.annotation.Qualifier("clickHouseDataSource") DataSource dataSource
    ) throws Exception {
        com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean factoryBean = new com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        factoryBean.setMapperLocations(
                new PathMatchingResourcePatternResolver()
                        .getResources("classpath:mapper/clickhouse/*.xml")
        );
        return factoryBean.getObject();
    }
}
