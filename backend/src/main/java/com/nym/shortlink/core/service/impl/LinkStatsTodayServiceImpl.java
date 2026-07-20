package com.nym.shortlink.core.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nym.shortlink.core.dao.entity.LinkStatsTodayDO;
import com.nym.shortlink.core.dao.mapper.LinkStatsTodayMapper;
import com.nym.shortlink.core.service.LinkStatsTodayService;
import org.springframework.stereotype.Service;

/**
 * 短链接今日统计接口实现层
 */
@Service
public class LinkStatsTodayServiceImpl extends ServiceImpl<LinkStatsTodayMapper, LinkStatsTodayDO> implements LinkStatsTodayService {
}
