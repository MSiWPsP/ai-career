package com.xucheng.aicareer;

import com.xucheng.aicareer.mapper.UserMapper;
import com.xucheng.aicareer.mapper.UserProfileMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class AiCareerServerApplicationTests {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserProfileMapper userProfileMapper;

    @Autowired
    private DataSource dataSource;

    @Test
    void contextLoadsAndMappersAreRegistered() {
        assertThat(userMapper).isNotNull();
        assertThat(userProfileMapper).isNotNull();
    }

    @Test
    void databaseConnectionCanExecuteSimpleQuery() throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT 1")) {
            assertThat(resultSet.next()).isTrue();
            assertThat(resultSet.getInt(1)).isEqualTo(1);
        }
    }
}
