package io.moyam.chatbot.domain.typoword.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 오타 교정 데이터를 관리하는 엔티티
 * 
 * 사용자가 자주 입력하는 오타나 줄임말을 올바른 단어로 매핑하여
 * 의도분석의 정확도를 높이는데 사용됩니다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Typoword {

    private Long id;
    private String incorrectWord;
    private String correctWord;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    /**
     * 오타 교정 규칙의 유효성 검증
     * 
     * @return 올바른 오타 규칙인 경우 true
     */
    public boolean isValid() {
        return incorrectWord != null && !incorrectWord.trim().isEmpty() &&
               correctWord != null && !correctWord.trim().isEmpty() &&
               !incorrectWord.equals(correctWord);  // 같은 단어면 교정 불필요
    }
    
    /**
     * 교정 전후 유사도 확인 (간단한 검증)
     * 
     * @return 교정 전후 단어가 너무 다르면 false (오등록 방지)
     */
    public boolean isSimilarityReasonable() {
        if (!isValid()) return false;
        
        // 길이 차이가 5자 이상이면 의심스러운 매핑으로 판단
        return Math.abs(incorrectWord.length() - correctWord.length()) <= 5;
    }
    
    /**
     * 오타 교정 규칙 요약 정보 생성
     * 
     * @return 관리자용 요약 문자열
     */
    public String getSummary() {
        return String.format("'%s' → '%s' (%s)", 
                incorrectWord, correctWord, 
                description != null ? description : "설명 없음");
    }
}
