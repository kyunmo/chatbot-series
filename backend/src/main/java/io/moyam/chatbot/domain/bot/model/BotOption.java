package io.moyam.chatbot.domain.bot.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BotOption {
    private Long id;
    private Long botId;
    private String optionKey;
    private String optionValue;
    private ValueType valueType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public enum ValueType {
        STRING, NUMBER, BOOLEAN, JSON
    }
}
