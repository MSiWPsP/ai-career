# AI职途仓库开发约定

## 项目依据

- 本仓库是“AI职途——大学生智能职业成长平台”，不是自由设计的新项目。
- 开发前先阅读 `docs/` 中与当前任务相关的文档；PRD、功能模块、API、数据库、Agent 和 UI 文档是实现依据。
- 不擅自修改业务边界、数据模型或接口语义。文档之间存在冲突时，先结合现有实现判断；会导致破坏性变更或明显偏离文档时再询问用户。
- 数据库用户表必须使用文档中的 `user`，不要恢复为 `sys_user`。

## 工程结构

- `ai-career-server/`：Java 21、Spring Boot、MyBatis-Plus、MySQL、JWT、Knife4j/OpenAPI。
- `ai-career-web/`：Vue 3、TypeScript、Vite、Pinia、Axios、Element Plus、ECharts。
- 后端保持现有 Controller / Service / Mapper / DTO / VO 分层和统一 `Result` 响应结构。
- 前端优先复用现有 API 封装、类型、Store、布局与全局主题，不另建平行架构。

## 前端约定

- 菜单和页面功能图标统一使用 `@element-plus/icons-vue` 与 `<el-icon>`，不要使用 Unicode 符号或 Emoji 代替图标。
- 页面应同时处理加载、空数据、成功和失败状态。
- 修改用户昵称或头像后，要同步当前用户状态和持久化登录会话，避免布局与页面显示不一致。
- 涉及 UI 的变更除构建外，应尽量用真实浏览器检查主要路径和控制台错误。

## 配置与敏感信息

- 本地数据库覆盖配置位于被忽略的 `application-local.yml`；不要把数据库密码或访问密钥提交到 Git。
- 阿里云 OSS 使用环境变量 `OSS_ACCESS_KEY_ID` 和 `OSS_ACCESS_KEY_SECRET` 获取凭证。
- OSS 公共配置位于 `application.yml` 的 `aliyun.oss` 下；当前 Bucket 为 `xucheng-zzyl`，Endpoint 为上海节点。
- 头像上传支持 JPG、PNG、WebP、GIF，最大 5MB；替换或移除头像时清理本项目拥有的旧 OSS 对象。

## 常用验证命令

在 `ai-career-server/`：

```powershell
mvn clean test
mvn spring-boot:run
```

在 `ai-career-web/`：

```powershell
npm install
npm run build
npm run dev
```

## Git 与交付

- 开始工作前检查 `git status`，保留用户已有修改。
- 新分支默认使用 `codex/` 前缀；按完整业务增量提交，提交信息说明实际变化。
- 不提交 `target/`、`dist/`、浏览器截图、临时账号数据、IDE 文件或任何密钥。
- 完成后至少运行与改动相关的测试；后端业务改动优先运行 `mvn clean test`，前端改动运行 `npm run build`。
- 交付时说明分支、提交哈希、验证结果、尚未实现的接口以及需要用户执行的环境操作。
