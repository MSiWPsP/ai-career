package com.xucheng.aicareer.agent.career.tool;

/** Tool 返回给模型的单条可核验业务事实。 */
public record CareerToolEvidence(String evidenceId, String text) {
}
