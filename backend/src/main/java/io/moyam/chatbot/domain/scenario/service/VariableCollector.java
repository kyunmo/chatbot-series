package io.moyam.chatbot.domain.scenario.service;

import io.moyam.chatbot.domain.conversation.model.ConversationContext;
import io.moyam.chatbot.domain.scenario.model.ScenarioStep;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 변수 수집기
 * 사용자 입력을 검증하고 컨텍스트에 저장
 */
@Component
@Slf4j
public class VariableCollector {
    
    /**
     * 시나리오 단계에서 사용자 입력을 변수로 수집
     */
    @SuppressWarnings("unchecked")
    public boolean collectVariable(ScenarioStep step, String userInput, ConversationContext context) {
        if (step.getConditions() == null || userInput == null) {
            return true; // 수집할 변수가 없으면 성공으로 처리
        }
        
        Map<String, Object> conditions = step.getConditions();
        Map<String, Object> variableMapping = (Map<String, Object>) conditions.get("variable_mapping");
        
        if (variableMapping == null) {
            return true;
        }
        
        try {
            String target = (String) variableMapping.get("target");
            String validation = (String) variableMapping.get("validation");
            
            // 입력값 검증
            if (!validateInput(userInput, validation)) {
                return false;
            }
            
            // 변수 저장
            if (target != null) {
                if (context.getVariables() == null) {
                    context.setVariables(new HashMap<>());
                }
                
                String trimmedInput = userInput.trim();
                context.getVariables().put(target, trimmedInput);
                
                // 특별한 변수들은 context 필드에도 저장
                if ("userName".equals(target)) {
                    context.setUserName(trimmedInput);
                    log.info("Setting userName in context: {}", trimmedInput);
                } else if ("userType".equals(target)) {
                    context.setUserType(trimmedInput);
                    log.info("Setting userType in context: {}", trimmedInput);
                }
                
                log.info("Variable collected: {} = {}", target, trimmedInput);
                log.debug("All variables after collection: {}", context.getVariables());
            }
            
            return true;
            
        } catch (Exception e) {
            log.error("Error collecting variable from step {}: {}", step.getId(), e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 입력값 검증
     */
    private boolean validateInput(String input, String validation) {
        if (validation == null || validation.trim().isEmpty()) {
            return true; // 검증 규칙이 없으면 통과
        }
        
        String trimmedInput = input.trim();
        
        // 검증 규칙 파싱 (간단한 구현)
        String[] rules = validation.split("\\|");
        
        for (String rule : rules) {
            rule = rule.trim();
            
            if ("required".equals(rule)) {
                if (trimmedInput.isEmpty()) {
                    return false;
                }
            } else if (rule.startsWith("min:")) {
                int minLength = Integer.parseInt(rule.substring(4));
                if (trimmedInput.length() < minLength) {
                    return false;
                }
            } else if (rule.startsWith("max:")) {
                int maxLength = Integer.parseInt(rule.substring(4));
                if (trimmedInput.length() > maxLength) {
                    return false;
                }
            } else if (rule.startsWith("regex:")) {
                String pattern = rule.substring(6);
                if (!trimmedInput.matches(pattern)) {
                    return false;
                }
            }
        }
        
        return true;
    }
    
    /**
     * 검증 실패 메시지 생성
     */
    @SuppressWarnings("unchecked")
    public String getValidationMessage(ScenarioStep step) {
        if (step.getConditions() == null) {
            return "올바른 형식으로 입력해주세요.";
        }
        
        Map<String, Object> conditions = step.getConditions();
        Map<String, Object> variableMapping = (Map<String, Object>) conditions.get("variable_mapping");
        
        if (variableMapping != null) {
            String message = (String) variableMapping.get("message");
            if (message != null && !message.trim().isEmpty()) {
                return message;
            }
        }
        
        return "올바른 형식으로 입력해주세요.";
    }
}
