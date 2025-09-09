package io.moyam.chatbot.domain.intent.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 의도분석 결과를 담는 모델 클래스
 * 
 * IntentRecognizer에서 사용자 입력을 분석한 결과를 표현합니다.
 * 분석 결과에 따라 적절한 응답을 생성하거나 다음 처리 단계를 결정하는데 사용됩니다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntentAnalysisResult {
    
    /**
     * 의도 유형
     * - SCENARIO: 시나리오 실행 요청
     * - BASIC_CONVERSATION: 기본 대화 (인사말, 감사인사 등)
     * - KNOWLEDGE: 지식베이스 검색 결과 (6편에서 구현)
     * - FEEDBACK: 학습된 피드백 데이터 매칭
     * - FALLBACK: 매칭 실패 시 기본 응답
     */
    private String intentType;
    
    /**
     * 매칭된 대상의 ID
     * - SCENARIO: 시나리오 ID
     * - BASIC_CONVERSATION: ConversationBlock ID
     * - KNOWLEDGE: Knowledge ID
     * - FEEDBACK: Feedback ID
     * - FALLBACK: null
     */
    private Long targetId;
    
    /**
     * 확신도 (0.0 ~ 1.0)
     * 매칭 결과에 대한 신뢰도를 나타냅니다.
     * - 1.0: 완전 일치
     * - 0.8: 높은 유사도
     * - 0.6: 임계값 수준의 매칭
     * - 0.0: 매칭 실패
     */
    private Double confidence;
    
    /**
     * 생성된 응답 메시지
     * 분석 결과에 따라 생성된 ChatBot 응답 텍스트
     */
    private String responseMessage;
    
    /**
     * 추가 메타데이터
     * 응답 생성 시 필요한 부가 정보를 저장
     * 예: 선택 버튼 정보, 다음 단계 힌트 등
     */
    private Object metadata;
    
    /**
     * 매칭 실패 시 사용하는 폴백 결과 생성
     * 
     * @param message 폴백 응답 메시지
     * @return 폴백 타입의 IntentAnalysisResult
     */
    public static IntentAnalysisResult fallback(String message) {
        return IntentAnalysisResult.builder()
                .intentType("FALLBACK")
                .targetId(null)
                .confidence(0.0)
                .responseMessage(message)
                .metadata(null)
                .build();
    }
    
    /**
     * 기본 대화 매칭 결과 생성
     * 
     * @param blockId 매칭된 ConversationBlock ID
     * @param confidence 매칭 확신도
     * @param message 응답 메시지
     * @return 기본대화 타입의 IntentAnalysisResult
     */
    public static IntentAnalysisResult basicConversation(Long blockId, Double confidence, String message) {
        return IntentAnalysisResult.builder()
                .intentType("BASIC_CONVERSATION")
                .targetId(blockId)
                .confidence(confidence)
                .responseMessage(message)
                .build();
    }
    
    /**
     * 시나리오 실행 요청 결과 생성
     * 
     * @param scenarioId 실행할 시나리오 ID
     * @param confidence 매칭 확신도
     * @return 시나리오 타입의 IntentAnalysisResult
     */
    public static IntentAnalysisResult scenario(Long scenarioId, Double confidence) {
        return IntentAnalysisResult.builder()
                .intentType("SCENARIO")
                .targetId(scenarioId)
                .confidence(confidence)
                .responseMessage("시나리오를 시작합니다.")
                .build();
    }
    
    /**
     * 매칭 성공 여부 확인
     * 
     * @return 폴백이 아닌 경우 true
     */
    public boolean isMatched() {
        return !"FALLBACK".equals(intentType) && targetId != null;
    }
    
    /**
     * 높은 확신도 매칭 여부 확인
     * 
     * @return 확신도가 0.8 이상인 경우 true
     */
    public boolean isHighConfidence() {
        return confidence != null && confidence >= 0.8;
    }
}
