package io.moyam.chatbot.domain.scenario.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 동적 선택 버튼 옵션
 * WebSocket 응답에서 사용자에게 표시할 선택지를 정의
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChoiceOption {
    
    /**
     * 선택지 값 (서버로 전송될 실제 값)
     */
    private String value;
    
    /**
     * 사용자에게 표시될 라벨
     */
    private String label;
    
    /**
     * 선택지 이모지 (선택사항)
     */
    private String emoji;
    
    /**
     * 선택지 설명 (선택사항)
     */
    private String description;
    
    /**
     * 다음 단계 ID (조건 평가에서 사용)
     */
    private Long nextStepId;
}
