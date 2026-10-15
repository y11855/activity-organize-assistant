# 验收清单（Checklist）

## 登录
- [x] admin/zhangsan/lisi 可用 123456 登录，返回 token（Redis 24h）
- [x] 未登录/无效 token 访问受保护接口返回 code=401，前端自动跳登录页
- [x] 登出后 token 失效

## 问卷封面
- [x] 编辑对话框支持选择 JPG/PNG/WebP，前端校验格式与 5MB 大小并给出明确错误提示
- [x] 上传成功即时预览，支持替换与删除
- [x] 封面 URL 保存到 questionnaire.cover_image，列表页卡片与填写页正确展示
- [x] 后端二次校验（空文件/5MB/扩展名），上传文件可通过 /api/uploads/** 访问（HTTP 200）

## 我的任务（权限隔离）
- [x] GET /tasks/mine 仅返回当前用户被分配的任务（zhangsan/lisi 互不可见）
- [x] 越权完成他人任务返回 403「只能操作分配给自己的任务」
- [x] 任务条目含任务内容、截止时间、完成状态、交付要求、所属活动
- [x] 前端支持状态筛选、截止升/降序排序、标记完成

## 活动创建 + AI 分工 + 消息
- [x] 创建活动支持人工设置预计人数（expectedCount）与页面选择成员账号（memberIds）
- [x] AI 编排生成的任务全部落在所选成员名单内（E2E：4 任务 → 张三/李四）
- [x] 发起人确认分工后（confirm-tasks），被分配成员收到站内消息（含任务清单/截止时间/交付要求），E2E notified=2，两成员均查到消息
- [x] 非创建人调用 confirm-tasks 返回 1005 无权操作
- [x] 消息铃铛未读角标、抽屉列表、单条/全部已读正常

## 工程质量
- [x] 后端 mvn compile 通过（JDK 21）
- [x] 前端 npm run build（vue-tsc）通过
- [x] 雪花 ID 以字符串序列化，前端详情页/填写页路由 id 精度不丢失，详情页正常渲染
- [x] 浏览器走查无红色 console 报错
