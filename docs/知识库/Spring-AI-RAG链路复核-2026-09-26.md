# Spring AI RAG 链路复核（2026-09-26）

## 结论

当前链路已经在聊天调用、结构化输出、ChatMemory 和 Embedding 业务契约上使用 Spring AI。此次先替换了最明显的重复底层实现：删除手写 JDK `HttpClient` Embedding 客户端，让检索与导入统一依赖 `EmbeddingModel`。

PGVector 存取、检索编排、切片和引用校验暂不直接替换。这些代码包含当前产品的版本切换、失效过滤、查询超时、SSE 阶段、引用信任边界和回答发送前校验，直接套用一个端到端 Advisor 会改变已通过评测的行为。

## 逐段复核

| 链路 | 当前做法 | Spring AI 能力 | 处理结论 |
|---|---|---|---|
| Embedding 调用 | 原先手写 HTTP、JSON 解析和向量转换 | `EmbeddingModel`、`AbstractEmbeddingModel`、OpenAI SDK | 已重构。业务只依赖 `EmbeddingModel`，导入使用批量 `embed(List<String>)`。 |
| 模型回答 | `ChatClient` 与 `.entity(GroundedCareerAnswer.class)` | `ChatClient`、结构化输出 | 已使用，继续保留。 |
| 对话记忆 | Spring AI Advisor 接入业务消息 | `MessageChatMemoryAdvisor` | 已使用，继续保留。 |
| 向量存取 | 独立 PostgreSQL DataSource 与受控 SQL | `PgVectorStore` | 可迁移，但需先证明版本激活、失效日期、模型过滤、查询超时和引用元数据语义等价。 |
| 检索扩展 | 本地确定性多意图查询规划 | `MultiQueryExpander` | 暂不替换。框架实现会额外调用模型，增加延迟和非确定性。 |
| 召回与合并 | 多查询批量 Embedding、并行 PG 查询、按规划顺序轮询合并 | `DocumentRetriever`、`DocumentJoiner` | 后续可先让本地实现适配这些接口，再评测行为不变，减少一次性迁移风险。 |
| Prompt 增强 | Service 显式检索后把实际片段交给 Agent | `QuestionAnswerAdvisor`、`RetrievalAugmentationAdvisor` | 暂不整体接管。现有 SSE 必须先发检索阶段，且服务端要保存本轮实际片段、核验逐字摘录并在发送正文前完成校验。 |
| Markdown 切片 | 保留 H2 标题、稳定切片序号 | 通用文本切分器 | 保留。标题与序号是评测定位和结构化引用的一部分。 |
| 引用快照与事实校验 | 保存文档版本、切片序号、SHA-256，逐字核验 | 无直接等价的通用 API | 保留为领域与审计逻辑。 |

## Embedding 兼容层为什么仍然存在

Spring AI 2.0.1 的标准 `OpenAiEmbeddingModel` 会读取响应 usage。本机百炼 OpenAI 兼容接口没有返回 `usage.prompt_tokens`，直接使用会使真实检索降级。

新的 `CompatibleOpenAiEmbeddingModel` 继承 `AbstractEmbeddingModel`，复用 Spring AI 的 `OpenAiEmbeddingOptions`、`OpenAiSetup`、OpenAI SDK 和 `EmbeddingResponse`。它只补一项兼容行为：不读取缺失的 usage。这样供应商差异被限制在单个 Bean 内，检索服务、导入器和测试都使用标准 `EmbeddingModel`。

若后续 Spring AI 或百炼修复 usage 兼容，可删除该类，注册官方 `OpenAiEmbeddingModel`，业务代码无需改动。

## PgVectorStore 的迁移条件

引入 Spring AI `PgVectorStore` 前，先建立独立 RAG `DataSource` 和 `JdbcTemplate`，不能影响 MySQL 主数据源。迁移实现需满足以下等价条件：

1. 新版本全部写入成功后才原子激活，导入失败时旧版本继续可检索。
2. 检索必须过滤 `active`、复审/失效日期、Embedding 模型和维度。
3. JDBC 查询超时、连接超时和 Socket 超时继续可配置，故障仍安全降级。
4. 返回结果包含引用快照需要的文档 ID、版本、切片序号、章节和正文哈希来源数据。
5. 80 条检索集、并发评测、导入幂等和三类故障测试不退化。

满足这些条件后，可把向量表交给 `PgVectorStore`，文档发布和版本状态仍由知识治理表负责。当前只有受控目录导入，还没有设计完成的管理员知识治理表，因此此时迁移会把表结构改动与框架替换绑在一起。

## 后续建议顺序

1. 管理员知识接口设计时补齐 MySQL 文档治理与发布状态，再决定 `PgVectorStore` 表和迁移脚本。
2. 先用 Spring AI RAG 的 `DocumentRetriever`、`DocumentJoiner` 接口包住现有稳定实现，保持评测和 SSE 行为。
3. 在隔离表做 `PgVectorStore` 对照实现，跑完整检索、回答、并发和故障评测。
4. 只有对照结果达标后再删除自定义向量 SQL；`RetrievalAugmentationAdvisor` 仅在能够暴露实际检索文档并满足发送前校验时考虑接入。

## 本轮验证

- 真实百炼 Embedding + PostgreSQL 冒烟：2 项通过；目标文档召回 10/10，跳过检索 5/5。
- 批量导入：5 篇文档、24 个切片成功，版本与切片数量不变。
- 80 条检索评测：路由 80/80、目标文档 Recall@5 60/60、目标章节 Recall@5 59/60，P50 203 ms、P95 400 ms。
- 4 工作线程、12 次检索：目标文档命中 12/12，P50 462 ms、P95/最大 1511 ms。该小样本包含进程首次连接抖动，不能视为稳定负载结论。
- Embedding、PostgreSQL 不可用与聊天降级相关测试继续通过。
- 后端 `mvn -o clean test`：88 项，0 失败、0 错误，8 项外部评测按默认配置跳过。

参考：

- [Spring AI Embedding API](https://docs.spring.io/spring-ai/reference/api/embeddings.html)
- [Spring AI PGVector](https://docs.spring.io/spring-ai/reference/api/vectordbs/pgvector.html)
- [Spring AI RAG](https://docs.spring.io/spring-ai/reference/api/retrieval-augmented-generation.html)
- [Spring AI Advisors](https://docs.spring.io/spring-ai/reference/api/advisors.html)
