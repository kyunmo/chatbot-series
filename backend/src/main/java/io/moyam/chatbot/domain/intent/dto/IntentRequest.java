package io.moyam.chatbot.domain.intent.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 의도분석 요청 DTO
 * 
 * REST API나 내부 서비스 호출 시 의도분석 요청 데이터를 담는 클래스
 * 현재는 WebSocket 기반이지만, 향후 REST API 제공 시 활용
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntentRequest {
    
    /**
     * 분석할 사용자 입력 텍스트
     */
    @NotBlank(message = "분석할 텍스트는 필수입니다")
    @Size(max = 1000, message = "입력 텍스트는 1000자를 초과할 수 없습니다")
    private String input;
    
    /**
     * 세션 ID (선택사항)
     * 사용자별 맞춤 분석이나 로깅에 활용
     */
    private String sessionId;
    
    /**
     * 분석 모드 (선택사항)
     * - "default": 기본 분석
     * - "strict": 엄격한 임계값 적용
     * - "loose": 느슨한 임계값 적용
     */
    @Builder.Default
    private String analysisMode = "default";
    
    /**
     * 강제 의도 타입 (테스트/디버깅용)
     * 특정 의도로 강제 분석 시 사용
     */
    private String forceIntentType;
    
    /**
     * 컨텍스트 정보 (향후 확장용)
     * 이전 대화 맥락이나 사용자 정보 등
     */
    private String context;
    
    /**
     * 디버그 모드 여부
     * true 시 분석 과정 상세 정보 포함
     */
    @Builder.Default
    private Boolean debugMode = false;
    
    /**
     * 요청 검증
     */
    public boolean isValid() {
        return input != null && 
               !input.trim().isEmpty() && 
               input.length() <= 1000;
    }
    
    /**
     * 분석 모드 검증
     */
    public boolean isValidAnalysisMode() {
        if (analysisMode == null) return false;
        return analysisMode.matches("^(default|strict|loose)$");
    }
    
    /**
     * 정규화된 입력 텍스트 반환
     */
    public String getNormalizedInput() {
        return input != null ? input.trim() : "";
    }
}
