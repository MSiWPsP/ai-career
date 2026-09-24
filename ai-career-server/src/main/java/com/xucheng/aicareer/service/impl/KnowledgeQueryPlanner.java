package com.xucheng.aicareer.service.impl;

import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 将宽泛或包含多个方面的职业问题拆成少量可控检索意图。
 *
 * <p>规划过程只使用本地领域规则，不增加模型调用和用户等待时间；原问题始终保留，补充查询
 * 只帮助召回不同章节，不能改变回答含义。每次最多产生三个补充查询。</p>
 */
@Component
public class KnowledgeQueryPlanner {

    private static final int MAX_QUERIES = 4;

    public List<String> plan(String question) {
        if (question == null || question.isBlank()) {
            return List.of();
        }
        String original = question.strip();
        Set<String> queries = new LinkedHashSet<>();
        queries.add(original);

        if (containsAny(original, "核心能力", "哪些能力", "什么能力")
                && containsAny(original, "Java", "后端")) {
            queries.add("Java 后端岗位 语言基础 计算机基础");
            queries.add("Java 后端 Spring Boot 接口 服务开发 参数校验 业务分层");
            queries.add("Java 后端 SQL 索引 事务 执行计划 Redis 缓存失效 更新 降级");
        }
        if (containsAny(original, "分阶段", "学习路线", "路线怎么", "路线如何")) {
            queries.add("后端学习路线 Java SQL 可运行基础");
            queries.add("后端学习路线 Spring Boot 业务闭环 项目实践");
            queries.add("后端学习路线 测试 日志 配置 部署 故障定位");
        }
        if (original.contains("简历") && (original.contains("投递") || original.contains("面试"))) {
            queries.add("实习简历 可验证材料 项目证据");
            queries.add("实习投递 岗位要求 反馈记录 复盘");
            queries.add("实习面试 设计依据 真实案例 准备");
        }
        if (containsAny(original, "一定能", "保证录用", "保证拿到", "就业保证")) {
            queries.add("学习路线 就业保证 个人基础 可投入时间 目标岗位");
            queries.add("实习招聘 目标公司 流程 公开要求 不确定性");
        }

        return queries.stream().limit(MAX_QUERIES).toList();
    }

    private boolean containsAny(String value, String... candidates) {
        for (String candidate : candidates) {
            if (value.contains(candidate)) {
                return true;
            }
        }
        return false;
    }
}
