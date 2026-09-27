package com.xucheng.aicareer;

import org.junit.jupiter.api.Test;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.mock.env.MockEnvironment;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KnowledgeImportLifecycleTests {

    @Test
    void closesApplicationContextAfterSuccessfulOneShotImport() {
        ConfigurableApplicationContext context = mock(ConfigurableApplicationContext.class);
        when(context.getEnvironment()).thenReturn(new MockEnvironment()
                .withProperty("ai.rag.import", "true"));

        AiCareerServerApplication.closeAfterOneShotKnowledgeImport(context);

        verify(context).close();
    }

    @Test
    void keepsNormalWebApplicationRunning() {
        ConfigurableApplicationContext context = mock(ConfigurableApplicationContext.class);
        when(context.getEnvironment()).thenReturn(new MockEnvironment()
                .withProperty("ai.rag.import", "false"));

        AiCareerServerApplication.closeAfterOneShotKnowledgeImport(context);

        verify(context, never()).close();
    }
}
