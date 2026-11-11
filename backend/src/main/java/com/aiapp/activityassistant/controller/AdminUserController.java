package com.aiapp.activityassistant.controller;

import com.aiapp.activityassistant.common.context.UserContext;
import com.aiapp.activityassistant.common.dto.PageQuery;
import com.aiapp.activityassistant.common.result.PageVO;
import com.aiapp.activityassistant.common.result.Result;
import com.aiapp.activityassistant.dto.AdminUserCreateDTO;
import com.aiapp.activityassistant.dto.ResetPasswordDTO;
import com.aiapp.activityassistant.dto.UserRoleAssignDTO;
import com.aiapp.activityassistant.dto.UserStatusDTO;
import com.aiapp.activityassistant.service.RbacService;
import com.aiapp.activityassistant.vo.UserAdminVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台用户管理
 */
@Tag(name = "后台-用户管理")
@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
@RequiresPermissions("system:user:view")
public class AdminUserController {

    private final RbacService rbacService;

    @Operation(summary = "分页查询用户")
    @GetMapping
    public Result<PageVO<UserAdminVO>> page(@RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            PageQuery query) {
        return Result.success(rbacService.pageUsers(query, keyword, status));
    }

    @Operation(summary = "新建用户")
    @PostMapping
    @RequiresPermissions("system:user:manage")
    public Result<Long> create(@Valid @RequestBody AdminUserCreateDTO dto) {
        return Result.success(rbacService.createUser(dto));
    }

    @Operation(summary = "分配用户角色")
    @PutMapping("/{id}/roles")
    @RequiresPermissions("system:user:manage")
    public Result<Void> assignRoles(@PathVariable Long id, @Valid @RequestBody UserRoleAssignDTO dto) {
        rbacService.assignUserRoles(id, dto.getRoleIds(), UserContext.getCurrentUserId());
        return Result.success();
    }

    @Operation(summary = "启用/禁用账号")
    @PutMapping("/{id}/status")
    @RequiresPermissions("system:user:manage")
    public Result<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody UserStatusDTO dto) {
        rbacService.updateUserStatus(id, dto.getStatus(), UserContext.getCurrentUserId());
        return Result.success();
    }

    @Operation(summary = "重置密码")
    @PutMapping("/{id}/password")
    @RequiresPermissions("system:user:manage")
    public Result<Void> resetPassword(@PathVariable Long id, @Valid @RequestBody ResetPasswordDTO dto) {
        rbacService.resetPassword(id, dto.getPassword());
        return Result.success();
    }

    @Operation(summary = "删除用户")
    @DeleteMapping("/{id}")
    @RequiresPermissions("system:user:manage")
    public Result<Void> delete(@PathVariable Long id) {
        rbacService.deleteUser(id, UserContext.getCurrentUserId());
        return Result.success();
    }
}
