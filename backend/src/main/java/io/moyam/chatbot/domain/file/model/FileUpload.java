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
public class FileUpload {
    private Long id;
    private Long userId;
    private String originalFilename;
    private String storedFilename;
    private String filePath;
    private String contentType;
    private Long fileSize;
    private String fileHash;
    private FileStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public enum FileStatus {
        ACTIVE, DELETED, QUARANTINE
    }
}
