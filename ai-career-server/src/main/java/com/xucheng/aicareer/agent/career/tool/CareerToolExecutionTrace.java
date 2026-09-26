package com.xucheng.aicareer.agent.career.tool;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 记录本轮 Tool 实际返回给模型的证据，供最终回答在发送前校验。
 *
 * <p>该对象通过 ToolContext 传递，不进入模型输入。同步方法兼容供应商并行调用多个 Tool。</p>
 */
public final class CareerToolExecutionTrace {

    private final Map<String, Integer> callCounts = new LinkedHashMap<>();
    private final Map<String, CareerToolEvidence> evidenceById = new LinkedHashMap<>();

    public synchronized List<CareerToolEvidence> record(String toolName, List<String> facts) {
        int call = callCounts.merge(toolName, 1, Integer::sum);
        List<CareerToolEvidence> evidence = java.util.stream.IntStream.range(0, facts.size())
                .mapToObj(index -> new CareerToolEvidence(
                        toolName + '-' + call + '-' + (index + 1), facts.get(index)))
                .toList();
        evidence.forEach(item -> evidenceById.put(item.evidenceId(), item));
        return evidence;
    }

    public synchronized Map<String, CareerToolEvidence> snapshot() {
        return Map.copyOf(evidenceById);
    }

    public synchronized boolean hasEvidence() {
        return !evidenceById.isEmpty();
    }
}
