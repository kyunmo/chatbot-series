package io.moyam.chatbot.domain.user.repository;

import io.moyam.chatbot.domain.user.model.User;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
@Rollback
class UserMapperTest {

    @Autowired
    private UserMapper userMapper;

    @Test
    void 사용자_생성_및_조회_테스트() {
        // Given
        User user = User.builder()
                .email("test@moyam.io")
                .passwordHash("hashedPassword123")
                .name("테스트 사용자")
                .isActive(true)
                .build();

        // When
        userMapper.insert(user);

        // Then
        assertThat(user.getId()).isNotNull();

        var foundUser = userMapper.findById(user.getId());
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getEmail()).isEqualTo("test@moyam.io");
    }
}
