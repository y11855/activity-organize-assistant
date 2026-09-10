# 大学生软件实训教学 AI 检查评价系统

> 基于前后端分离架构：**Java (SpringBoot) 3 + Vue3**，融合大模型完成学生实训成果的自动化合规校验、步骤完整性检查、逻辑漏洞识别与多维评分。

![License](https://img.shields.io/badge/license-Apache--2.0-blue)
![Java](https://img.shields.io/badge/Java-17-orange)
![SpringBoot](https://img.shields.io/badge/SpringBoot-3.2.5-green)
![Vue](https://img.shields.io/badge/Vue-3.4-brightgreen)
![Vite](https://img.shields.io/badge/Vite-5-646CFF)
![TypeScript](https://img.shields.io/badge/TypeScript-5-3179C6)

## 功能概览

| 模块 | 说明 |
| --- | --- |
| 文件上传与解析 | 支持 Word / PDF / 截图上传，服务端自动抽取文本 |
| AI 合规校验 | 接入本地 / 云端大模型，校验实训步骤合规性、完整性与逻辑漏洞 |
| 评价指标管理 | 自定义指标 + 权重，支持 AI 评分 + 教师人工评分加权 |
| 统计报表 | 带图表的实训统计，支持 PDF / Excel 导出 |
| 权限体系 | 学生 / 教师 / 管理员三角色 |

## 技术栈

- **后端**: Java 17 + SpringBoot 3.2 + MyBatis-Plus + MySQL 8 + Redis + MinIO + Knife4j + WebFlux
- **前端**: Vue3 + TypeScript + Vite 5 + Pinia + Vue Router + Element Plus + ECharts
- **AI**: 可插拔式接入（OpenAI / 通义千问 / 本地 Ollama）
- **构建**: Maven(后端) + pnpm(前端)

## 目录结构

```
template-web/
├── backend/                # SpringBoot 后端
│   ├── pom.xml
│   ├── env.example         # 环境变量模板
│   ├── db/init.sql         # 数据库初始化脚本
│   └── src/main/
│       ├── java/com/aiapp/trainingeval/
│       │   ├── TrainingEvalApplication.java
│       │   ├── config/        # 配置类
│       │   ├── controller/    # 控制层
│       │   ├── service/impl/  # 业务层
│       │   ├── mapper/        # 数据访问层
│       │   ├── entity/        # 数据库实体
│       │   ├── dto/vo/        # 入参 / 出参对象
│       │   ├── common/        # result/exception/utils/enums
│       │   ├── interceptor/   # 拦截器
│       │   └── ai/            # 大模型适配层 (provider/)
│       └── resources/
│           ├── application.yml
│           └── mapper/         # MyBatis XML
└── frontend/              # Vue3 前端
    ├── package.json
    ├── .env.example
    ├── vite.config.ts
    └── src/
        ├── main.ts
        ├── App.vue
        ├── api/            # 接口封装（按模块拆分）
        ├── assets/
        ├── components/     # 通用组件
        ├── composables/    # 组合式函数
        ├── layouts/        # 布局
        ├── router/         # 路由
        ├── stores/         # Pinia
        ├── styles/         # 全局样式
        ├── types/          # TS 类型
        ├── utils/          # 工具
        └── views/          # 页面（按业务模块分目录）
```

## 命名规范

### 后端 (Java)

| 对象 | 规范 | 示例 |
| --- | --- | --- |
| 包名 | 全小写单数 | `com.aiapp.trainingeval.controller` |
| 类名 | 大驼峰 PascalCase | `TrainingServiceImpl` |
| 方法/变量 | 小驼峰 camelCase | `listByStudentId` |
| 常量 | 全大写下划线 | `MAX_UPLOAD_SIZE` |
| 实体类 | 名词单数，`@TableName` 对应下划线表 | `Training` → `training` |
| Controller | `XxxController`，RESTful 路径复数小写 | `/api/trainings` |
| Service | 接口 `XxxService` + 实现 `XxxServiceImpl` | `TrainingService` |
| Mapper | `XxxMapper extends BaseMapper<Xxx>` | `TrainingMapper` |
| DTO/VO | 入参 `XxxQueryDTO` / `XxxSaveDTO`，出参 `XxxVO` | `TrainingSaveDTO` |
| 数据库 | 表/字段全小写下划线，表名单数 | `training` / `student_id` |

### 前端 (Vue3 + TS)

| 对象 | 规范 | 示例 |
| --- | --- | --- |
| 目录/文件 | kebab-case，组件文件用大驼峰 | `user-profile/` / `UserProfile.vue` |
| 组件名 | 大驼峰，多词避免单字母 | `<TrainingList />` |
| 组合式函数 | `useXxx` 前缀 | `useAuth` |
| Pinia store | `useXxxStore` | `useUserStore` |
| API 文件 | 按模块拆分，函数小驼峰 | `training.ts` / `getTrainingList` |
| 路由 path | kebab-case 复数 | `/trainings/:id` |
| TS 类型 | 大驼峰，接口前缀不加 I | `interface Training` |
| CSS class | BEM: `block__element--modifier` | `.training-list__item--active` |
| 常量 | 全大写下划线 | `API_BASE_URL` |

### 通用 Git 规范

- 分支: `main` / `develop` / `feature/xxx` / `fix/xxx` / `release/x.x.x`
- Commit: `type(scope): subject`，type ∈ feat|fix|docs|style|refactor|test|chore|perf
  - 示例: `feat(ai): 接入通义千问合规校验`

## 快速启动

### 方式一：Docker Compose（推荐）

```bash
# 1. 启动 MySQL / Redis / MinIO
docker compose up -d

# 2. 配置后端环境变量
cp backend/env.example backend/.env.local
# 编辑 .env.local 填入 MySQL/Redis/MinIO 密码与大模型 API Key

# 3. 启动后端
cd backend
mvn spring-boot:run          # http://localhost:8080/api

# 4. 启动前端
cd frontend
pnpm install
pnpm dev                     # http://localhost:5173
```

### 方式二：手动准备中间件

如本机已有 MySQL/Redis，直接配置环境变量即可：

```bash
export MYSQL_USER=root
export MYSQL_PASSWORD=your_password
export OPENAI_API_KEY=sk-xxxxxxxx
# ...
```

## 环境变量

### 后端 ([env.example](backend/env.example))

| 变量 | 必填 | 说明 |
| --- | --- | --- |
| `MYSQL_URL` / `MYSQL_USER` / `MYSQL_PASSWORD` | ✅ | 数据库连接 |
| `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD` | ✅ | Redis 连接 |
| `MINIO_*` | 可选 | 仅当文件存储选择 minio 时 |
| `OPENAI_API_KEY` | 二选一 | OpenAI 模式下使用 |
| `DASHSCOPE_API_KEY` | 二选一 | 通义千问模式下使用 |

通过 `training-eval.ai.provider` 切换：`openai` / `qwen` / `ollama`。

### 前端 ([.env.example](frontend/.env.example))

| 变量 | 说明 |
| --- | --- |
| `VITE_API_BASE_URL` | 后端 API 基础地址（开发环境默认 `http://localhost:8080`） |
| `VITE_APP_TITLE` | 浏览器标题 |

## 文档

- 接口文档（启动后端后访问）：http://localhost:8080/api/doc.html
- 贡献指南：[CONTRIBUTING.md](CONTRIBUTING.md)

## 开发计划

- [x] 项目脚手架与命名规范
- [x] AI Provider 适配骨架（OpenAI / Qwen / Ollama）
- [x] Word / PDF 文档解析
- [ ] JWT 鉴权与权限控制
- [ ] 文件上传接口
- [ ] AI 校验完整链路实现
- [ ] 报表导出（PDF / Excel）

## License

本项目基于 [Apache License 2.0](LICENSE) 开源，欢迎学习和二次开发。
