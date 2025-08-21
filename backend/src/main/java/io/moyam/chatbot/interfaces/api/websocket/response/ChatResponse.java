package io.moyam.chatbot.interfaces.api.websocket.response;

import io.moyam.chatbot.domain.scenario.model.ScenarioExecutionResult;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

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
    private boolean isScenarioEnd;    // 시나리오 완료 여부
    private List<String> quickReplies; // 빠른 답변 옵션

    public static ChatResponse fromScenarioResult(ScenarioExecutionResult result, String sessionId) {
        return ChatResponse.builder()
                .message(result.getCurrentStep().getContent())
                .sessionId(sessionId)
                .isFromBot(true)
                .currentStepId(result.getCurrentStep().getId())
                .nextStepId(result.getNextStep() != null ? result.getNextStep().getId() : null)
                .isScenarioEnd(result.isCompleted())
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static ChatResponse error(String errorMessage) {
        return ChatResponse.builder()
                .message(errorMessage)
                .isFromBot(true)
                .timestamp(LocalDateTime.now())
                .build();
    }
}