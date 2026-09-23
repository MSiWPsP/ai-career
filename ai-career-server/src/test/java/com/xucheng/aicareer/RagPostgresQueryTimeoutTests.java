package com.xucheng.aicareer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.sql.DriverManager;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 使用本地 PostgreSQL 的 pg_sleep 验证 JDBC 查询超时；不修改知识表。 */
@EnabledIfEnvironmentVariable(named = "AI_RAG_TIMEOUT_EVAL", matches = "true")
@SpringBootTest(properties = {"ai.rag.enabled=true", "ai.rag.import=false",
        "spring.main.web-application-type=none"})
class RagPostgresQueryTimeoutTests {

    @Value("${ai.rag.jdbc-url}")
    private String jdbcUrl;
    @Value("${ai.rag.username}")
    private String username;
    @Value("${ai.rag.password}")
    private String password;

    @Test
    void postgresCancelsSlowQueryAtConfiguredTimeout() throws Exception {
        try (var connection = DriverManager.getConnection(jdbcUrl, username, password);
             var statement = connection.prepareStatement("SELECT pg_sleep(5)")) {
            statement.setQueryTimeout(1);
            assertThatThrownBy(statement::executeQuery).isInstanceOf(SQLException.class);
        }
    }
}
