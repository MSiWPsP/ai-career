# AI职途前端

基于 Vue 3、TypeScript、Vite、Element Plus、Pinia、Axios 和 ECharts 的前端 MVP。页面与接口组织遵循仓库 `docs` 目录中的现有架构。

## 已实现

- 登录与注册
- 首页数据概览
- 职业画像与技能自评
- 职业规划报告与历史版本
- 成长任务列表、筛选、统计和状态更新
- 模拟面试配置、会话展示、历史记录与报告
- 能力雷达、趋势和评分历史
- 阿里云 OSS 头像上传、替换与移除
- 基于真实画像和任务状态生成的通知菜单

职业规划生成、模拟面试开始/作答/结束等 AI 接口尚未由后端提供，相关入口会明确显示“待接入”，不会伪造业务结果。

## 本地运行

环境要求：Node.js 20+、npm，以及已启动在 `http://localhost:8080` 的后端服务。

```bash
npm install
npm run dev
```

浏览器访问 `http://localhost:5173`。开发服务器会把 `/api` 请求代理到本地后端。

如需连接其他后端地址，复制 `.env.example` 为 `.env.local` 并修改：

```env
VITE_API_BASE_URL=/api
```

## OSS 头像上传

后端使用 `aliyun.oss` 中配置的上海地域 Endpoint 与 Bucket。启动后端前，需要设置具备对应 Bucket 读写权限的环境变量：

```text
OSS_ACCESS_KEY_ID
OSS_ACCESS_KEY_SECRET
```

头像支持 JPG、PNG、WebP 和 GIF，单个文件最大 5MB。替换或移除头像时，系统会回收属于当前 Bucket 的旧头像文件。

## 构建检查

```bash
npm run build
```

该命令会先执行 Vue/TypeScript 类型检查，再生成生产构建到 `dist`。
