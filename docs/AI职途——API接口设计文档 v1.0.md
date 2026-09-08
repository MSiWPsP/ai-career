# AI职途——大学生智能职业成长平台
## API 接口设计文档 v1.0

---

# 一、文档目标

本文档用于定义 AI职途 系统前后端交互接口。

主要目标：

1. 明确前端页面需要调用哪些接口；
2. 统一接口路径、请求方式和返回格式；
3. 明确 Agent 相关接口；
4. 明确职业规划和模拟面试的数据流；
5. 为 Spring Boot Controller、Service 和 Vue Axios 调用提供开发依据。

---

# 二、接口总体规范

系统基础接口前缀建议：

```text
/api
```

例如：

```text
/api/user/login

/api/profile

/api/career/plan

/api/interview/start
```

---

# 三、统一返回结果

推荐统一封装：

```java
public class Result<T> {

    private Integer code;

    private String message;

    private T data;

}
```

成功：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

失败：

```json
{
  "code": 400,
  "message": "参数错误",
  "data": null
}
```

---

# 四、状态码建议

建议业务层使用：

| code | 含义 |
|---|---|
| 200 | 成功 |
| 400 | 参数错误 |
| 401 | 未登录 |
| 403 | 无权限 |
| 404 | 数据不存在 |
| 409 | 业务冲突 |
| 500 | 系统异常 |
| 5001 | AI 服务异常 |
| 5002 | Agent 输出解析失败 |
| 5003 | AI Tool 调用异常 |

---

# 五、JWT 认证

登录成功后返回：

```text
token
```

前端后续请求：

```http
Authorization: Bearer {token}
```

前端 Axios 拦截器统一携带 Token。

---

# 六、接口模块划分

系统 API 划分为：

```text
/api/auth
用户认证

/api/user
用户信息

/api/profile
职业画像

/api/skill
技能画像

/api/career
职业规划

/api/task
成长任务

/api/interview
AI模拟面试

/api/ability
能力成长

/api/knowledge
知识库
```

---

# 七、认证模块

## 7.1 用户登录

接口：

```http
POST /api/auth/login
```

请求：

```json
{
  "username": "xucheng",
  "password": "123456"
}
```

返回：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "token": "xxxxxx",
    "userId": 10001,
    "nickname": "许城",
    "avatar": ""
  }
}
```

---

## 7.2 用户注册

```http
POST /api/auth/register
```

请求：

```json
{
  "username": "xucheng",
  "password": "123456",
  "nickname": "许城"
}
```

返回：

```json
{
  "code": 200,
  "message": "注册成功",
  "data": null
}
```

---

# 八、用户模块

## 8.1 获取当前用户信息

```http
GET /api/user/me
```

返回：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 10001,
    "username": "xucheng",
    "nickname": "许城",
    "avatar": "",
    "role": "student"
  }
}
```

---

## 8.2 修改用户信息

```http
PUT /api/user/me
```

请求：

```json
{
  "nickname": "许城",
  "avatar": "https://xxx.com/avatar.png"
}
```

---

# 九、职业画像模块

## 9.1 获取当前职业画像

```http
GET /api/profile
```

返回：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "education": "本科",
    "major": "软件工程",
    "grade": "大三",
    "graduationYear": 2028,
    "careerStage": "LEARNING",
    "targetPosition": "Java后端开发工程师",
    "targetCity": "南京",
    "targetTime": "6个月",
    "dailyStudyHours": 3,
    "careerGoal": "半年后寻找Java后端实习"
  }
}
```

---

## 9.2 创建职业画像

```http
POST /api/profile
```

请求：

```json
{
  "education": "本科",
  "major": "软件工程",
  "grade": "大三",
  "graduationYear": 2028,
  "careerStage": "LEARNING",
  "targetPosition": "Java后端开发工程师",
  "targetCity": "南京",
  "targetTime": "6个月",
  "dailyStudyHours": 3,
  "careerGoal": "半年后寻找Java后端实习"
}
```

---

## 9.3 修改职业画像

```http
PUT /api/profile
```

请求结构与创建相同。

---

## 9.4 判断职业画像是否完善

```http
GET /api/profile/completion
```

返回：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "completed": true,
    "score": 90,
    "missingFields": []
  }
}
```

如果没有完善：

```json
{
  "completed": false,
  "score": 60,
  "missingFields": [
    "targetPosition",
    "dailyStudyHours"
  ]
}
```

该接口用于：

> 用户进入 AI 职业规划前进行检查。

---

# 十、技能画像模块

## 10.1 获取用户技能列表

```http
GET /api/skill
```

返回：

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "skillName": "Java",
      "skillCategory": "PROGRAMMING",
      "level": 4,
      "score": 80,
      "source": "SELF"
    },
    {
      "id": 2,
      "skillName": "Redis",
      "skillCategory": "MIDDLEWARE",
      "level": 2,
      "score": 40,
      "source": "SELF"
    }
  ]
}
```

---

# 十一、批量保存技能

职业画像页面通常会一次选择很多技能。

因此不建议：

```text
一个技能一个POST请求
```

推荐：

```http
PUT /api/skill
```

请求：

```json
{
  "skills": [
    {
      "skillName": "Java",
      "skillCategory": "PROGRAMMING",
      "level": 4
    },
    {
      "skillName": "SpringBoot",
      "skillCategory": "FRAMEWORK",
      "level": 3
    },
    {
      "skillName": "Redis",
      "skillCategory": "MIDDLEWARE",
      "level": 2
    }
  ]
}
```

Service 自动根据等级生成：

```text
score
```

例如：

```text
level = 4
score = 80
```

---

# 十二、职业规划模块总体接口

```text
/api/career/chat

/api/career/plan/generate

/api/career/plan/current

/api/career/plan/history

/api/career/plan/{id}

/api/career/plan/regenerate
```

---

# 十三、AI职业规划师普通聊天

接口：

```http
POST /api/career/chat
```

请求：

```json
{
  "message": "我现在应该先学Redis还是微服务？"
}
```

不需要前端传 userId。

后端从：

```text
JWT
```

获取当前用户。

---

## 13.1 返回

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "conversationId": "career:10001",
    "content": "结合你目前的技能情况，建议先学习Redis……"
  }
}
```

---

# 十四、职业规划聊天流程

```text
Vue
 ↓
POST /career/chat
 ↓
CareerController
 ↓
CareerService
 ↓
CareerPlannerAgent
 ↓
ChatClient
 ↓
ChatMemory
 ↓
Tool Calling
 ↓
返回自然语言答案
```

---

# 十五、生成正式职业规划

```http
POST /api/career/plan/generate
```

该接口用于用户第一次点击：

```text
生成职业规划
```

请求可以非常简单：

```json
{}
```

因为：

```text
用户画像
技能
历史数据
```

全部由后端读取。

---

# 十六、CareerPlannerAgent 数据输入

Service 获取：

```text
UserProfile

UserSkill

CareerTask

InterviewReport
```

然后调用：

```text
CareerPlannerAgent
```

最终生成：

```text
CareerPlanResult
```

---

# 十七、生成职业规划返回

```json
{
  "code": 200,
  "message": "职业规划生成成功",
  "data": {
    "id": 20001,
    "version": 1,
    "targetPosition": "Java后端开发工程师",
    "matchScore": 76,
    "summary": "当前具备Java Web基础，但Redis和JVM仍需加强。",
    "advantages": [
      "Java基础较好",
      "具备Spring Boot项目经验"
    ],
    "weaknesses": [
      "Redis能力不足",
      "JVM知识较弱"
    ],
    "roadmap": [
      {
        "stage": 1,
        "name": "Java核心强化",
        "goal": "提升Java核心知识掌握程度",
        "duration": "2周",
        "topics": [
          "集合",
          "并发",
          "JVM"
        ]
      }
    ]
  }
}
```

---

# 十八、获取当前职业规划

```http
GET /api/career/plan/current
```

返回当前：

```text
status = 1
```

的职业规划。

---

# 十九、获取历史职业规划

```http
GET /api/career/plan/history
```

返回：

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 20003,
      "version": 3,
      "targetPosition": "Java后端开发工程师",
      "matchScore": 82,
      "createTime": "2026-09-20 10:00:00"
    },
    {
      "id": 20002,
      "version": 2,
      "matchScore": 79,
      "createTime": "2026-09-15 10:00:00"
    }
  ]
}
```

---

# 二十、获取指定职业规划

```http
GET /api/career/plan/{id}
```

用于：

> 查看历史规划详情。

---

# 二十一、根据最新能力重新规划

```http
POST /api/career/plan/regenerate
```

请求：

```json
{
  "reason": "INTERVIEW",
  "sourceInterviewId": 30001
}
```

后端：

```text
读取旧规划
 ↓
读取最新面试
 ↓
读取最新技能
 ↓
CareerPlannerAgent
 ↓
生成新规划
 ↓
旧规划 status = 0
 ↓
新规划 status = 1
```

---

# 二十二、成长任务模块

接口：

```text
GET  /api/task
GET  /api/task/{id}
PUT  /api/task/{id}/status
GET  /api/task/statistics
```

---

# 二十三、获取当前成长任务

```http
GET /api/task
```

可支持查询参数：

```http
GET /api/task?status=0
```

或者：

```http
GET /api/task?planId=20001
```

---

# 二十四、任务列表返回

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 50001,
      "stageName": "Java核心强化",
      "taskName": "复习HashMap",
      "taskDescription": "掌握底层结构和扩容机制",
      "taskType": "KNOWLEDGE",
      "priority": 3,
      "status": 0,
      "deadline": "2026-09-15"
    }
  ]
}
```

---

# 二十五、修改任务状态

```http
PUT /api/task/{id}/status
```

请求：

```json
{
  "status": 2
}
```

其中：

```text
0 待开始

1 进行中

2 已完成

3 已跳过
```

---

# 二十六、获取成长任务统计

```http
GET /api/task/statistics
```

返回：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 10,
    "completed": 4,
    "processing": 2,
    "waiting": 4,
    "completionRate": 40
  }
}
```

首页 Dashboard 可以直接使用。

---

# 二十七、AI模拟面试模块总体接口

```text
POST /api/interview/start

POST /api/interview/{id}/answer

POST /api/interview/{id}/finish

GET /api/interview/{id}

GET /api/interview/{id}/messages

GET /api/interview/{id}/report

GET /api/interview/history
```

---

# 二十八、开始模拟面试

```http
POST /api/interview/start
```

请求：

```json
{
  "targetPosition": "Java后端开发工程师",
  "interviewType": "TECHNICAL",
  "difficulty": "MEDIUM",
  "maxQuestions": 10
}
```

---

# 二十九、开始面试后端逻辑

```text
JWT获取userId
       ↓
创建 interview
       ↓
生成 interviewId
       ↓
conversationId =
interview:{interviewId}
       ↓
获取用户技能
       ↓
InterviewerAgent
       ↓
生成第一题
       ↓
保存 interview_message
       ↓
返回前端
```

---

# 三十、开始面试返回

```json
{
  "code": 200,
  "message": "面试开始",
  "data": {
    "interviewId": 30001,
    "conversationId": "interview:30001",
    "status": 1,
    "question": "请介绍一下HashMap的底层数据结构。"
  }
}
```

---

# 三十一、提交用户回答

接口：

```http
POST /api/interview/{id}/answer
```

请求：

```json
{
  "answer": "JDK8中的HashMap底层是数组、链表和红黑树。"
}
```

---

# 三十二、提交回答后端流程

```text
用户回答
 ↓
保存 user message
 ↓
读取 interview
 ↓
InterviewerAgent
 ↓
分析当前回答
 ↓
InterviewTurnResult
 ↓
保存 assistant message
 ↓
更新 question_count
 ↓
返回下一问题
```

---

# 三十三、面试单轮返回

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "interviewId": 30001,
    "message": "你提到了红黑树，那么HashMap在什么情况下会进行树化？",
    "finished": false
  }
}
```

前端不展示：

```text
score

evaluation

nextAction
```

这些内部数据可以由后端自己保存或参与最终计算。

---

# 三十四、内部 InterviewTurnResult

后端内部：

```json
{
  "response": "你提到了红黑树，那么HashMap在什么情况下会进行树化？",
  "topic": "Java-HashMap",
  "score": 78,
  "evaluation": "回答基本正确，但缺少树化条件",
  "nextAction": "FOLLOW_UP",
  "nextDifficulty": "MEDIUM",
  "finished": false
}
```

前端只获得：

```text
response

finished
```

---

# 三十五、为什么不能把内部评分直接返回前端

真实面试过程中，如果每回答一句立即显示：

```text
78分
```

会破坏模拟面试体验。

因此：

```text
面试过程
隐藏评价

面试结束
统一展示报告
```

---

# 三十六、主动结束面试

```http
POST /api/interview/{id}/finish
```

请求：

```json
{}
```

后端：

```text
更新interview状态
 ↓
读取完整消息记录
 ↓
生成面试报告
 ↓
保存interview_report
 ↓
写ability_score
 ↓
更新user_skill
```

---

# 三十七、Agent 自动结束

如果：

```text
finished = true
```

后端也可以自动调用：

```text
finishInterview()
```

无需用户再次请求。

---

# 三十八、结束面试返回

```json
{
  "code": 200,
  "message": "面试已结束",
  "data": {
    "interviewId": 30001,
    "reportId": 40001
  }
}
```

前端随后：

```text
跳转面试报告页
```

---

# 三十九、获取面试详情

```http
GET /api/interview/{id}
```

返回：

```json
{
  "id": 30001,
  "targetPosition": "Java后端开发工程师",
  "interviewType": "TECHNICAL",
  "difficulty": "MEDIUM",
  "status": 2,
  "questionCount": 10,
  "startTime": "...",
  "endTime": "..."
}
```

---

# 四十、获取完整聊天记录

```http
GET /api/interview/{id}/messages
```

返回：

```json
[
  {
    "role": "assistant",
    "content": "请介绍一下HashMap的底层结构。",
    "messageOrder": 1
  },
  {
    "role": "user",
    "content": "JDK8是数组、链表和红黑树。",
    "messageOrder": 2
  }
]
```

---

# 四十一、获取面试报告

```http
GET /api/interview/{id}/report
```

返回：

```json
{
  "code": 200,
  "message": "success",
  "data": {
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
      "Java集合基础较好",
      "MySQL知识掌握较完整"
    ],
    "weaknesses": [
      "Redis能力较弱",
      "网络基础不足"
    ],
    "suggestions": [
      {
        "topic": "Redis",
        "priority": "HIGH",
        "content": "重点学习缓存穿透、缓存击穿和缓存雪崩"
      }
    ],
    "summary": "当前已经具备Java后端基础能力，但中间件和计算机基础仍需强化。"
  }
}
```

---

# 四十二、获取历史面试

```http
GET /api/interview/history
```

支持分页：

```http
GET /api/interview/history?page=1&pageSize=10
```

返回：

```json
{
  "records": [
    {
      "id": 30001,
      "targetPosition": "Java后端开发工程师",
      "interviewType": "TECHNICAL",
      "difficulty": "MEDIUM",
      "totalScore": 76,
      "createTime": "2026-09-10 15:00:00"
    }
  ],
  "total": 5
}
```

---

# 四十三、能力成长模块

接口：

```text
GET /api/ability/current

GET /api/ability/history

GET /api/ability/radar

GET /api/ability/trend
```

---

# 四十四、获取当前能力画像

```http
GET /api/ability/current
```

返回：

```json
{
  "Java": 82,
  "SpringBoot": 72,
  "MySQL": 75,
  "Redis": 56,
  "ComputerNetwork": 50
}
```

---

# 四十五、获取雷达图数据

```http
GET /api/ability/radar
```

返回：

```json
{
  "indicators": [
    {
      "name": "Java",
      "max": 100
    },
    {
      "name": "SpringBoot",
      "max": 100
    },
    {
      "name": "MySQL",
      "max": 100
    },
    {
      "name": "Redis",
      "max": 100
    }
  ],
  "values": [
    82,
    72,
    75,
    56
  ]
}
```

前端可以直接喂：

```text
ECharts radar
```

---

# 四十六、获取能力趋势

```http
GET /api/ability/trend
```

可以：

```http
GET /api/ability/trend?abilityName=Redis
```

返回：

```json
{
  "abilityName": "Redis",
  "records": [
    {
      "score": 40,
      "date": "2026-09-01"
    },
    {
      "score": 55,
      "date": "2026-09-10"
    },
    {
      "score": 68,
      "date": "2026-09-20"
    }
  ]
}
```

---

# 四十七、Dashboard首页接口

为了避免首页请求十几个接口，可以额外聚合：

```http
GET /api/dashboard
```

返回：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "targetPosition": "Java后端开发工程师",
    "matchScore": 78,
    "taskStatistics": {
      "total": 10,
      "completed": 4,
      "completionRate": 40
    },
    "latestInterview": {
      "id": 30001,
      "score": 76,
      "date": "2026-09-10"
    },
    "abilityRadar": {
      "Java": 82,
      "SpringBoot": 72,
      "MySQL": 75,
      "Redis": 56
    }
  }
}
```

---

# 四十八、RAG知识库接口

RAG 第一阶段可以先不开放给普通用户。

管理员接口：

```text
POST   /api/knowledge

GET    /api/knowledge

DELETE /api/knowledge/{id}

POST   /api/knowledge/{id}/index
```

---

# 四十九、上传知识文档

```http
POST /api/knowledge
```

使用：

```text
multipart/form-data
```

参数：

```text
file

category
```

例如：

```text
category = JAVA_INTERVIEW
```

后端：

```text
文件
 ↓
文本解析
 ↓
Document
 ↓
切分
 ↓
Embedding
 ↓
VectorStore
```

---

# 五十、知识分类

可以：

```text
CAREER_POSITION

CAREER_PATH

JAVA_INTERVIEW

SPRING_INTERVIEW

MYSQL_INTERVIEW

REDIS_INTERVIEW

COMPUTER_BASIC
```

---

# 五十一、接口对应 Controller

建议：

```text
AuthController

UserController

ProfileController

SkillController

CareerController

TaskController

InterviewController

AbilityController

DashboardController

KnowledgeController
```

---

# 五十二、AuthController

```java
@RestController
@RequestMapping("/api/auth")
public class AuthController {

}
```

包含：

```text
POST /login

POST /register
```

---

# 五十三、ProfileController

```java
@RestController
@RequestMapping("/api/profile")
public class ProfileController {

}
```

包含：

```text
GET /

POST /

PUT /

GET /completion
```

---

# 五十四、CareerController

```java
@RestController
@RequestMapping("/api/career")
public class CareerController {

}
```

包含：

```text
POST /chat

POST /plan/generate

GET /plan/current

GET /plan/history

GET /plan/{id}

POST /plan/regenerate
```

---

# 五十五、InterviewController

```java
@RestController
@RequestMapping("/api/interview")
public class InterviewController {

}
```

包含：

```text
POST /start

POST /{id}/answer

POST /{id}/finish

GET /{id}

GET /{id}/messages

GET /{id}/report

GET /history
```

---

# 五十六、接口不要直接返回 Entity

错误：

```java
@GetMapping
public UserProfile getProfile()
```

推荐：

```java
@GetMapping
public Result<UserProfileVO> getProfile()
```

原因：

Entity 是：

> 数据库模型。

VO 是：

> 前端模型。

DTO 是：

> 请求数据模型。

Agent Result 是：

> AI结构化结果。

四者职责不同。

---

# 五十七、DTO 建议

请求对象：

```text
LoginDTO

RegisterDTO

UserProfileDTO

UserSkillBatchDTO

CareerChatDTO

CareerRegenerateDTO

InterviewStartDTO

InterviewAnswerDTO

TaskStatusDTO
```

---

# 五十八、VO 建议

返回：

```text
UserVO

UserProfileVO

UserSkillVO

CareerPlanVO

CareerTaskVO

InterviewVO

InterviewMessageVO

InterviewReportVO

AbilityRadarVO

AbilityTrendVO

DashboardVO
```

---

# 五十九、Agent Result 建议

单独：

```text
CareerPlanResult

RoadmapStage

CareerTaskResult

InterviewTurnResult

InterviewReportResult

ImprovementSuggestion
```

不要和：

```text
VO
```

混用。

---

# 六十、职业规划接口调用关系

```text
POST /career/plan/generate
          ↓
CareerController
          ↓
CareerService
          ↓
获取Profile
          ↓
获取Skills
          ↓
CareerPlannerAgent
          ↓
CareerPlanResult
          ↓
保存CareerPlan
          ↓
保存CareerTask
          ↓
CareerPlanVO
```

---

# 六十一、模拟面试接口调用关系

```text
POST /interview/start
         ↓
InterviewController
         ↓
InterviewService
         ↓
创建Interview
         ↓
InterviewerAgent
         ↓
第一道问题
         ↓
保存Message
         ↓
返回
```

回答：

```text
POST /interview/{id}/answer
         ↓
保存用户回答
         ↓
InterviewerAgent
         ↓
InterviewTurnResult
         ↓
保存AI问题
         ↓
返回下一题
```

---

# 六十二、面试结束调用关系

```text
finish
 ↓
读取完整面试
 ↓
InterviewerAgent
 ↓
InterviewReportResult
 ↓
保存Report
 ↓
写AbilityScore
 ↓
更新UserSkill
 ↓
返回Report
```

---

# 六十三、自动重新规划

面试报告页面可以显示按钮：

```text
根据本次面试调整学习规划
```

点击：

```http
POST /api/career/plan/regenerate
```

请求：

```json
{
  "reason": "INTERVIEW",
  "sourceInterviewId": 30001
}
```

然后：

```text
CareerPlannerAgent
```

重新生成。

这是整个项目最重要的：

> Agent协作闭环接口。

---

# 六十四、权限控制

普通学生用户只能访问自己的：

```text
职业画像

职业规划

任务

面试

报告
```

因此：

**不要让前端随便传 userId。**

例如：

错误：

```http
GET /api/profile?userId=10001
```

推荐：

```http
GET /api/profile
```

后端：

```text
JWT
 ↓
解析当前userId
```

避免：

> 用户修改 URL 查看其他人的数据。

---

# 六十五、ID权限校验

例如：

```http
GET /api/interview/30001/report
```

Service 必须判断：

```text
interview.user_id
==
当前登录userId
```

否则返回：

```text
403
```

---

# 六十六、分页规范

统一：

```text
page

pageSize
```

例如：

```http
GET /api/interview/history?page=1&pageSize=10
```

返回：

```json
{
  "records": [],
  "total": 50,
  "page": 1,
  "pageSize": 10
}
```

---

# 六十七、参数校验

Spring Boot 可以使用：

```text
jakarta.validation
```

例如：

```java
@NotBlank
private String targetPosition;

@NotNull
private String interviewType;
```

Controller：

```java
public Result<?> start(
        @Valid @RequestBody InterviewStartDTO dto
)
```

---

# 六十八、全局异常处理

建议：

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

}
```

统一处理：

```text
MethodArgumentNotValidException

BusinessException

AiServiceException

Exception
```

---

# 六十九、AI异常统一处理

例如：

```text
大模型超时

结构化输出失败

DashScope异常

Tool异常
```

前端统一收到：

```json
{
  "code": 5001,
  "message": "AI服务暂时不可用，请稍后重试",
  "data": null
}
```

不要把：

```text
模型SDK异常堆栈
```

直接暴露给前端。

---

# 七十、接口日志

建议记录：

```text
requestId

userId

接口

请求时间

执行耗时

是否成功
```

Agent 接口额外记录：

```text
Agent名称

conversationId

model

AI耗时
```

---

# 七十一、职业规划接口不建议流式输出第一版

普通聊天：

```text
POST /career/chat
```

后面可以升级 SSE：

```text
text/event-stream
```

实现打字机效果。

但是比赛 MVP 第一阶段：

```text
普通HTTP
```

已经足够。

等核心功能跑通再升级。

---

# 七十二、面试聊天也可以后期 SSE

后期：

```text
InterviewerAgent
 ↓
Stream
 ↓
SSE
 ↓
Vue
```

实现类似：

> ChatGPT 打字输出。

但是仍然建议：

```text
MVP先用call()
```

避免初期复杂度过高。

---

# 七十三、前端 API 文件划分

Vue3 可以：

```text
src/api
│
├── auth.js
├── user.js
├── profile.js
├── skill.js
├── career.js
├── task.js
├── interview.js
├── ability.js
└── dashboard.js
```

---

# 七十四、career.js

例如：

```javascript
export const generateCareerPlan = () => {
  return request.post('/career/plan/generate')
}

export const getCurrentCareerPlan = () => {
  return request.get('/career/plan/current')
}

export const careerChat = (data) => {
  return request.post('/career/chat', data)
}
```

---

# 七十五、interview.js

例如：

```javascript
export const startInterview = (data) => {
  return request.post('/interview/start', data)
}

export const answerInterview = (id, data) => {
  return request.post(`/interview/${id}/answer`, data)
}

export const finishInterview = (id) => {
  return request.post(`/interview/${id}/finish`)
}

export const getInterviewReport = (id) => {
  return request.get(`/interview/${id}/report`)
}
```

---

# 七十六、第一阶段必须完成的接口

P0：

```text
POST /auth/login

GET /profile

POST /profile

PUT /profile

GET /skill

PUT /skill


POST /career/chat

POST /career/plan/generate

GET /career/plan/current


GET /task

PUT /task/{id}/status


POST /interview/start

POST /interview/{id}/answer

POST /interview/{id}/finish

GET /interview/{id}/report


GET /ability/radar
```

---

# 七十七、第二阶段接口

P1：

```text
GET /career/plan/history

POST /career/plan/regenerate

GET /task/statistics

GET /interview/history

GET /interview/{id}/messages

GET /ability/trend

GET /dashboard
```

---

# 七十八、第三阶段接口

P2：

```text
知识库管理

文件上传

RAG索引

管理员后台

SSE流式输出

语音面试
```

---

# 七十九、完整核心接口清单

| 模块 | 方法 | API | 作用 |
|---|---|---|---|
| 认证 | POST | `/api/auth/login` | 登录 |
| 认证 | POST | `/api/auth/register` | 注册 |
| 用户 | GET | `/api/user/me` | 当前用户 |
| 画像 | GET | `/api/profile` | 获取画像 |
| 画像 | POST | `/api/profile` | 创建画像 |
| 画像 | PUT | `/api/profile` | 修改画像 |
| 技能 | GET | `/api/skill` | 获取技能 |
| 技能 | PUT | `/api/skill` | 批量保存技能 |
| 职业 | POST | `/api/career/chat` | AI职业咨询 |
| 职业 | POST | `/api/career/plan/generate` | 生成规划 |
| 职业 | GET | `/api/career/plan/current` | 当前规划 |
| 职业 | GET | `/api/career/plan/history` | 历史规划 |
| 职业 | POST | `/api/career/plan/regenerate` | 重新规划 |
| 任务 | GET | `/api/task` | 成长任务 |
| 任务 | PUT | `/api/task/{id}/status` | 更新任务 |
| 面试 | POST | `/api/interview/start` | 开始面试 |
| 面试 | POST | `/api/interview/{id}/answer` | 回答问题 |
| 面试 | POST | `/api/interview/{id}/finish` | 结束面试 |
| 面试 | GET | `/api/interview/{id}/report` | 面试报告 |
| 面试 | GET | `/api/interview/history` | 历史面试 |
| 能力 | GET | `/api/ability/radar` | 技能雷达图 |
| 能力 | GET | `/api/ability/trend` | 能力趋势 |
| 首页 | GET | `/api/dashboard` | Dashboard |

---

# 八十、项目核心 API 链路

最终前后端最核心的一条链：

```text
POST /profile
      ↓
PUT /skill
      ↓
POST /career/plan/generate
      ↓
GET /career/plan/current
      ↓
GET /task
      ↓
POST /interview/start
      ↓
POST /interview/{id}/answer
      ↓
POST /interview/{id}/finish
      ↓
GET /interview/{id}/report
      ↓
GET /ability/radar
      ↓
POST /career/plan/regenerate
```

这条链完整对应：

> 职业画像 → AI职业规划 → 成长任务 → AI模拟面试 → 能力诊断 → 动态重新规划。

---

# 八十一、开发建议

正式开发时建议顺序：

```text
① Result统一返回

② JWT登录

③ User/Profile/Skill普通CRUD

④ CareerPlan普通业务结构

⑤ 接CareerPlannerAgent

⑥ 成长任务

⑦ Interview普通表结构

⑧ 接InterviewerAgent

⑨ 面试报告

⑩ AbilityScore

⑪ 动态重新规划

⑫ RAG

⑬ SSE流式输出
```

不要一开始先写：

```text
RAG
+
Agent
+
JWT
+
SSE
+
复杂前端
```

否则调试范围过大。

---

# 八十二、接口设计核心原则

### 1. 前端不传 userId

通过 JWT 获取当前用户。

### 2. Agent 不直接控制数据库

Service 负责最终保存。

### 3. Agent 输出使用专用 Result DTO

例如：

```text
CareerPlanResult

InterviewTurnResult

InterviewReportResult
```

### 4. 前端获得的是 VO

避免直接暴露 Entity。

### 5. AI内部分析不全部暴露给前端

例如面试单轮：

```text
score

evaluation

nextAction
```

属于内部数据。

### 6. 历史数据不覆盖

职业规划和面试报告均保留历史记录。

### 7. API围绕业务动作设计

重点体现：

```text
生成规划

进行面试

生成报告

更新能力

重新规划
```

而不是只做普通 CRUD。

---

# 八十三、最终接口架构

```text
Vue3
 │
 │ Axios
 ↓
Spring Boot Controller
 │
 ↓
Service
 │
 ├───────────────┐
 ↓               ↓
MyBatis-Plus    Agent
 │               │
 ↓               ↓
MySQL        Spring AI
                 │
          ┌──────┼──────┐
          ↓      ↓      ↓
        Memory  Tool   RAG
                 │
                 ↓
            国产大模型
```

最终通过 API 将传统业务系统与 AI Agent 能力连接起来，形成完整的 AI 职业成长平台。