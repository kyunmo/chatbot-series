package io.moyam.chatbot.domain.scenario.service;

import io.moyam.chatbot.domain.conversation.model.ConversationContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 메시지 템플릿 처리기
 * ${variable} 형태의 변수를 실제 값으로 치환
 */
@Component
@Slf4j
public class MessageTemplateProcessor {
    
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");
    
    /**
     * 템플릿 문자열의 변수를 실제 값으로 치환
     */
    public String processTemplate(String template, ConversationContext context) {
        if (template == null || template.trim().isEmpty()) {
            return "";
        }
        
        try {
            // 모든 변수 수집
            Map<String, Object> allVariables = collectAllVariables(context);
            
            log.debug("Processing template: {}", template);
            log.debug("Available variables: {}", allVariables);
            
            // 변수 치환
            String result = template;
            Matcher matcher = VARIABLE_PATTERN.matcher(template);
            
            while (matcher.find()) {
                String variableName = matcher.group(1);
                String placeholder = "${" + variableName + "}";
                
                Object value = allVariables.get(variableName);
                String replacementValue = value != null ? String.valueOf(value) : getDefaultValue(variableName);
                
                log.debug("Replacing {} with {}", placeholder, replacementValue);
                result = result.replace(placeholder, replacementValue);
            }
            
            log.debug("Template processed result: {}", result);
            return result;
            
        } catch (Exception e) {
            log.error("Error processing template: {}", template, e);
            return template; // 오류 시 원본 반환
        }
    }
    
    /**
     * 모든 사용 가능한 변수 수집
     */
    private Map<String, Object> collectAllVariables(ConversationContext context) {
        Map<String, Object> allVariables = new HashMap<>();
        
        // 1. 사용자 변수 추가
        if (context.getVariables() != null) {
            allVariables.putAll(context.getVariables());
        }
        
        // 2. 시스템 변수 추가
        if (context.getSystemVariables() != null) {
            allVariables.putAll(context.getSystemVariables());
        }
        
        // 3. 동적 시스템 변수 생성
        allVariables.putAll(getSystemVariables(context));
        
        return allVariables;
    }
    
    /**
     * 시스템 변수 생성
     */
    private Map<String, Object> getSystemVariables(ConversationContext context) {
        Map<String, Object> systemVars = new HashMap<>();
        
        // 날짜/시간 변수
        LocalDateTime now = LocalDateTime.now();
        systemVars.put("today", now.format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일")));
        systemVars.put("now", now.format(DateTimeFormatter.ofPattern("HH:mm")));
        systemVars.put("hour", now.getHour());
        systemVars.put("dayOfWeek", now.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.KOREAN));
        
        // 사용자 정보
        systemVars.put("userName", getUserName(context));
        systemVars.put("userType", getUserType(context));
        
        // 세션 정보
        if (context.getVariables() != null) {
            systemVars.put("sessionId", context.getVariables().get("sessionId"));
        }
        
        return systemVars;
    }
    
    private String getUserName(ConversationContext context) {
        log.debug("Getting userName from context...");
        log.debug("Context userName field: {}", context.getUserName());
        log.debug("Context variables: {}", context.getVariables());
        
        if (context.getUserName() != null && !context.getUserName().trim().isEmpty()) {
            log.debug("Using userName from context field: {}", context.getUserName());
            return context.getUserName();
        }
        
        if (context.getVariables() != null) {
            Object userName = context.getVariables().get("userName");
            if (userName != null && !userName.toString().trim().isEmpty()) {
                log.debug("Using userName from variables: {}", userName);
                return userName.toString();
            }
        }
        
        log.debug("No userName found, using default: 사용자");
        return "사용자";
    }
    
    private String getUserType(ConversationContext context) {
        if (context.getUserType() != null) {
            return context.getUserType();
        }
        
        if (context.getVariables() != null) {
            Object userType = context.getVariables().get("userType");
            if (userType != null) {
                return userType.toString();
            }
        }
        
        return "basic";
    }
    
    private String getDefaultValue(String variableName) {
        switch (variableName) {
            case "userName":
                return "사용자";
            case "userType":
                return "basic";
            case "today":
                return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일"));
            case "now":
                return LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
            default:
                return "{" + variableName + "}";
        }
    }
}
