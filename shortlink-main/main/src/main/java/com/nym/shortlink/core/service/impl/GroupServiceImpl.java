package com.nym.shortlink.core.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nym.shortlink.core.common.biz.user.UserContext;
import com.nym.shortlink.core.common.convention.exception.ClientException;
import com.nym.shortlink.core.common.convention.exception.ServiceException;
import com.nym.shortlink.core.common.convention.result.Result;
import com.nym.shortlink.core.dao.entity.GroupDO;
import com.nym.shortlink.core.dao.entity.GroupUniqueDO;
import com.nym.shortlink.core.dao.mapper.GroupMapper;
import com.nym.shortlink.core.dao.mapper.GroupUniqueMapper;
import com.nym.shortlink.core.dto.req.ShortLinkGroupSortReqDTO;
import com.nym.shortlink.core.dto.req.ShortLinkGroupUpdateReqDTO;
import com.nym.shortlink.core.dto.resp.ShortLinkGroupRespDTO;
import com.nym.shortlink.core.service.ShortLinkService;
import com.nym.shortlink.core.dto.resp.ShortLinkGroupCountQueryRespDTO;
import com.nym.shortlink.core.service.GroupService;
import com.nym.shortlink.core.toolkit.RandomGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.nym.shortlink.core.common.constant.RedisCacheConstant.LOCK_GROUP_CREATE_KEY;

/**
 * 短链接分组接口实现层
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GroupServiceImpl extends ServiceImpl<GroupMapper, GroupDO> implements GroupService {

    private final RBloomFilter<String> gidRegisterCachePenetrationBloomFilter;
    private final GroupUniqueMapper groupUniqueMapper;
    private final ShortLinkService shortLinkService;
    private final RedissonClient redissonClient;

    @Value("${short-link.group.max-num}")
    private Integer groupMaxNum;

    @Override
    public void saveGroup(String groupName) {
        saveGroup(UserContext.getUsername(), groupName);
    }

    @Override
    public void saveGroup(String username, String groupName) {
        RLock lock = redissonClient.getLock(String.format(LOCK_GROUP_CREATE_KEY, username));
        lock.lock();
        try {
            LambdaQueryWrapper<GroupDO> queryWrapper = Wrappers.lambdaQuery(GroupDO.class)
                    .eq(GroupDO::getUsername, username)
                    .eq(GroupDO::getDelFlag, 0);
            List<GroupDO> groupDOList = baseMapper.selectList(queryWrapper);
            if (CollUtil.isNotEmpty(groupDOList) && groupDOList.size() == groupMaxNum) {
                throw new ClientException(String.format("已超出最大分组数：%d", groupMaxNum));
            }
            int retryCount = 0;
            int maxRetries = 10;
            String gid = null;
            while (retryCount < maxRetries) {
                gid = saveGroupUniqueReturnGid();
                if (StrUtil.isNotEmpty(gid)) {
                    GroupDO groupDO = GroupDO.builder()
                            .gid(gid)
                            .sortOrder(0)
                            .username(username)
                            .name(groupName)
                            .build();
                    baseMapper.insert(groupDO);
                    gidRegisterCachePenetrationBloomFilter.add(gid);
                    break;
                }
                retryCount++;
            }
            if (StrUtil.isEmpty(gid)) {
                throw new ServiceException("生成分组标识频繁");
            }
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

/**
 * 查询用户的短链接分组列表
 * @return 返回包含分组信息和短链接数量的响应DTO列表
 */
    @Override
    public List<ShortLinkGroupRespDTO> listGroup() {
    // 创建查询条件构造器，查询未删除且属于当前用户的分组
        LambdaQueryWrapper<GroupDO> queryWrapper = Wrappers.lambdaQuery(GroupDO.class)
                .eq(GroupDO::getDelFlag, 0)  // 设置删除标志为0（未删除）
                .eq(GroupDO::getUsername, UserContext.getUsername())  // 设置用户名为当前登录用户
                .orderByDesc(GroupDO::getSortOrder, GroupDO::getUpdateTime);  // 按排序顺序和更新时间降序排列
    // 执行查询，获取用户的分组列表
        List<GroupDO> groupDOList = baseMapper.selectList(queryWrapper);
    // 获取每个分组下的短链接数量
        List<ShortLinkGroupCountQueryRespDTO> listResult = shortLinkService
                .listGroupShortLinkCount(groupDOList.stream().map(GroupDO::getGid).toList());
    // 将DO对象列表转换为响应DTO列表
        List<ShortLinkGroupRespDTO> shortLinkGroupRespDTOList = BeanUtil.copyToList(groupDOList, ShortLinkGroupRespDTO.class);
    // 为每个分组设置短链接数量
        shortLinkGroupRespDTOList.forEach(each -> {
        // 在查询结果中查找当前分组的短链接数量信息
            Optional<ShortLinkGroupCountQueryRespDTO> first = listResult.stream()
                    .filter(item -> Objects.equals(item.getGid(), each.getGid()))
                    .findFirst();
        // 如果找到匹配的分组，则设置短链接数量
            first.ifPresent(item -> each.setShortLinkCount(first.get().getShortLinkCount()));
        });
        return shortLinkGroupRespDTOList;
    }

    @Override
    public void updateGroup(ShortLinkGroupUpdateReqDTO requestParam) {
        LambdaUpdateWrapper<GroupDO> updateWrapper = Wrappers.lambdaUpdate(GroupDO.class)
                .eq(GroupDO::getUsername, UserContext.getUsername())
                .eq(GroupDO::getGid, requestParam.getGid())
                .eq(GroupDO::getDelFlag, 0);
        GroupDO groupDO = new GroupDO();
        groupDO.setName(requestParam.getName());
        baseMapper.update(groupDO, updateWrapper);
    }

    @Override
    public void deleteGroup(String gid) {
        if (StrUtil.isBlank(gid)) {
            throw new ClientException("分组标识不能为空");
        }
        LambdaUpdateWrapper<GroupDO> updateWrapper = Wrappers.lambdaUpdate(GroupDO.class)
                .eq(GroupDO::getUsername, UserContext.getUsername())
                .eq(GroupDO::getGid, gid)
                .eq(GroupDO::getDelFlag, 0);
        GroupDO groupDO = new GroupDO();
        groupDO.setDelFlag(1);
        baseMapper.update(groupDO, updateWrapper);
    }

    @Override
    public void sortGroup(List<ShortLinkGroupSortReqDTO> requestParam) {
        requestParam.forEach(each -> {
            GroupDO groupDO = GroupDO.builder()
                    .sortOrder(each.getSortOrder())
                    .build();
            LambdaUpdateWrapper<GroupDO> updateWrapper = Wrappers.lambdaUpdate(GroupDO.class)
                    .eq(GroupDO::getUsername, UserContext.getUsername())
                    .eq(GroupDO::getGid, each.getGid())
                    .eq(GroupDO::getDelFlag, 0);
            baseMapper.update(groupDO, updateWrapper);
        });
    }

    private String saveGroupUniqueReturnGid() {
        String gid = RandomGenerator.generateRandom();
        if (gidRegisterCachePenetrationBloomFilter.contains(gid)) {
            return null;
        }
        GroupUniqueDO groupUniqueDO = GroupUniqueDO.builder()
                .gid(gid)
                .build();
        try {
            groupUniqueMapper.insert(groupUniqueDO);
        } catch (DuplicateKeyException e) {
            return null;
        }
        return gid;
    }
}
