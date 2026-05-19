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

    // 布隆过滤器，用于防止缓存穿透
    private final RBloomFilter<String> gidRegisterCachePenetrationBloomFilter;
    // 分组唯一性映射器
    private final GroupUniqueMapper groupUniqueMapper;
    // 短链接服务
    private final ShortLinkService shortLinkService;
    // Redisson客户端，用于分布式锁
    private final RedissonClient redissonClient;

    // 从配置中获取的最大分组数
    @Value("${short-link.group.max-num}")
    private Integer groupMaxNum;

    /**
     * 保存分组，使用当前登录用户
     * @param groupName 分组名称
     */
    @Override
    public void saveGroup(String groupName) {
        saveGroup(UserContext.getUsername(), groupName);
    }

    /**
     * 保存分组
     * @param username 用户名
     * @param groupName 分组名称
     */
    @Override
    public void saveGroup(String username, String groupName) {
        // 创建分布式锁，防止并发创建分组
        RLock lock = redissonClient.getLock(String.format(LOCK_GROUP_CREATE_KEY, username));
        lock.lock();
        try {
            // 查询用户已存在的分组
            LambdaQueryWrapper<GroupDO> queryWrapper = Wrappers.lambdaQuery(GroupDO.class)
                    .eq(GroupDO::getUsername, username)
                    .eq(GroupDO::getDelFlag, 0);
            List<GroupDO> groupDOList = baseMapper.selectList(queryWrapper);
            // 检查是否超过最大分组数
            if (CollUtil.isNotEmpty(groupDOList) && groupDOList.size() == groupMaxNum) {
                throw new ClientException(String.format("已超出最大分组数：%d", groupMaxNum));
            }
            // 重试机制，确保生成唯一GID
            int retryCount = 0;
            int maxRetries = 10;
            String gid = null;
            while (retryCount < maxRetries) {
                gid = saveGroupUniqueReturnGid();
                if (StrUtil.isNotEmpty(gid)) {
                    // 创建并保存分组对象
                    GroupDO groupDO = GroupDO.builder()
                            .gid(gid)
                            .sortOrder(0)
                            .username(username)
                            .name(groupName)
                            .build();
                    baseMapper.insert(groupDO);
                    // 将GID添加到布隆过滤器
                    gidRegisterCachePenetrationBloomFilter.add(gid);
                    break;
                }
                retryCount++;
            }
            // 如果重试次数用完仍未生成唯一GID，抛出异常
            if (StrUtil.isEmpty(gid)) {
                throw new ServiceException("生成分组标识频繁");
            }
        } finally {
            // 确保锁被释放
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

    /**
     * 更新分组信息
     * @param requestParam 更新请求参数DTO
     */
    @Override
    public void updateGroup(ShortLinkGroupUpdateReqDTO requestParam) {
        // 创建更新条件构造器
        LambdaUpdateWrapper<GroupDO> updateWrapper = Wrappers.lambdaUpdate(GroupDO.class)
                .eq(GroupDO::getUsername, UserContext.getUsername())
                .eq(GroupDO::getGid, requestParam.getGid())
                .eq(GroupDO::getDelFlag, 0);
        // 创建分组对象并设置要更新的名称
        GroupDO groupDO = new GroupDO();
        groupDO.setName(requestParam.getName());
        // 执行更新操作
        baseMapper.update(groupDO, updateWrapper);
    }

    /**
     * 删除分组
     * @param gid 分组标识
     */
    @Override
    public void deleteGroup(String gid) {
        // 检查分组标识是否为空
        if (StrUtil.isBlank(gid)) {
            throw new ClientException("分组标识不能为空");
        }
        // 创建更新条件构造器
        LambdaUpdateWrapper<GroupDO> updateWrapper = Wrappers.lambdaUpdate(GroupDO.class)
                .eq(GroupDO::getUsername, UserContext.getUsername())
                .eq(GroupDO::getGid, gid)
                .eq(GroupDO::getDelFlag, 0);
        // 创建分组对象并设置删除标志为1（已删除）
        GroupDO groupDO = new GroupDO();
        groupDO.setDelFlag(1);
        // 执行更新操作
        baseMapper.update(groupDO, updateWrapper);
    }

    /**
     * 分组排序
     * @param requestParam 排序请求参数DTO列表
     */
    @Override
    public void sortGroup(List<ShortLinkGroupSortReqDTO> requestParam) {
        // 遍历排序请求参数列表
        requestParam.forEach(each -> {
            // 创建分组对象并设置排序顺序
            GroupDO groupDO = GroupDO.builder()
                    .sortOrder(each.getSortOrder())
                    .build();
            // 创建更新条件构造器
            LambdaUpdateWrapper<GroupDO> updateWrapper = Wrappers.lambdaUpdate(GroupDO.class)
                    .eq(GroupDO::getUsername, UserContext.getUsername())
                    .eq(GroupDO::getGid, each.getGid())
                    .eq(GroupDO::getDelFlag, 0);
            // 执行更新操作
            baseMapper.update(groupDO, updateWrapper);
        });
    }

    /**
     * 保存唯一分组并返回GID
     * @return 分组标识
     */
    private String saveGroupUniqueReturnGid() {
        // 生成随机GID
        String gid = RandomGenerator.generateRandom();
        // 检查GID是否已存在于布隆过滤器中
        if (gidRegisterCachePenetrationBloomFilter.contains(gid)) {
            return null;
        }
        // 创建唯一分组对象
        GroupUniqueDO groupUniqueDO = GroupUniqueDO.builder()
                .gid(gid)
                .build();
        try {
            // 尝试插入唯一分组记录
            groupUniqueMapper.insert(groupUniqueDO);
        } catch (DuplicateKeyException e) {
            // 如果主键冲突，返回null表示需要重试
            return null;
        }
        // 返回生成的GID
        return gid;
    }
}
