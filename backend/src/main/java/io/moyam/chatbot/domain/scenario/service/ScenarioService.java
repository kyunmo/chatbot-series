package io.moyam.chatbot.domain.scenario.service;

import io.moyam.chatbot.domain.conversation.model.ConversationContext;
import io.moyam.chatbot.domain.scenario.model.*;
import io.moyam.chatbot.domain.scenario.repository.ScenarioMapper;
import io.moyam.chatbot.domain.scenario.repository.ScenarioStepMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ScenarioService {

    private final ScenarioMapper scenarioMapper;
    private final ScenarioStepMapper scenarioStepMapper;
    private final ConditionEvaluator conditionEvaluator;
    private final MessageTemplateProcessor templateProcessor;
    private final VariableCollector variableCollector;
    
    private final Map<String, ConversationContext> conversationContexts = new ConcurrentHashMap<>();

    /**
     * 시나리오 시작
     */
    public ScenarioExecutionResult startScenario(String sessionId, Long scenarioId) {
        try {
            log.info("Starting scenario {} for session {}", scenarioId, sessionId);
            
            ScenarioStep startStep = getStartScenarioStep(scenarioId);
            log.info("Found start step: id={}, content={}", startStep.getId(), startStep.getContent());
            
            // 기존 컨텍스트 완전 삭제
            clearContext(sessionId);
            
            // Step 1부터 강제 시작 (사용자 입력 없이)
            return executeStep(sessionId, startStep.getId(), null);
        } catch (Exception e) {
            log.error("Error starting scenario {} for session {}", scenarioId, sessionId, e);
            return createErrorResult("시나리오를 시작할 수 없습니다. 다시 시도해주세요.");
        }
    }

    /**
     * 시나리오 단계 실행 (고도화된 버전)
     */
    public ScenarioExecutionResult executeStep(String sessionId, Long stepId, String userInput) {
        try {
            // 1. 현재 단계 조회
            ScenarioStep currentStep = getScenarioStepSafely(stepId);
            if (currentStep == null) {
                return createErrorResult("올바르지 않은 단계입니다. 처음부터 다시 시작해주세요.");
            }

            // 2. 컨텍스트 조회/생성
            ConversationContext context = getOrCreateContext(sessionId, currentStep);

            // 3. 변수 수집 (사용자 입력이 있는 경우)
            if (userInput != null && !userInput.trim().isEmpty()) {
                // 공통 명령어 처리
                if (isCommonCommand(userInput)) {
                    return handleCommonCommand(sessionId, userInput, context);
                }
                
                log.debug("Processing user input '{}' for step {}", userInput, currentStep.getId());
                
                // 변수 수집 및 검증
                boolean hasVariableMapping = hasVariableMapping(currentStep);
                log.debug("Step {} has variable mapping: {}", currentStep.getId(), hasVariableMapping);
                
                if (hasVariableMapping) {
                    if (!variableCollector.collectVariable(currentStep, userInput, context)) {
                        String validationMessage = variableCollector.getValidationMessage(currentStep);
                        return createRetryResult(currentStep, validationMessage);
                    }
                    
                    log.debug("Variables after collection: {}", context.getVariables());
                    log.debug("Context userName: {}", context.getUserName());
                    
                    // 변수 수집 후 자동으로 다음 단계로 진행 (variable_mapping이 있는 경우)
                    if (currentStep.getNextStepId() != null) {
                        ScenarioStep nextStep = getScenarioStepSafely(currentStep.getNextStepId());
                        if (nextStep != null) {
                            log.debug("Auto-advancing to next step {} after variable collection", nextStep.getId());
                            
                            // 컨텍스트 업데이트
                            context.setCurrentStepId(nextStep.getId());
                            context.setLastInteraction(LocalDateTime.now());
                            if (context.getVisitedSteps() == null) {
                                context.setVisitedSteps(new java.util.ArrayList<>());
                            }
                            context.getVisitedSteps().add(currentStep.getId().toString());
                            context.getVariables().put("lastInput", userInput.trim());

                            // 업데이트된 컨텍스트로 템플릿 처리
                            String processedMessage = templateProcessor.processTemplate(nextStep.getContent(), context);
                            List<ChoiceOption> choices = conditionEvaluator.extractChoices(nextStep);

                            log.debug("Next step message processed: {}", processedMessage);

                            return ScenarioExecutionResult.builder()
                                .currentStep(nextStep)
                                .nextStep(null) // 자동 진행 완료
                                .context(context)
                                .isCompleted(false)
                                .choices(choices)
                                .processedMessage(processedMessage)
                                .build();
                        }
                    }
                } else {
                    // 변수 수집이 아닌 경우: 조건 평가 수행
                    ScenarioStep nextStep = conditionEvaluator.evaluateConditions(currentStep, userInput, context);

                    if (nextStep != null) {
                        // 다음 단계로 바로 진행
                        context.setCurrentStepId(nextStep.getId());
                        context.setLastInteraction(LocalDateTime.now());
                        if (context.getVisitedSteps() == null) {
                            context.setVisitedSteps(new java.util.ArrayList<>());
                        }
                        context.getVisitedSteps().add(currentStep.getId().toString());
                        context.getVariables().put("lastInput", userInput.trim());

                        String processedMessage = templateProcessor.processTemplate(nextStep.getContent(), context);
                        List<ChoiceOption> choices = conditionEvaluator.extractChoices(nextStep);

                        return ScenarioExecutionResult.builder()
                            .currentStep(nextStep)
                            .nextStep(null) // 이미 진행 완료
                            .context(context)
                            .isCompleted(false)
                            .choices(choices)
                            .processedMessage(processedMessage)
                            .build();
                    } else {
                        // 매칭되는 선택지가 없음
                        String processedMessage = templateProcessor.processTemplate(currentStep.getContent(), context);
                        List<ChoiceOption> choices = conditionEvaluator.extractChoices(currentStep);

                        return ScenarioExecutionResult.builder()
                            .currentStep(currentStep)
                            .nextStep(null)
                            .context(context)
                            .isCompleted(false)
                            .choices(choices)
                            .processedMessage(processedMessage)
                            .build();
                    }
                }

            }

            // 사용자 입력이 없는 경우 (시나리오 시작 시)
            String processedMessage = templateProcessor.processTemplate(currentStep.getContent(), context);
            List<ChoiceOption> choices = conditionEvaluator.extractChoices(currentStep);

            // 컨텍스트 업데이트
            updateContext(context, currentStep, null, userInput);

            return ScenarioExecutionResult.builder()
                .currentStep(currentStep)
                .nextStep(null)
                .context(context)
                .isCompleted(false)
                .choices(choices)
                .processedMessage(processedMessage)
                .build();
                
        } catch (Exception e) {
            log.error("Error executing step {} for session {}", stepId, sessionId, e);
            return createErrorResult("처리 중 오류가 발생했습니다.");
        }
    }

    /**
     * 컨텍스트 조회 또는 생성
     */
    private ConversationContext getOrCreateContext(String sessionId, ScenarioStep currentStep) {
        return conversationContexts.computeIfAbsent(sessionId, k -> {
            log.info("Creating new context for session {} with step {}", sessionId, currentStep.getId());
            
            ConversationContext context = ConversationContext.builder()
                .scenarioId(currentStep.getScenarioId())
                .currentStepId(currentStep.getId())
                .variables(new HashMap<>())
                .systemVariables(new HashMap<>())
                .lastInteraction(LocalDateTime.now())
                .build();
            
            // 세션 ID를 변수에 저장
            context.getVariables().put("sessionId", sessionId);
            
            log.debug("New context created: currentStepId={}, variables={}", 
                     context.getCurrentStepId(), context.getVariables());
            
            return context;
        });
    }
    
    /**
     * 컨텍스트 업데이트
     */
    private void updateContext(ConversationContext context, ScenarioStep currentStep, 
                             ScenarioStep nextStep, String userInput) {
        // 다음 단계가 있으면 컨텍스트 업데이트
        if (nextStep != null) {
            log.debug("Updating context currentStepId from {} to {}",
                     context.getCurrentStepId(), nextStep.getId());
            context.setCurrentStepId(nextStep.getId());
        }

        context.setLastInteraction(LocalDateTime.now());

        // 방문한 단계 기록
        if (context.getVisitedSteps() == null) {
            context.setVisitedSteps(new java.util.ArrayList<>());
        }
        context.getVisitedSteps().add(currentStep.getId().toString());

        // 마지막 입력 저장
        if (userInput != null && !userInput.trim().isEmpty()) {
            context.getVariables().put("lastInput", userInput.trim());
        }

        log.debug("Context updated: currentStepId={}, variables={}",
                 context.getCurrentStepId(), context.getVariables());
    }

    /**
     * 대화 컨텍스트 조회
     */
    public ConversationContext getContext(String sessionId) {
        return conversationContexts.get(sessionId);
    }

    /**
     * 대화 컨텍스트 초기화
     */
    public void clearContext(String sessionId) {
        conversationContexts.remove(sessionId);
    }

    @Transactional
    public Scenario createScenario(Scenario scenario) {
        scenarioMapper.insert(scenario);
        log.debug("Created scenario: {}", scenario.getId());
        return scenario;
    }

    @Cacheable(value = "scenarios", key = "#id")
    public Scenario getScenario(Long id) {
        return scenarioMapper.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Scenario not found: " + id));
    }

    public List<Scenario> getScenariosByBot(Long botId) {
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
            @CacheEvict(value = "scenarioSteps", allEntries = true)
    })
    public void deleteScenario(Long id) {
        scenarioMapper.deleteById(id);
        log.debug("Deleted scenario: {}", id);
    }

    @Cacheable(value = "scenarioSteps", key = "#stepId")
    public ScenarioStep getScenarioStep(Long stepId) {
        return scenarioStepMapper.findById(stepId)
                .orElseThrow(() -> new IllegalArgumentException("Step not found: " + stepId));
    }

    @Cacheable(value = "scenarioSteps", key = "'start_' + #scenarioId")
    public ScenarioStep getStartScenarioStep(Long scenarioId) {
        try {
            // 시작 단계 조회
            Optional<ScenarioStep> startStep = scenarioStepMapper.findStartStep(scenarioId);
            
            if (startStep.isPresent()) {
                log.info("Found start step: id={}, content={}", 
                    startStep.get().getId(), startStep.get().getContent());
                return startStep.get();
            } else {
                log.warn("No start step found for scenario {}, using step 1 as fallback", scenarioId);
                // 시작 단계가 없으면 Step 1을 시작 단계로 사용
                return scenarioStepMapper.findById(1L)
                    .orElseThrow(() -> new IllegalArgumentException("No start step found for scenario: " + scenarioId));
            }
        } catch (Exception e) {
            log.error("Error finding start step for scenario {}", scenarioId, e);
            throw new IllegalArgumentException("No start step found for scenario: " + scenarioId);
        }
    }

    @Cacheable(value = "scenarioSteps", key = "'next_' + #currentStepId")
    public ScenarioStep getNextScenarioStep(Long currentStepId) {
        return scenarioStepMapper.findNextStep(currentStepId).orElse(null);
    }

    /**
     * 안전한 시나리오 단계 조회
     */
    private ScenarioStep getScenarioStepSafely(Long stepId) {
        try {
            return scenarioStepMapper.findById(stepId).orElse(null);
        } catch (Exception e) {
            log.error("Error finding scenario step: {}", stepId, e);
            return null;
        }
    }
    
    /**
     * 변수 매핑이 있는지 확인
     */
    @SuppressWarnings("unchecked")
    private boolean hasVariableMapping(ScenarioStep step) {
        if (step.getConditions() == null) {
            return false;
        }
        
        Map<String, Object> variableMapping = (Map<String, Object>) step.getConditions().get("variable_mapping");
        return variableMapping != null && !variableMapping.isEmpty();
    }

    /**
     * 공통 명령어 확인
     */
    private boolean isCommonCommand(String input) {
        String lowerInput = input.toLowerCase();
        return lowerInput.equals("메뉴") || lowerInput.equals("처음") ||
                lowerInput.equals("시작") || lowerInput.equals("도움말") ||
                lowerInput.equals("취소") || lowerInput.equals("종료");
    }

    /**
     * 공통 명령어 처리
     */
    private ScenarioExecutionResult handleCommonCommand(String sessionId, String command, ConversationContext context) {
        switch (command.toLowerCase()) {
            case "메뉴": case "처음": case "시작":
                return executeStep(sessionId, 2L, null);
            case "도움말":
                return executeStep(sessionId, 12L, null);
            case "취소": case "종료":
                clearContext(sessionId);
                return createSimpleResult("대화가 종료되었습니다. 다시 이용해주세요!");
            default:
                return createErrorResult("잘 모르겠습니다. '메뉴'라고 입력하시면 메인 메뉴로 이동합니다.");
        }
    }

    /**
     * 오류 결과 생성
     */
    private ScenarioExecutionResult createErrorResult(String message) {
        ScenarioStep errorStep = ScenarioStep.builder()
                .id(-1L)
                .content(message)
                .stepType(ScenarioStep.StepType.MESSAGE)
                .inputType(ScenarioStep.InputType.TEXT)
                .build();

        return ScenarioExecutionResult.builder()
                .currentStep(errorStep)
                .nextStep(null)
                .context(null)
                .isCompleted(true)
                .errorMessage(message)
                .build();
    }

    /**
     * 재시도 결과 생성
     */
    private ScenarioExecutionResult createRetryResult(ScenarioStep currentStep, String additionalMessage) {
        String retryMessage = currentStep.getContent() + "\n\n" + additionalMessage;
        
        ScenarioStep retryStep = ScenarioStep.builder()
                .id(currentStep.getId())
                .content(retryMessage)
                .stepType(currentStep.getStepType())
                .inputType(currentStep.getInputType())
                .nextStepId(currentStep.getNextStepId())
                .conditions(currentStep.getConditions())
                .build();

        return ScenarioExecutionResult.builder()
                .currentStep(retryStep)
                .nextStep(null)
                .context(null)
                .isCompleted(false)
                .processedMessage(retryMessage)
                .build();
    }

    /**
     * 단순 메시지 결과 생성
     */
    private ScenarioExecutionResult createSimpleResult(String message) {
        ScenarioStep simpleStep = ScenarioStep.builder()
                .id(-1L)
                .content(message)
                .stepType(ScenarioStep.StepType.MESSAGE)
                .inputType(ScenarioStep.InputType.TEXT)
                .build();

        return ScenarioExecutionResult.builder()
                .currentStep(simpleStep)
                .nextStep(null)
                .context(null)
                .isCompleted(true)
                .processedMessage(message)
                .build();
    }
}
