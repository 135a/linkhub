package com.nym.shortlink.core.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nym.shortlink.core.dao.entity.LinkStatsTodayDO;
import com.nym.shortlink.core.dao.mapper.LinkStatsTodayMapper;
import com.nym.shortlink.core.service.LinkStatsTodayService;
import org.springframework.stereotype.Service;

/**
 * 短链接今日统计接口实现层
 * 该类实现了短链接今日统计的服务接口，继承自ServiceImpl，提供了基础的CRUD操作
 */
@Service
public class LinkStatsTodayServiceImpl extends ServiceImpl<LinkStatsTodayMapper, LinkStatsTodayDO> implements LinkStatsTodayService {
    // 使用@Service注解标记该类为Spring服务组件
    // 继承ServiceImpl类，获得了对LinkStatsTodayDO实体的基本数据库操作能力
    // 实现LinkStatsTodayService接口，定义了短链接今日统计的业务逻辑
}
