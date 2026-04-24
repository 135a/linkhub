/*
 * Copyright © 2026 NageOffer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.nym.shortlink.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nym.shortlink.admin.common.biz.user.UserContext;
import com.nym.shortlink.admin.common.convention.errorcode.BaseErrorCode;
import com.nym.shortlink.admin.common.convention.exception.ClientException;
import com.nym.shortlink.admin.common.convention.exception.ServiceException;
import com.nym.shortlink.admin.common.convention.result.Result;
import com.nym.shortlink.admin.dao.entity.GroupDO;
import com.nym.shortlink.admin.dao.entity.GroupUniqueDO;
import com.nym.shortlink.admin.dao.mapper.GroupMapper;
import com.nym.shortlink.admin.dao.mapper.GroupUniqueMapper;
import com.nym.shortlink.admin.dto.req.ShortLinkGroupSortReqDTO;
import com.nym.shortlink.admin.dto.req.ShortLinkGroupUpdateReqDTO;
import com.nym.shortlink.admin.dto.resp.ShortLinkGroupRespDTO;
import com.nym.shortlink.admin.remote.ShortLinkActualRemoteService;
import com.nym.shortlink.admin.remote.dto.resp.ShortLinkGroupCountQueryRespDTO;
import com.nym.shortlink.admin.service.GroupService;
import com.nym.shortlink.admin.toolkit.RandomGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.nym.shortlink.admin.common.constant.RedisCacheConstant.LOCK_GROUP_CREATE_KEY;

/**
 * 短链接分组接口实现层
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GroupServiceImpl extends ServiceImpl<GroupMapper, GroupDO> implements GroupService {

    private final RBloomFilter<String> gidRegisterCachePenetrationBloomFilter;
    private final GroupUniqueMapper groupUniqueMapper;
    private final ShortLinkActualRemoteService shortLinkActualRemoteService;
    private final RedissonClient redissonClient;

    @Value("${short-link.group.max-num}")
    private Integer groupMaxNum;

    @Override
    public void saveGroup(String groupName) {
        try {
            log.info("saveGroup start, groupName: {}", groupName);
            String username = UserContext.getUsername();
            log.info("saveGroup username: {}", username);
            if (StrUtil.isEmpty(username)) {
                log.info("saveGroup username is empty");
                throw new ClientException("用户未登录");
            }
            saveGroup(username, groupName);
            log.info("saveGroup end");
        } catch (ClientException e) {
            log.error("saveGroup client exception: {}", e);
            throw e;
        } catch (Exception e) {
            log.error("saveGroup exception: {}", e);
            throw new ServiceException("系统执行出错");
        }
    }

    @Override
    public void saveGroup(String username, String groupName) {
        log.info("saveGroup start, username: {}, groupName: {}", username, groupName);
        if (StrUtil.isEmpty(username)) {
            log.info("saveGroup username is empty");
            throw new ClientException("用户未登录");
        }
        RLock lock = redissonClient.getLock(String.format(LOCK_GROUP_CREATE_KEY, username));
        // Bounded wait/lease to keep the critical path responsive:
        // - waitTime 200ms:  two concurrent creations for the same user return
        //   with a retryable error instead of serialising indefinitely.
        // - leaseTime 2s:    if the holder crashes the lock self-releases, so
        //   no one is blocked by a stale owner.
        boolean acquired;
        try {
            acquired = lock.tryLock(200, 2000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new ClientException(BaseErrorCode.GROUP_CREATE_BUSY);
        }
        if (!acquired) {
            throw new ClientException(BaseErrorCode.GROUP_CREATE_BUSY);
        }
        try {
            LambdaQueryWrapper<GroupDO> queryWrapper = Wrappers.lambdaQuery(GroupDO.class)
                    .eq(GroupDO::getUsername, username)
                    .eq(GroupDO::getDelFlag, 0);
            log.info("saveGroup queryWrapper: {}", queryWrapper);
            List<GroupDO> groupDOList = baseMapper.selectList(queryWrapper);
            log.info("saveGroup groupDOList size: {}", groupDOList.size());
            if (CollUtil.isNotEmpty(groupDOList) && groupDOList.size() == groupMaxNum) {
                throw new ClientException(String.format("已超出最大分组数：%d", groupMaxNum));
            }
            int retryCount = 0;
            int maxRetries = 10;
            String gid = null;
            while (retryCount < maxRetries) {
                log.info("saveGroup retryCount: {}", retryCount);
                gid = saveGroupUniqueReturnGid();
                log.info("saveGroup gid: {}", gid);
                if (StrUtil.isNotEmpty(gid)) {
                    GroupDO groupDO = GroupDO.builder()
                            .gid(gid)
                            .sortOrder(0)
                            .username(username)
                            .name(groupName)
                            .build();
                    log.info("saveGroup groupDO: {}", groupDO);
                    baseMapper.insert(groupDO);
                    log.info("saveGroup insert groupDO success");
                    break;
                }
                retryCount++;
            }
            if (StrUtil.isEmpty(gid)) {
                throw new ServiceException("生成分组标识频繁");
            }
            log.info("saveGroup end");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    public List<ShortLinkGroupRespDTO> listGroup() {
        log.info("listGroup start");
        try {
            String username = UserContext.getUsername();
            log.info("listGroup username: {}", username);
            if (StrUtil.isEmpty(username)) {
                log.info("listGroup username is empty");
                throw new ClientException("用户未登录");
            }
            LambdaQueryWrapper<GroupDO> queryWrapper = Wrappers.lambdaQuery(GroupDO.class)
                    .eq(GroupDO::getDelFlag, 0)
                    .eq(GroupDO::getUsername, username)
                    .orderByDesc(GroupDO::getSortOrder, GroupDO::getUpdateTime);
            log.info("listGroup queryWrapper: {}", queryWrapper);
            List<GroupDO> groupDOList = baseMapper.selectList(queryWrapper);
            log.info("listGroup groupDOList size: {}", groupDOList.size());
            List<ShortLinkGroupRespDTO> shortLinkGroupRespDTOList = BeanUtil.copyToList(groupDOList, ShortLinkGroupRespDTO.class);
            log.info("listGroup shortLinkGroupRespDTOList size: {}", shortLinkGroupRespDTOList.size());
            if (CollUtil.isNotEmpty(groupDOList)) {
                List<String> gidList = groupDOList.stream().map(GroupDO::getGid).toList();
                log.info("listGroup gidList: {}", gidList);
                try {
                    Result<List<ShortLinkGroupCountQueryRespDTO>> listResult = shortLinkActualRemoteService
                            .listGroupShortLinkCount(gidList);
                    log.info("listGroup listResult: {}", listResult);
                    if (listResult != null && listResult.getData() != null) {
                        shortLinkGroupRespDTOList.forEach(each -> {
                            Optional<ShortLinkGroupCountQueryRespDTO> first = listResult.getData().stream()
                                    .filter(item -> Objects.equals(item.getGid(), each.getGid()))
                                    .findFirst();
                            first.ifPresent(item -> each.setShortLinkCount(first.get().getShortLinkCount()));
                        });
                    }
                } catch (Exception e) {
                    log.error("listGroup call shortLinkActualRemoteService exception: {}", e);
                    // 如果调用 Project 服务失败，继续执行，不影响分组列表的返回
                }
            }
            log.info("listGroup end");
            return shortLinkGroupRespDTOList;
        } catch (ClientException e) {
            log.error("listGroup client exception: {}", e);
            throw e;
        } catch (Exception e) {
            log.error("listGroup exception: {}", e);
            // 如果出现其他异常，返回一个空的分组列表，而不是抛出异常
            return new ArrayList<>();
        }
    }

    @Override
    public void updateGroup(ShortLinkGroupUpdateReqDTO requestParam) {
        String username = UserContext.getUsername();
        if (StrUtil.isEmpty(username)) {
            throw new ClientException("用户未登录");
        }
        LambdaUpdateWrapper<GroupDO> updateWrapper = Wrappers.lambdaUpdate(GroupDO.class)
                .eq(GroupDO::getUsername, username)
                .eq(GroupDO::getGid, requestParam.getGid())
                .eq(GroupDO::getDelFlag, 0);
        GroupDO groupDO = new GroupDO();
        groupDO.setName(requestParam.getName());
        baseMapper.update(groupDO, updateWrapper);
    }

    @Override
    public void deleteGroup(String gid) {
        String username = UserContext.getUsername();
        if (StrUtil.isEmpty(username)) {
            throw new ClientException("用户未登录");
        }
        LambdaUpdateWrapper<GroupDO> updateWrapper = Wrappers.lambdaUpdate(GroupDO.class)
                .eq(GroupDO::getUsername, username)
                .eq(GroupDO::getGid, gid)
                .eq(GroupDO::getDelFlag, 0);
        GroupDO groupDO = new GroupDO();
        groupDO.setDelFlag(1);
        baseMapper.update(groupDO, updateWrapper);
    }

    @Override
    public void sortGroup(List<ShortLinkGroupSortReqDTO> requestParam) {
        String username = UserContext.getUsername();
        if (StrUtil.isEmpty(username)) {
            throw new ClientException("用户未登录");
        }
        requestParam.forEach(each -> {
            GroupDO groupDO = GroupDO.builder()
                    .sortOrder(each.getSortOrder())
                    .build();
            LambdaUpdateWrapper<GroupDO> updateWrapper = Wrappers.lambdaUpdate(GroupDO.class)
                    .eq(GroupDO::getUsername, username)
                    .eq(GroupDO::getGid, each.getGid())
                    .eq(GroupDO::getDelFlag, 0);
            baseMapper.update(groupDO, updateWrapper);
        });
    }

    private String saveGroupUniqueReturnGid() {
        String gid = RandomGenerator.generateRandom();
        GroupUniqueDO groupUniqueDO = GroupUniqueDO.builder()
                .gid(gid)
                .build();
        try {
            groupUniqueMapper.insert(groupUniqueDO);
        } catch (DuplicateKeyException e) {
            return null;
        }
        gidRegisterCachePenetrationBloomFilter.add(gid);
        return gid;
    }
}
