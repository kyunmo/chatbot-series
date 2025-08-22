package io.moyam.chatbot.domain.conversation.model;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationContext {
    private Long scenarioId;
    private Long currentStepId;
    private Map<String, Object> variables;  // 대화 중 수집된 정보
    private Map<String, Object> systemVariables; // 시스템 변수 추가
    private LocalDateTime lastInteraction;

    // 새로 추가되는 필드들
    private String userName;                      // 사용자 이름
    private String userType;                      // 사용자 타입 (basic/premium)
    private Map<String, Object> sessionData;     // 세션별 데이터
    private List<String> visitedSteps;           // 방문한 단계 기록
}