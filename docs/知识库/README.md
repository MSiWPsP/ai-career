# AI职途首批职业知识库

本目录是首期 RAG 的人工编写知识源，不是用户数据，也不是模型自动生成的长期事实。每篇文档均以“可执行的职业准备原则”为主，避免把某一招聘平台、某年薪资或不稳定的岗位数量写成通用事实。内容发布前仍需维护者复审。

| 文件 | 分类 | 适用场景 |
|---|---|---|
| `java-backend-capabilities.md` | CAREER_POSITION | Java 后端岗位能力和差距分析 |
| `backend-learning-path.md` | CAREER_PATH | 后端学习顺序与阶段目标 |
| `internship-preparation.md` | CAREER_GUIDANCE | 实习与校招准备 |
| `project-evidence.md` | PROJECT_PRACTICE | 项目实践、简历和面试表达 |
| `career-decision.md` | CAREER_GUIDANCE | 职业方向选择与决策 |

这些文档均为本项目原创整理，`sourceType=PROJECT_ORIGINAL`。它们是建议框架，不是所有企业的统一招聘标准；回答仍应结合用户实际画像与目标岗位，并表达不确定性。

首期导入只读取仓库内的受控清单，不接受普通用户上传。更新知识时应修改正文、复审日期并重新索引；模型或维度变化必须重建整个向量表。参考设计见 [RAG 详细设计](../AI职途——RAG职业知识库详细设计文档%20v1.0.md)。

当前 80 题检索评测集及本地验收局限见 [评测与加固记录](评测与加固记录-2026-09-21.md)；通过检索指标不等于通过生产回答质量门槛。
