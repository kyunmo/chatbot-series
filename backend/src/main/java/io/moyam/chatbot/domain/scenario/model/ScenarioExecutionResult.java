package io.moyam.chatbot.domain.scenario.model;

import io.moyam.chatbot.domain.conversation.model.ConversationContext;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.Collections;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScenarioExecutionResult {
    private ScenarioStep currentStep;
    private ScenarioStep nextStep;
    private ConversationContext context;
    private boolean isCompleted;
    
    /**
     * 동적 선택 버튼 목록
     * 사용자에게 표시할 선택지들
     */
    @Builder.Default
    private List<ChoiceOption> choices = Collections.emptyList();
    
    /**
     * 처리된 메시지 (변수 치환 완료)
     */
    private String processedMessage;
    
    /**
     * 에러 메시지 (에러 발생시만)
     */
    private String errorMessage;
}
