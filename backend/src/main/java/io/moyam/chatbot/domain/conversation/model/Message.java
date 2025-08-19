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
public class Message {
    private Long id;
    private Long conversationId;
    private SenderType senderType;
    private String content;
    private MessageType messageType;
    private Map<String, Object> metadata;
    private Boolean isRead;
    private LocalDateTime createdAt;

    public enum SenderType {
        USER, BOT, SYSTEM
    }

    public enum MessageType {
        TEXT, IMAGE, FILE, QUICK_REPLY, CARD
    }
}
