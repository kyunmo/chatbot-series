package io.moyam.chatbot.interfaces.api.websocket.response;

import io.moyam.chatbot.domain.scenario.model.ChoiceOption;
import io.moyam.chatbot.domain.scenario.model.ScenarioExecutionResult;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class ChatResponse {
    private String message;           // 봇 응답 메시지
    private String sessionId;         // 세션 ID
    private boolean isFromBot;        // 봇 메시지 여부
    private LocalDateTime timestamp;  // 메시지 시간

    // 시나리오 관련
    private Long currentStepId;       // 현재 단계
    private Long nextStepId;          // 다음 단계
    private Long scenarioId;          // 시나리오 ID
    private boolean isScenarioEnd;    // 시나리오 완료 여부
    
    // 고도화된 기능
    @Builder.Default
    private List<ChoiceOption> choices = Collections.emptyList(); // 동적 선택 버튼
    private String messageType;       // 메시지 타입 (text, choice, error, info)
    private Map<String, Object> variables;  // 변수 정보 (디버깅용)
    private String processedMessage;  // 변수 치환된 메시지
    
    // 의도분석 관련
    private Double confidence;        // 의도분석 확신도 (0.0 ~ 1.0)
    private String intentType;        // 의도 타입 (BASIC_CONVERSATION, FALLBACK 등)
    
    // 레거시 지원
    private List<String> quickReplies; // 빠른 답변 옵션 (레거시)

    public static ChatResponse fromScenarioResult(ScenarioExecutionResult result, String sessionId) {
        if (result == null) {
            return ChatResponse.error("시나리오 실행 결과가 없습니다.");
        }
        
        // 메시지 타입 결정
        String messageType = "text";
        if (result.getChoices() != null && !result.getChoices().isEmpty()) {
            messageType = "choice";
        } else if (result.getErrorMessage() != null) {
            messageType = "error";
        }
        
        // 표시할 메시지 결정 (null 체크 추가)
        String displayMessage = null;
        if (result.getProcessedMessage() != null) {
            displayMessage = result.getProcessedMessage();
        } else if (result.getCurrentStep() != null) {
            displayMessage = result.getCurrentStep().getContent();
        } else {
            displayMessage = "시나리오 단계를 찾을 수 없습니다.";
        }
        
        return ChatResponse.builder()
                .message(displayMessage)
                .sessionId(sessionId)
                .isFromBot(true)
                .currentStepId(result.getCurrentStep() != null ? result.getCurrentStep().getId() : null)
                .nextStepId(result.getNextStep() != null ? result.getNextStep().getId() : null)
                .scenarioId(result.getContext() != null ? result.getContext().getScenarioId() : null)
                .isScenarioEnd(result.isCompleted())
                .choices(result.getChoices() != null ? result.getChoices() : Collections.emptyList())
                .messageType(messageType)
                .variables(result.getContext() != null ? result.getContext().getVariables() : null)
                .processedMessage(result.getProcessedMessage())
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static ChatResponse error(String errorMessage) {
        return ChatResponse.builder()
                .message(errorMessage)
                .isFromBot(true)
                .messageType("error")
                .confidence(0.0)  // 에러는 확신도 0
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    public static ChatResponse info(String infoMessage, String sessionId) {
        return ChatResponse.builder()
                .message(infoMessage)
                .sessionId(sessionId)
                .isFromBot(true)
                .messageType("info")
                .confidence(1.0)  // 정보성 메시지는 확신도 최대
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static ChatResponse fromIntentAnalysis(String message, String sessionId, 
                                                 Double confidence, String intentType, 
                                                 List<ChoiceOption> choices) {
        return ChatResponse.builder()
                .message(message)
                .sessionId(sessionId)
                .isFromBot(true)
                .messageType("text")
                .confidence(confidence)
                .intentType(intentType)
                .choices(choices != null ? choices : Collections.emptyList())
                .timestamp(LocalDateTime.now())
                .build();
    }
}