# 贡献指南

感谢你愿意为本项目贡献代码或建议！在开始前请阅读以下规范。

## 一、开发环境

| 工具 | 版本 |
| --- | --- |
| JDK | 17+ |
| Node.js | 18+ |
| pnpm | 8+ |
| Maven | 3.8+ |
| MySQL | 8.0+ |
| Redis | 6+ |

## 二、本地启动

1. Fork 仓库并 clone 到本地
2. 复制环境变量模板并填入真实值：

   ```bash
   cp backend/env.example backend/.env.local
   cp frontend/.env.development frontend/.env.local
   ```

3. 启动中间件（推荐使用 `docker-compose up -d`）
4. 启动后端：`cd backend && mvn spring-boot:run`
5. 启动前端：`cd frontend && pnpm install && pnpm dev`

## 三、提交规范

### 分支命名

- `feature/<简述>` 新功能
- `fix/<简述>` 修复 Bug
- `refactor/<简述>` 重构
- `docs/<简述>` 文档

### Commit 信息

采用 [Conventional Commits](https://www.conventionalcommits.org/)：

```
type(scope): subject

body（可选）
```

| type | 说明 |
| --- | --- |
| feat | 新功能 |
| fix | Bug 修复 |
| docs | 文档变更 |
| style | 代码格式（不影响逻辑） |
| refactor | 重构 |
| test | 测试相关 |
| chore | 构建/工具 |
| perf | 性能优化 |

`scope` 可填：`ai` / `backend` / `frontend` / `docs` / `ci` 等。

示例：`feat(ai): 接入通义千问合规校验`

## 四、代码规范

- 后端：包名小写、类名 PascalCase、方法 camelCase；遵循项目已有的分层（controller/service/mapper）
- 前端：目录 kebab-case、组件 PascalCase、组合式函数 `useXxx`
- 使用 `pnpm lint` 与 `mvn -q -DskipTests compile` 进行自检
- 提交前请确保没有引入新的 warning

## 五、Pull Request 流程

1. 从 `develop`（或 `main`）切出新分支
2. 一次 PR 只做一件事，保持可回滚
3. PR 描述请说明：
   - 解决的问题 / 实现的功能
   - 测试方法
   - 关联 Issue（如 `Closes #12`）
4. 通过 CI 检查 + 至少一位 Reviewer 通过后合并

## 六、Issue

- Bug 报告请使用 `.github/ISSUE_TEMPLATE/bug_report.md` 模板
- 新需求请使用 `feature_request.md` 模板
- 重复 Issue 请帮忙在评论中标注 `Duplicate of #xxx`

## 七、行为准则

请保持友善、尊重所有贡献者。欢迎任何背景的开发者参与。
