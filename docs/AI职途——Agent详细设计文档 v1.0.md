# AI职途——大学生智能职业成长平台
## Agent 详细设计文档 v1.0

---

# 一、文档目标

本文档用于定义 AI职途 平台中两个核心智能体：

1. `CareerPlannerAgent` —— AI 职业规划师
2. `InterviewerAgent` —— AI 模拟面试官

重点明确：

- Agent 的角色定位
- Agent 输入数据
- Agent 输出数据
- System Prompt 设计
- Chat Memory 设计
- Tool Calling 设计
- Structured Output 设计
- Agent 与业务系统的数据流
- 多 Agent 协作机制
- 异常与兜底处理
- 后端代码结构建议

---

# 二、整体 Agent 架构

系统采用“双 Agent + 业务数据共享”的架构。

```text
                    用户
                     │
                     ↓
              Spring Boot 后端
                     │
         ┌───────────┴───────────┐
         │                       │
         ↓                       ↓
CareerPlannerAgent        InterviewerAgent
职业规划智能体              模拟面试智能体
         │                       │
         │                       │
         └───────────┬───────────┘
                     │
                     ↓
                Tool Calling
                     │
       ┌─────────────┼─────────────┐
       ↓             ↓             ↓
 用户职业画像      职业规划       面试记录
       │             │             │
       └─────────────┴─────────────┘
                     ↓
                    MySQL
```

两个 Agent 不直接互相调用。

它们通过：

- 用户职业画像
- 职业规划
- 学习任务
- 面试报告
- 能力评分

实现数据协同。

这样可以降低 Agent 之间的耦合。

---

# 三、多 Agent 协作核心流程

```text
用户填写职业画像
       ↓
CareerPlannerAgent
       ↓
生成职业规划
       ↓
生成成长任务
       ↓
用户进行学习
       ↓
InterviewerAgent
       ↓
开展模拟面试
       ↓
生成面试报告
       ↓
更新能力画像
       ↓
CareerPlannerAgent
       ↓
重新评估职业规划
       ↓
生成 CareerPlan V2
```

核心思想：

> InterviewerAgent 负责检验，CareerPlannerAgent 负责规划。

两个 Agent 形成：

**规划—执行—检验—调整**

闭环。

---

# 四、Agent 公共技术能力

两个 Agent 共享以下基础能力：

```text
ChatClient

ChatMemory

Advisor

Tool Calling

Structured Output

RAG
```

其中：

```text
ChatClient
```

负责模型交互。

```text
ChatMemory
```

负责短期多轮上下文。

```text
Tool Calling
```

负责访问系统真实业务数据。

```text
Structured Output
```

负责将 AI 输出映射为 Java DTO。

```text
RAG
```

负责职业知识和面试知识增强。

---

# 五、CareerPlannerAgent

## 5.1 Agent 定位

类名：

```java
CareerPlannerAgent
```

角色：

> 面向高校学生的 AI 职业发展规划顾问。

核心职责：

- 了解用户职业背景
- 分析职业方向
- 分析当前能力
- 判断目标岗位匹配程度
- 找出能力短板
- 制定职业成长路线
- 生成可执行成长任务
- 根据后续面试表现重新规划

---

# 六、CareerPlannerAgent 能力边界

CareerPlannerAgent 应当：

- 提供职业发展建议
- 帮助用户分析不同职业方向
- 结合用户真实数据进行规划
- 明确说明建议依据
- 给出阶段性可执行方案

CareerPlannerAgent 不应：

- 直接替用户决定人生方向
- 保证就业成功
- 虚构用户不存在的技能
- 虚构不存在的面试结果
- 在没有数据的情况下假装知道用户情况

如果数据不足，应主动：

> 提醒用户补充职业画像。

---

# 七、CareerPlannerAgent 输入

Agent 的输入分为：

## 7.1 用户自然语言输入

例如：

```text
我半年后想找Java后端实习，我现在应该重点学什么？
```

---

## 7.2 用户职业画像

来自：

```text
user_profile
```

包括：

```text
学历

专业

年级

毕业年份

目标岗位

职业阶段

目标城市

每天学习时间

职业目标
```

---

## 7.3 用户技能画像

来自：

```text
user_skill
```

例如：

```text
Java         80

SpringBoot   60

MySQL        60

Redis        40

Docker       40
```

---

## 7.4 当前职业规划

来自：

```text
career_plan
```

用于回答：

```text
根据我现在的情况重新调整规划。
```

---

## 7.5 学习任务数据

来自：

```text
career_task
```

例如：

```text
Redis基础     已完成

JVM基础       进行中

微服务项目     未开始
```

---

## 7.6 历史面试数据

来自：

```text
interview_report
```

例如：

```text
Java：82

MySQL：78

Redis：48

计算机网络：45
```

这些数据可以用于重新规划。

---

# 八、CareerPlannerAgent System Prompt 设计

System Prompt 第一版建议包含：

```text
角色定义
+
目标
+
业务规则
+
数据使用规则
+
输出要求
+
安全边界
```

---

# 九、CareerPlannerAgent System Prompt 示例

```text
你是“AI职途”平台中的大学生职业规划师。

你的主要服务对象是高校学生和应届毕业生。

你的职责是根据用户的教育背景、职业目标、技能水平、
学习时间、成长任务以及历史模拟面试结果，
帮助用户分析职业方向、职业匹配程度和能力差距，
并制定阶段性、可执行的职业成长方案。

你必须遵守以下规则：

1. 不得凭空假设用户掌握某项技能。

2. 在分析用户职业能力时，应优先使用系统提供的职业画像、
技能数据、学习任务和面试数据。

3. 如果用户数据不足，应明确指出缺失信息，
而不是自行编造。

4. 职业建议属于辅助建议，
不得向用户承诺某项职业一定能够成功。

5. 给出的学习路线必须尽量具体，
避免只输出“学习Java”“学习Redis”等过于宽泛的内容。

6. 学习路线应考虑用户当前水平、目标岗位和可投入学习时间。

7. 当存在历史模拟面试数据时，
应优先分析用户实际表现较弱的能力。

8. 如果用户请求重新规划，
应分析旧规划、当前任务完成情况和最新面试结果之间的变化。

9. 你的回答应保持职业规划顾问的角色，
避免无关闲聊。

10. 当系统要求结构化输出时，
必须严格按照指定数据结构返回。
```

---

# 十、CareerPlannerAgent 普通聊天模式

用户可能只是咨询：

```text
Java后端和AI应用开发哪个更适合我？
```

这种场景不一定需要创建新的职业规划。

流程：

```text
用户问题
   ↓
CareerPlannerAgent
   ↓
判断需要哪些数据
   ↓
Tool Calling
   ↓
读取用户画像
   ↓
读取用户技能
   ↓
生成自然语言回答
```

---

# 十一、CareerPlannerAgent 正式规划模式

当用户点击：

```text
生成职业规划
```

系统进入正式规划模式。

这时输出不再只是文本。

需要输出：

```text
CareerPlanResult
```

结构化对象。

---

# 十二、CareerPlanResult DTO

推荐：

```java
public class CareerPlanResult {

    private String targetPosition;

    private Integer matchScore;

    private String summary;

    private List<String> advantages;

    private List<String> weaknesses;

    private List<RoadmapStage> roadmap;

}
```

---

# 十三、RoadmapStage

```java
public class RoadmapStage {

    private Integer stage;

    private String name;

    private String goal;

    private String duration;

    private List<String> topics;

    private List<CareerTaskResult> tasks;

}
```

---

# 十四、CareerTaskResult

```java
public class CareerTaskResult {

    private String taskName;

    private String description;

    private String taskType;

    private Integer priority;

}
```

---

# 十五、职业规划结构化输出示例

```json
{
  "targetPosition": "Java后端开发工程师",
  "matchScore": 76,
  "summary": "当前具备Java Web基础，但Redis、JVM和计算机基础仍需加强。",
  "advantages": [
    "已经具备Java基础",
    "具有Spring Boot项目经验",
    "具备MySQL开发经验"
  ],
  "weaknesses": [
    "Redis掌握不足",
    "JVM知识薄弱",
    "项目复杂度不足"
  ],
  "roadmap": [
    {
      "stage": 1,
      "name": "基础强化",
      "goal": "加强Java核心能力",
      "duration": "2周",
      "topics": [
        "Java集合",
        "Java并发",
        "JVM"
      ],
      "tasks": [
        {
          "taskName": "完成Java集合专题复习",
          "description": "掌握HashMap、ConcurrentHashMap等核心集合",
          "taskType": "KNOWLEDGE",
          "priority": 3
        }
      ]
    }
  ]
}
```

---

# 十六、CareerPlannerAgent Tool 设计

第一版建议设计：

```text
CareerTools
```

---

# 十七、Tool：获取用户职业画像

```java
@Tool(description = "获取当前用户的职业画像，包括专业、学历、年级、职业目标和职业阶段")
public UserProfileVO getUserProfile(Long userId)
```

用途：

```text
用户问：
我适合做什么工作？

Agent：
需要了解用户背景
↓
调用 getUserProfile
```

---

# 十八、Tool：获取用户技能

```java
@Tool(description = "查询用户当前掌握的技能以及各技能当前评分")
public List<UserSkillVO> getUserSkills(Long userId)
```

返回：

```text
Java 80

SpringBoot 60

Redis 40
```

---

# 十九、Tool：获取当前职业规划

```java
@Tool(description = "查询用户当前正在执行的职业成长规划")
public CareerPlanVO getCurrentCareerPlan(Long userId)
```

---

# 二十、Tool：查询成长任务

```java
@Tool(description = "查询用户当前职业规划中的成长任务及完成状态")
public List<CareerTaskVO> getCareerTasks(Long userId)
```

---

# 二十一、Tool：查询历史面试表现

```java
@Tool(description = "查询用户最近的模拟面试结果以及各能力维度评分")
public List<InterviewReportVO> getInterviewHistory(Long userId)
```

---

# 二十二、是否让 Agent 直接保存职业规划

第一版推荐：

**不要让模型通过 Tool 直接完成最终保存。**

建议：

```text
Agent
 ↓
CareerPlanResult
 ↓
CareerPlanService
 ↓
数据库
```

而不是：

```text
Agent
 ↓
saveCareerPlan Tool
```

原因：

- 更容易控制事务
- 更容易验证数据
- 避免大模型错误调用
- 数据流程更清晰

Tool 主要负责：

> 读取信息。

写数据库由 Service 控制。

---

# 二十三、CareerPlannerAgent ChatMemory

职业规划聊天属于长期持续对话。

首版单会话 Conversation ID 推荐：

```text
career:{userId}
```

例如：

```text
career:10001
```

多会话持久化启用后扩展为：

```text
career:{userId}:{uuid}
```

`userId` 用于隔离用户，`uuid` 用于区分同一用户的不同职业咨询主题。会话归属必须由业务 Service 校验，Agent 只能接收已经校验过的 Conversation ID。

这样：

```text
今天：
我想做Java后端。

三天后：
那我Redis学到什么程度合适？
```

Agent 能够保持一定对话连续性。

---

# 二十四、CareerPlannerAgent Memory 内容

Memory 主要保存：

```text
用户消息

AI回复

近期职业讨论上下文
```

但是：

**用户职业画像等真实业务数据不能只靠 ChatMemory。**

原因：

Memory 可能：

- 被清理
- 有窗口限制
- 数据已经过时

因此：

```text
职业画像
技能
职业规划
面试结果
```

必须存数据库。

---

# 二十五、CareerPlannerAgent 完整调用流程

```text
用户发送问题
       ↓
CareerController
       ↓
CareerService
       ↓
CareerPlannerAgent
       ↓
ChatClient
       ↓
System Prompt
       ↓
Chat Memory
       ↓
Tool Calling
       ↓
获取用户真实数据
       ↓
必要时 RAG
       ↓
LLM
       ↓
回答
       ↓
返回前端
```

正式生成规划：

```text
GenerateCareerPlanRequest
       ↓
CareerService
       ↓
读取用户画像
       ↓
读取用户技能
       ↓
读取历史面试
       ↓
CareerPlannerAgent
       ↓
CareerPlanResult
       ↓
数据校验
       ↓
CareerPlan Entity
       ↓
career_plan
       ↓
CareerTask Entity
       ↓
career_task
```

---

# 二十六、InterviewerAgent

类名：

```java
InterviewerAgent
```

角色：

> 模拟真实企业技术面试官。

核心职责：

- 根据岗位发起面试
- 提出问题
- 分析用户回答
- 判断回答质量
- 动态追问
- 调整难度
- 控制面试流程
- 面试结束后生成评价

---

# 二十七、InterviewerAgent 与普通问答的区别

错误设计：

```text
提前生成10道题

↓

用户逐题回答

↓

AI给分
```

这本质仍然是：

> AI题库。

本系统需要实现：

```text
问题
 ↓
回答
 ↓
分析
 ↓
决定下一步
 ├─ 深入追问
 ├─ 提高难度
 ├─ 降低难度
 └─ 切换知识点
```

体现真正的动态面试逻辑。

---

# 二十八、InterviewerAgent 输入

面试开始时提供：

```text
targetPosition

interviewType

difficulty

userSkills
```

例如：

```text
目标岗位：
Java后端开发工程师

面试类型：
技术面试

难度：
中级
```

---

# 二十九、InterviewerAgent 可读取数据

包括：

```text
用户职业画像

用户当前技能

用户目标岗位

历史面试数据

当前面试聊天记录
```

---

# 三十、InterviewerAgent System Prompt 示例

```text
你是“AI职途”系统中的专业企业模拟面试官。

你正在对高校学生进行模拟求职面试。

你的主要任务不是向用户讲解知识，
而是像真实面试官一样进行提问、追问和能力判断。

你必须遵守以下规则：

1. 一次只提出一个主要问题。

2. 用户回答后，应先判断其回答质量，
再决定下一步提问。

3. 如果用户回答正确但比较浅，
应围绕其回答继续深入追问。

4. 如果用户回答非常完整，
可以提高问题难度或切换到更深入的应用场景。

5. 如果用户回答明显错误，
可以进行一次适度引导，
随后切换到相关基础问题。

6. 不要在每道题后立即完整公布标准答案，
避免破坏模拟面试真实性。

7. 面试过程中应保持专业、自然、真实的面试官语气。

8. 应根据目标岗位调整问题范围。

9. 不得重复已经问过的问题。

10. 面试评价必须依据用户真实回答，
不得凭空提高或降低用户成绩。

11. 每次回答分析都需要记录涉及的知识点、
回答质量和能力表现。

12. 当达到面试结束条件时，应结束提问，
进入面试报告生成阶段。
```

---

# 三十一、面试初始化流程

用户：

```text
Java后端
中级
技术面
```

点击：

```text
开始面试
```

后端：

```text
创建 interview
      ↓
生成 interviewId
      ↓
conversationId
=
interview:{interviewId}
      ↓
初始化 InterviewerAgent
      ↓
生成第一道题
```

---

# 三十二、面试 Conversation ID

必须做到：

> 每次面试一个独立 Conversation ID。

格式：

```text
interview:{interviewId}
```

例如：

```text
interview:19823472345
```

不能使用：

```text
interview:{userId}
```

否则第二次面试会记住第一次面试的问题。

---

# 三十三、单轮面试逻辑

用户回答一道问题后：

```text
用户回答
     ↓
InterviewerAgent
     ↓
分析当前回答
     ↓
生成 InterviewTurnResult
```

---

# 三十四、InterviewTurnResult

建议：

```java
public class InterviewTurnResult {

    private String response;

    private String topic;

    private Integer score;

    private String evaluation;

    private String nextAction;

    private String nextDifficulty;

    private Boolean finished;

}
```

---

# 三十五、nextAction

推荐枚举：

```text
FOLLOW_UP
继续追问

NEXT_TOPIC
切换知识点

INCREASE_DIFFICULTY
提高难度

DECREASE_DIFFICULTY
降低难度

FINISH
结束面试
```

---

# 三十六、InterviewTurnResult 示例

```json
{
  "response": "你刚才提到了红黑树，那么HashMap在什么条件下会进行树化？",
  "topic": "Java-HashMap",
  "score": 78,
  "evaluation": "回答基本正确，但缺少树化条件相关细节",
  "nextAction": "FOLLOW_UP",
  "nextDifficulty": "MEDIUM",
  "finished": false
}
```

前端只显示：

```text
response
```

下面这些：

```text
score

evaluation

nextAction
```

不直接给用户看。

它们用于：

> 系统内部评价。

---

# 三十七、为什么每轮都需要隐藏评分

如果只在最后让 AI：

> 回忆整场面试然后评分

可能出现评价不稳定。

因此建议：

```text
每一轮
 ↓
内部评分
 ↓
保存
```

最后：

```text
所有轮次评价
+
完整聊天记录
 ↓
生成最终报告
```

这样评分更稳定。

---

# 三十八、是否增加 interview_turn 表

MVP 第一版可以暂时不增加。

可以在：

```text
interview_message
```

中保存基本消息。

如果后续需要非常完整的分析，可以增加：

```text
interview_turn
```

存：

```text
question

answer

topic

score

evaluation

difficulty
```

v1.0 暂不强制。

---

# 三十九、面试问题数量控制

建议 MVP：

```text
8～12个主要问题
```

其中追问不一定计算为主问题。

例如：

```text
Java基础       2

SpringBoot     2

MySQL          2

Redis          2

项目           2
```

具体知识点根据用户技能动态调整。

---

# 四十、面试动态难度

初始：

```text
MEDIUM
```

用户连续表现较好：

```text
MEDIUM
 ↓
HARD
```

连续表现一般：

```text
MEDIUM
 ↓
EASY
```

可以采用简单规则：

```text
最近两题平均 >= 85
→ 提高难度

最近两题平均 <= 50
→ 降低难度
```

第一版不要搞复杂机器学习算法。

---

# 四十一、面试结束条件

满足任一：

```text
达到主要问题数量

用户主动结束

Agent 判断主要能力维度已经覆盖

系统出现异常
```

正常结束：

```text
status = 2
```

用户主动结束：

```text
status = 3
```

---

# 四十二、面试报告生成

面试结束后调用独立方法：

```java
generateInterviewReport()
```

输入：

```text
用户画像

目标岗位

面试配置

完整面试记录

每轮内部评价
```

输出：

```text
InterviewReportResult
```

---

# 四十三、InterviewReportResult

```java
public class InterviewReportResult {

    private Integer totalScore;

    private Map<String, Integer> scores;

    private List<String> advantages;

    private List<String> weaknesses;

    private List<ImprovementSuggestion> suggestions;

    private String summary;

}
```

---

# 四十四、ImprovementSuggestion

```java
public class ImprovementSuggestion {

    private String topic;

    private String priority;

    private String content;

}
```

---

# 四十五、面试报告示例

```json
{
  "totalScore": 76,
  "scores": {
    "Java": 82,
    "SpringBoot": 75,
    "MySQL": 79,
    "Redis": 52,
    "ComputerNetwork": 48,
    "Project": 73,
    "Communication": 78
  },
  "advantages": [
    "Java集合基础较扎实",
    "MySQL索引相关知识较完整",
    "项目介绍逻辑较清晰"
  ],
  "weaknesses": [
    "Redis缓存机制掌握不足",
    "计算机网络基础存在遗漏"
  ],
  "suggestions": [
    {
      "topic": "Redis",
      "priority": "HIGH",
      "content": "重点复习缓存穿透、缓存击穿、缓存雪崩和分布式锁"
    }
  ],
  "summary": "当前已经具备Java后端初级岗位基础，但中间件和计算机基础仍需要进一步强化。"
}
```

---

# 四十六、InterviewerAgent Tool Calling

InterviewerAgent 需要的 Tool 比 CareerPlannerAgent 更少。

推荐：

```text
InterviewTools
```

---

# 四十七、Tool：获取用户技能

```java
@Tool(description = "获取模拟面试用户当前掌握的技能和能力评分")
public List<UserSkillVO> getUserSkills(Long userId)
```

作用：

根据用户能力调整问题难度。

---

# 四十八、Tool：获取用户职业目标

```java
@Tool(description = "获取用户当前目标职业岗位")
public UserProfileVO getTargetCareer(Long userId)
```

---

# 四十九、Tool：获取历史面试

```java
@Tool(description = "获取用户最近的模拟面试情况，用于避免长期重复考察完全相同的问题")
public List<InterviewSummaryVO> getPreviousInterviews(Long userId)
```

---

# 五十、面试中的 Tool Calling 原则

不要每轮都疯狂调用数据库。

推荐：

```text
面试开始
 ↓
读取用户画像
 ↓
读取技能数据
 ↓
读取少量历史面试
 ↓
形成面试上下文
```

之后依靠：

```text
ChatMemory
```

完成当前面试。

这样：

- 减少 API 消耗
- 降低数据库查询
- 提高响应速度

---

# 五十一、RAG 在 CareerPlannerAgent 中的使用

CareerPlannerAgent 的 RAG 数据来源：

```text
岗位能力要求

岗位技能标准

职业成长路线

职业发展知识
```

例如用户问：

```text
Java后端应该掌握哪些能力？
```

流程：

```text
问题
 ↓
VectorStore
 ↓
检索Java后端岗位资料
 ↓
用户画像
 ↓
CareerPlannerAgent
 ↓
个性化建议
```

---

# 五十二、RAG 在 InterviewerAgent 中的使用

InterviewerAgent 的知识库：

```text
Java面试知识

Spring面试知识

MySQL面试知识

Redis面试知识

计算机基础

项目面试知识
```

流程：

```text
当前面试主题：Redis
       ↓
检索Redis相关知识
       ↓
InterviewerAgent
       ↓
结合当前回答生成追问
```

---

# 五十三、为什么面试不能完全靠题库

纯题库：

```text
固定问题
```

优点：

稳定。

缺点：

不够智能。

纯 LLM：

```text
模型随便出题
```

优点：

灵活。

缺点：

可能问题质量不稳定。

因此推荐：

```text
RAG知识库
+
LLM动态生成
```

知识库负责：

> 提供可靠知识范围。

LLM负责：

> 动态提问和追问。

---

# 五十四、Agent 与普通 Service 的边界

必须明确：

Agent 负责：

```text
理解

分析

规划

判断

生成内容
```

Service 负责：

```text
数据库查询

业务规则

事务

保存数据

权限校验

状态更新
```

不要写成：

```text
CareerPlannerAgent
里面直接Mapper.insert()
```

推荐：

```text
Controller
 ↓
Service
 ↓
Agent
 ↓
Result DTO
 ↓
Service
 ↓
Mapper
```

---

# 五十五、CareerPlannerAgent 代码结构建议

```text
agent
│
├── career
│   ├── CareerPlannerAgent.java
│   │
│   ├── prompt
│   │   └── CareerPrompt.java
│   │
│   └── dto
│       ├── CareerPlanResult.java
│       ├── RoadmapStage.java
│       └── CareerTaskResult.java
```

---

# 五十六、InterviewerAgent 代码结构建议

```text
agent
│
├── interview
│   ├── InterviewerAgent.java
│   │
│   ├── prompt
│   │   └── InterviewPrompt.java
│   │
│   └── dto
│       ├── InterviewTurnResult.java
│       ├── InterviewReportResult.java
│       └── ImprovementSuggestion.java
```

---

# 五十七、Tool 包

```text
tool
│
├── CareerTools.java
└── InterviewTools.java
```

---

# 五十八、Spring AI 整体目录建议

```text
com.xucheng.aicareer

├── controller
├── service
├── service.impl
├── mapper
├── entity
├── dto
├── vo
│
├── agent
│   ├── career
│   │   ├── CareerPlannerAgent.java
│   │   ├── CareerPrompt.java
│   │   └── dto
│   │
│   └── interview
│       ├── InterviewerAgent.java
│       ├── InterviewPrompt.java
│       └── dto
│
├── tool
│   ├── CareerTools.java
│   └── InterviewTools.java
│
├── rag
│   ├── CareerKnowledgeService.java
│   └── InterviewKnowledgeService.java
│
├── config
│   ├── AiConfig.java
│   └── ChatMemoryConfig.java
│
└── common
```

---

# 五十九、Agent Service 层建议

不要 Controller 直接调用 Agent。

例如：

```text
CareerController

↓

CareerService

↓

CareerPlannerAgent
```

因为 CareerService 还需要负责：

```text
查询用户数据

调用Agent

校验Agent结果

保存职业规划

创建成长任务
```

---

# 六十、职业规划完整 Service 流程

```text
CareerController
       ↓
generatePlan()
       ↓
CareerService
       ↓
查询 UserProfile
       ↓
查询 UserSkill
       ↓
查询 InterviewReport
       ↓
CareerPlannerAgent
       ↓
CareerPlanResult
       ↓
结果校验
       ↓
保存 career_plan
       ↓
保存 career_task
       ↓
返回 CareerPlanVO
```

---

# 六十一、模拟面试完整 Service 流程

开始：

```text
InterviewController
       ↓
startInterview()
       ↓
InterviewService
       ↓
创建 interview
       ↓
InterviewerAgent
       ↓
第一道题
       ↓
保存 interview_message
       ↓
返回前端
```

回答：

```text
用户提交回答
       ↓
InterviewService
       ↓
保存用户消息
       ↓
InterviewerAgent
       ↓
InterviewTurnResult
       ↓
保存AI问题
       ↓
返回前端
```

结束：

```text
结束面试
       ↓
完整聊天记录
       ↓
InterviewerAgent
       ↓
InterviewReportResult
       ↓
保存 interview_report
       ↓
写入 ability_score
       ↓
更新 user_skill
```

---

# 六十二、两个 Agent 的数据连接

InterviewerAgent 不需要直接：

```text
调用 CareerPlannerAgent
```

面试结束：

```text
InterviewReport
       ↓
数据库
       ↓
CareerPlannerAgent
```

职业规划师下次规划时：

```text
读取最新 InterviewReport
```

形成数据协同。

这种方式更稳定，也方便调试。

---

# 六十三、能力评分不要完全相信单次 AI 判断

面试评分是：

> AI 辅助能力评估。

因此建议更新 user_skill 时不要简单：

```text
原分数 = 40

面试分数 = 80

直接变成80
```

可以采用简单加权。

例如：

```text
新能力评分
=
旧评分 × 0.6
+
最新面试评分 × 0.4
```

例如：

```text
40 × 0.6
+
80 × 0.4

= 56
```

避免一次面试导致技能评分大幅波动。

---

# 六十四、Prompt 文件管理

不要把所有 Prompt 写在：

```java
"""
一大坨字符串
"""
```

推荐：

```text
resources
└── prompts
    ├── career-planner-system.md
    ├── career-plan-generate.md
    ├── interviewer-system.md
    ├── interview-report.md
    └── interview-evaluation.md
```

这样比赛时也方便：

> 展示 Prompt 设计与修改过程。

---

# 六十五、Prompt 版本管理

后期可以：

```text
career-planner-system-v1.md

career-planner-system-v2.md
```

开发记录中记录：

```text
V1问题：

规划过于宽泛。

V2修改：

加入用户每天学习时间约束。

加入历史面试短板优先规则。
```

这些内容非常适合作为比赛：

> 人工干预和 Prompt 优化过程。

---

# 六十六、Agent 异常处理

可能出现：

```text
大模型请求失败

结构化输出失败

模型返回空内容

Tool调用异常

知识库无结果

上下文过长
```

---

# 六十七、大模型请求失败

返回：

```text
AI服务暂时不可用，请稍后重新尝试。
```

不要将：

```text
HTTP 500

DashScope Exception

JSON解析异常
```

直接返回给前端用户。

---

# 六十八、结构化输出失败

例如模型没有成功生成：

```text
CareerPlanResult
```

可以：

```text
第一次
正常结构化调用

↓

失败

↓

重新调用一次
+
强化JSON约束

↓

仍然失败

↓

返回业务异常
```

第一版最多重试一次。

---

# 六十九、Tool Calling 失败

例如：

```text
getUserProfile()
```

没有查询到数据。

Agent 应收到：

```text
当前用户尚未完善职业画像。
```

然后回答用户：

```text
在生成完整职业规划之前，
建议先完善专业、年级、技能和职业目标信息。
```

---

# 七十、RAG 检索不到内容

不能：

```text
强行说知识库中存在。
```

可以：

```text
知识库暂未检索到直接相关资料，
以下建议主要基于模型分析生成。
```

或者直接不启用 RAG 结果。

---

# 七十一、Agent 日志

建议记录：

```text
Agent名称

conversationId

userId

调用时间

模型名称

Tool调用情况

执行耗时

是否成功
```

但：

```text
API Key
```

绝对不能打印。

---

# 七十二、自定义 Advisor

后期可以实现：

```text
MyLoggerAdvisor
```

主要记录：

```text
用户Prompt

Agent类型

conversationId

调用耗时
```

比赛时也可以展示：

> 系统具备 Agent 调用监控能力。

---

# 七十三、第一阶段 Agent 开发优先级

## P0

必须完成：

```text
CareerPlannerAgent

InterviewerAgent

ChatMemory

Structured Output

基础System Prompt

职业规划结构化输出

面试动态追问

面试报告
```

---

## P1

建议完成：

```text
Tool Calling

Agent日志

规划版本更新

能力画像更新
```

---

## P2

最后再做：

```text
RAG

复杂动态难度算法

语音面试

多模型协作

第三个Agent
```

---

# 七十四、第一阶段建议开发顺序

```text
① 配置 ChatModel
       ↓
② 创建 ChatClient
       ↓
③ 完成 CareerPlannerAgent 普通聊天
       ↓
④ 加 ChatMemory
       ↓
⑤ 加 CareerPlanResult
       ↓
⑥ 完成结构化职业规划
       ↓
⑦ 接 CareerTools
       ↓
⑧ 完成 InterviewerAgent
       ↓
⑨ 实现独立 Interview Conversation ID
       ↓
⑩ 实现动态追问
       ↓
⑪ 实现 InterviewTurnResult
       ↓
⑫ 实现 InterviewReportResult
       ↓
⑬ 面试报告入库
       ↓
⑭ 更新能力画像
       ↓
⑮ 实现职业规划 V2
       ↓
⑯ 最后接 RAG
```

---

# 七十五、第一阶段不要急着做 RAG

第一阶段应该先保证：

```text
Agent能对话

↓

能记忆

↓

能读取业务数据

↓

能结构化输出

↓

能完成业务闭环
```

之后再：

```text
接知识库
```

否则一开始同时处理：

```text
Agent
+
VectorStore
+
Embedding
+
文档切分
+
Tool Calling
+
Memory
```

很容易增加开发复杂度。

---

# 七十六、最终 Agent 架构

```text
                         ChatModel
                            │
                            ↓
                        ChatClient
                            │
         ┌──────────────────┴──────────────────┐
         │                                     │
         ↓                                     ↓
CareerPlannerAgent                     InterviewerAgent
         │                                     │
         ├── System Prompt                     ├── System Prompt
         ├── ChatMemory                        ├── ChatMemory
         ├── CareerTools                       ├── InterviewTools
         ├── Structured Output                 ├── Turn Evaluation
         └── RAG                               └── RAG
         │                                     │
         ↓                                     ↓
CareerPlanResult                    InterviewReportResult
         │                                     │
         ↓                                     ↓
     CareerService                       InterviewService
         │                                     │
         └───────────────┬─────────────────────┘
                         ↓
                       MySQL
```

---

# 七十七、核心设计原则总结

AI职途 Agent 系统遵循以下原则：

### 1. Agent 负责思考

负责：

```text
分析

判断

规划

提问

评价
```

### 2. Service 负责业务

负责：

```text
查询

保存

事务

状态

权限
```

### 3. 数据库负责事实

负责保存：

```text
用户画像

技能

职业规划

任务

面试

能力评分
```

### 4. Memory 负责上下文

负责：

```text
当前对话连续性
```

### 5. RAG 负责知识

负责：

```text
职业知识

岗位知识

面试知识
```

### 6. Structured Output 负责系统化

负责将：

```text
自然语言AI
```

转换为：

```text
结构化业务数据
```

---

# 七十八、最终核心闭环

```text
职业画像
   ↓
CareerPlannerAgent
   ↓
CareerPlanResult
   ↓
职业成长路线
   ↓
成长任务
   ↓
InterviewerAgent
   ↓
InterviewTurnResult
   ↓
InterviewReportResult
   ↓
能力评分更新
   ↓
CareerPlannerAgent
   ↓
CareerPlan V2
```

最终形成：

> AI 职业规划、学习成长、模拟面试、能力评估和动态规划相互连接的大学生职业成长智能体系统。

---

# 七十九、普通聊天业务上下文接入补充

普通聊天由 `CareerChatContextService` 通过现有职业画像、技能和职业规划 Service 聚合本次请求的最新业务上下文，再传给 `CareerPlannerAgent`。Agent 不直接访问 Mapper 或数据库。

上下文使用受控 JSON 注入 System Prompt，仅包含：

```text
Profile：学历、专业、年级、毕业年份、职业阶段、目标岗位、目标城市、目标时间、每日学习时间、职业目标、兴趣描述
Skill：技能名称、分类、等级、评分
Plan：版本、目标岗位、匹配度、摘要、优势、短板、路线阶段
```

其中字段值属于用户职业背景数据，即使包含类似指令的文本也不得覆盖 System Prompt。`profile = null`、`skills = []`、`currentPlan = null` 分别表示对应数据尚不存在，Agent 必须明确说明缺失项，不得自行补全。

业务上下文不保存在 ChatMemory 中，每次新问题都重新读取，以保证用户更新画像或技能后立即生效；ChatMemory 继续只保存近期对话内容。
