package com.nym.shortlink.core.config;

import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.sql.DataSource;

/**
 * ClickHouse 数据源配置
 * <p>
 * 与 MySQL 主库数据源相互独立，通过 @Qualifier 区分。
 * 连接细节和 HikariCP 池参数由 application.yaml 中的 clickhouse.datasource.* 配置管理。
 */
@Configuration // 标识这是一个配置类，用于定义 Spring Bean
@MapperScan(  // 扫描指定包下的 MyBatis Mapper 接口，并注入到 Spring 容器中
        basePackages = "com.nym.shortlink.core.dao.mapper.clickhouse", // 指定 Mapper 接口所在的包路径
        sqlSessionFactoryRef = "clickHouseSqlSessionFactory" // 指定使用的 SqlSessionFactory Bean 名称
)
public class ClickHouseDataSourceConfig { // ClickHouse 数据源配置类

    @Bean(name = "clickHouseDataSource") // 定义名为 clickHouseDataSource 的 Bean
    @ConfigurationProperties("clickhouse.datasource") // 将 application.yaml 中 clickhouse.datasource 开头的属性绑定到这个 Bean
    public DataSource clickHouseDataSource() { // 创建 ClickHouse 数据源方法
        return DataSourceBuilder.create() // 创建数据源构建器
                .driverClassName("com.clickhouse.jdbc.ClickHouseDriver") // 设置 ClickHouse JDBC 驱动类名
                .build(); // 构建并返回数据源
    }

    @Bean(name = "clickHouseSqlSessionFactory") // 定义名为 clickHouseSqlSessionFactory 的 Bean
    public SqlSessionFactory clickHouseSqlSessionFactory( // 创建 SqlSessionFactory 方法
            @Qualifier("clickHouseDataSource") DataSource dataSource // 注入名为 clickHouseDataSource 的数据源 Bean
    ) throws Exception { // 方法声明可能抛出异常
        MybatisSqlSessionFactoryBean factoryBean = new MybatisSqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        factoryBean.setMapperLocations(
                new PathMatchingResourcePatternResolver()
                        .getResources("classpath:mapper/clickhouse/*.xml")
        );
        return factoryBean.getObject();
    }
}
