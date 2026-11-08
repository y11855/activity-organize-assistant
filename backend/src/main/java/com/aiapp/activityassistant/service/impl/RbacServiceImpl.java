package com.aiapp.activityassistant.service.impl;

import cn.hutool.crypto.digest.BCrypt;
import com.aiapp.activityassistant.common.dto.PageQuery;
import com.aiapp.activityassistant.common.exception.BusinessException;
import com.aiapp.activityassistant.common.redis.RedisService;
import com.aiapp.activityassistant.common.result.PageVO;
import com.aiapp.activityassistant.common.result.ResultCode;
import com.aiapp.activityassistant.dto.AdminUserCreateDTO;
import com.aiapp.activityassistant.dto.RoleSaveDTO;
import com.aiapp.activityassistant.entity.Permission;
import com.aiapp.activityassistant.entity.Role;
import com.aiapp.activityassistant.entity.RolePermission;
import com.aiapp.activityassistant.entity.User;
import com.aiapp.activityassistant.entity.UserRole;
import com.aiapp.activityassistant.mapper.PermissionMapper;
import com.aiapp.activityassistant.mapper.RoleMapper;
import com.aiapp.activityassistant.mapper.RolePermissionMapper;
import com.aiapp.activityassistant.mapper.UserMapper;
import com.aiapp.activityassistant.mapper.UserRoleMapper;
import com.aiapp.activityassistant.service.LoginCryptoService;
import com.aiapp.activityassistant.service.RbacService;
import com.aiapp.activityassistant.service.TokenService;
import com.aiapp.activityassistant.vo.PermissionVO;
import com.aiapp.activityassistant.vo.RoleVO;
import com.aiapp.activityassistant.vo.UserAdminVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * RBAC 业务实现。
 */
@Service
@RequiredArgsConstructor
public class RbacServiceImpl implements RbacService {

    private static final String ADMIN_CODE = "admin";

    /**
     * 用户角色/权限码缓存 key。每次请求拦截器都要加载这两项，属于"不常变的资源"，
     * 缓存 30 分钟避免每个请求都多表 JOIN 查库；授权变更时主动失效对应用户的缓存。
     */
    private static final String ROLES_CACHE_KEY = "rbac:roles:";
    private static final String PERMS_CACHE_KEY = "rbac:perms:";
    private static final Duration RBAC_CACHE_TTL = Duration.ofMinutes(30);

    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final UserRoleMapper userRoleMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final UserMapper userMapper;
    private final LoginCryptoService loginCryptoService;
    private final TokenService tokenService;
    private final RedisService redisService;

    // ==================== 权限查询 ====================

    @Override
    public List<String> listRoleCodes(Long userId) {
        List<String> cached = getCachedCodes(ROLES_CACHE_KEY + userId);
        if (cached != null) {
            return cached;
        }
        List<String> codes = roleMapper.selectRoleCodesByUserId(userId);
        codes = codes == null ? Collections.emptyList() : codes;
        // 以逗号拼接字符串存储，与 Redis 的 JSON 序列化器兼容（List 反序列化有类型信息兼容问题）
        redisService.set(ROLES_CACHE_KEY + userId, String.join(",", codes), RBAC_CACHE_TTL);
        return codes;
    }

    @Override
    public List<String> listRoleNames(Long userId) {
        return listRolesOfUser(userId).stream().map(Role::getName).toList();
    }

    @Override
    public List<String> listPermissionCodes(Long userId) {
        List<String> cached = getCachedCodes(PERMS_CACHE_KEY + userId);
        if (cached != null) {
            return cached;
        }
        List<String> codes;
        // admin 角色始终拥有全部权限，避免误删 role_permission 后管理员被锁死
        if (listRoleCodes(userId).contains(ADMIN_CODE)) {
            codes = permissionMapper.selectList(new LambdaQueryWrapper<Permission>()
                            .orderByAsc(Permission::getSort))
                    .stream().map(Permission::getCode).toList();
        } else {
            codes = permissionMapper.selectPermissionCodesByUserId(userId);
            codes = codes == null ? Collections.emptyList() : codes;
        }
        redisService.set(PERMS_CACHE_KEY + userId, String.join(",", codes), RBAC_CACHE_TTL);
        return codes;
    }

    private List<Role> listRolesOfUser(Long userId) {
        List<Long> roleIds = userRoleMapper.selectList(new LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getUserId, userId))
                .stream().map(UserRole::getRoleId).toList();
        if (roleIds.isEmpty()) {
            return Collections.emptyList();
        }
        return roleMapper.selectBatchIds(roleIds).stream()
                .sorted((a, b) -> Long.compare(a.getId(), b.getId()))
                .toList();
    }

    // ==================== 角色管理 ====================

    @Override
    public List<RoleVO> listRoles() {
        List<Role> roles = roleMapper.selectList(new LambdaQueryWrapper<Role>().orderByAsc(Role::getId));
        List<RolePermission> rolePermissions = rolePermissionMapper.selectList(null);
        List<Permission> permissions = permissionMapper.selectList(null);
        List<UserRole> userRoles = userRoleMapper.selectList(null);

        Map<Long, String> permissionCodeById = permissions.stream()
                .collect(Collectors.toMap(Permission::getId, Permission::getCode));
        Map<Long, List<String>> permissionCodesByRole = new HashMap<>();
        for (RolePermission rp : rolePermissions) {
            String code = permissionCodeById.get(rp.getPermissionId());
            if (code != null) {
                permissionCodesByRole.computeIfAbsent(rp.getRoleId(), k -> new ArrayList<>()).add(code);
            }
        }
        Map<Long, Long> userCountByRole = userRoles.stream()
                .collect(Collectors.groupingBy(UserRole::getRoleId, Collectors.counting()));

        return roles.stream().map(role -> {
            RoleVO vo = new RoleVO();
            vo.setId(role.getId());
            vo.setCode(role.getCode());
            vo.setName(role.getName());
            vo.setDescription(role.getDescription());
            vo.setBuiltin(role.getBuiltin());
            vo.setPermissionCodes(permissionCodesByRole.getOrDefault(role.getId(), Collections.emptyList()));
            vo.setUserCount(userCountByRole.getOrDefault(role.getId(), 0L));
            return vo;
        }).toList();
    }

    @Override
    public List<PermissionVO> listPermissions() {
        return permissionMapper.selectList(new LambdaQueryWrapper<Permission>()
                .orderByAsc(Permission::getGroupName).orderByAsc(Permission::getSort))
                .stream().map(p -> {
                    PermissionVO vo = new PermissionVO();
                    vo.setCode(p.getCode());
                    vo.setName(p.getName());
                    vo.setGroupName(p.getGroupName());
                    vo.setSort(p.getSort());
                    return vo;
                }).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createRole(RoleSaveDTO dto) {
        String code = dto.getCode().trim();
        if (roleMapper.selectCount(new LambdaQueryWrapper<Role>().eq(Role::getCode, code)) > 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "角色编码已存在");
        }
        Role role = new Role();
        role.setCode(code);
        role.setName(dto.getName().trim());
        role.setDescription(dto.getDescription());
        role.setBuiltin(0);
        try {
            roleMapper.insert(role);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "角色编码已存在");
        }
        return role.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRole(Long id, RoleSaveDTO dto) {
        Role role = roleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "角色不存在");
        }
        // 编码创建后不可修改，避免历史授权与权限码失配
        role.setName(dto.getName().trim());
        role.setDescription(dto.getDescription());
        roleMapper.updateById(role);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRole(Long id) {
        Role role = roleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "角色不存在");
        }
        if (Integer.valueOf(1).equals(role.getBuiltin())) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "内置角色不可删除");
        }
        long userCount = userRoleMapper.selectCount(new LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getRoleId, id));
        if (userCount > 0) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "角色已分配给用户，无法删除");
        }
        rolePermissionMapper.delete(new LambdaQueryWrapper<RolePermission>()
                .eq(RolePermission::getRoleId, id));
        roleMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignPermissions(Long roleId, List<String> permissionCodes) {
        Role role = roleMapper.selectById(roleId);
        if (role == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "角色不存在");
        }
        if (ADMIN_CODE.equals(role.getCode())) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "内置管理员角色的权限不可修改");
        }
        List<String> codes = permissionCodes == null ? Collections.emptyList()
                : permissionCodes.stream().filter(Objects::nonNull).distinct().toList();
        List<Permission> permissions = codes.isEmpty() ? Collections.emptyList()
                : permissionMapper.selectList(new LambdaQueryWrapper<Permission>()
                        .in(Permission::getCode, codes));
        if (permissions.size() != codes.size()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "存在无效的权限编码");
        }
        rolePermissionMapper.delete(new LambdaQueryWrapper<RolePermission>()
                .eq(RolePermission::getRoleId, roleId));
        for (Permission permission : permissions) {
            RolePermission rp = new RolePermission();
            rp.setRoleId(roleId);
            rp.setPermissionId(permission.getId());
            rolePermissionMapper.insert(rp);
        }
        // 角色权限变化影响所有持有该角色的用户，批量失效其缓存
        userRoleMapper.selectList(new LambdaQueryWrapper<UserRole>()
                        .eq(UserRole::getRoleId, roleId))
                .forEach(ur -> evictUserCache(ur.getUserId()));
    }

    // ==================== 用户管理 ====================

    @Override
    public PageVO<UserAdminVO> pageUsers(PageQuery query, String keyword, Integer status) {
        String kw = keyword == null ? null : keyword.trim();
        Page<User> page = userMapper.selectPage(new Page<>(query.getPage(), query.getSize()),
                new LambdaQueryWrapper<User>()
                        .and(kw != null && !kw.isBlank(), w -> w
                                .like(User::getUsername, kw)
                                .or()
                                .like(User::getNickname, kw))
                        .eq(status != null, User::getStatus, status)
                        .orderByDesc(User::getCreateTime));

        List<User> users = page.getRecords();
        if (users.isEmpty()) {
            return PageVO.of(new ArrayList<>(), page.getTotal(), page.getCurrent(), page.getSize());
        }

        List<Long> userIds = users.stream().map(User::getId).toList();
        List<UserRole> userRoles = userRoleMapper.selectList(new LambdaQueryWrapper<UserRole>()
                .in(UserRole::getUserId, userIds));
        List<Long> roleIds = userRoles.stream().map(UserRole::getRoleId).distinct().toList();
        Map<Long, Role> roleById = roleIds.isEmpty() ? Collections.emptyMap()
                : roleMapper.selectBatchIds(roleIds).stream()
                        .collect(Collectors.toMap(Role::getId, r -> r));
        Map<Long, List<UserRole>> userRolesByUser = userRoles.stream()
                .collect(Collectors.groupingBy(UserRole::getUserId));

        List<UserAdminVO> records = users.stream().map(user -> {
            UserAdminVO vo = new UserAdminVO();
            vo.setId(user.getId());
            vo.setUsername(user.getUsername());
            vo.setNickname(user.getNickname());
            vo.setStatus(user.getStatus());
            vo.setCreateTime(user.getCreateTime());
            List<UserRole> urs = userRolesByUser.getOrDefault(user.getId(), Collections.emptyList());
            List<Role> roles = urs.stream()
                    .map(ur -> roleById.get(ur.getRoleId()))
                    .filter(Objects::nonNull)
                    .toList();
            vo.setRoleIds(roles.stream().map(Role::getId).toList());
            vo.setRoleCodes(roles.stream().map(Role::getCode).toList());
            vo.setRoleNames(roles.stream().map(Role::getName).toList());
            return vo;
        }).toList();

        return PageVO.of(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createUser(AdminUserCreateDTO dto) {
        String rawPassword = loginCryptoService.decrypt(dto.getPassword());
        validatePassword(rawPassword);
        String username = dto.getUsername().trim();
        if (userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername, username)) > 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "用户名已被占用");
        }
        List<Role> roles = validateRoleIds(dto.getRoleIds());
        Integer status = dto.getStatus() == null ? 1 : dto.getStatus();
        if (status != 0 && status != 1) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "账号状态只能为 0 或 1");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(BCrypt.hashpw(rawPassword));
        user.setNickname(dto.getNickname() == null || dto.getNickname().isBlank()
                ? username
                : dto.getNickname().trim());
        user.setStatus(status);
        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "用户名已被占用");
        }
        for (Role role : roles) {
            UserRole ur = new UserRole();
            ur.setUserId(user.getId());
            ur.setRoleId(role.getId());
            userRoleMapper.insert(ur);
        }
        return user.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignUserRoles(Long userId, List<Long> roleIds, Long currentUserId) {
        User user = requireUser(userId);
        if (Objects.equals(userId, currentUserId)) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "不能修改自己的角色");
        }
        List<Role> roles = validateRoleIds(roleIds);
        boolean hadAdmin = listRoleCodes(userId).contains(ADMIN_CODE);
        boolean stayAdmin = roles.stream().anyMatch(r -> ADMIN_CODE.equals(r.getCode()));
        if (hadAdmin && !stayAdmin && Integer.valueOf(1).equals(user.getStatus())) {
            ensureNotLastAdmin(userId);
        }

        userRoleMapper.delete(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId));
        for (Role role : roles) {
            UserRole ur = new UserRole();
            ur.setUserId(userId);
            ur.setRoleId(role.getId());
            userRoleMapper.insert(ur);
        }
        evictUserCache(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addUserRoleByCode(Long userId, String roleCode) {
        Role role = roleMapper.selectOne(new LambdaQueryWrapper<Role>()
                .eq(Role::getCode, roleCode)
                .last("LIMIT 1"));
        if (role == null) {
            throw new BusinessException(ResultCode.SERVER_ERROR, "默认角色不存在：" + roleCode);
        }
        long exists = userRoleMapper.selectCount(new LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getUserId, userId)
                .eq(UserRole::getRoleId, role.getId()));
        if (exists > 0) {
            return;
        }
        UserRole ur = new UserRole();
        ur.setUserId(userId);
        ur.setRoleId(role.getId());
        userRoleMapper.insert(ur);
        evictUserCache(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserStatus(Long userId, Integer status, Long currentUserId) {
        User user = requireUser(userId);
        if (Objects.equals(userId, currentUserId)) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "不能修改自己的账号状态");
        }
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "账号状态只能为 0 或 1");
        }
        // 从启用改为禁用，且目标是管理员时，必须保留其他可用管理员
        if (status == 0 && Integer.valueOf(1).equals(user.getStatus())
                && listRoleCodes(userId).contains(ADMIN_CODE)) {
            ensureNotLastAdmin(userId);
        }
        user.setStatus(status);
        userMapper.updateById(user);
        if (status == 0) {
            tokenService.revokeAll(userId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(Long userId, String encryptedPassword) {
        User user = requireUser(userId);
        String rawPassword = loginCryptoService.decrypt(encryptedPassword);
        validatePassword(rawPassword);
        user.setPassword(BCrypt.hashpw(rawPassword));
        userMapper.updateById(user);
        // 密码变更后强制重新登录
        tokenService.revokeAll(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long userId, Long currentUserId) {
        User user = requireUser(userId);
        if (Objects.equals(userId, currentUserId)) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "不能删除自己");
        }
        if (Integer.valueOf(1).equals(user.getStatus()) && listRoleCodes(userId).contains(ADMIN_CODE)) {
            ensureNotLastAdmin(userId);
        }
        userRoleMapper.delete(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId));
        userMapper.deleteById(userId);
        tokenService.revokeAll(userId);
        evictUserCache(userId);
    }

    // ==================== 私有方法 ====================

    /** 读取缓存的编码列表（逗号拼接字符串）；未命中返回 null */
    private List<String> getCachedCodes(String key) {
        Object cached;
        try {
            cached = redisService.get(key);
        } catch (Exception e) {
            // 历史上以错误格式写入的缓存，读取失败时直接清除，不影响正常查询
            redisService.delete(key);
            return null;
        }
        if (cached instanceof String s) {
            return s.isEmpty() ? Collections.emptyList() : Arrays.asList(s.split(","));
        }
        return null;
    }

    /** 用户授权信息变更后，删除其角色/权限码缓存 */
    private void evictUserCache(Long userId) {
        redisService.delete(ROLES_CACHE_KEY + userId);
        redisService.delete(PERMS_CACHE_KEY + userId);
    }

    private User requireUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        return user;
    }

    private List<Role> validateRoleIds(List<Long> roleIds) {
        List<Long> ids = roleIds == null ? Collections.emptyList()
                : roleIds.stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "请至少选择一个角色");
        }
        List<Role> roles = roleMapper.selectBatchIds(ids);
        if (roles.size() != ids.size()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "存在无效的角色");
        }
        return roles;
    }

    private void ensureNotLastAdmin(Long excludeUserId) {
        List<Long> otherAdmins = roleMapper.selectOtherEnabledAdminIds(excludeUserId);
        if (otherAdmins == null || otherAdmins.isEmpty()) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "系统至少保留一名启用中的管理员");
        }
    }

    private void validatePassword(String rawPassword) {
        if (rawPassword == null || rawPassword.length() < 6 || rawPassword.length() > 20) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "密码长度需为 6-20 位");
        }
    }
}
