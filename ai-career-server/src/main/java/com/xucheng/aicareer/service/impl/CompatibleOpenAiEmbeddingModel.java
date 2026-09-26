package com.xucheng.aicareer.service.impl;

import com.openai.client.OpenAIClient;
import com.openai.core.RequestOptions;
import com.openai.models.embeddings.CreateEmbeddingResponse;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.AbstractEmbeddingModel;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.embedding.EmbeddingResponseMetadata;
import org.springframework.ai.model.EmbeddingUtils;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.setup.OpenAiSetup;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;

/**
 * 百炼 OpenAI 兼容接口的 Spring AI {@link org.springframework.ai.embedding.EmbeddingModel} 实现。
 *
 * <p>请求、超时和响应模型复用 Spring AI 2.0.1 内置的 OpenAI SDK；只在响应转换处忽略百炼未返回的
 * usage 字段。Spring AI 自带的 {@code OpenAiEmbeddingModel} 当前会强制读取该字段，因此暂时保留这个
 * 很薄的兼容层，业务代码只依赖标准 EmbeddingModel 接口。</p>
 */
@Component("ragEmbeddingModel")
@ConditionalOnProperty(name = "ai.rag.enabled", havingValue = "true")
public class CompatibleOpenAiEmbeddingModel extends AbstractEmbeddingModel {

    private final OpenAIClient openAiClient;
    private final OpenAiEmbeddingOptions options;
    private final int dimension;

    public CompatibleOpenAiEmbeddingModel(
            @Value("${ai.rag.embedding-base-url}") String baseUrl,
            @Value("${ai.rag.embedding-api-key}") String apiKey,
            @Value("${ai.rag.embedding-model}") String model,
            @Value("${ai.rag.embedding-dimension:1024}") int dimension,
            @Value("${ai.rag.embedding-timeout-seconds:15}") int timeoutSeconds,
            @Value("${ai.rag.embedding-max-retries:0}") int maxRetries) {
        if (dimension < 1 || timeoutSeconds < 1 || maxRetries < 0) {
            throw new IllegalArgumentException("RAG Embedding 参数无效");
        }
        this.options = OpenAiEmbeddingOptions.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .model(model)
                .dimensions(dimension)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .maxRetries(maxRetries)
                .build();
        this.openAiClient = OpenAiSetup.setupSyncClient(
                options.getBaseUrl(), options.getApiKey(), options.getCredential(),
                options.getMicrosoftDeploymentName(), options.getMicrosoftFoundryServiceVersion(),
                options.getOrganizationId(), options.isMicrosoftFoundry(), options.isGitHubModels(),
                options.getModel(), options.getTimeout(), options.getMaxRetries(), options.getProxy(),
                options.getCustomHeaders(), ObservationRegistry.NOOP, null, List.of());
        this.dimension = dimension;
    }

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        Assert.notEmpty(request.getInstructions(), "至少需要一条 Embedding 输入");
        if (options.getApiKey() == null || options.getApiKey().isBlank()
                || "not-configured".equals(options.getApiKey())) {
            throw new IllegalStateException("RAG Embedding API Key 未配置");
        }
        try {
            OpenAiEmbeddingOptions requestOptions = OpenAiEmbeddingOptions.builder()
                    .from(options)
                    .merge(request.getOptions())
                    .build();
            CreateEmbeddingResponse response = openAiClient.embeddings().create(
                    requestOptions.toOpenAiCreateParams(request.getInstructions()),
                    RequestOptions.builder().timeout(requestOptions.getTimeout()).build());
            List<Embedding> embeddings = response.data().stream()
                    .sorted(Comparator.comparingLong(com.openai.models.embeddings.Embedding::index))
                    .map(item -> validatedEmbedding(item.embedding(), item.index()))
                    .toList();
            if (embeddings.size() != request.getInstructions().size()) {
                throw new IllegalStateException("Embedding 响应数量与请求不一致");
            }
            EmbeddingResponseMetadata metadata = new EmbeddingResponseMetadata();
            metadata.setModel(response.model());
            return new EmbeddingResponse(embeddings, metadata);
        } catch (RuntimeException exception) {
            if (exception instanceof IllegalStateException) {
                throw exception;
            }
            throw new IllegalStateException("Embedding 请求失败", exception);
        }
    }

    @Override
    public float[] embed(Document document) {
        Assert.notNull(document, "Document 不能为空");
        Assert.hasText(document.getText(), "Document 内容不能为空");
        return embed(document.getText());
    }

    @Override
    public int dimensions() {
        return dimension;
    }

    private Embedding validatedEmbedding(List<Float> values, long index) {
        if (values.size() != dimension) {
            throw new IllegalStateException("Embedding 响应维度无效");
        }
        float[] vector = EmbeddingUtils.toPrimitive(values);
        for (float value : vector) {
            if (!Float.isFinite(value)) {
                throw new IllegalStateException("Embedding 包含无效数值");
            }
        }
        return new Embedding(vector, Math.toIntExact(index));
    }
}
