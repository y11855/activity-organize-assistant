# 任务分解（Tasks）

依据 `spec.md` 执行，顺序即依赖关系。

## 阶段一：数据库
- [x] T1 编写并执行 `backend/db/upgrade-2026-10-14.sql`：questionnaire.cover_image；activity.expected_count + member_ids；message 表；测试用户（id 2/3/4/5，密码 123456）
- [x] T2 同步修改 `backend/db/init.sql`（表结构 + 种子用户）

## 阶段二：真实登录
- [x] T3 `UserMapper` + `UserService(Impl)`（getByUsername、listAll）
- [x] T4 `AuthController`（login/logout/current）+ `LoginDTO/LoginVO`；token=UUID 存 Redis（24h）
- [x] T5 `AuthInterceptor` + `WebMvcConfig`（白名单：/auth/login、/uploads/**、knife4j、druid、error）；`UserContext` 无上下文抛 401；`ActivityAgent.run` 显式设置/清理创建人上下文
- [x] T6 `UserController`：`GET /users` 成员列表
- [x] T7 前端 `api/auth.ts` + `api/user.ts` + types；`Login.vue` 改真实登录

## 阶段三：上传 + 问卷封面
- [x] T8 `FileController`（类型/大小校验、UUID 落盘 backend/uploads/）+ `WebMvcConfig` 资源映射 `/uploads/**`
- [x] T9 后端问卷：实体/VO 加 coverImage；`QuestionnaireSaveDTO` + `PUT /questionnaires/{id}`；`GET /questionnaires/mine`；`GET /questionnaires/{id}`；`POST /questionnaires/{id}/responses`
- [x] T10 前端 `api/file.ts` + questionnaire api 扩展；`QuestionnaireList.vue` 重建（封面卡片 + 编辑对话框：前端格式/大小校验、预览/替换/删除）
- [x] T11 `QuestionnaireFill.vue`（封面 + 答题 + 提交）+ 路由登记

## 阶段四：我的任务 + 消息
- [x] T12 后端：`GET /tasks/mine?status=`（MyTaskVO 含 activityTitle）；`complete` 增加负责人权限校验（403）
- [x] T13 message 表实体/Mapper/Service/Controller（mine、unread-count、read、read-all）
- [x] T14 `POST /activities/{id}/confirm-tasks`：按负责人分组批量发消息
- [x] T15 前端 `TaskList.vue` 重建（筛选/排序/标记完成）
- [x] T16 前端 `api/message.ts` + `BasicLayout.vue` 铃铛 + 未读角标 + 消息抽屉（30s 轮询）

## 阶段五：活动创建增强 + AI 分工
- [x] T17 后端：Activity 实体/DTO 加 expectedCount/memberIds；`Planner` 成员名单 prompt + assigneeId 映射 + deadlineOffsetHours 换算
- [x] T18 前端：`ActivityList.vue` 创建对话框加人数 + 成员多选；`ActivityDetail.vue` 展示人数/成员 + 任务状态 tag + 「确认分工并通知成员」按钮

## 阶段六：验证
- [x] T19 `mvn -q compile` 通过；前端 `npm run build` 通过
- [x] T20 启动后端，PowerShell 逐项 API 测试：登录/401/任务隔离/403/上传校验/封面回显/确认分工→消息到达
- [x] T21 前端关键页面走查（登录、问卷封面、我的任务、消息抽屉、活动创建对话框、活动详情页）
- [ ] T22 汇总结果并询问用户是否提交 git

## 验证中追加修复
- [x] F1 DTO 日期反序列化：ActivitySaveDTO/TaskAssignDTO/ReminderCreateDTO 加 `@JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")`，兼容 el-date-picker 空格分隔格式
- [x] F2 雪花 ID 前端精度丢失：新增 `JacksonConfig` 全局 Long→String 序列化；前端 types/api 的 id 字段同步改 string，路由参数改 String(route.params.id)
- [x] F3 start.ps1 需在 backend 目录执行；启动前清理 8080 占用
