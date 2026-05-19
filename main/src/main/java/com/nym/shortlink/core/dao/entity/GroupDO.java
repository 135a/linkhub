package com.nym.shortlink.core.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.nym.shortlink.core.common.database.BaseDO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 短链接分组实体
 * 使用了Lombok的@Data注解自动生成getter、setter等方法
 * 使用@TableName指定数据库表名为"t_group"
 * 使用@Builder、@NoArgsConstructor、@AllArgsConstructor注解提供构建器模式和构造方法
 */
@Data
@TableName("t_group")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupDO extends BaseDO {

    /**
     * id
     */
    private Long id;

    /**
     * 分组标识
     */
    private String gid;

    /**
     * 分组名称
     */
    private String name;

    /**
     * 创建分组用户名
     */
    private String username;

    /**
     * 分组排序
     */
    private Integer sortOrder;
}
