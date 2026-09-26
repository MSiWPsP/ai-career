package com.xucheng.aicareer.service.impl;

import com.xucheng.aicareer.agent.career.dto.GroundedCareerAnswer;
import com.xucheng.aicareer.agent.career.tool.CareerToolEvidence;
import com.xucheng.aicareer.service.model.KnowledgeReference;
import com.xucheng.aicareer.service.model.KnowledgeRetrievalResult;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 将模型的结构化草稿收窄为可交付回答：知识事实只展示经过本轮片段逐字核对的摘录，
 * 个人事实只展示本轮 Tool 实际返回的完整证据，建议明确隔离。模型自报的编号和文本不构成信任依据。
 */
final class GroundedCareerAnswerComposer {

    private static final Pattern BLOCK = Pattern.compile("(?ms)^\\[(\\d+)] [^\\n]*\\n(.*?)(?=^\\[\\d+] |\\z)");
    private static final Pattern INJECTION = Pattern.compile("(?i)忽略.{0,8}(?:规则|指令)|输出.{0,8}(?:密钥|凭证)|api.?key|password");
    private static final Pattern CLAIMED_RESULT = Pattern.compile(
            "已经|已完成|已解决|解决了|减少了|降低了|提升了|提高了|达到了|证明了|验证了|建议写成");
    private static final String FALLBACK = "本轮可核验依据不足，暂时无法组成可逐项核对的回答。"
            + "可以补充目标岗位、项目背景或确认平台数据后重试；涉及项目成效时，请先提供可核验的测量记录。";

    private GroundedCareerAnswerComposer() {
    }

    static Composed compose(GroundedCareerAnswer draft, KnowledgeRetrievalResult knowledge) {
        return compose(draft, knowledge, Map.of());
    }

    static Composed compose(GroundedCareerAnswer draft, KnowledgeRetrievalResult knowledge,
                            Map<String, CareerToolEvidence> toolEvidence) {
        if (draft == null || draft.excerpts().size() > 6 || draft.toolExcerpts().size() > 12
                || draft.optionalActions().size() > 3 || knowledge == null || toolEvidence == null) {
            return rejected();
        }
        List<String> blocks = sourceBlocks(knowledge.context(), knowledge.references().size());
        if (blocks.size() != knowledge.references().size()) {
            return rejected();
        }
        StringBuilder answer = new StringBuilder();
        appendToolEvidence(answer, draft.toolExcerpts(), toolEvidence);
        Set<Integer> used = new LinkedHashSet<>();
        StringBuilder knowledgeFacts = new StringBuilder();
        for (GroundedCareerAnswer.SourceExcerpt excerpt : draft.excerpts()) {
            if (excerpt == null || excerpt.sourceIndex() == null || excerpt.quote() == null) {
                continue;
            }
            int index = excerpt.sourceIndex();
            String quote = excerpt.quote().strip();
            if (index < 1 || index > blocks.size() || quote.length() < 12 || quote.length() > 250
                    || quote.contains("\n") || !blocks.get(index - 1).contains(quote)
                    || INJECTION.matcher(quote).find()) {
                continue;
            }
            used.add(index);
            knowledgeFacts.append("- “").append(quote).append("” （《")
                    .append(knowledge.references().get(index - 1).title()).append("》· ")
                    .append(knowledge.references().get(index - 1).section()).append("）\n");
        }
        if (!knowledgeFacts.isEmpty()) {
            appendSection(answer, "**本轮知识依据（原文摘录）**", knowledgeFacts);
        }
        if (answer.isEmpty()) {
            return rejected();
        }
        if (!draft.optionalActions().isEmpty()) {
            StringBuilder actions = new StringBuilder();
            for (String action : draft.optionalActions()) {
                if (action == null || action.length() > 180
                        || !(action.startsWith("可以") || action.startsWith("建议"))
                        || action.contains("\n") || !RagAnswerFactGuard.review(action).accepted()
                        || CLAIMED_RESULT.matcher(action).find()
                        || action.matches(".*(?:你已经|你目前掌握|你做过|你的项目已经).*")) {
                    continue;
                }
                actions.append("- ").append(action).append('\n');
            }
            if (!actions.isEmpty()) {
                appendSection(answer, "**可选行动（需结合个人情况验证）**", actions);
            }
        }
        List<KnowledgeReference> references = new ArrayList<>();
        for (int index : used) {
            references.add(knowledge.references().get(index - 1));
        }
        return new Composed(answer.toString().stripTrailing(), List.copyOf(references), true);
    }

    private static void appendToolEvidence(StringBuilder answer,
                                           List<GroundedCareerAnswer.ToolExcerpt> excerpts,
                                           Map<String, CareerToolEvidence> evidenceById) {
        Set<String> used = new LinkedHashSet<>();
        StringBuilder facts = new StringBuilder();
        for (GroundedCareerAnswer.ToolExcerpt excerpt : excerpts) {
            if (excerpt == null || excerpt.evidenceId() == null || excerpt.quote() == null) {
                continue;
            }
            String evidenceId = excerpt.evidenceId().strip();
            String quote = excerpt.quote().strip();
            CareerToolEvidence evidence = evidenceById.get(evidenceId);
            if (evidence == null || !used.add(evidenceId) || quote.length() > 300
                    || quote.contains("\n") || !quote.equals(evidence.text())
                    || INJECTION.matcher(quote).find()) {
                continue;
            }
            facts.append("- ").append(quote).append('\n');
        }
        if (!facts.isEmpty()) {
            appendSection(answer, "**本轮读取的平台数据**", facts);
        }
    }

    private static void appendSection(StringBuilder answer, String title, StringBuilder content) {
        if (!answer.isEmpty()) {
            answer.append('\n');
        }
        answer.append(title).append('\n').append(content);
    }

    private static List<String> sourceBlocks(String context, int count) {
        List<String> blocks = new ArrayList<>(java.util.Collections.nCopies(count, ""));
        Matcher matcher = BLOCK.matcher(context);
        while (matcher.find()) {
            int index = Integer.parseInt(matcher.group(1));
            if (index < 1 || index > count || !blocks.get(index - 1).isEmpty()) {
                return List.of();
            }
            blocks.set(index - 1, matcher.group(2));
        }
        return blocks;
    }

    private static Composed rejected() {
        return new Composed(FALLBACK, List.of(), false);
    }

    record Composed(String content, List<KnowledgeReference> references, boolean accepted) {
    }
}
