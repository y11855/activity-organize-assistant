package com.aiapp.activityassistant.service;

import com.aiapp.activityassistant.common.dto.PageQuery;
import com.aiapp.activityassistant.common.result.PageVO;
import com.aiapp.activityassistant.dto.AdminUserCreateDTO;
import com.aiapp.activityassistant.dto.RoleSaveDTO;
import com.aiapp.activityassistant.vo.PermissionVO;
import com.aiapp.activityassistant.vo.RoleVO;
import com.aiapp.activityassistant.vo.UserAdminVO;

import java.util.List;

/**
 * RBAC：用户角色/权限查询与后台管理。
 */
public interface RbacService {

    /** 用户拥有的角色编码 */
    List<String> listRoleCodes(Long userId);

    /** 用户拥有的角色名称 */
    List<String> listRoleNames(Long userId);

    /** 用户拥有的权限编码（admin 角色始终返回全量） */
    List<String> listPermissionCodes(Long userId);

    /** 全部角色（含权限编码与用户数） */
    List<RoleVO> listRoles();

    /** 全部权限 */
    List<PermissionVO> listPermissions();

    /** 新建角色 */
    Long createRole(RoleSaveDTO dto);

    /** 修改角色名称/说明（编码不可改） */
    void updateRole(Long id, RoleSaveDTO dto);

    /** 删除角色（内置角色、仍被用户使用的角色不可删） */
    void deleteRole(Long id);

    /** 全量替换角色权限（admin 角色不可改） */
    void assignPermissions(Long roleId, List<String> permissionCodes);

    /** 后台分页查询用户 */
    PageVO<UserAdminVO> pageUsers(PageQuery query, String keyword, Integer status);

    /** 后台新建用户 */
    Long createUser(AdminUserCreateDTO dto);

    /** 全量替换用户角色 */
    void assignUserRoles(Long userId, List<Long> roleIds, Long currentUserId);

    /** 为用户追加一个角色（注册时授予默认 member 角色） */
    void addUserRoleByCode(Long userId, String roleCode);

    /** 启用/禁用账号 */
    void updateUserStatus(Long userId, Integer status, Long currentUserId);

    /** 重置密码（入参为 RSA 密文） */
    void resetPassword(Long userId, String encryptedPassword);

    /** 删除用户（逻辑删除并吊销 token） */
    void deleteUser(Long userId, Long currentUserId);
}
