package io.moyam.chatbot.domain.scenario.service;

import io.moyam.chatbot.domain.conversation.model.ConversationContext;
import io.moyam.chatbot.domain.scenario.model.*;
import io.moyam.chatbot.domain.scenario.repository.ScenarioMapper;
import io.moyam.chatbot.domain.scenario.repository.ScenarioStepMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
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
@Slf4j
public class ScenarioService {
    
    private final ScenarioMapper scenarioMapper;
    private final ScenarioStepMapper scenarioStepMapper;
    private final Map<String, ConversationContext> conversationContexts = new ConcurrentHashMap<>();
    
    // 기본 CRUD
    @Transactional
    @CacheEvict(value = "scenarios", key = "#scenario.id")
    public Scenario createScenario(Scenario scenario) {
        scenarioMapper.insert(scenario);
        log.debug("Created scenario: {}", scenario.getId());
        return scenario;
    }

    @Cacheable(value = "scenarios", key = "#id")
    public Scenario getScenario(Long id) {
        log.debug("Loading scenario from database: {}", id);
        return scenarioMapper.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Scenario not found: " + id));
    }
    
    @Cacheable(value = "scenarios", key = "'bot_' + #botId")
    public List<Scenario> getScenariosByBot(Long botId) {
        log.debug("Loading scenarios for bot: {}", botId);
        return scenarioMapper.findByBotId(botId);
    }
    
    @Transactional
    @CacheEvict(value = "scenarios", key = "#scenario.id")
    public void updateScenario(Scenario scenario) {
        scenarioMapper.update(scenario);
        log.debug("Updated scenario: {}", scenario.getId());
    }
    
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "scenarios", key = "#id"),
        @CacheEvict(value = "scenarioSteps", allEntries = true) // 관련 단계들도 모두 삭제
    })
    public void deleteScenario(Long id) {
        scenarioMapper.deleteById(id);
        log.debug("Deleted scenario: {}", id);
    }
    
    // 시나리오 실행
    public ScenarioExecutionResult executeStep(String sessionId, Long stepId, String userInput) {
        
        // 1. 현재 단계 조회 (캐싱 적용)
        ScenarioStep currentStep = getScenarioStep(stepId);
        
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
        ScenarioStep nextStep = getNextScenarioStep(stepId);
        
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
        ScenarioStep startStep = getStartScenarioStep(scenarioId);
        return executeStep(sessionId, startStep.getId(), null);
    }
    
    // 캐싱된 ScenarioStep 조회
    @Cacheable(value = "scenarioSteps", key = "#stepId")
    public ScenarioStep getScenarioStep(Long stepId) {
        log.debug("Loading scenario step from database: {}", stepId);
        return scenarioStepMapper.findById(stepId)
            .orElseThrow(() -> new IllegalArgumentException("Step not found: " + stepId));
    }
    
    @Cacheable(value = "scenarioSteps", key = "'start_' + #scenarioId")  
    public ScenarioStep getStartScenarioStep(Long scenarioId) {
        log.debug("Loading start step for scenario: {}", scenarioId);
        return scenarioStepMapper.findStartStep(scenarioId)
            .orElseThrow(() -> new IllegalArgumentException("No start step found for scenario: " + scenarioId));
    }
    
    @Cacheable(value = "scenarioSteps", key = "'next_' + #currentStepId")
    public ScenarioStep getNextScenarioStep(Long currentStepId) {
        log.debug("Loading next step for: {}", currentStepId);
        return scenarioStepMapper.findNextStep(currentStepId).orElse(null);
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
