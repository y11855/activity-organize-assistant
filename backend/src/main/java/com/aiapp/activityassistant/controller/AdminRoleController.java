package com.aiapp.activityassistant.controller;

import com.aiapp.activityassistant.common.result.Result;
import com.aiapp.activityassistant.dto.RolePermissionAssignDTO;
import com.aiapp.activityassistant.dto.RoleSaveDTO;
import com.aiapp.activityassistant.service.RbacService;
import com.aiapp.activityassistant.vo.PermissionVO;
import com.aiapp.activityassistant.vo.RoleVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 后台角色与权限管理
 */
@Tag(name = "后台-角色权限")
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('system:role:view')")
public class AdminRoleController {

    private final RbacService rbacService;

    @Operation(summary = "角色列表（含权限与用户数）")
    @GetMapping("/roles")
    public Result<List<RoleVO>> roles() {
        return Result.success(rbacService.listRoles());
    }

    @Operation(summary = "全部权限项")
    @GetMapping("/permissions")
    public Result<List<PermissionVO>> permissions() {
        return Result.success(rbacService.listPermissions());
    }

    @Operation(summary = "新建角色")
    @PostMapping("/roles")
    @PreAuthorize("hasAuthority('system:role:manage')")
    public Result<Long> create(@Valid @RequestBody RoleSaveDTO dto) {
        return Result.success(rbacService.createRole(dto));
    }

    @Operation(summary = "修改角色名称/说明（编码不可改）")
    @PutMapping("/roles/{id}")
    @PreAuthorize("hasAuthority('system:role:manage')")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody RoleSaveDTO dto) {
        rbacService.updateRole(id, dto);
        return Result.success();
    }

    @Operation(summary = "删除角色")
    @DeleteMapping("/roles/{id}")
    @PreAuthorize("hasAuthority('system:role:manage')")
    public Result<Void> delete(@PathVariable Long id) {
        rbacService.deleteRole(id);
        return Result.success();
    }

    @Operation(summary = "分配角色权限（全量替换）")
    @PutMapping("/roles/{id}/permissions")
    @PreAuthorize("hasAuthority('system:role:manage')")
    public Result<Void> assignPermissions(@PathVariable Long id,
            @Valid @RequestBody RolePermissionAssignDTO dto) {
        rbacService.assignPermissions(id, dto.getPermissionCodes());
        return Result.success();
    }
}
