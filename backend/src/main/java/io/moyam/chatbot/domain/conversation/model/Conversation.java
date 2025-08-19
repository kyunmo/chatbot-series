package io.moyam.chatbot.domain.conversation.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Conversation {
    private Long id;
    private Long userId;
    private Long botId;
    private Long currentScenarioId;
    private Long currentStepId;
    private Map<String, Object> contextData;
    private ConversationStatus status;
    private String sessionId;
    private LocalDateTime startedAt;
    private LocalDateTime lastMessageAt;
    private LocalDateTime endedAt;

    public enum ConversationStatus {
        ACTIVE, PAUSED, COMPLETED, ABANDONED
    }
}
