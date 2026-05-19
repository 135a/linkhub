package com.nym.shortlink.core.config;

import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.sql.DataSource;

/**
 * ClickHouse 数据源配置
 * <p>
 * 与 MySQL 主库数据源相互独立，通过 @Qualifier 区分。
 * 连接细节和 HikariCP 池参数由 application.yaml 中的 clickhouse.datasource.* 配置管理。
 * <p>
 * 使用 @Lazy 延迟初始化，避免在 DataSourceAutoConfiguration 条件判断阶段
 * 因存在 DataSource bean 而跳过主数据源的自动装配。
 */
@Lazy
@Configuration
@MapperScan(
        basePackages = "com.nym.shortlink.core.dao.clickhouse",
        sqlSessionFactoryRef = "clickHouseSqlSessionFactory"
)
public class ClickHouseDataSourceConfig {

    @Bean(name = "clickHouseDataSource")
    @ConfigurationProperties("clickhouse.datasource")
    public DataSource clickHouseDataSource() {
        return DataSourceBuilder.create()
                .driverClassName("com.clickhouse.jdbc.ClickHouseDriver")
                .build();
    }

    @Bean(name = "clickHouseSqlSessionFactory")
    public SqlSessionFactory clickHouseSqlSessionFactory(
            @Qualifier("clickHouseDataSource") DataSource dataSource
    ) throws Exception {
        MybatisSqlSessionFactoryBean factoryBean = new MybatisSqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        factoryBean.setMapperLocations(
                new PathMatchingResourcePatternResolver()
                        .getResources("classpath:mapper/clickhouse/*.xml")
        );
        return factoryBean.getObject();
    }
}
