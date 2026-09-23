package com.xucheng.aicareer.service.impl;

import com.xucheng.aicareer.agent.career.dto.GroundedCareerAnswer;
import com.xucheng.aicareer.service.model.KnowledgeReference;
import com.xucheng.aicareer.service.model.KnowledgeRetrievalResult;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 将模型的结构化草稿收窄为可交付回答：知识事实只展示经过本轮片段逐字核对的摘录，
 * 可选建议明确隔离；丢弃不合约的单项，仅在没有有效摘录时拒绝整段回答。
 */
final class GroundedCareerAnswerComposer {

    private static final Pattern BLOCK = Pattern.compile("(?ms)^\\[(\\d+)] [^\\n]*\\n(.*?)(?=^\\[\\d+] |\\z)");
    private static final Pattern INJECTION = Pattern.compile("(?i)忽略.{0,8}(?:规则|指令)|输出.{0,8}(?:密钥|凭证)|api.?key|password");
    private static final Pattern CLAIMED_RESULT = Pattern.compile(
            "已经|已完成|已解决|解决了|减少了|降低了|提升了|提高了|达到了|证明了|验证了|建议写成");
    private static final String FALLBACK = "本轮知识片段不足以组成可逐项核对的回答。"
            + "可以补充目标岗位或项目背景后重试；涉及项目成效时，请先提供可核验的测量记录。";

    private GroundedCareerAnswerComposer() {
    }

    static Composed compose(GroundedCareerAnswer draft, KnowledgeRetrievalResult knowledge) {
        if (draft == null || draft.excerpts() == null || draft.excerpts().isEmpty()
                || draft.excerpts().size() > 4 || draft.optionalActions() == null
                || draft.optionalActions().size() > 3) {
            return rejected();
        }
        List<String> blocks = sourceBlocks(knowledge.context(), knowledge.references().size());
        if (blocks.size() != knowledge.references().size()) {
            return rejected();
        }
        StringBuilder answer = new StringBuilder("**本轮知识依据（原文摘录）**\n");
        Set<Integer> used = new LinkedHashSet<>();
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
            answer.append("- “").append(quote).append("” （《")
                    .append(knowledge.references().get(index - 1).title()).append("》· ")
                    .append(knowledge.references().get(index - 1).section()).append("）\n");
        }
        if (used.isEmpty()) {
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
                answer.append("\n**可选行动（需结合个人情况验证）**\n").append(actions);
            }
        }
        List<KnowledgeReference> references = new ArrayList<>();
        for (int index : used) {
            references.add(knowledge.references().get(index - 1));
        }
        return new Composed(answer.toString().stripTrailing(), List.copyOf(references), true);
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
