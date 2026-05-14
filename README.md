# JianDou（煎豆）

JianDou（煎豆）是一个面向视频内容生产的文本到视频工作台。

## 社区与支持

- QQ 交流群：`1090387362` 

## 用户使用指南

- [用户使用指南](./docs/USER_GUIDE.md)：包含 API Key 配置、Docker Compose 启动、本地开发启动和常用访问地址说明。

## 主要特色功能

- TXT 小说 / 正文直达生成：支持上传 TXT 或直接粘贴正文，在同一工作台内完成文本输入、提示词生成和任务创建。
- 四段模型链路可配置：文本模型、视觉模型、关键帧模型、视频模型独立选择，便于按厂商和能力灵活组合。
- 输出参数动态约束：支持画幅、清晰度、输出数量、时长区间、Seed 等控制项，并会根据当前视频模型能力自动过滤可选配置。
- 实时任务进度追踪：创建任务后可持续查看阶段进度、状态、Trace 数量、耗时、更新时间和视频预览。
- 任务管理与运维动作：支持任务列表筛选、详情查看，以及重试、暂停、继续、终止、删除、评分等操作。
- 高分 Seed 复用：自动汇总高评分任务中的可用 Seed，一键回填到当前任务，提升复用效率和出片稳定性。
- 前台工作台 + 独立后台：同时提供用户前台、独立后台以及 Spring Boot 3 后端服务，便于内容生产和管理运维分层协作。

## 贡献与反馈

欢迎为 JianDou 做出贡献！如果你发现 bug、有功能建议或想参与开发，请通过以下方式参与：

- **报告 Bug**：在 [Issues](https://github.com/imi4u36d/JianDou/issues) 中提交问题描述，尽量包含复现步骤和环境信息。
- **功能建议**：在 [Issues](https://github.com/imi4u36d/JianDou/issues) 中描述你的需求和预期效果，我们会评估并讨论实现方案。
- **提交代码**：Fork 仓库 → 创建分支 → 提交 Pull Request。请确保代码通过测试，并遵循项目的代码风格。

## Star History

<a href="https://www.star-history.com/?repos=imi4u36d%2FJianDou&type=date&legend=top-left">
  <picture>
    <source media="(prefers-color-scheme: dark)" srcset="https://api.star-history.com/chart?repos=imi4u36d/JianDou&type=date&theme=dark&legend=top-left" />
    <source media="(prefers-color-scheme: light)" srcset="https://api.star-history.com/chart?repos=imi4u36d/JianDou&type=date&legend=top-left" />
    <img alt="Star History Chart" src="https://api.star-history.com/chart?repos=imi4u36d/JianDou&type=date&legend=top-left" />
  </picture>
</a>

## 部署命令

项目内置了基于 Docker Compose 的开发环境和生产环境启动脚本。首次执行前，请先按需准备环境文件：

```bash
cp .env.dev.example .env.dev
cp .env.prod.example .env.prod
```

常用部署命令如下：

```bash
# 启动开发环境（会按需构建镜像）
npm run compose:dev

# 构建开发环境镜像
npm run compose:dev:build

# 停止开发环境
npm run compose:dev:down

# 查看开发环境日志
npm run compose:dev:logs

# 启动生产环境（会按需构建镜像）
npm run compose:prod

# 构建生产环境镜像
npm run compose:prod:build

# 停止生产环境
npm run compose:prod:down

# 查看生产环境日志
npm run compose:prod:logs
```

生产环境默认以 Java 21 Jar 运行，面向 2C8G 应用服务器和 1C1G RDS 的默认参数在 `.env.prod.example` 中。API 镜像固定构建 `jvm-runtime` 阶段。

如果只需要本地分别启动前端、后台或 Spring Boot 服务，也可以使用：

```bash
npm run web:dev
npm run admin:dev
npm run api:dev
```

## License

本项目采用仓库内的 [License](./License)。
