package io.moyam.chatbot.interfaces.api.websocket.request;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ChatRequest {
    private String message;      // 사용자 입력
    private String sessionId;    // 세션 ID
    private Long stepId;         // 시나리오 단계 ID (선택적)
    private LocalDateTime timestamp;
}