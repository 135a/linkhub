package com.nym.shortlink.core.config;

import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.clickhouse.jdbc.ClickHouseDataSource;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
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
@Configuration // 标识这是一个配置类
@MapperScan(  // 扫描 Mapper 接口，指定包路径和对应的 SqlSessionFactory
        basePackages = "com.nym.shortlink.core.dao.mapper.clickhouse", // Mapper 接口所在包
        sqlSessionFactoryRef = "clickHouseSqlSessionFactory" // 指定使用的 SqlSessionFactory
)
public class ClickHouseDataSourceConfig {

    @Value("${clickhouse.datasource.url:jdbc:clickhouse://localhost:8123/shortlink_stats}") // 从配置文件中获取 URL，默认值为 jdbc:clickhouse://localhost:8123/shortlink_stats
    private String url;

    @Value("${clickhouse.datasource.username:admin}") // 从配置文件中获取用户名，默认值为 admin
    private String username;

    @Value("${clickhouse.datasource.password:admin}") // 从配置文件中获取密码，默认值为 admin
    private String password;

/**
 * 创建并配置一个名为 clickHouseDataSource 的数据源 Bean
 * 使用 HikariCP 作为连接池实现，用于连接 ClickHouse 数据库
 *
 * @return DataSource 配置好的 ClickHouse 数据源实例
 * @throws Exception 如果创建数据源过程中发生异常
 */
    @Bean(name = "clickHouseDataSource") // 定义一个名为 clickHouseDataSource 的 Bean
    public DataSource clickHouseDataSource() throws Exception {
        HikariConfig hikariConfig = new com.zaxxer.hikari.HikariConfig(); // 创建 HikariConfig 配置对象
        hikariConfig.setDriverClassName("com.clickhouse.jdbc.ClickHouseDriver"); // 设置 ClickHouse JDBC 驱动类名
        hikariConfig.setJdbcUrl(url); // 设置 JDBC 连接 URL
        hikariConfig.setUsername(username); // 设置用户名
        if (password != null && !password.isEmpty()) { // 检查密码是否为空
            hikariConfig.setPassword(password); // 设置密码
        }
        hikariConfig.setMaximumPoolSize(100); // 设置连接池最大连接数
        hikariConfig.setMinimumIdle(10); // 设置连接池最小空闲连接数
        hikariConfig.setConnectionTimeout(30000); // 设置连接超时时间（毫秒）
        hikariConfig.setIdleTimeout(600000); // 设置空闲连接超时时间（毫秒）
        hikariConfig.setMaxLifetime(1800000); // 设置连接最大生命周期（毫秒）
        hikariConfig.setPoolName("ClickHouseHikariPool"); // 设置连接池名称
        return new HikariDataSource(hikariConfig); // 创建并返回 HikariDataSource 实例
    }

    @Bean(name = "clickHouseSqlSessionFactory") // 定义一个名为 clickHouseSqlSessionFactory 的 Bean
    public SqlSessionFactory clickHouseSqlSessionFactory(
            @Qualifier("clickHouseDataSource") DataSource dataSource // 注入名为 clickHouseDataSource 的 DataSource
    ) throws Exception {
        MybatisSqlSessionFactoryBean factoryBean = new MybatisSqlSessionFactoryBean(); // 创建 MybatisSqlSessionFactoryBean 实例
        factoryBean.setDataSource(dataSource); // 设置数据源
        factoryBean.setMapperLocations( // 设置 Mapper XML 文件位置
                new PathMatchingResourcePatternResolver()
                        .getResources("classpath:mapper/clickhouse/*.xml") // 获取指定路径下的所有 XML 文件
        );
        return factoryBean.getObject(); // 返回 SqlSessionFactory 实例
    }
}
