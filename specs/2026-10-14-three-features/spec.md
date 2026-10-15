# 三大功能模块实施规格（Spec）

日期：2026-10-14
项目：活动管家（e:\template-web，Spring Boot 3.2.5 + Vue3 + Element Plus）

## 目标

1. **报名问卷封面上传**：创建/编辑问卷时上传封面图（JPG/PNG/WebP，≤5MB），支持预览/替换/删除；问卷列表页与填写页展示封面。
2. **任务分工个人查看**：每个参与人只能查看/管理分配给自己的任务（内容、截止时间、完成状态、交付要求），支持状态筛选、排序、标记完成；权限隔离。
3. **完善我的活动 + 真实登录**：替换 mock 登录为账号密码真实登录；创建活动时可手动设置人数并直接选择成员账号；AI 生成分工后，发起人确认即向被选成员发送站内消息。

## 关键设计决策

### A. 真实登录（Token + Redis）

- 不引入 Spring Security 全家桶，使用轻量方案：
  - `POST /auth/login`：username+password → hutool `BCrypt.checkpw` 校验 → 生成 UUID token，存 Redis `login:token:{token}` → userId，TTL 24h；返回 `{token, userInfo}`。
  - `POST /auth/logout`：删除 Redis key。
  - `GET /auth/current`：返回当前用户信息。
- `AuthInterceptor`（HandlerInterceptor）：除白名单外所有请求校验 `Authorization: Bearer {token}`，查 Redis，命中则 `UserContext.setCurrentUserId` 并刷新 TTL，请求结束 `clear()`。白名单：`/auth/login`、`/uploads/**`、Knife4j 文档、`/druid/**`、`/error`。
- `UserContext.getCurrentUserId()` 改为：无上下文时抛 `BusinessException(UNAUTHORIZED)`（不再默认返回 1L）。
- **异步线程补偿**：`ActivityAgent.run` 开头 `UserContext.setCurrentUserId(activity.getCreatorId())`，finally 中 `clear()`（异步线程无请求上下文）。
- 密码哈希：复用已有 BCrypt 哈希 `$2a$10$2GpwFmBnAa9WCxIvp0otQesHl39jYCAMG9YlYiuAezvH7R./dsw8a`（=123456）。运行时若校验失败，用 hutool 现场生成新哈希并修正 SQL。
- 新增 `UserMapper` + `UserService`（getByUsername / listAll）。
- 新增 `UserController`：`GET /users` 返回全部用户 `{id, username, nickname}`，供成员选择器使用（需登录）。

### B. 文件上传（本地磁盘）

- 不启用 MinIO，采用本地磁盘最简方案：
  - `FileController`：`POST /files/upload`（MultipartFile，参数名 `file`）。
  - 校验：扩展名 ∈ {jpg, jpeg, png, webp} 且大小 ≤ 5MB，失败抛 BusinessException 并给出明确中文提示。
  - 存储：`backend/uploads/` 目录，文件名 `UUID.扩展名`。
  - 返回：`{url: "/api/uploads/{filename}"}`（经 Vite 代理 `/api` 可直连；context-path=/api 与之一致）。
  - `WebMvcConfig` 注册资源映射 `/uploads/**` → `file:uploads/`（工作目录为 backend/）。

### C. 问卷封面

- DB：`ALTER TABLE questionnaire ADD COLUMN cover_image VARCHAR(512) NULL COMMENT '封面图URL'`。
- `Questionnaire` 实体、`QuestionnaireVO` 增加 `coverImage`。
- 新增 `QuestionnaireSaveDTO`（title/description/questions/coverImage）与 `PUT /questionnaires/{id}`（校验活动归属）。
- 新增 `GET /questionnaires/mine`：当前用户所有活动下的问卷列表（含封面、报名数）。
- 新增 `GET /questionnaires/{id}`：填写页用详情（登录即可，不限创建人）。
- 新增 `POST /questionnaires/{id}/responses`：提交答卷（`QuestionnaireResponse` 落库，respondent 取当前用户）。

### D. 我的任务（权限隔离）

- `GET /tasks/mine?status=`：仅返回 `assignee_id = 当前用户` 的任务，按截止时间升序；status 可选过滤。返回 `MyTaskVO`（含 activityTitle，批量查 activity 填充）。
- `PUT /tasks/{id}/complete` 增加权限：仅任务负责人本人可标记完成，否则 403（assigneeId 为空时允许活动创建人操作）。
- 前端 TaskList.vue 重建为「我的分工」：状态筛选、截止正/倒序切换、标记完成按钮。

### E. 消息中心

- 新表 `message`：`id, user_id, title, content, activity_id, read_status(0未读/1已读)` + 审计字段 + 逻辑删除。
- `MessageService`：send（单发/批量）、listMine、unreadCount、markRead、markAllRead。
- `MessageController`：`GET /messages/mine`、`GET /messages/unread-count`、`PUT /messages/{id}/read`、`PUT /messages/read-all`。
- 确认分工：`POST /activities/{id}/confirm-tasks`（仅创建人）→ 汇总该活动任务，按负责人分组，为每位负责人插入一条消息（含其任务清单、截止时间、交付要求）。

### F. 活动创建：人数 + 成员选择 + AI 分工

- DB：`ALTER TABLE activity ADD COLUMN expected_count INT NULL, ADD COLUMN member_ids VARCHAR(512) NULL COMMENT '选择成员ID JSON数组'`。
- `ActivitySaveDTO` 增加 `expectedCount`、`memberIds: List<Long>`；`Activity` 实体同步。
- `Planner.plan`：若 activity.memberIds 非空，查 User 表得到 `id:昵称` 名单，注入 prompt，要求 AI 在成员间分配任务并返回 `assigneeId/assigneeName/deadlineOffsetHours`（相对当前的小时数，Agent 换算为 deadline = now + offset）。兜底逻辑保持不崩。
- 前端创建对话框：增加「预计人数」el-input-number +「参与成员」el-select multiple（数据源 `GET /users`）。
- ActivityDetail：展示人数与成员、任务状态用 el-tag 渲染、新增「确认分工并通知成员」按钮 → 调 confirm-tasks → 提示已通知。

### G. 前端页面

- `Login.vue`：真实登录表单（校验非空、loading、错误提示由拦截器弹出），成功后存 token+userInfo（role 包装为 roles 数组）跳转 /activities。
- `api/auth.ts`（login/logout/current）、`api/user.ts`（listUsers）、`api/file.ts`（uploadFile，FormData）、`api/message.ts`；types 增补。
- `QuestionnaireList.vue` 重建：卡片网格（el-image 封面 + 默认占位 + 状态标签 + 报名数），编辑对话框（封面上传：前端先校验格式/大小并给明确提示，预览/替换/删除），发布/截止按钮。
- 新增 `QuestionnaireFill.vue`（路由 `/questionnaires/:id/fill`）：顶部封面图 + 标题描述 + 渲染题目（single→radio，text→textarea）+ 提交。
- `TaskList.vue` 重建（见 D）。
- `BasicLayout.vue`：头部右侧加消息铃铛 + el-badge 未读数，点击开 el-drawer 消息列表（点击标记已读、全部已读）；30s 轮询未读数。
- `ActivityList.vue` 创建对话框扩展（见 F）。
- 路由：登记 fill 页面（hidden）。

### H. 数据库变更（一个升级脚本 + 同步 init.sql）

新建 `backend/db/upgrade-2026-10-14.sql`：
1. `ALTER TABLE questionnaire ADD cover_image`；
2. `ALTER TABLE activity ADD expected_count, member_ids`；
3. `CREATE TABLE message`；
4. 补充测试用户 id=2 张三 / 3 李四 / 4 王五 / 5 赵六（username 同拼音，密码均为 123456 的 BCrypt，与 task_assignment 既有 assignee_id 对齐）。
同步修改 `backend/db/init.sql` 对应表结构与种子用户（保证全新部署一致）。

## 受影响文件清单

**后端新增**：`controller/{AuthController,UserController,FileController,MessageController}.java`、`config/{AuthInterceptor,WebMvcConfig}.java`、`service/{UserService,MessageService}.java` + impl、`mapper/{UserMapper,MessageMapper}.java`、`entity/Message.java`、`dto/{LoginDTO,QuestionnaireSaveDTO,ResponseSubmitDTO}.java`、`vo/{LoginVO,UserSimpleVO,MyTaskVO,MessageVO}.java`
**后端修改**：`UserContext`、`ActivityAgent`（上下文补偿）、`Activity`/`ActivitySaveDTO`（expectedCount/memberIds）、`Questionnaire`/`QuestionnaireVO`、`QuestionnaireController/Service(Impl)`（update/mine/detail/responses）、`TaskController/Service(Impl)`（mine + complete 权限）、`Planner`（成员 prompt + assigneeId 映射 + deadline 换算）、`db/init.sql`
**前端新增**：`api/{auth,user,file,message}.ts`、`views/questionnaire/QuestionnaireFill.vue`
**前端修改**：`Login.vue`、`QuestionnaireList.vue`、`TaskList.vue`、`ActivityList.vue`、`ActivityDetail.vue`、`BasicLayout.vue`、`router/index.ts`、`types/index.ts`、`api/{questionnaire,task,activity}.ts`

## 验证方案

1. `mvn -q compile`（JAVA_HOME 指向 JDK 21，dangerouslyDisableSandbox）。
2. 前端 `npm run build` 做类型检查。
3. 启动后端（backend/start.ps1，后台），用 PowerShell `Invoke-RestMethod` 逐项验证：
   - 登录成功/失败；无 token 访问受保护接口 → 401；
   - admin 与张三分别登录，`/tasks/mine` 互不可见对方任务；张三标记自己任务完成成功、标记他人任务 → 403；
   - 上传 jpg/png/webp 正常返回 URL；上传 gif 或 >5MB 文件 → 明确错误提示；
   - 问卷保存封面后 `/questionnaires/mine` 返回 coverImage；
   - 创建活动带 memberIds → 等 Agent 完成 → confirm-tasks → 张三/李四登录 `/messages/mine` 收到各自任务消息。
4. 前端页面走查（登录页、问卷卡片封面、我的任务、消息抽屉）。

## 明确不做

- 不引入 Spring Security / JWT 框架；不启用 MinIO；不做注册功能；不改动 ActivityServiceImpl 的 @Lazy 循环依赖处理与 ReminderScheduler 逻辑；不改 application-dev.yml 密文。
