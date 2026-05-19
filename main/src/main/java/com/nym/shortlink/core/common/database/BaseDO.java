package com.nym.shortlink.core.common.database;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.util.Date;

/**
 * 数据库持久层对象基础属性
 * 该类作为所有实体类的基类，提供通用的数据库字段
 */
@Data  // Lombok注解，自动生成getter、setter等方法
public class BaseDO {

    /**
     * 创建时间
     * 使用@TableField注解标记为数据库字段，并在插入时自动填充当前时间
     */
    @TableField(fill = FieldFill.INSERT)  // MyBatis-Plus注解，表示在插入时自动填充
    private Date createTime;

    /**
     * 修改时间
     * 使用@TableField注解标记为数据库字段，并在插入和更新时自动填充当前时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)  // MyBatis-Plus注解，表示在插入和更新时自动填充
    private Date updateTime;

    /**
     * 删除标识 0：未删除 1：已删除
     * 使用@TableField注解标记为数据库字段，并在插入时自动填充默认值0
     * 用于实现逻辑删除功能
     */
    @TableField(fill = FieldFill.INSERT)  // MyBatis-Plus注解，表示在插入时自动填充
    private Integer delFlag;
}
