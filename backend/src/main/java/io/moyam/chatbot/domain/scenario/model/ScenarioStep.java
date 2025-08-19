package io.moyam.chatbot.domain.scenario.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScenarioStep {
    private Long id;
    private Long scenarioId;
    private StepType stepType;
    private String content;
    private InputType inputType;
    private Map<String, Object> conditions;
    private Long nextStepId;
    private Boolean isStartStep;
    private Integer orderIndex;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public enum StepType {
        MESSAGE, QUESTION, CONDITION, ACTION
    }

    public enum InputType {
        TEXT, NUMBER, EMAIL, PHONE, YES_NO, CHOICE
    }
}