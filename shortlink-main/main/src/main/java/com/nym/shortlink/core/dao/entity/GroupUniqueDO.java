package com.nym.shortlink.core.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 短链接分组唯一路由实体
 * 使用@Data注解自动生成getter、setter等方法
 * 使用@TableName注解指定对应的数据库表名为"t_group_unique"
 * 使用@Builder注解支持构建器模式创建对象
 * 使用@NoArgsConstructor和@AllArgsConstructor注解分别生成无参和全参构造方法
 */
@Data
@TableName("t_group_unique")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupUniqueDO {

    /**
     * id
     */
    private Long id;

    /**
     * 分组标识
     */
    private String gid;
}
