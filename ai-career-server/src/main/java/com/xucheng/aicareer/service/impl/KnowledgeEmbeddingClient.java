package com.xucheng.aicareer.service.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * 百炼 OpenAI 兼容向量接口的窄适配器。当前供应商响应未带 Spring AI 2.0.1 必需的
 * usage.prompt_tokens，因此只读取标准 data[0].embedding；不记录密钥或原始响应正文。
 */
@Component
@ConditionalOnProperty(name = "ai.rag.enabled", havingValue = "true")
public class KnowledgeEmbeddingClient {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5)).build();
    private final ObjectMapper objectMapper;
    private final URI endpoint;
    private final String apiKey;
    private final String model;
    private final int dimension;

    public KnowledgeEmbeddingClient(ObjectMapper objectMapper,
                                    @Value("${ai.rag.embedding-base-url}") String baseUrl,
                                    @Value("${ai.rag.embedding-api-key}") String apiKey,
                                    @Value("${ai.rag.embedding-model}") String model,
                                    @Value("${ai.rag.embedding-dimension:1024}") int dimension) {
        this.objectMapper = objectMapper;
        this.endpoint = URI.create(baseUrl.replaceAll("/+$", "") + "/embeddings");
        this.apiKey = apiKey;
        this.model = model;
        this.dimension = dimension;
    }

    public float[] embed(String text) {
        if (apiKey == null || apiKey.isBlank() || "not-configured".equals(apiKey)) {
            throw new IllegalStateException("RAG Embedding API Key 未配置");
        }
        try {
            String payload = objectMapper.writeValueAsString(Map.of(
                    "model", model, "input", text, "dimensions", dimension, "encoding_format", "float"));
            HttpRequest request = HttpRequest.newBuilder(endpoint)
                    .timeout(Duration.ofSeconds(15))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Embedding 服务返回 HTTP " + response.statusCode());
            }
            Map<?, ?> json = objectMapper.readValue(response.body(), Map.class);
            Object data = json.get("data");
            if (!(data instanceof List<?> rows) || rows.isEmpty()
                    || !(rows.getFirst() instanceof Map<?, ?> first)
                    || !(first.get("embedding") instanceof List<?> values)
                    || values.size() != dimension) {
                throw new IllegalStateException("Embedding 响应维度或结构无效");
            }
            float[] vector = new float[dimension];
            for (int index = 0; index < dimension; index++) {
                if (!(values.get(index) instanceof Number number) || !Float.isFinite(number.floatValue())) {
                    throw new IllegalStateException("Embedding 包含无效数值");
                }
                vector[index] = number.floatValue();
            }
            return vector;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Embedding 请求被中断", exception);
        } catch (Exception exception) {
            if (exception instanceof IllegalStateException stateException) {
                throw stateException;
            }
            throw new IllegalStateException("Embedding 请求失败", exception);
        }
    }
}
