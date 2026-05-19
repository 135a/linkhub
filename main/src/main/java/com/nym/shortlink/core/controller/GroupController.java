package com.nym.shortlink.core.controller;

import com.nym.shortlink.core.common.biz.ratelimit.RateLimit;
import com.nym.shortlink.core.common.convention.result.Result;
import com.nym.shortlink.core.common.convention.result.Results;
import com.nym.shortlink.core.dto.req.ShortLinkGroupSaveReqDTO;
import com.nym.shortlink.core.dto.req.ShortLinkGroupSortReqDTO;
import com.nym.shortlink.core.dto.req.ShortLinkGroupUpdateReqDTO;
import com.nym.shortlink.core.dto.resp.ShortLinkGroupRespDTO;
import com.nym.shortlink.core.service.GroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import lombok.extern.slf4j.Slf4j;

/**
 * 短链接分组控制层
 * 提供短链接分组的增删改查等操作接口
 */
@RestController // 标识为RESTful控制器
@RequiredArgsConstructor // 使用Lombok生成带参构造器，注入依赖
@Slf4j // 使用Lombok生成日志对象
public class GroupController {

    private final GroupService groupService; // 注入分组服务接口

    /**
     * 新增短链接分组
     * 限制QPS为5，防止频繁请求
     * @param requestParam 分组名称请求参数
     * @return 返回操作结果
     */
    @RateLimit(resource = "save_group", qps = 5) // 限流注解，限制资源访问频率
    @PostMapping("/api/short-link/admin/v1/group") // POST请求映射
    public Result<Void> save(@Valid @RequestBody ShortLinkGroupSaveReqDTO requestParam) {
        groupService.saveGroup(requestParam.getName()); // 调用服务层保存分组
        return Results.success(); // 返回成功结果
    }

    /**
     * 查询短链接分组集合
     * 限制QPS为20，允许较高的查询频率
     * @return 返回分组列表结果
     */
    @RateLimit(resource = "list_group", qps = 20) // 限流注解，限制资源访问频率
    @GetMapping("/api/short-link/admin/v1/group") // GET请求映射
    public Result<List<ShortLinkGroupRespDTO>> listGroup() {
        Result<List<ShortLinkGroupRespDTO>> result = Results.success(groupService.listGroup()); // 调用服务层获取分组列表
        return result; // 返回查询结果
    }

    /**
     * 修改短链接分组名称
     * 限制QPS为5，防止频繁修改
     * @param requestParam 分组更新请求参数
     * @return 返回操作结果
     */
    @RateLimit(resource = "update_group", qps = 5) // 限流注解，限制资源访问频率
    @PutMapping("/api/short-link/admin/v1/group") // PUT请求映射
    public Result<Void> updateGroup(@Valid @RequestBody ShortLinkGroupUpdateReqDTO requestParam) {
        groupService.updateGroup(requestParam); // 调用服务层更新分组
        return Results.success(); // 返回成功结果
    }

    /**
     * 删除短链接分组
     * 限制QPS为5，防止频繁删除
     * @param gid 分组ID
     * @return 返回操作结果
     */
    @RateLimit(resource = "delete_group", qps = 5) // 限流注解，限制资源访问频率
    @DeleteMapping("/api/short-link/admin/v1/group") // DELETE请求映射
    public Result<Void> updateGroup(@RequestParam String gid) {
        groupService.deleteGroup(gid); // 调用服务层删除分组
        return Results.success(); // 返回成功结果
    }

    /**
     * 排序短链接分组
     * 限制QPS为10，允许适度的排序操作频率
     * @param requestParam 分组排序请求参数列表
     * @return 返回操作结果
     */
    @RateLimit(resource = "sort_group", qps = 10) // 限流注解，限制资源访问频率
    @PostMapping("/api/short-link/admin/v1/group/sort") // POST请求映射
    public Result<Void> sortGroup(@RequestBody List<ShortLinkGroupSortReqDTO> requestParam) {
        groupService.sortGroup(requestParam); // 调用服务层排序分组
        return Results.success(); // 返回成功结果
    }
}
