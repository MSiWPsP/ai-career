# AI职途——大学生智能职业成长平台
## 数据库设计文档 v1.0

---

# 一、文档目标

本文档基于《AI职途——功能模块设计文档 v1.0》，用于明确系统核心业务数据结构。

数据库设计目标：

1. 支撑用户职业画像；
2. 支撑职业规划及版本管理；
3. 支撑成长任务；
4. 支撑 AI 模拟面试全过程；
5. 支撑面试报告和能力分析；
6. 支撑后续 Agent Tool Calling；
7. 支撑成长闭环中的数据更新；
8. 保持第一版结构简单，避免过度设计。

---

# 二、数据库选型

数据库采用：

```text id="kbds01"
MySQL 8.x
```

后端 ORM 框架：

```text id="kbds02"
MyBatis-Plus
```

推荐字符集：

```text id="kbds03"
utf8mb4
```

推荐排序规则：

```text id="kbds04"
utf8mb4_unicode_ci
```

---

# 三、核心表设计概览

第一版系统建议使用以下核心表：

```text id="kbds05"
1. user
   用户表

2. user_profile
   用户职业画像表

3. user_skill
   用户技能表

4. career_plan
   职业规划表

5. career_task
   成长任务表

6. interview
   模拟面试主表

7. interview_message
   模拟面试消息记录表

8. interview_report
   面试报告表

9. ability_score
   用户能力评分表

10. career_chat_session
    职业规划聊天会话表

11. career_chat_message
    职业规划聊天消息表
```

可选扩展表：

```text id="kbds06"
12. knowledge_document
    知识库文档表

13. career_position
    职业岗位信息表
```

基础 MVP 使用前 9 张表；启用职业规划聊天多会话持久化时增加第 10、11 张表。

---

# 四、整体 ER 关系

```text id="kbds07"
User
 │
 ├── UserProfile
 │
 ├── UserSkill
 │
 ├── CareerPlan
 │      │
 │      └── CareerTask
 │
 ├── Interview
 │      │
 │      ├── InterviewMessage
 │      │
 │      └── InterviewReport
 │
 ├── AbilityScore
 │
 └── CareerChatSession
        │
        └── CareerChatMessage
```

关系说明：

```text id="kbds08"
一个用户
    ↓
一个职业画像

一个用户
    ↓
多个技能记录

一个用户
    ↓
多个职业规划版本

一个职业规划
    ↓
多个成长任务

一个用户
    ↓
多次模拟面试

一次模拟面试
    ↓
多条消息记录

一次模拟面试
    ↓
一份面试报告

一个用户
    ↓
多条能力评分记录

一个用户
    ↓
多个职业规划聊天会话

一个职业规划聊天会话
    ↓
多条职业规划聊天消息
```

---

# 五、统一字段规范

所有核心业务表建议统一包含：

```text id="kbds09"
id
create_time
update_time
```

如果使用逻辑删除：

```text id="kbds10"
is_deleted
```

推荐：

```text id="kbds11"
is_deleted

0 = 正常
1 = 已删除
```

---

# 六、user 用户表

## 6.1 表作用

用于保存系统用户账号和基础信息。

表名：

```text id="kbds12"
user
```

---

## 6.2 字段设计

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT | 主键 |
| username | VARCHAR(50) | 用户名 |
| password | VARCHAR(255) | 加密密码 |
| nickname | VARCHAR(50) | 昵称 |
| avatar | VARCHAR(500) | 头像 |
| role | VARCHAR(20) | 用户角色 |
| status | TINYINT | 用户状态 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |
| is_deleted | TINYINT | 是否删除 |

---

## 6.3 role

例如：

```text id="kbds13"
student
admin
```

---

## 6.4 status

```text id="kbds14"
0 = 禁用
1 = 正常
```

---

# 七、user_profile 用户职业画像表

## 7.1 表作用

保存用户职业规划所需的核心背景信息。

表名：

```text id="kbds15"
user_profile
```

一个用户原则上只有一条当前职业画像。

---

## 7.2 字段设计

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT | 主键 |
| user_id | BIGINT | 用户 ID |
| education | VARCHAR(50) | 学历 |
| major | VARCHAR(100) | 专业 |
| grade | VARCHAR(50) | 年级 |
| graduation_year | INT | 毕业年份 |
| career_stage | VARCHAR(50) | 职业发展阶段 |
| target_position | VARCHAR(100) | 当前目标岗位 |
| target_city | VARCHAR(100) | 目标城市 |
| target_time | VARCHAR(100) | 目标时间 |
| daily_study_hours | DECIMAL(4,1) | 每日可投入学习时间 |
| career_goal | VARCHAR(500) | 当前职业目标描述 |
| interest_description | VARCHAR(1000) | 职业兴趣补充 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

---

## 7.3 career_stage

建议枚举：

```text id="kbds16"
EXPLORE
职业探索

LEARNING
学习提升

INTERNSHIP
实习准备

CAMPUS_RECRUITMENT
校招准备

JOB_SEARCH
正式求职
```

---

# 八、user_skill 用户技能表

## 8.1 表作用

保存用户当前掌握的技能。

不建议把：

```text id="kbds17"
Java、SpringBoot、MySQL、Redis
```

直接塞到 `user_profile` 的一个字符串字段中。

单独建表后，后续：

- 修改技能
- 做雷达图
- Agent 查询
- 能力评分
- 动态调整

都会方便很多。

---

## 8.2 字段设计

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT | 主键 |
| user_id | BIGINT | 用户 ID |
| skill_name | VARCHAR(100) | 技能名称 |
| skill_category | VARCHAR(100) | 技能分类 |
| level | TINYINT | 掌握等级 |
| score | INT | 当前评分 |
| source | VARCHAR(50) | 评分来源 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

---

# 九、技能等级

建议：

```text id="kbds18"
0 未学习
1 入门
2 基础
3 熟悉
4 掌握
5 熟练
```

对应：

```text id="kbds19"
0   → 0分
1   → 20分
2   → 40分
3   → 60分
4   → 80分
5   → 100分
```

---

# 十、skill_category

例如：

```text id="kbds20"
PROGRAMMING
编程语言

FRAMEWORK
开发框架

DATABASE
数据库

MIDDLEWARE
中间件

COMPUTER_BASIC
计算机基础

DEVOPS
开发运维

AI
人工智能

SOFT_SKILL
软技能
```

---

# 十一、source

技能评分可能来自不同地方。

例如：

```text id="kbds21"
SELF
用户自评

INTERVIEW
AI模拟面试

AGENT
AI综合分析

SYSTEM
系统计算
```

第一版可以主要使用：

```text id="kbds22"
SELF
INTERVIEW
```

---

# 十二、career_plan 职业规划表

## 12.1 表作用

保存 CareerPlannerAgent 生成的职业规划结果。

特别注意：

**不要每次重新规划都覆盖原来的职业规划。**

应该保留版本。

---

## 12.2 字段设计

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT | 主键 |
| user_id | BIGINT | 用户 ID |
| version | INT | 规划版本 |
| target_position | VARCHAR(100) | 目标岗位 |
| match_score | INT | 职业匹配度 |
| summary | TEXT | 规划总体说明 |
| advantages | JSON | 用户优势 |
| weaknesses | JSON | 用户短板 |
| roadmap | JSON | 成长路线 |
| status | TINYINT | 是否当前有效版本 |
| source_interview_id | BIGINT | 触发重新规划的面试 ID |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

---

# 十三、为什么 roadmap 建议 JSON

职业路线天然属于多层结构：

```text id="kbds23"
阶段一
  ├─ 目标
  ├─ 学习内容
  └─ 周期

阶段二
  ├─ 目标
  ├─ 学习内容
  └─ 周期
```

例如：

```json id="kbds24"
[
  {
    "stage": 1,
    "name": "Java基础强化",
    "duration": "2周",
    "goal": "提升Java核心基础",
    "topics": [
      "集合",
      "并发",
      "JVM"
    ]
  },
  {
    "stage": 2,
    "name": "中间件强化",
    "duration": "3周",
    "goal": "掌握Redis核心应用",
    "topics": [
      "缓存",
      "分布式锁"
    ]
  }
]
```

第一版使用 JSON 会比继续拆十几张表简单很多。

---

# 十四、career_plan status

例如：

```text id="kbds25"
0 = 历史版本

1 = 当前版本
```

当生成 V2 时：

```text id="kbds26"
V1 status = 0

V2 status = 1
```

这样就能查询：

> 当前职业规划

也能查看：

> 历史规划变化。

---

# 十五、career_task 成长任务表

## 15.1 表作用

把职业规划中的学习路线真正拆解为可执行任务。

表名：

```text id="kbds27"
career_task
```

---

## 15.2 字段设计

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT | 主键 |
| user_id | BIGINT | 用户 ID |
| career_plan_id | BIGINT | 所属职业规划 |
| stage_name | VARCHAR(100) | 所属阶段 |
| task_name | VARCHAR(200) | 任务名称 |
| task_description | VARCHAR(1000) | 任务描述 |
| task_type | VARCHAR(50) | 任务类型 |
| priority | TINYINT | 优先级 |
| status | TINYINT | 任务状态 |
| start_date | DATE | 计划开始日期 |
| deadline | DATE | 截止日期 |
| finish_time | DATETIME | 完成时间 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

---

# 十六、task_type

例如：

```text id="kbds28"
KNOWLEDGE
知识学习

PROJECT
项目实践

INTERVIEW
模拟面试

CAREER
职业认知
```

---

# 十七、priority

```text id="kbds29"
1 = 低

2 = 普通

3 = 高
```

---

# 十八、task status

```text id="kbds30"
0 = 待开始

1 = 进行中

2 = 已完成

3 = 已跳过
```

---

# 十九、interview 模拟面试主表

## 19.1 表作用

保存每一次 AI 模拟面试的整体信息。

表名：

```text id="kbds31"
interview
```

---

## 19.2 字段设计

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT | 主键 |
| user_id | BIGINT | 用户 ID |
| target_position | VARCHAR(100) | 目标岗位 |
| interview_type | VARCHAR(50) | 面试类型 |
| difficulty | VARCHAR(50) | 难度 |
| status | TINYINT | 面试状态 |
| conversation_id | VARCHAR(100) | AI 会话 ID |
| question_count | INT | 已提问数量 |
| start_time | DATETIME | 开始时间 |
| end_time | DATETIME | 结束时间 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

---

# 二十、interview_type

例如：

```text id="kbds32"
TECHNICAL
技术面

PROJECT
项目面

HR
HR面

COMPREHENSIVE
综合面
```

MVP 优先：

```text id="kbds33"
TECHNICAL

PROJECT
```

---

# 二十一、difficulty

```text id="kbds34"
EASY
初级

MEDIUM
中级

HARD
高级
```

---

# 二十二、interview status

```text id="kbds35"
0 = 未开始

1 = 进行中

2 = 已完成

3 = 用户主动终止

4 = 异常结束
```

---

# 二十三、conversation_id

这个字段非常重要。

可以设计成：

```text id="kbds36"
interview:{interviewId}
```

例如：

```text id="kbds37"
interview:10086
```

Spring AI ChatMemory 使用这个值区分不同面试上下文。

这样：

```text id="kbds38"
第一次Java面试

和

第二次Java面试
```

不会共享一套 Memory。

---

# 二十四、interview_message 面试消息表

## 24.1 表作用

保存整个模拟面试聊天过程。

这是非常重要的一张表。

ChatMemory 可以负责短期上下文。

数据库负责：

> 永久保存面试记录。

---

## 24.2 字段设计

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT | 主键 |
| interview_id | BIGINT | 面试 ID |
| user_id | BIGINT | 用户 ID |
| role | VARCHAR(20) | 消息角色 |
| content | TEXT | 消息内容 |
| question_category | VARCHAR(100) | 问题分类 |
| question_level | VARCHAR(50) | 当前问题难度 |
| message_order | INT | 消息顺序 |
| create_time | DATETIME | 创建时间 |

---

# 二十五、role

```text id="kbds39"
assistant
AI面试官

user
用户

system
系统
```

实际数据库主要保存：

```text id="kbds40"
assistant

user
```

即可。

---

# 二十六、question_category

例如：

```text id="kbds41"
Java

Spring

MySQL

Redis

JVM

ComputerNetwork

Project
```

这样以后统计：

> Redis 回答表现怎么样

会方便很多。

---

# 二十七、interview_report 面试报告表

## 27.1 表作用

保存一次完整面试结束后的结构化分析结果。

---

## 27.2 字段设计

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT | 主键 |
| interview_id | BIGINT | 面试 ID |
| user_id | BIGINT | 用户 ID |
| total_score | INT | 综合得分 |
| scores | JSON | 分项评分 |
| advantages | JSON | 优势 |
| weaknesses | JSON | 薄弱项 |
| suggestions | JSON | 改进建议 |
| summary | TEXT | 综合评价 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

---

# 二十八、scores 示例

```json id="kbds42"
{
  "Java": 82,
  "SpringBoot": 76,
  "MySQL": 80,
  "Redis": 55,
  "ComputerNetwork": 48,
  "Project": 73,
  "Communication": 78
}
```

前端直接拿这个 JSON 数据：

```text id="kbds43"
ECharts
```

绘制雷达图。

---

# 二十九、advantages 示例

```json id="kbds44"
[
  "Java集合相关知识掌握较好",
  "MySQL索引理解较完整",
  "项目介绍结构比较清晰"
]
```

---

# 三十、weaknesses 示例

```json id="kbds45"
[
  "Redis缓存体系掌握不足",
  "TCP基础知识存在遗漏",
  "项目性能优化经验较少"
]
```

---

# 三十一、suggestions 示例

```json id="kbds46"
[
  {
    "topic": "Redis",
    "priority": "HIGH",
    "content": "重点学习缓存穿透、缓存击穿和缓存雪崩"
  },
  {
    "topic": "计算机网络",
    "priority": "MEDIUM",
    "content": "复习TCP连接建立和断开流程"
  }
]
```

---

# 三十二、ability_score 用户能力评分表

## 32.1 为什么额外增加这张表

`user_skill` 保存的是：

> 用户当前技能状态。

而：

```text id="kbds47"
ability_score
```

保存的是：

> 用户能力变化历史。

例如：

```text id="kbds48"
Redis

9月1日：40

9月10日：52

9月20日：68
```

这样才能画：

> 能力成长趋势图。

---

# 三十三、ability_score 字段

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT | 主键 |
| user_id | BIGINT | 用户 ID |
| ability_name | VARCHAR(100) | 能力名称 |
| score | INT | 能力评分 |
| source_type | VARCHAR(50) | 数据来源 |
| source_id | BIGINT | 来源业务 ID |
| create_time | DATETIME | 创建时间 |

---

# 三十四、source_type

例如：

```text id="kbds49"
SELF
自我评估

INTERVIEW
模拟面试

AGENT
Agent分析
```

例如：

```text id="kbds50"
ability_name = Redis

score = 55

source_type = INTERVIEW

source_id = 10001
```

代表：

> 第 10001 次面试计算出了 Redis = 55 分。

---

# 三十五、用户能力更新逻辑

面试结束：

```text id="kbds51"
InterviewReport

        ↓

读取 scores

        ↓

ability_score
新增历史评分

        ↓

user_skill
更新当前技能分数
```

例如：

原：

```text id="kbds52"
Redis = 40
```

最新面试：

```text id="kbds53"
Redis = 55
```

更新：

```text id="kbds54"
user_skill

Redis = 55
```

同时：

```text id="kbds55"
ability_score

Redis 40

Redis 55
```

均保留。

---

# 三十六、成长闭环数据流

整个数据库的数据闭环：

```text id="kbds56"
user_profile
+
user_skill
        ↓
CareerPlannerAgent
        ↓
career_plan
        ↓
career_task
        ↓
用户学习
        ↓
interview
        ↓
interview_message
        ↓
interview_report
        ↓
ability_score
        ↓
更新 user_skill
        ↓
重新调用 CareerPlannerAgent
        ↓
career_plan V2
```

---

# 三十七、职业规划版本机制

例如：

第一次：

```text id="kbds57"
CareerPlan

version = 1

status = 1
```

经过一次面试后重新规划。

原来的：

```text id="kbds58"
version = 1

status = 0
```

新规划：

```text id="kbds59"
version = 2

status = 1

source_interview_id = 10086
```

因此用户可以看到：

```text id="kbds60"
职业规划 V1

↓

第一次模拟面试

↓

职业规划 V2
```

这个数据结构特别适合比赛展示“动态职业成长”。

---

# 三十八、可选表：knowledge_document

如果后续加入 RAG，可以建立：

```text id="kbds61"
knowledge_document
```

字段：

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT | 主键 |
| title | VARCHAR(200) | 文档标题 |
| category | VARCHAR(100) | 分类 |
| content | LONGTEXT | 文档正文 |
| source | VARCHAR(500) | 来源 |
| status | TINYINT | 状态 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

不过：

如果最终使用：

```text id="kbds62"
PGVector

Milvus

Redis Vector Store

Elasticsearch
```

那么向量数据不一定存 MySQL。

MySQL 只负责：

> 知识文档元数据。

---

# 三十九、可选表：career_position

如果后期希望系统支持多个职业方向，可以建立：

```text id="kbds63"
career_position
```

例如：

```text id="kbds64"
Java后端开发工程师

前端开发工程师

AI应用开发工程师

测试开发工程师
```

字段：

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT | 主键 |
| position_name | VARCHAR(100) | 岗位名称 |
| description | TEXT | 岗位介绍 |
| required_skills | JSON | 核心技能 |
| skill_standard | JSON | 技能标准 |
| learning_path | JSON | 推荐路线 |
| status | TINYINT | 状态 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

---

# 四十、Java后端实体对应

后端实体大致为：

```text id="kbds65"
User

UserProfile

UserSkill

CareerPlan

CareerTask

Interview

InterviewMessage

InterviewReport

AbilityScore
```

---

# 四十一、后端包结构

推荐：

```text id="kbds66"
com.xucheng

├── entity
│   ├── User.java
│   ├── UserProfile.java
│   ├── UserSkill.java
│   ├── CareerPlan.java
│   ├── CareerTask.java
│   ├── Interview.java
│   ├── InterviewMessage.java
│   ├── InterviewReport.java
│   └── AbilityScore.java
│
├── mapper
│
├── service
│
└── controller
```

---

# 四十二、DTO 与实体不要完全混用

例如创建职业画像：

```text id="kbds67"
UserProfileDTO
```

数据库：

```text id="kbds68"
UserProfile
```

返回前端：

```text id="kbds69"
UserProfileVO
```

Agent 返回：

```text id="kbds70"
CareerPlanResult
```

不要直接：

```text id="kbds71"
让大模型生成 CareerPlan Entity
```

更推荐：

```text id="kbds72"
LLM
 ↓
CareerPlanResult
 ↓
Service处理
 ↓
CareerPlan
 ↓
数据库
```

这样 Agent 层和数据库层不会强耦合。

---

# 四十三、Agent Tool 与数据库关系

后面可以实现：

```text id="kbds73"
CareerTools
```

例如：

```java id="kbds74"
@Tool(description = "获取当前用户职业画像")
public UserProfileVO getUserProfile(Long userId) {
    return profileService.getProfile(userId);
}
```

```java id="kbds75"
@Tool(description = "获取用户当前技能情况")
public List<UserSkillVO> getUserSkills(Long userId) {
    return skillService.getUserSkills(userId);
}
```

```java id="kbds76"
@Tool(description = "获取用户最近模拟面试报告")
public InterviewReportVO getLatestInterviewReport(Long userId) {
    return interviewService.getLatestReport(userId);
}
```

```java id="kbds77"
@Tool(description = "获取用户当前职业规划")
public CareerPlanVO getCurrentCareerPlan(Long userId) {
    return careerPlanService.getCurrentPlan(userId);
}
```

---

# 四十四、推荐索引设计

## user

```text id="kbds78"
UNIQUE INDEX(username)
```

---

## user_profile

```text id="kbds79"
UNIQUE INDEX(user_id)
```

---

## user_skill

```text id="kbds80"
INDEX(user_id)

UNIQUE INDEX(user_id, skill_name)
```

---

## career_plan

```text id="kbds81"
INDEX(user_id)

INDEX(user_id, status)
```

---

## career_task

```text id="kbds82"
INDEX(user_id)

INDEX(career_plan_id)

INDEX(user_id, status)
```

---

## interview

```text id="kbds83"
INDEX(user_id)

INDEX(user_id, create_time)
```

---

## interview_message

```text id="kbds84"
INDEX(interview_id)

INDEX(interview_id, message_order)
```

---

## interview_report

```text id="kbds85"
UNIQUE INDEX(interview_id)

INDEX(user_id)
```

---

## ability_score

```text id="kbds86"
INDEX(user_id, ability_name)

INDEX(user_id, create_time)
```

---

# 四十五、外键设计建议

项目开发阶段：

**不强制建议在 MySQL 中建立大量物理外键。**

例如：

```text id="kbds87"
career_plan.user_id
```

逻辑上引用：

```text id="kbds88"
user.id
```

但是可以由 Service 层维护数据关系。

原因：

- 开发更灵活
- 删除处理更方便
- MyBatis-Plus 项目更常见
- 避免后续 AI 自动生成数据时受到过多数据库外键约束

但必须保证业务逻辑上的关联正确。

---

# 四十六、时间字段规范

推荐统一：

```text id="kbds89"
create_time

update_time
```

Java 类型：

```text id="kbds90"
LocalDateTime
```

日期：

```text id="kbds91"
LocalDate
```

---

# 四十七、ID策略

推荐：

```text id="kbds92"
BIGINT
```

搭配：

```text id="kbds93"
MyBatis-Plus Snowflake ID
```

例如：

```java id="kbds94"
@TableId(type = IdType.ASSIGN_ID)
private Long id;
```

这样以后部署多实例也方便。

---

# 四十八、JSON字段对应Java类型

例如：

```text id="kbds95"
career_plan.advantages
career_plan.weaknesses
career_plan.roadmap
interview_report.scores
interview_report.suggestions
```

数据库：

```text id="kbds96"
JSON
```

Java 可以先：

```text id="kbds97"
String
```

简单处理。

后期可以使用：

```text id="kbds98"
Jackson

TypeHandler
```

直接映射：

```java id="kbds99"
List<String>

List<RoadmapStage>

Map<String,Integer>
```

---

# 四十九、第一版不建议拆得太细

例如职业规划路线本来可以继续设计：

```text id="kbds100"
career_plan_stage

career_plan_stage_topic

career_plan_stage_skill

career_plan_resource
```

第一版没有必要。

否则系统会出现：

```text id="kbds101"
20多张表
```

但比赛真正演示的只有几分钟。

第一版原则：

> 核心业务独立成表，复杂 AI 输出使用 JSON。

---

# 五十、MVP最终数据库

第一阶段正式开发只需要：

```text id="kbds102"
user

user_profile

user_skill

career_plan

career_task

interview

interview_message

interview_report

ability_score
```

共：

```text id="kbds103"
9张表
```

已经可以完成：

```text id="kbds104"
用户登录

职业画像

技能画像

职业规划

学习路线

成长任务

AI模拟面试

聊天记录

面试报告

雷达图

成长趋势

职业规划动态更新
```

---

# 五十一、最终 ER 简图

```text id="kbds105"
┌──────────────┐
│     user     │
└──────┬───────┘
       │
       ├───────────────┐
       │               │
       ↓               ↓
┌──────────────┐  ┌──────────────┐
│ user_profile │  │  user_skill  │
└──────────────┘  └──────────────┘

       │
       ↓
┌──────────────┐
│ career_plan  │
└──────┬───────┘
       │
       ↓
┌──────────────┐
│ career_task  │
└──────────────┘


┌──────────────┐
│  interview   │
└──────┬───────┘
       │
       ├────────────────┐
       │                │
       ↓                ↓
┌─────────────────┐ ┌──────────────────┐
│interview_message│ │interview_report  │
└─────────────────┘ └────────┬─────────┘
                              │
                              ↓
                      ┌────────────────┐
                      │ ability_score  │
                      └────────────────┘
```

---

# 五十二、最终数据闭环

```text id="kbds106"
user_profile
+
user_skill
       ↓
CareerPlannerAgent
       ↓
career_plan
       ↓
career_task
       ↓
用户完成学习
       ↓
interview
       ↓
interview_message
       ↓
InterviewerAgent
       ↓
interview_report
       ↓
ability_score
       ↓
user_skill更新
       ↓
CareerPlannerAgent重新规划
       ↓
career_plan新版本
```

这套数据库结构重点支撑的是：

**职业画像 → AI规划 → 行动任务 → AI面试 → 能力评估 → 动态重新规划**

的完整职业成长闭环。

---

# 五十三、职业规划聊天持久化扩展

## 53.1 career_chat_session

保存会话标题、状态、消息数量、最近消息摘要和最近活跃时间。`conversation_id` 使用 `career:{userId}:{uuid}`，并建立唯一索引；查询和更新时仍必须同时校验 `user_id`。

## 53.2 career_chat_message

保存用户和 AI 的完整消息记录。`client_message_id` 是客户端生成的幂等标识，使用 `(session_id, client_message_id, role)` 唯一索引防止重试产生重复消息。

消息状态：

```text
0 = 处理中
1 = 已完成
2 = 失败，可使用相同 client_message_id 重试
```

ChatMemory 只加载最近窗口；完整聊天历史以 `career_chat_message` 为事实来源。后端重启时，业务 Service 从已完成消息重新构建近期 ChatMemory。
