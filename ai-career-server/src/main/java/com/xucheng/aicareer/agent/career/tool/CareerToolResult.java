package com.xucheng.aicareer.agent.career.tool;

import java.util.List;

/** Tool 的受控返回结构；只包含回答需要的最小业务事实。 */
public record CareerToolResult(
        String toolName,
        boolean available,
        String message,
        List<CareerToolEvidence> evidence) {

    public CareerToolResult {
        evidence = evidence == null ? List.of() : List.copyOf(evidence);
    }
}
