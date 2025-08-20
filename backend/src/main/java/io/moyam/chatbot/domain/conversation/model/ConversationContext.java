package io.moyam.chatbot.domain.conversation.model;

import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationContext {
    private Long scenarioId;
    private Long currentStepId;
    private Map<String, Object> variables;  // 대화 중 수집된 정보
    private LocalDateTime lastInteraction;
}