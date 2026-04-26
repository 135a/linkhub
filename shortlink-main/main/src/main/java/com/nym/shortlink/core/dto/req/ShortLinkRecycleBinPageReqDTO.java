package com.nym.shortlink.core.dto.req;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nym.shortlink.core.dao.entity.ShortLinkDO;
import lombok.Data;

import java.util.List;

/**
 * 回收站短链接分页请求参数
 */
@Data
public class ShortLinkRecycleBinPageReqDTO extends Page<ShortLinkDO> {

    /**
     * 分组标识
     */
    private List<String> gidList;
}
