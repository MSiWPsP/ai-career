package com.xucheng.aicareer.service.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/** 区分用户个人平台数据问题与公共职业知识问题，避免纯个人查询误走 RAG。 */
@Component
public class CareerToolIntentRouter {

    private static final Pattern PERSONAL_CUE = Pattern.compile(
            "我的|我目前|我现在|当前|最近|上次|现有|还有|完成率|未完成|待完成");
    private static final Pattern TOOL_DOMAIN = Pattern.compile(
            "任务|进度|(?:模拟)?面试|能力(?:评分|雷达|趋势|变化|记录)");
    private static final Pattern ADVICE = Pattern.compile(
            "如何|怎么(?:学|提升|改进|准备|安排)|给.{0,6}建议|学习路线|下一步|结合.{0,12}(?:建议|规划)");

    private final boolean enabled;

    public CareerToolIntentRouter(@Value("${ai.tool-calling.career.enabled:true}") boolean enabled) {
        this.enabled = enabled;
    }

    public boolean requiresTools(String message) {
        return enabled && message != null
                && PERSONAL_CUE.matcher(message).find()
                && TOOL_DOMAIN.matcher(message).find();
    }

    public boolean isPersonalDataOnly(String message) {
        return requiresTools(message) && !ADVICE.matcher(message).find();
    }
}
