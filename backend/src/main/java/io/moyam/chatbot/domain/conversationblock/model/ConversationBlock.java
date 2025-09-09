package io.moyam.chatbot.domain.conversationblock.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Arrays;

/**
 * 기본 대화 블록 엔티티
 * 
 * 인사말, 감사인사, 도움요청 등 일반적인 대화 패턴을 관리합니다.
 * 키워드 기반 매칭으로 적절한 응답을 제공합니다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationBlock {
    
    private Long id;
    
    /**
     * 대화 블록 제목 (예: "인사말", "감사인사", "도움요청")
     */
    private String title;
    
    /**
     * 매칭할 키워드들 (쉼표로 구분)
     * 예: "안녕,하이,hello,hi,안녕하세요"
     */
    private String keywords;
    
    /**
     * 매칭 시 반환할 응답 메시지
     */
    private String responseMessage;
    
    /**
     * 활성화 여부
     */
    private Boolean isActive;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    /**
     * 키워드 문자열을 List로 변환
     * IntentRecognizer에서 매칭 시 사용
     */
    public List<String> getKeywordList() {
        if (keywords == null || keywords.trim().isEmpty()) {
            return List.of();
        }
        return Arrays.stream(keywords.split(","))
                .map(String::trim)
                .filter(keyword -> !keyword.isEmpty())
                .toList();
    }
    
    /**
     * 특정 키워드가 포함되어 있는지 확인
     */
    public boolean containsKeyword(String keyword) {
        return getKeywordList().stream()
                .anyMatch(k -> k.equalsIgnoreCase(keyword.trim()));
    }
    
    /**
     * 새로운 키워드 추가
     */
    public void addKeyword(String newKeyword) {
        if (newKeyword == null || newKeyword.trim().isEmpty()) {
            return;
        }
        
        if (keywords == null || keywords.trim().isEmpty()) {
            keywords = newKeyword.trim();
        } else {
            keywords = keywords + "," + newKeyword.trim();
        }
    }
}
