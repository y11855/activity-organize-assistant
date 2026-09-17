# 社团/班级活动组织 AI 助手（活动管家）

> **一句话发起活动，全流程自动跑。** 基于 Agent 编排（规划→执行→交付）+ 工具调用（日历、表单、消息）+ 定时任务，帮学生干部自动完成活动策划、报名问卷、任务分工、提醒催办与复盘总结。

![License](https://img.shields.io/badge/license-Apache--2.0-blue)
![Java](https://img.shields.io/badge/Java-17-orange)
![SpringBoot](https://img.shields.io/badge/SpringBoot-3.2.5-green)
![Vue](https://img.shields.io/badge/Vue-3.4-brightgreen)
![Vite](https://img.shields.io/badge/Vite-5-646CFF)

## 核心功能

| 功能 | 说明 |
| --- | --- |
| 活动策划与物料清单 | 一句话描述活动，AI 自动生成策划文案与物料清单 |
| 报名问卷生成与统计 | 自动生成问卷题目，支持发布/截止与报名统计 |
| 任务分工派发到人 | AI 规划任务并派发到指定负责人 |
| 活动前定时提醒 | 可配置触发时间的提醒，**支持取消**，避免打扰 |
| 活动后复盘总结 | AI 生成亮点/问题/改进建议 |

## 技术栈

- **后端**: Java 17 + SpringBoot 3.2 + MyBatis-Plus + MySQL 8 + Redis
- **前端**: Vue3 + TypeScript + Vite 5 + Pinia + Vue Router + Element Plus
- **AI Agent**: 规划(Planner) → 执行(ToolDispatcher) → 交付(Deliverer) 三段编排
- **工具调用**: Calendar / Form / Message 三类工具，统一调度
- **定时任务**: Spring Scheduling 每分钟扫描到期提醒

## 工程化要点

| 要点 | 实现 |
| --- | --- |
| **工具调用幂等** | [ToolDispatcher](backend/src/main/java/com/aiapp/activityassistant/tool/ToolDispatcher.java) 基于 `toolName + params` 生成 SHA-256 幂等键，命中成功记录直接返回缓存结果 |
| **失败重试** | 同一调度器内按 `retry-count` 重试，全部失败后落库 `tool_call_log` |
| **权限控制** | [ActivityService.checkOwnership](backend/src/main/java/com/aiapp/activityassistant/service/impl/ActivityServiceImpl.java) 校验 `creatorId == 当前用户`，只能操作自己的活动 |
| **提醒可取消** | [ReminderService.cancel](backend/src/main/java/com/aiapp/activityassistant/service/impl/ReminderServiceImpl.java) 将状态置为已取消，调度器只扫描 `status=0` 的记录 |

## 目录结构

```
template-web/
├── backend/
│   ├── pom.xml
│   ├── env.example
│   ├── db/init.sql
│   └── src/main/java/com/aiapp/activityassistant/
│       ├── ActivityAssistantApplication.java
│       ├── agent/              # Agent 编排层
│       │   ├── ActivityAgent.java      # 主编排：规划→执行→交付
│       │   ├── Planner.java            # 规划器（调大模型生成策划/物料/问卷/任务）
│       │   ├── Deliverer.java          # 交付器（创建定时提醒）
│       │   └── PlannerResult.java
│       ├── tool/               # 工具调用层
│       │   ├── Tool.java
│       │   ├── ToolDispatcher.java     # 调度器：幂等 + 重试
│       │   ├── CalendarTool.java
│       │   ├── FormTool.java
│       │   └── MessageTool.java
│       ├── schedule/           # 定时任务
│       │   └── ReminderScheduler.java  # 每分钟扫描到期提醒
│       ├── common/             # 通用层（result/exception/context/utils）
│       ├── config/             # 配置类
│       ├── ai/                 # 大模型适配（OpenAI/Qwen/Ollama）
│       ├── controller/         # REST 控制器
│       ├── service/impl/       # 业务层
│       ├── mapper/             # 数据访问层
│       ├── entity/             # 数据库实体
│       └── dto/vo/             # 入参/出参对象
└── frontend/
    ├── package.json
    └── src/
        ├── api/                # 接口（activity/task/questionnaire/reminder）
        ├── views/              # 页面
        │   ├── activity/       # 活动列表 + 详情
        │   ├── task/           # 任务分工
        │   ├── questionnaire/  # 报名问卷
        │   └── reminder/       # 定时提醒
        └── ...
```

## 命名规范

### 后端 (Java)

| 对象 | 规范 | 示例 |
| --- | --- | --- |
| 包名 | 全小写单数 | `com.aiapp.activityassistant.controller` |
| 类名 | PascalCase | `ActivityAgent`、`ToolDispatcher` |
| 方法/变量 | camelCase | `checkOwnership` |
| 常量 | 全大写下划线 | `MAX_RETRY_COUNT` |
| Controller | `XxxController`，路径复数小写 | `/activities` |
| Service | `XxxService` + `XxxServiceImpl` | `ActivityService` |
| Mapper | `XxxMapper extends BaseMapper<Xxx>` | `ActivityMapper` |
| DTO/VO | `XxxSaveDTO` / `XxxVO` | `ActivitySaveDTO` |
| 数据库 | 表/字段全小写下划线，表名单数 | `activity` / `creator_id` |

### 前端 (Vue3 + TS)

| 对象 | 规范 | 示例 |
| --- | --- | --- |
| 目录/文件 | kebab-case，组件 PascalCase | `ActivityDetail.vue` |
| 组合式函数 | `useXxx` | `useAuth` |
| Pinia store | `useXxxStore` | `useUserStore` |
| API | 按模块拆分，函数小驼峰 | `activity.ts` / `createActivity` |
| TS 类型 | PascalCase | `interface ActivityVO` |
| CSS class | BEM | `.activity-list__header` |

### Git 规范

- 分支: `feature/xxx` / `fix/xxx` / `refactor/xxx`
- Commit: `type(scope): subject`，type ∈ feat|fix|docs|style|refactor|test|chore|perf

## 快速启动

```bash
# 1. 启动中间件（MySQL / Redis / MinIO）
docker compose up -d

# 2. 配置环境变量
cp backend/env.example backend/.env.local   # 填入 MySQL 密码与 AI API Key

# 3. 启动后端
cd backend
mvn spring-boot:run          # http://localhost:8080/api

# 4. 启动前端
cd frontend
pnpm install
pnpm dev                     # http://localhost:5173
```

## 环境变量

### 后端 ([env.example](backend/env.example))

| 变量 | 必填 | 说明 |
| --- | --- | --- |
| `MYSQL_URL` / `MYSQL_USER` / `MYSQL_PASSWORD` | ✅ | 数据库连接 |
| `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD` | ✅ | Redis 连接 |
| `OPENAI_API_KEY` / `DASHSCOPE_API_KEY` | 二选一 | 大模型密钥 |

通过 `activity-assistant.ai.provider` 切换：`openai` / `qwen` / `ollama`。

## 文档

- 接口文档：启动后端后访问 http://localhost:8080/api/doc.html
- 贡献指南：[CONTRIBUTING.md](CONTRIBUTING.md)

## License

本项目基于 [Apache License 2.0](LICENSE) 开源。
