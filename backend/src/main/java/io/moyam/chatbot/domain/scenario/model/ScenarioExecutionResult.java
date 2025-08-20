package io.moyam.chatbot.domain.scenario.model;

import io.moyam.chatbot.domain.conversation.model.ConversationContext;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

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
}
