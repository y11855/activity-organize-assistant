# RBAC 权限系统实施计划

## Context（为什么做）

项目已有 Redis token 登录（`AuthInterceptor` + `UserContext`）与统一响应 `Result`，但权限仅有 `user.role` 字符串，前端菜单写死、后端只做“活动创建人”归属校验，没有角色/权限模型、没有后台分配入口。本次按禅道风格补齐多角色 RBAC：
- 角色与权限可配置，后台可给用户分配角色、给角色分配权限；
- 前端菜单/路由按权限码过滤，后端业务接口按权限码拦截（越权返回业务码 403）；
- 后台用户管理支持新建/删除、重置密码、启用/禁用。

已确认决策：**多角色**、**菜单+业务接口都拦**、**Spring Security 最小接入**、后台包含用户增删改与账号状态。

## 一、数据模型

新增四张表（`BaseEntity` 仅用于 role/permission；关联表不继承、无逻辑删除）：

```sql
role(id PK, code UNIQUE, name, description, builtin TINYINT, create_time, update_time, deleted)
permission(id PK, code UNIQUE, name, group_name, sort, create_time, update_time, deleted)
user_role(user_id, role_id)          -- 联合主键
role_permission(role_id, permission_id) -- 联合主键
```

`user` 表变更：新增 `status TINYINT NOT NULL DEFAULT 1`（1 启用 / 0 禁用）；把 `user.role` 数据迁入 `user_role` 后 **DROP COLUMN role**，实体 `User` 同步删除 `role` 字段（避免双份事实来源）。

种子数据（固定 ID，便于迁移）：
- 角色：1 `admin` 系统管理员（builtin=1）、2 `leader` 活动负责人（builtin=1）、3 `member` 普通成员（builtin=1）
- 权限码（1-8）：
  `activity:view`、`task:view`、`questionnaire:view`、`reminder:view`、
  `system:user:view`、`system:user:manage`、`system:role:view`、`system:role:manage`
- 授权：admin 全部 8 个；leader/member 先给 4 个业务 view（后台可再调整）
- 账号迁移：`INSERT IGNORE INTO user_role SELECT u.id, CASE u.role WHEN 'admin' THEN 1 WHEN 'leader' THEN 2 ELSE 3 END FROM user u;`，无映射用户兜底 member

## 二、后端改造

1. **依赖与安全配置**
   - `pom.xml` 增加 `spring-boot-starter-security`。
   - 新增 `config/SecurityConfig`：`@EnableWebSecurity` + `@EnableMethodSecurity`；`SecurityFilterChain` 设 `csrf.disable`、`sessionCreationPolicy(STATELESS)`、关闭 formLogin/httpBasic/logout、`requestCache.disable`、`anyRequest().permitAll()`。**不调用 `.cors()`**（项目无 CorsConfigurationSource）。
   - 认证仍由 `AuthInterceptor` + Redis token 负责，不改登录入口；Security 仅用于 `@PreAuthorize` 鉴权。

2. **实体/Mapper**
   - 新增 `entity/Role`、`Permission`、`UserRole`、`RolePermission` 与对应 Mapper。
   - `RoleMapper`/`PermissionMapper` 用 `@Select` 提供联表查询：
     - `selectRoleCodesByUserId(userId)`
     - `selectPermissionCodesByUserId(userId)`（user→user_role→role→role_permission→permission，`DISTINCT`，过滤 deleted/status）
   - **不做 Redis 权限缓存**：每请求一次聚合查询，授权变更立即生效，避免集群缓存版本问题。

3. **认证链路**
   - `UserContext` 扩展：在 userId 之外保存 `roles`/`permissions`，提供 `hasPermission()`；`clear()` 一并清理。
   - `AuthInterceptor.preHandle`：token→userId 后加载用户，`status=0` 或用户已删则返回 403「账号已被禁用」；查询角色与权限码写入 `UserContext`，并设置 `SecurityContextHolder` 的 `UsernamePasswordAuthenticationToken(userId, null, authorities)`；`afterCompletion` 清理 UserContext（SecurityContext 由过滤器清理，另加 `clearContext()` 保险）。
   - 若角色包含 `admin`，在服务层直接返回全量权限码集合（防数据缺失把管理员锁死）。
   - `LoginVO` 增加 `roles: List<String>`、`permissions: List<String>`（移除单个 `role`）；`AuthService.login` 校验 `status`；`issueToken` 与 `currentUser` 统一填充角色/权限；`register` 改为写入 `user_role`（member）。
   - **token 吊销**：`AuthService` 维护 `login:user-tokens:{userId}` Set；登出移除；重置密码/禁用/删除时遍历删除该用户全部 token（`RedisService` 增加 sAdd/sMembers/sRemove 封装）。

4. **接口鉴权（只加在 Controller，不能加在 Service/@Async 线程）**
   - `ActivityController` → `@PreAuthorize("hasAuthority('activity:view')")`
   - `TaskController` → `task:view`；`QuestionnaireController` → `questionnaire:view`；`ReminderController` → `reminder:view`
   - `GET /users`（成员选择器）保持仅登录可访问，**不加** `system:user:view`，否则普通成员无法建活动。
   - 后台接口：
     - `GET /admin/users`、`GET /admin/roles`、`GET /admin/permissions` → 对应 `system:*:view`
     - 用户增删/改角色/重置密码/启停、角色增删改/授权 → 对应 `system:*:manage`
   - `GlobalExceptionHandler` 显式增加 `@ExceptionHandler(AccessDeniedException.class)` → `Result.fail(FORBIDDEN)`（否则被兜底 500 吞掉）；`ResultCode` 增加账号禁用/角色占用等业务码（1006 起）。

5. **后台服务与接口**
   - 新增 `AdminUserController`（`/admin/users`）：
     - 分页列表（keyword/status 过滤，返回 roles/roleNames/status/createTime，不含密码）
     - 新建用户（复用 RSA 解密 + 6-20 位 + BCrypt；用户名沿用注册正则；roleIds 至少一个）
     - 修改角色（`roleIds` 全量替换）、启用/禁用、重置密码、逻辑删除
   - 新增 `AdminRoleController`（`/admin/roles`）：
     - 角色列表（含 permissionCodes、userCount）、新建/改名/删除、全量替换角色权限
   - 防锁死校验（`@Transactional`，计数查询带 `FOR UPDATE`）：
     - 不能改自己的角色/状态、不能删除自己
     - 不能让系统失去最后一个“启用中的 admin 用户”
     - 内置角色（admin/leader/member）不可删除、code 不可改；admin 角色权限不可修改
     - 角色仍被用户占用时不可删除
     - 分配/新建时校验 roleIds 均存在

## 三、前端改造

- `types/index.ts`：`UserInfo` 增加 `permissions: string[]`；新增 `RoleVO`、`PermissionVO`、`UserAdminVO`。
- `stores/user.ts`：增加 `permissions` getter、`hasPermission(code)`、`hasAnyPermission(codes)`、`refreshUserInfo()`（调 `GET /auth/current`）。
- `Login.vue` / `Register.vue`：写入 `roles: res.roles`、`permissions: res.permissions`。
- `router/index.ts`：
  - 新增 `/system/users`（`meta.permission='system:user:view'`）、`/system/roles`（`system:role:view`）、隐藏的 `/403` 页面。
  - 守卫：已登录但 store 无 permissions 时先 `refreshUserInfo()`；路由有 `meta.permission` 且无权限 → `next('/403')`（避免与 `/activities` 互相重定向）；刷新失败则登出。
- `BasicLayout.vue`：菜单过滤条件改为 `!hidden && (!meta.permission || hasPermission(meta.permission))`；`onMounted` 刷新一次当前用户权限。
- 新增 `api/admin.ts` 与页面：
  - `views/system/UserManage.vue`：搜索 + 分页表格（用户名/昵称/角色标签/状态开关/创建时间），新建用户弹窗、分配角色弹窗（多选）、重置密码弹窗、删除确认。
  - `views/system/RoleManage.vue`：左侧角色列表（新增/删除），右侧按 `groupName` 分组的权限复选框 + 保存；内置标签、admin 权限只读。
- 复用现有 `encryptPassword`、`PageVO`、`el-pagination` 写法。

## 四、数据库脚本

- 新增 `backend/db/upgrade-2026-10-25-rbac.sql`（按顺序）：
  1. `CREATE TABLE IF NOT EXISTS` 四张新表
  2. `user` 增加 `status`
  3. 种子数据（`ON DUPLICATE KEY UPDATE`，固定 ID）
  4. `INSERT IGNORE` 把 `user.role` 迁入 `user_role`，并为无映射用户补 member
  5. `ALTER TABLE user DROP COLUMN role`
- 同步修改 `backend/db/init.sql`：user 表去 role、加 status；新增四张表与种子；用户初始化数据后补 `user_role` 插入。
- 执行注意：**先停后端再跑迁移**（旧代码仍读 `user.role`），脚本对 DROP COLUMN 只跑一次；本地库先备份。

## 五、顺带清理残留

- 删除 `frontend/src/views/tree/TreeDemo.vue`、`frontend/src/components/TreeView.vue` 与 `router/index.ts` 中 `/tree` 路由（活动分解树仅剩前端演示页，属实训残留）。

## 六、验证清单

1. `mvn clean compile`、`npx vue-tsc --noEmit` 通过。
2. 停后端 → 执行迁移 → 启动；`admin/123456` 登录返回 8 个权限、roles=[admin]。
3. member 账号：菜单只显示有权限的项；直链 `/system/users` 跳 403；`curl /api/admin/users` 返回业务码 403；`/api/activities` 正常。
4. 后台：新建用户→分配 leader→重置密码→用新密码登录；禁用后旧 token 立即 401；启用恢复。
5. 新建自定义角色只勾 `activity:view`，分配给测试用户，登录后仅见“我的活动”。
6. 防锁死：改自己角色、禁用最后一个 admin、删除被占用角色、修改 admin 权限均返回明确错误。
7. 清理冒烟测试用户/角色与活动数据。

## 七、实施顺序

1. SQL 脚本 + `pom.xml` + `SecurityConfig`
2. 实体/Mapper + `RbacService`（含防锁死校验）
3. `UserContext`/`AuthInterceptor`/`AuthService`/`LoginVO`/`UserContext` 改造 + token 吊销
4. Controller 鉴权注解 + `AdminUserController`/`AdminRoleController` + `GlobalExceptionHandler`
5. 前端 types/store/router/layout + `api/admin.ts` + 两个后台页面
6. 删除 TreeDemo 残留
7. 迁移数据库、编译、端到端冒烟
