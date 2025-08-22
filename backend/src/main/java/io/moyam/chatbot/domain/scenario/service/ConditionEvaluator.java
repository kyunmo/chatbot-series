package io.moyam.chatbot.domain.scenario.service;

import io.moyam.chatbot.domain.conversation.model.ConversationContext;
import io.moyam.chatbot.domain.scenario.model.ChoiceOption;
import io.moyam.chatbot.domain.scenario.model.ScenarioStep;
import io.moyam.chatbot.domain.scenario.repository.ScenarioStepMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * JSONB 기반 동적 조건 평가 엔진
 * 시나리오 단계의 조건을 평가하여 다음 단계를 결정
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ConditionEvaluator {
    
    private final ScenarioStepMapper scenarioStepMapper;
    
    /**
     * 조건을 평가하여 다음 단계를 결정
     * 
     * @param currentStep 현재 단계
     * @param userInput 사용자 입력
     * @param context 대화 컨텍스트
     * @return 다음 단계 (조건에 맞는 단계가 없으면 null)
     */
    public ScenarioStep evaluateConditions(ScenarioStep currentStep, 
                                         String userInput, 
                                         ConversationContext context) {
        
        if (currentStep.getConditions() == null || currentStep.getConditions().isEmpty()) {
            return getDefaultNextStep(currentStep);
        }
        
        Map<String, Object> conditions = currentStep.getConditions();
        String conditionType = (String) conditions.get("type");
        
        if (conditionType == null) {
            return getDefaultNextStep(currentStep);
        }
        
        try {
            switch (conditionType) {
                case "user_choice":
                    return evaluateUserChoice(conditions, userInput, context);
                case "conditional":
                    return evaluateConditionalRules(conditions, context);
                case "time_based":
                    return evaluateTimeBased(conditions, context);
                case "variable_check":
                    return evaluateVariableCheck(conditions, context);
                default:
                    log.warn("Unknown condition type: {}", conditionType);
                    return getDefaultNextStep(currentStep);
            }
        } catch (Exception e) {
            log.error("Error evaluating conditions for step {}: {}", currentStep.getId(), e.getMessage(), e);
            return getDefaultNextStep(currentStep);
        }
    }
    
    /**
     * 사용자 선택 기반 조건 평가
     */
    @SuppressWarnings("unchecked")
    private ScenarioStep evaluateUserChoice(Map<String, Object> conditions, 
                                           String userInput, 
                                           ConversationContext context) {
        
        List<Map<String, Object>> choices = (List<Map<String, Object>>) conditions.get("choices");
        if (choices == null || choices.isEmpty()) {
            log.debug("No choices found in conditions");
            return null;
        }
        
        log.debug("Evaluating user choice '{}' against {} options", userInput, choices.size());
        
        for (Map<String, Object> choice : choices) {
            String value = (String) choice.get("value");
            log.debug("Checking choice: value='{}', label='{}'", value, choice.get("label"));
            
            if (value != null && value.equals(userInput)) {
                Object nextStepObj = choice.get("next_step");
                if (nextStepObj != null) {
                    Long nextStepId = ((Number) nextStepObj).longValue();
                    log.info("User choice '{}' matched! Moving to step {}", userInput, nextStepId);
                    
                    // 변수 저장 (선택한 값)
                    if (context.getVariables() != null) {
                        context.getVariables().put("lastChoice", value);
                        context.getVariables().put("lastChoiceLabel", choice.get("label"));
                    }
                    
                    return getScenarioStepSafely(nextStepId);
                }
            }
        }
        
        log.warn("No matching choice found for user input: '{}'", userInput);
        return null; // 매칭되는 선택지 없음
    }
    
    /**
     * 조건부 규칙 평가
     */
    @SuppressWarnings("unchecked")
    private ScenarioStep evaluateConditionalRules(Map<String, Object> conditions,
                                                 ConversationContext context) {
        
        List<Map<String, Object>> rules = (List<Map<String, Object>>) conditions.get("rules");
        if (rules == null || rules.isEmpty()) {
            return getDefaultStepFromConditions(conditions);
        }
        
        for (Map<String, Object> rule : rules) {
            String condition = (String) rule.get("condition");
            if (evaluateExpression(condition, context)) {
                Object nextStepObj = rule.get("next_step");
                if (nextStepObj != null) {
                    Long nextStepId = ((Number) nextStepObj).longValue();
                    return getScenarioStepSafely(nextStepId);
                }
            }
        }
        
        // 기본 단계
        return getDefaultStepFromConditions(conditions);
    }
    
    /**
     * 시간 기반 조건 평가
     */
    @SuppressWarnings("unchecked")
    private ScenarioStep evaluateTimeBased(Map<String, Object> conditions,
                                         ConversationContext context) {
        
        LocalDateTime now = LocalDateTime.now();
        int hour = now.getHour();
        
        List<Map<String, Object>> rules = (List<Map<String, Object>>) conditions.get("rules");
        if (rules == null || rules.isEmpty()) {
            return getDefaultStepFromConditions(conditions);
        }
        
        for (Map<String, Object> rule : rules) {
            String condition = (String) rule.get("condition");
            if (evaluateTimeCondition(condition, hour)) {
                Object nextStepObj = rule.get("next_step");
                if (nextStepObj != null) {
                    Long nextStepId = ((Number) nextStepObj).longValue();
                    return getScenarioStepSafely(nextStepId);
                }
            }
        }
        
        return getDefaultStepFromConditions(conditions);
    }
    
    /**
     * 변수 체크 조건 평가
     */
    private ScenarioStep evaluateVariableCheck(Map<String, Object> conditions,
                                             ConversationContext context) {
        // 향후 확장: 변수 존재 여부, 값 체크 등
        return getDefaultStepFromConditions(conditions);
    }
    
    /**
     * 선택지 목록 추출
     */
    @SuppressWarnings("unchecked")
    public List<ChoiceOption> extractChoices(ScenarioStep step) {
        if (step.getConditions() == null || step.getConditions().isEmpty()) {
            return Collections.emptyList();
        }
        
        Map<String, Object> conditions = step.getConditions();
        if (!"user_choice".equals(conditions.get("type"))) {
            return Collections.emptyList();
        }
        
        List<Map<String, Object>> choices = (List<Map<String, Object>>) conditions.get("choices");
        if (choices == null || choices.isEmpty()) {
            return Collections.emptyList();
        }
        
        return choices.stream()
            .map(choice -> ChoiceOption.builder()
                .value((String) choice.get("value"))
                .label((String) choice.get("label"))
                .emoji((String) choice.get("emoji"))
                .description((String) choice.get("description"))
                .nextStepId(choice.get("next_step") != null ? 
                    ((Number) choice.get("next_step")).longValue() : null)
                .build())
            .collect(Collectors.toList());
    }
    
    /**
     * 간단한 표현식 평가 (기본 구현)
     */
    private boolean evaluateExpression(String expression, ConversationContext context) {
        if (expression == null || expression.trim().isEmpty()) {
            return false;
        }
        
        // 기본적인 변수 치환 및 평가
        // 향후 Spring Expression Language (SpEL) 사용 고려
        
        if (context.getVariables() != null) {
            String userType = (String) context.getVariables().get("userType");
            if (expression.contains("userType") && userType != null) {
                return expression.replace("${userType}", "'" + userType + "'")
                    .replace("==", ".equals(")
                    .contains("true"); // 매우 간단한 구현
            }
        }
        
        return false;
    }
    
    /**
     * 시간 조건 평가
     */
    private boolean evaluateTimeCondition(String condition, int hour) {
        if (condition == null) return false;
        
        // 간단한 시간 조건 평가: "hour >= 9 && hour <= 18"
        try {
            return condition.replace("hour", String.valueOf(hour))
                .replace("&&", " && ")
                .replace("||", " || ")
                .contains("true"); // 실제로는 더 정교한 평가 필요
        } catch (Exception e) {
            log.warn("Failed to evaluate time condition: {}", condition, e);
            return false;
        }
    }
    
    /**
     * 기본 다음 단계 가져오기
     */
    private ScenarioStep getDefaultNextStep(ScenarioStep currentStep) {
        if (currentStep.getNextStepId() != null) {
            return getScenarioStepSafely(currentStep.getNextStepId());
        }
        return null;
    }
    
    /**
     * 조건에서 기본 단계 가져오기
     */
    private ScenarioStep getDefaultStepFromConditions(Map<String, Object> conditions) {
        Object defaultStepObj = conditions.get("default_step");
        if (defaultStepObj != null) {
            Long defaultStepId = ((Number) defaultStepObj).longValue();
            return getScenarioStepSafely(defaultStepId);
        }
        return null;
    }
    
    /**
     * 안전한 시나리오 단계 조회
     */
    private ScenarioStep getScenarioStepSafely(Long stepId) {
        try {
            return scenarioStepMapper.findById(stepId).orElse(null);
        } catch (Exception e) {
            log.error("Failed to fetch scenario step {}: {}", stepId, e.getMessage(), e);
            return null;
        }
    }
}
