package io.moyam.chatbot.domain.scenario.repository;

import io.moyam.chatbot.domain.bot.model.Bot;
import io.moyam.chatbot.domain.bot.repository.BotMapper;
import io.moyam.chatbot.domain.scenario.model.Scenario;
import io.moyam.chatbot.domain.user.model.User;
import io.moyam.chatbot.domain.user.repository.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
class ScenarioMapperTest {

    @Autowired
    private ScenarioMapper scenarioMapper;

    @Autowired
    private BotMapper botMapper;

    @Autowired
    private UserMapper userMapper;

    private Long botId;

    @BeforeEach
    void setUp() {
        // 사용자 생성
        User user = User.builder()
                .email("scenario@moyam.io")
                .passwordHash("hashedPassword123")
                .name("시나리오 테스터")
                .isActive(true)
                .build();
        userMapper.insert(user);

        // 봇 생성
        Bot bot = Bot.builder()
                .userId(user.getId())
                .name("시나리오 테스트 봇")
                .description("시나리오 테스트용")
                .isActive(true)
                .build();
        botMapper.insert(bot);
        botId = bot.getId();
    }

    @Test
    void 시나리오_생성_및_조회_테스트() {
        // Given
        Scenario scenario = Scenario.builder()
                .botId(botId)
                .name("기본 인사")
                .description("처음 만났을 때 인사하는 시나리오")
                .isDefault(true)
                .build();

        // When
        scenarioMapper.insert(scenario);

        // Then
        assertThat(scenario.getId()).isNotNull();

        var foundScenario = scenarioMapper.findById(scenario.getId());
        assertThat(foundScenario).isPresent();
        assertThat(foundScenario.get().getName()).isEqualTo("기본 인사");
        assertThat(foundScenario.get().getIsDefault()).isTrue();
    }

    @Test
    void 봇별_시나리오_목록_조회_테스트() {
        // Given
        Scenario scenario1 = Scenario.builder()
                .botId(botId)
                .name("일정 확인")
                .description("오늘 일정을 확인하는 시나리오")
                .isDefault(false)
                .build();

        Scenario scenario2 = Scenario.builder()
                .botId(botId)
                .name("메모 작성")
                .description("새로운 메모를 작성하는 시나리오")
                .isDefault(false)
                .build();

        scenarioMapper.insert(scenario1);
        scenarioMapper.insert(scenario2);

        // When
        var scenarios = scenarioMapper.findByBotId(botId);

        // Then
        assertThat(scenarios).hasSize(2);
        assertThat(scenarios.stream().map(Scenario::getName))
                .contains("일정 확인", "메모 작성");
    }
}
