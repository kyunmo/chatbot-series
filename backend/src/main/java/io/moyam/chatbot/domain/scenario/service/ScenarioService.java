package io.moyam.chatbot.domain.scenario.service;

import io.moyam.chatbot.domain.conversation.model.ConversationContext;
import io.moyam.chatbot.domain.scenario.model.*;
import io.moyam.chatbot.domain.scenario.repository.ScenarioMapper;
import io.moyam.chatbot.domain.scenario.repository.ScenarioStepMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScenarioService {
    
    private final ScenarioMapper scenarioMapper;
    private final ScenarioStepMapper scenarioStepMapper;
    private final Map<String, ConversationContext> conversationContexts = new ConcurrentHashMap<>();
    
    // 기본 CRUD
    @Transactional
    public Scenario createScenario(Scenario scenario) {
        scenarioMapper.insert(scenario);
        return scenario;
    }
    
    public Scenario getScenario(Long id) {
        return scenarioMapper.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Scenario not found: " + id));
    }
    
    public List<Scenario> getScenariosByBot(Long botId) {
        return scenarioMapper.findByBotId(botId);
    }
    
    @Transactional
    public void updateScenario(Scenario scenario) {
        scenarioMapper.update(scenario);
    }
    
    @Transactional
    public void deleteScenario(Long id) {
        scenarioMapper.deleteById(id);
    }
    
    // ⭐ 핵심 기능: 시나리오 실행
    public ScenarioExecutionResult executeStep(String sessionId, Long stepId, String userInput) {
        
        // 1. 현재 단계 조회
        ScenarioStep currentStep = scenarioStepMapper.findById(stepId)
            .orElseThrow(() -> new IllegalArgumentException("Step not found: " + stepId));
        
        // 2. 대화 컨텍스트 조회/생성
        ConversationContext context = conversationContexts.computeIfAbsent(sessionId, 
            k -> ConversationContext.builder()
                .scenarioId(currentStep.getScenarioId())
                .currentStepId(stepId)
                .variables(new HashMap<>())
                .lastInteraction(LocalDateTime.now())
                .build());
        
        // 3. 사용자 입력 처리 (단순하게)
        if (userInput != null) {
            context.getVariables().put("lastInput", userInput);
        }
        
        // 4. 다음 단계 결정 (복잡한 조건 없이 next_step_id만 사용)
        ScenarioStep nextStep = scenarioStepMapper.findNextStep(stepId).orElse(null);
        
        // 5. 컨텍스트 업데이트
        if (nextStep != null) {
            context.setCurrentStepId(nextStep.getId());
        }
        context.setLastInteraction(LocalDateTime.now());
        
        return ScenarioExecutionResult.builder()
            .currentStep(currentStep)
            .nextStep(nextStep)
            .context(context)
            .isCompleted(nextStep == null)
            .build();
    }
    
    // 시나리오 시작
    public ScenarioExecutionResult startScenario(String sessionId, Long scenarioId) {
        ScenarioStep startStep = scenarioStepMapper.findStartStep(scenarioId)
            .orElseThrow(() -> new IllegalArgumentException("No start step found for scenario: " + scenarioId));
        
        return executeStep(sessionId, startStep.getId(), null);
    }
    
    // 컨텍스트 조회 (디버깅용)
    public ConversationContext getContext(String sessionId) {
        return conversationContexts.get(sessionId);
    }
    
    // 컨텍스트 초기화
    public void clearContext(String sessionId) {
        conversationContexts.remove(sessionId);
    }
}
