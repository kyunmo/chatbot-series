package io.moyam.chatbot.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("dev")
class DatabaseConnectionTest {

    @Autowired  // 생성자 주입 대신 필드 주입 사용
    private DataSource dataSource;

    @Test
    void 데이터베이스_연결_테스트() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            assertThat(connection).isNotNull();
            assertThat(connection.isValid(1)).isTrue();

            // PostgreSQL 연결 확인
            var metaData = connection.getMetaData();
            assertThat(metaData.getDatabaseProductName()).containsIgnoringCase("PostgreSQL");
        }
    }

    @Test
    void 테이블_존재_확인() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            var metaData = connection.getMetaData();

            // 주요 테이블 존재 확인
            var tables = metaData.getTables(null, "public", null, new String[]{"TABLE"});
            var tableNames = new java.util.ArrayList<String>();

            while (tables.next()) {
                tableNames.add(tables.getString("TABLE_NAME"));
            }

            assertThat(tableNames).contains("users", "bots", "scenarios", "scenario_steps",
                    "conversations", "messages", "file_uploads", "file_links", "bot_options");
        }
    }
}
