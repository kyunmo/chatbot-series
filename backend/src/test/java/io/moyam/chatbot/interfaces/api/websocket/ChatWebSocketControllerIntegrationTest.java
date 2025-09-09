package io.moyam.chatbot.interfaces.api.websocket;

import io.moyam.chatbot.domain.conversation.model.ConversationContext;
import io.moyam.chatbot.domain.conversation.service.IntentRecognizer;
import io.moyam.chatbot.domain.conversationblock.service.ConversationBlockService;
import io.moyam.chatbot.domain.intent.model.IntentAnalysisResult;
import io.moyam.chatbot.domain.scenario.model.ScenarioExecutionResult;
import io.moyam.chatbot.domain.scenario.model.ScenarioStep;
import io.moyam.chatbot.domain.scenario.service.ScenarioService;
import io.moyam.chatbot.interfaces.api.websocket.request.ChatRequest;
import io.moyam.chatbot.interfaces.api.websocket.response.ChatResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatWebSocketController 통합 테스트")
class ChatWebSocketControllerIntegrationTest {

    @Mock
    private ScenarioService scenarioService;
    
    @Mock
    private IntentRecognizer intentRecognizer;
    
    @Mock
    private ConversationBlockService conversationBlockService;
    
    @InjectMocks
    private ChatWebSocketController chatController;

    private static final String TEST_SESSION_ID = "test-session-123";

    @BeforeEach
    void setUp() {
        // Lenient 모드로 설정하여 불필요한 stubbing 경고 방지
        lenient().when(scenarioService.getContext(anyString())).thenReturn(null);
    }

    @Test
    @DisplayName("일반 대화 처리 - 인사말 입력 테스트")
    void testGeneralConversationHandling() {
        // Given
        ChatRequest request = ChatRequest.builder()
                .message("안녕하세요")
                .build();
        
        IntentAnalysisResult mockResult = IntentAnalysisResult.builder()
                .intentType("BASIC_CONVERSATION")
                .targetId(1L)
                .confidence(0.95)
                .responseMessage("안녕하세요! 무엇을 도와드릴까요?")
                .build();
        
        when(intentRecognizer.analyze("안녕하세요")).thenReturn(mockResult);
        when(intentRecognizer.isStartScenarioIntent(mockResult)).thenReturn(false);
        
        // When
        ChatResponse response = chatController.handleMessage(TEST_SESSION_ID, request);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.getMessage()).contains("안녕하세요");
        assertThat(response.getSessionId()).isEqualTo(TEST_SESSION_ID);
        assertThat(response.isFromBot()).isTrue();
        assertThat(response.getConfidence()).isEqualTo(0.95);
        assertThat(response.getMessageType()).isEqualTo("text");
        assertThat(response.getChoices()).hasSize(2); // 기본 제안 버튼들
        
        verify(intentRecognizer).analyze("안녕하세요");
        verify(intentRecognizer).isStartScenarioIntent(mockResult);
    }

    @Test
    @DisplayName("시나리오 자동 시작 테스트")
    void testAutoStartScenario() {
        // Given
        ChatRequest request = ChatRequest.builder()
                .message("시작해줘")
                .build();
        
        IntentAnalysisResult mockResult = IntentAnalysisResult.builder()
                .intentType("BASIC_CONVERSATION")
                .targetId(5L)
                .confidence(0.9)
                .responseMessage("네! 시나리오를 시작해드릴게요.")
                .build();
        
        ScenarioExecutionResult mockScenarioResult = createMockScenarioExecutionResult("시나리오를 시작합니다!");
        
        when(intentRecognizer.analyze("시작해줘")).thenReturn(mockResult);
        when(intentRecognizer.isStartScenarioIntent(mockResult)).thenReturn(true);
        when(scenarioService.startScenario(TEST_SESSION_ID, 1L)).thenReturn(mockScenarioResult);
        
        // When
        ChatResponse response = chatController.handleMessage(TEST_SESSION_ID, request);
        
        // Then
        verify(scenarioService).startScenario(TEST_SESSION_ID, 1L);
        assertThat(response).isNotNull();
        assertThat(response.getMessage()).isEqualTo("시나리오를 시작합니다!");
    }

    @Test
    @DisplayName("시나리오 진행 중인 경우 기존 로직 유지 테스트")
    void testScenarioInProgress() {
        // Given
        ConversationContext existingContext = new ConversationContext();
        existingContext.setCurrentStepId(2L);
        
        ChatRequest request = ChatRequest.builder()
                .message("1번 선택")
                .build();
        
        ScenarioExecutionResult mockResult = createMockScenarioExecutionResult("좋은 선택입니다!");
        
        when(scenarioService.getContext(TEST_SESSION_ID)).thenReturn(existingContext);
        when(scenarioService.executeStep(TEST_SESSION_ID, 2L, "1번 선택")).thenReturn(mockResult);
        
        // When
        ChatResponse response = chatController.handleMessage(TEST_SESSION_ID, request);
        
        // Then
        verify(scenarioService).executeStep(TEST_SESSION_ID, 2L, "1번 선택");
        verify(intentRecognizer, never()).analyze(any()); // 의도분석 엔진은 호출되지 않아야 함
        assertThat(response).isNotNull();
        assertThat(response.getMessage()).isEqualTo("좋은 선택입니다!");
    }

    @Test
    @DisplayName("새로운 시나리오 시작 요청 (stepId 포함) 테스트")
    void testNewScenarioWithStepId() {
        // Given
        ChatRequest request = ChatRequest.builder()
                .message("시작")
                .stepId(1L)
                .build();
        
        ScenarioExecutionResult mockResult = createMockScenarioExecutionResult("새 시나리오를 시작합니다!");
        
        when(scenarioService.executeStep(TEST_SESSION_ID, 1L, "시작")).thenReturn(mockResult);
        
        // When
        ChatResponse response = chatController.handleMessage(TEST_SESSION_ID, request);
        
        // Then
        verify(scenarioService).executeStep(TEST_SESSION_ID, 1L, "시작");
        verify(intentRecognizer, never()).analyze(any()); // 의도분석 엔진은 호출되지 않아야 함
        assertThat(response).isNotNull();
        assertThat(response.getMessage()).isEqualTo("새 시나리오를 시작합니다!");
    }

    @Test
    @DisplayName("빈 메시지 입력 처리 테스트")
    void testEmptyMessageHandling() {
        // Given
        ChatRequest request1 = ChatRequest.builder()
                .message("")
                .build();
        
        ChatRequest request2 = ChatRequest.builder()
                .message(null)
                .build();
        
        ChatRequest request3 = ChatRequest.builder()
                .message("   ")
                .build();
        
        // When & Then
        ChatResponse response1 = chatController.handleMessage(TEST_SESSION_ID, request1);
        assertThat(response1.getMessage()).contains("메시지를 입력해주세요");
        assertThat(response1.getMessageType()).isEqualTo("error");
        
        ChatResponse response2 = chatController.handleMessage(TEST_SESSION_ID, request2);
        assertThat(response2.getMessage()).contains("메시지를 입력해주세요");
        
        ChatResponse response3 = chatController.handleMessage(TEST_SESSION_ID, request3);
        assertThat(response3.getMessage()).contains("메시지를 입력해주세요");
        
        // 의도분석 엔진이 호출되지 않아야 함
        verify(intentRecognizer, never()).analyze(any());
    }

    @Test
    @DisplayName("폴백 응답 처리 테스트")
    void testFallbackResponse() {
        // Given
        ChatRequest request = ChatRequest.builder()
                .message("알수없는복잡한입력")
                .build();
        
        IntentAnalysisResult mockResult = IntentAnalysisResult.builder()
                .intentType("FALLBACK")
                .targetId(null)
                .confidence(0.0)
                .responseMessage("죄송합니다. 잘 이해하지 못했어요. 다시 말씀해주세요.")
                .build();
        
        when(intentRecognizer.analyze("알수없는복잡한입력")).thenReturn(mockResult);
        when(intentRecognizer.isStartScenarioIntent(mockResult)).thenReturn(false);
        
        // When
        ChatResponse response = chatController.handleMessage(TEST_SESSION_ID, request);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.getMessage()).contains("잘 이해하지 못했어요");
        assertThat(response.getConfidence()).isEqualTo(0.0);
        assertThat(response.getMessageType()).isEqualTo("text");
        assertThat(response.getChoices()).hasSize(2); // 기본 제안 버튼들
    }

    @Test
    @DisplayName("응답에 제안 버튼이 포함되는지 테스트")
    void testSuggestedActionsGeneration() {
        // Given
        ChatRequest request = ChatRequest.builder()
                .message("도움말")
                .build();
        
        IntentAnalysisResult mockResult = IntentAnalysisResult.builder()
                .intentType("BASIC_CONVERSATION")
                .targetId(3L)
                .confidence(0.8)
                .responseMessage("도움말을 보여드릴게요!")
                .build();
        
        when(intentRecognizer.analyze("도움말")).thenReturn(mockResult);
        when(intentRecognizer.isStartScenarioIntent(mockResult)).thenReturn(false);
        
        // When
        ChatResponse response = chatController.handleMessage(TEST_SESSION_ID, request);
        
        // Then
        assertThat(response.getChoices()).hasSize(2);
        assertThat(response.getChoices()).extracting("label")
                .containsExactly("🚀 시나리오 시작", "❓ 도움말");
        assertThat(response.getChoices()).extracting("value")
                .containsExactly("start_demo", "help");
    }

    @Test
    @DisplayName("예외 발생 시 에러 응답 반환 테스트")
    void testExceptionHandling() {
        // Given
        ChatRequest request = ChatRequest.builder()
                .message("안녕하세요")
                .build();
        
        when(intentRecognizer.analyze("안녕하세요"))
                .thenThrow(new RuntimeException("의도분석 엔진 오류"));
        
        // When
        ChatResponse response = chatController.handleMessage(TEST_SESSION_ID, request);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.getMessage()).contains("오류가 발생했습니다");
        assertThat(response.isFromBot()).isTrue();
        assertThat(response.getMessageType()).isEqualTo("error");
    }

    @Test
    @DisplayName("시나리오 시작 실패 시 에러 응답 테스트")
    void testScenarioStartFailure() {
        // Given
        ChatRequest request = ChatRequest.builder()
                .message("시작")
                .build();
        
        IntentAnalysisResult mockResult = IntentAnalysisResult.builder()
                .intentType("BASIC_CONVERSATION")
                .targetId(5L)
                .confidence(0.9)
                .responseMessage("네! 시나리오를 시작해드릴게요.")
                .build();
        
        when(intentRecognizer.analyze("시작")).thenReturn(mockResult);
        when(intentRecognizer.isStartScenarioIntent(mockResult)).thenReturn(true);
        when(scenarioService.startScenario(TEST_SESSION_ID, 1L))
                .thenThrow(new RuntimeException("시나리오 시작 실패"));
        
        // When
        ChatResponse response = chatController.handleMessage(TEST_SESSION_ID, request);
        
        // Then
        assertThat(response.getMessage()).contains("시나리오를 시작할 수 없습니다");
        assertThat(response.getMessageType()).isEqualTo("error");
    }

    @Test
    @DisplayName("높은 확신도 응답 테스트")
    void testHighConfidenceResponse() {
        // Given
        ChatRequest request = ChatRequest.builder()
                .message("고마워요")
                .build();
        
        IntentAnalysisResult mockResult = IntentAnalysisResult.builder()
                .intentType("BASIC_CONVERSATION")
                .targetId(2L)
                .confidence(0.92)
                .responseMessage("천만에요! 언제든 도움이 필요하시면 말씀해주세요.")
                .build();
        
        when(intentRecognizer.analyze("고마워요")).thenReturn(mockResult);
        when(intentRecognizer.isStartScenarioIntent(mockResult)).thenReturn(false);
        
        // When
        ChatResponse response = chatController.handleMessage(TEST_SESSION_ID, request);
        
        // Then
        assertThat(response.getConfidence()).isGreaterThan(0.9);
        assertThat(response.getMessage()).contains("천만에요");
        assertThat(response.getMessageType()).isEqualTo("text");
    }

    @Test
    @DisplayName("세션 ID가 올바르게 설정되는지 테스트")
    void testSessionIdHandling() {
        // Given
        String anotherSessionId = "another-session-456";
        ChatRequest request = ChatRequest.builder()
                .message("테스트")
                .build();
        
        IntentAnalysisResult mockResult = IntentAnalysisResult.fallback("기본 응답");
        
        when(intentRecognizer.analyze("테스트")).thenReturn(mockResult);
        when(intentRecognizer.isStartScenarioIntent(mockResult)).thenReturn(false);
        
        // When
        ChatResponse response = chatController.handleMessage(anotherSessionId, request);
        
        // Then
        assertThat(response.getSessionId()).isEqualTo(anotherSessionId);
    }

    @Test
    @DisplayName("메시지 타임스탬프가 설정되는지 테스트")
    void testTimestampSetting() {
        // Given
        ChatRequest request = ChatRequest.builder()
                .message("시간 테스트")
                .build();
        
        IntentAnalysisResult mockResult = IntentAnalysisResult.fallback("응답");
        
        when(intentRecognizer.analyze("시간 테스트")).thenReturn(mockResult);
        when(intentRecognizer.isStartScenarioIntent(mockResult)).thenReturn(false);
        
        // When
        ChatResponse response = chatController.handleMessage(TEST_SESSION_ID, request);
        
        // Then
        assertThat(response.getTimestamp()).isNotNull();
        assertThat(response.getTimestamp()).isBeforeOrEqualTo(java.time.LocalDateTime.now());
    }
    
    /**
     * Mock ScenarioExecutionResult 생성 헬퍼 메서드
     */
    private ScenarioExecutionResult createMockScenarioExecutionResult(String content) {
        ScenarioStep mockStep = mock(ScenarioStep.class);
        when(mockStep.getId()).thenReturn(1L);
        when(mockStep.getContent()).thenReturn(content);
        
        ScenarioExecutionResult mockResult = mock(ScenarioExecutionResult.class);
        when(mockResult.getCurrentStep()).thenReturn(mockStep);
        when(mockResult.isCompleted()).thenReturn(false);
        when(mockResult.getChoices()).thenReturn(null);
        when(mockResult.getErrorMessage()).thenReturn(null);
        when(mockResult.getProcessedMessage()).thenReturn(null);
        when(mockResult.getContext()).thenReturn(null);
        when(mockResult.getNextStep()).thenReturn(null);
        
        return mockResult;
    }
}
