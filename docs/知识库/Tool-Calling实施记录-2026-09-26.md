# 职业规划师 Tool Calling 实施记录（2026-09-26）

## 本轮范围

职业规划师首期只开放以下读取能力：

1. 当前生效职业规划下的成长任务与完成率。
2. 当前能力评分及有限历史变化。
3. 最近三次已结束模拟面试及已有报告摘要。

职业画像、技能和当前职业规划已经由 `CareerChatContextService` 每轮加载，因此没有重复包装为 Tool。创建或修改任务、保存规划、更新画像等写操作仍由现有 Service 负责，模型不能直接执行。

## 调用链路

```text
用户问题
  -> CareerToolIntentRouter 判断是否需要个人平台数据
  -> CareerPlannerAgent 仅为该轮注册 CareerReadTools
  -> Spring AI 根据问题选择 @Tool
  -> ToolContext 携带已认证 userId 和本轮证据记录器
  -> CareerToolQueryService 按显式 userId 读取 MySQL
  -> Tool 返回有界事实和 evidenceId
  -> 模型生成 GroundedCareerAnswer
  -> Service 逐字校验 evidenceId 与事实
  -> 校验后的完整回答通过 SSE 发送并持久化
```

纯个人数据查询不会调用 Embedding 或 PostgreSQL。问题同时要求个人数据和通用建议时，Tool 与 RAG 可以在同一轮使用，知识引用卡片仍只来自本轮实际进入 Prompt 的 RAG 片段。

## 信任边界

- `userId` 不出现在模型可生成的函数参数中，只由后端通过 Spring AI `ToolContext` 注入。
- Tool 不依赖请求线程的 `UserContext`。SSE 生成运行在 Reactor 工作线程时，仍使用 Service 捕获并传入的认证用户 ID。
- 每次工具调用生成独立证据 ID。模型必须原样返回证据 ID 和整条事实，改写文本、伪造 ID、使用旧轮证据都会被拒绝。
- 任务名和报告摘要等用户可编辑字段只作为数据；包含提示注入语句的事实不会进入最终回答。
- 工具或数据库异常只返回“平台数据暂时不可用”，不会把异常详情、连接信息或凭证交给模型。
- 最终内容先完整生成并校验，再发送 SSE，避免不可靠分片先被用户看到。

## 配置

- `AI_CAREER_TOOLS_ENABLED`：是否启用职业规划师只读 Tool Calling，默认 `true`。
- `AI_TOOL_MAX_CALLS_PER_TOOL`：Spring AI 单轮每个工具最大调用次数，默认 `2`。
- `AI_TOOL_CALLING_TEST=true`：运行真实模型 Tool Calling 兼容性测试，默认跳过。

## 验证结果

- 工具路由、功能开关、ToolContext 用户绑定、工具异常降级通过单元测试。
- 工具证据的真实 ID、逐字匹配、伪造 ID 和改写文本拒绝通过单元测试。
- SSE 个人任务查询会先完成结构化校验，只发送 `GENERATING`、回答和 `done`，不会误报 RAG 来源。
- MySQL 集成测试确认任务、能力和面试查询只返回显式用户的数据。
- 真实百炼 `qwen3.7-plus` 测试已完成函数选择、工具调用、结果回传、结构化输出和服务端核验，单轮约 11.2 秒。

## 后续工作

1. 使用真实用户问题扩充任务、能力和面试 Tool Calling 评测集，统计工具选择准确率、重复调用率和端到端延迟。
2. 评估为工具证据新增结构化持久化快照。目前平台数据事实已在发送前核验，但历史消息只保存最终文本，不能用于审计某次工具读取时的原始快照。
3. 观察真实流量后再决定是否增加更多读取工具。写工具继续等待明确的确认流程、参数校验、权限校验和幂等设计。
