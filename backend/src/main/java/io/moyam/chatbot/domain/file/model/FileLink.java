package io.moyam.chatbot.domain.file.model;

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
public class FileLink {
    private Long id;
    private Long fileId;
    private String linkToken;
    private AccessType accessType;
    private LocalDateTime expiresAt;
    private Boolean isActive;
    private LocalDateTime createdAt;

    public enum AccessType {
        PUBLIC, PRIVATE, TEMPORARY
    }
}
