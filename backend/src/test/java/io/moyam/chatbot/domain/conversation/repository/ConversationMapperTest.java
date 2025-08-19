package io.moyam.chatbot.domain.conversation.repository;

import io.moyam.chatbot.domain.bot.model.Bot;
import io.moyam.chatbot.domain.bot.repository.BotMapper;
import io.moyam.chatbot.domain.conversation.model.Conversation;
import io.moyam.chatbot.domain.user.model.User;
import io.moyam.chatbot.domain.user.repository.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.annotation.Rollback;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
@Rollback
class ConversationMapperTest {

    @Autowired
    private ConversationMapper conversationMapper;

    @Autowired
    private BotMapper botMapper;

    @Autowired
    private UserMapper userMapper;

    private Long userId;
    private Long botId;

    @BeforeEach
    void setUp() {
        // 사용자 생성
        User user = User.builder()
                .email("conversation@moyam.io")
                .passwordHash("hashedPassword123")
                .name("대화 테스터")
                .isActive(true)
                .build();
        userMapper.insert(user);
        userId = user.getId();

        // 봇 생성
        Bot bot = Bot.builder()
                .userId(userId)
                .name("대화형 챗봇")
                .description("대화 테스트용 봇")
                .isActive(true)
                .build();
        botMapper.insert(bot);
        botId = bot.getId();
    }

    @Test
    void 대화_생성_및_조회_테스트() {
        // Given
        Map<String, Object> contextData = new HashMap<>();
        contextData.put("userName", "테스터");
        contextData.put("lastAction", "greeting");

        Conversation conversation = Conversation.builder()
                .userId(userId)
                .botId(botId)
                .contextData(contextData)
                .status(Conversation.ConversationStatus.ACTIVE)
                .sessionId("session-12345")
                .build();

        // When
        conversationMapper.insert(conversation);

        // Then
        assertThat(conversation.getId()).isNotNull();

        var foundConversation = conversationMapper.findById(conversation.getId());
        assertThat(foundConversation).isPresent();
        assertThat(foundConversation.get().getSessionId()).isEqualTo("session-12345");
        assertThat(foundConversation.get().getStatus()).isEqualTo(Conversation.ConversationStatus.ACTIVE);
        assertThat(foundConversation.get().getContextData()).containsEntry("userName", "테스터");
    }

    @Test
    void 세션ID로_대화_조회_테스트() {
        // Given
        Conversation conversation = Conversation.builder()
                .userId(userId)
                .botId(botId)
                .status(Conversation.ConversationStatus.ACTIVE)
                .sessionId("unique-session-789")
                .build();

        conversationMapper.insert(conversation);

        // When
        var foundConversation = conversationMapper.findBySessionId("unique-session-789");

        // Then
        assertThat(foundConversation).isPresent();
        assertThat(foundConversation.get().getBotId()).isEqualTo(botId);
        assertThat(foundConversation.get().getUserId()).isEqualTo(userId);
    }

    @Test
    void 봇별_대화_목록_조회_테스트() {
        // Given
        // 첫 번째 대화
        Conversation conversation1 = Conversation.builder()
                .userId(userId)
                .botId(botId)
                .status(Conversation.ConversationStatus.ACTIVE)
                .sessionId("session-001")
                .build();

        // 두 번째 대화
        Conversation conversation2 = Conversation.builder()
                .userId(userId)
                .botId(botId)
                .status(Conversation.ConversationStatus.COMPLETED)
                .sessionId("session-002")
                .build();

        conversationMapper.insert(conversation1);
        conversationMapper.insert(conversation2);

        // When
        var conversations = conversationMapper.findByBotId(botId);

        // Then
        assertThat(conversations).hasSize(2);
        assertThat(conversations.stream().map(Conversation::getSessionId))
                .contains("session-001", "session-002");
    }

    @Test
    void 대화_상태_업데이트_테스트() {
        // Given
        Conversation conversation = Conversation.builder()
                .userId(userId)
                .botId(botId)
                .status(Conversation.ConversationStatus.ACTIVE)
                .sessionId("session-update-test")
                .build();

        conversationMapper.insert(conversation);

        // When - 대화 완료로 상태 변경
        conversation.setStatus(Conversation.ConversationStatus.COMPLETED);
        conversationMapper.update(conversation);

        // Then
        var updatedConversation = conversationMapper.findById(conversation.getId());
        assertThat(updatedConversation).isPresent();
        assertThat(updatedConversation.get().getStatus()).isEqualTo(Conversation.ConversationStatus.COMPLETED);
    }

    @Test
    void 대화_삭제_테스트() {
        // Given
        Conversation conversation = Conversation.builder()
                .userId(userId)
                .botId(botId)
                .status(Conversation.ConversationStatus.ACTIVE)
                .sessionId("session-delete-test")
                .build();

        conversationMapper.insert(conversation);
        Long conversationId = conversation.getId();

        // When
        conversationMapper.deleteById(conversationId);

        // Then
        var deletedConversation = conversationMapper.findById(conversationId);
        assertThat(deletedConversation).isEmpty();
    }

    @Test
    void 컨텍스트_데이터_복잡한_객체_테스트() {
        // Given
        Map<String, Object> complexContext = new HashMap<>();
        complexContext.put("currentStep", 5);
        complexContext.put("userInput", "안녕하세요");
        complexContext.put("botResponse", "반갑습니다!");
        
        Map<String, String> userInfo = new HashMap<>();
        userInfo.put("name", "김테스트");
        userInfo.put("phone", "010-1234-5678");
        complexContext.put("userInfo", userInfo);

        Conversation conversation = Conversation.builder()
                .userId(userId)
                .botId(botId)
                .contextData(complexContext)
                .status(Conversation.ConversationStatus.ACTIVE)
                .sessionId("session-complex-context")
                .build();

        // When
        conversationMapper.insert(conversation);

        // Then
        var foundConversation = conversationMapper.findById(conversation.getId());
        assertThat(foundConversation).isPresent();
        
        Map<String, Object> retrievedContext = foundConversation.get().getContextData();
        assertThat(retrievedContext).containsEntry("currentStep", 5);
        assertThat(retrievedContext).containsEntry("userInput", "안녕하세요");
        assertThat(retrievedContext).containsKey("userInfo");
    }
}
