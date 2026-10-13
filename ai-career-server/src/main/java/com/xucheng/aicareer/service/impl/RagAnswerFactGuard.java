package com.xucheng.aicareer.service.impl;

import java.util.regex.Pattern;

/**
 * RAG 回答发出前的确定性事实闸门。用户自述和模型示例均不能证明项目已取得性能结果；
 * 命中高风险结果句时整段回答停止交付，改用不声称已有结果的固定建议。
 */
final class RagAnswerFactGuard {

    private static final String SAFE_REPLY = "目前没有可核验的项目测量记录，不能把性能改善或问题已解决写成既成事实。"
            + "可以先说明采取了哪些操作，再记录变更前后的相同负载、响应时间、吞吐量、数据库访问和资源使用情况；"
            + "只有核对原始记录后，才把实际结果写进项目经历。";
    private static final Pattern METRIC = Pattern.compile(
            "(?i)(?:提升|提高|降低|减少|缩短|加快|优化|节省|下降|由|从|达到|稳定在|变为|变成|降至|升至)"
                    + "[^。！？\\n]{0,24}(?:\\d+(?:\\.\\d+)?\\s*(?:%|％|倍|ms|毫秒|秒|qps|tps|次|GB|MB)|"
                    + "QPS|TPS|CPU|IO|I/O|耗时|响应时间|吞吐量)"
                    + "|(?:\\d+(?:\\.\\d+)?\\s*(?:%|％|倍|ms|毫秒|秒|qps|tps|次))"
                    + "[^。！？\\n]{0,24}(?:提升|提高|降低|减少|缩短|响应|耗时|吞吐|查询|IO|I/O|CPU)");
    private static final Pattern OUTCOME = Pattern.compile(
            "(?:显著|明显|成功|有效)?(?:减少|降低|缩短|提升|提高|解决|消除|避免)"
                    + "[^。！？\\n]{0,32}(?:查询次数|数据库访问|数据库查询|数据库\\s*IO\\s*次数|响应时间|响应速度|接口耗时|等待时间|延迟|超时|CPU|IO|I/O|性能问题|瓶颈|吞吐量)"
                    + "|(?:查询次数|数据库访问|数据库查询|数据库\\s*IO\\s*次数|响应时间|响应速度|接口耗时|等待时间|延迟|超时|CPU|IO|I/O|吞吐量)"
                    + "[^。！？\\n]{0,12}(?:减少|降低|缩短|提升|提高|改善|优化)");
    private static final Pattern CONDITIONAL = Pattern.compile(
            "^(?:可以|可尝试|建议|考虑|计划|拟|目标是|希望|预期|待|如果|若|通过.*验证|不要|不能|避免|未经|没有|尚未|未|不应)"
                    + ".*");
    private static final Pattern HIGH_RISK_QUESTION = Pattern.compile(
            "(?i)性能|提速|优化|压测|QPS|TPS|CPU|IO|I/O|响应时间|吞吐|耗时|百分比|%|％|项目成果|项目经历");

    private RagAnswerFactGuard() {
    }

    static boolean requiresPreflight(String question) {
        return question != null && HIGH_RISK_QUESTION.matcher(question).find();
    }

    static Review review(String answer) {
        if (answer == null || answer.isBlank()) {
            return new Review(SAFE_REPLY, false);
        }
        for (String sentence : answer.split("[。！？\\n]+")) {
            String normalized = sentence.strip().replaceFirst("^[\\s>*#\\-\\d.、：:]+", "");
            if (METRIC.matcher(normalized).find()
                    || (!CONDITIONAL.matcher(normalized).matches() && OUTCOME.matcher(normalized).find())) {
                return new Review(SAFE_REPLY, false);
            }
        }
        return new Review(answer, true);
    }

    record Review(String content, boolean accepted) {
    }
}
