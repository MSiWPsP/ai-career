package com.xucheng.aicareer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class AiCareerServerApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(AiCareerServerApplication.class, args);
        closeAfterOneShotKnowledgeImport(context);
    }

    /**
     * RAG 导入是一次性运维任务：所有 ApplicationRunner 成功执行后主动关闭上下文，
     * 避免数据源等非守护线程让容器长期驻留。导入异常会在 run 返回前向上抛出并保持非零退出。
     */
    static void closeAfterOneShotKnowledgeImport(ConfigurableApplicationContext context) {
        if (context.getEnvironment().getProperty("ai.rag.import", Boolean.class, false)) {
            context.close();
        }
    }
}
