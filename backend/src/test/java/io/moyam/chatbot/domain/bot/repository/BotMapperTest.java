package io.moyam.chatbot.domain.bot.repository;

import io.moyam.chatbot.domain.bot.model.Bot;
import io.moyam.chatbot.domain.user.model.User;
import io.moyam.chatbot.domain.user.repository.UserMapper;
import org.junit.jupiter.api.BeforeEach;
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
class BotMapperTest {

    @Autowired
    private BotMapper botMapper;

    @Autowired
    private UserMapper userMapper;

    private Long userId;

    @BeforeEach
    void setUp() {
        User user = User.builder()
                .email("botowner@moyam.io")
                .passwordHash("hashedPassword123")
                .name("봇 만드는 사람")
                .isActive(true)
                .build();
        userMapper.insert(user);
        userId = user.getId();
    }

    @Test
    void 봇_생성_및_조회_테스트() {
        // Given
        Bot bot = Bot.builder()
                .userId(userId)
                .name("내 개인비서 봇")
                .description("일정 관리하고 메모 정리해주는 봇")
                .isActive(true)
                .build();

        // When
        botMapper.insert(bot);

        // Then
        assertThat(bot.getId()).isNotNull();

        var foundBot = botMapper.findById(bot.getId());
        assertThat(foundBot).isPresent();
        assertThat(foundBot.get().getName()).isEqualTo("내 개인비서 봇");
        assertThat(foundBot.get().getUserId()).isEqualTo(userId);
    }

    @Test
    void 사용자별_봇_목록_조회_테스트() {
        // Given
        Bot bot1 = Bot.builder()
                .userId(userId)
                .name("메모 봇")
                .description("메모 정리용 봇")
                .isActive(true)
                .build();

        Bot bot2 = Bot.builder()
                .userId(userId)
                .name("일정 봇")
                .description("일정 관리용 봇")
                .isActive(true)
                .build();

        botMapper.insert(bot1);
        botMapper.insert(bot2);

        // When
        var bots = botMapper.findByUserId(userId);

        // Then
        assertThat(bots).hasSize(2);
        assertThat(bots.stream().map(Bot::getName)).contains("메모 봇", "일정 봇");
    }

    @Test
    void 봇_수정_테스트() {
        // Given
        Bot bot = Bot.builder()
                .userId(userId)
                .name("원래 봇")
                .description("원래 설명")
                .isActive(true)
                .build();
        botMapper.insert(bot);

        // When
        bot.setName("수정된 봇");
        bot.setDescription("수정된 설명");
        botMapper.update(bot);

        // Then
        var updatedBot = botMapper.findById(bot.getId());
        assertThat(updatedBot).isPresent();
        assertThat(updatedBot.get().getName()).isEqualTo("수정된 봇");
        assertThat(updatedBot.get().getDescription()).isEqualTo("수정된 설명");
    }
}
