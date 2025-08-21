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
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ScenarioService {

    private final ScenarioMapper scenarioMapper;
    private final ScenarioStepMapper scenarioStepMapper;
    private final Map<String, ConversationContext> conversationContexts = new ConcurrentHashMap<>();

    /**
     * 시나리오 시작
     */
    public ScenarioExecutionResult startScenario(String sessionId, Long scenarioId) {
        try {
            ScenarioStep startStep = getStartScenarioStep(scenarioId);
            clearContext(sessionId);
            return executeStep(sessionId, startStep.getId(), null);
        } catch (Exception e) {
            log.error("Error starting scenario {} for session {}", scenarioId, sessionId, e);
            return createErrorResult("시나리오를 시작할 수 없습니다. 다시 시도해주세요.");
        }
    }

    /**
     * 시나리오 단계 실행
     */
    public ScenarioExecutionResult executeStep(String sessionId, Long stepId, String userInput) {
        try {
            // 1. 현재 단계 조회
            ScenarioStep currentStep = getScenarioStepSafely(stepId);
            if (currentStep == null) {
                return createErrorResult("죄송합니다. 올바르지 않은 단계입니다. 처음부터 다시 시작해주세요.");
            }

            // 2. 대화 컨텍스트 조회/생성
            ConversationContext context = conversationContexts.computeIfAbsent(sessionId,
                    k -> ConversationContext.builder()
                            .scenarioId(currentStep.getScenarioId())
                            .currentStepId(stepId)
                            .variables(new HashMap<>())
                            .lastInteraction(LocalDateTime.now())
                            .build());

            // 3. 사용자 입력 처리
            if (userInput != null) {
                userInput = userInput.trim();

                // 공통 명령어 처리
                if (isCommonCommand(userInput)) {
                    return handleCommonCommand(sessionId, userInput, context);
                }

                // 입력값 검증
                if (!isValidInput(currentStep, userInput)) {
                    return createRetryResult(currentStep, "올바른 형식으로 입력해주세요. " + getInputGuide(currentStep));
                }

                context.getVariables().put("lastInput", userInput);
            }

            // 4. 다음 단계 결정
            ScenarioStep nextStep = determineNextStep(currentStep, userInput, context);

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

        } catch (Exception e) {
            log.error("Error executing step {} for session {}", stepId, sessionId, e);
            return createErrorResult("죄송합니다. 처리 중 오류가 발생했습니다. 처음부터 다시 시작해주세요.");
        }
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
        return scenarioStepMapper.findStartStep(scenarioId)
                .orElseThrow(() -> new IllegalArgumentException("No start step found for scenario: " + scenarioId));
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
     * 다음 단계 결정 (조건부 분기 처리)
     */
    private ScenarioStep determineNextStep(ScenarioStep currentStep, String userInput, ConversationContext context) {
        // 메인 메뉴 분기 처리
        if (currentStep.getId() == 2L && userInput != null) {
            switch (userInput.trim()) {
                case "1": return getScenarioStepSafely(3L);  // 일정 관리
                case "2": return getScenarioStepSafely(6L);  // 메모 작성
                case "3": return getScenarioStepSafely(9L);  // 계산
                case "4": return getScenarioStepSafely(12L); // 도움말
                default: return null;
            }
        }

        // 일정 메뉴 분기 처리
        if (currentStep.getId() == 4L && userInput != null) {
            switch (userInput.trim()) {
                case "1": case "2": case "3": return getScenarioStepSafely(5L);  // 일정 조회
                case "4": return getScenarioStepSafely(2L);  // 메인 메뉴
                default: return null;
            }
        }

        // 기본: next_step_id 따라가기
        if (currentStep.getNextStepId() != null) {
            return getScenarioStepSafely(currentStep.getNextStepId());
        }

        return null;
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
     * 입력값 검증
     */
    private boolean isValidInput(ScenarioStep step, String input) {
        if (input == null || input.trim().isEmpty()) {
            return false;
        }

        switch (step.getInputType()) {
            case CHOICE:
                if (step.getId() == 2L || step.getId() == 4L) {
                    return input.matches("[1-4]");
                }
                return true;
            case NUMBER:
                return input.matches("\\d+");
            case EMAIL:
                return input.contains("@");
            case YES_NO:
                return input.matches("(?i)(예|아니오|yes|no|y|n)");
            case TEXT:
            default:
                return true;
        }
    }

    /**
     * 입력 가이드 메시지
     */
    private String getInputGuide(ScenarioStep step) {
        switch (step.getInputType()) {
            case CHOICE:
                if (step.getId() == 2L || step.getId() == 4L) {
                    return "1, 2, 3, 4 중 하나를 입력해주세요.";
                }
                return "제시된 선택지 중 하나를 선택해주세요.";
            case NUMBER:
                return "숫자만 입력해주세요.";
            case EMAIL:
                return "올바른 이메일 형식으로 입력해주세요.";
            case YES_NO:
                return "예 또는 아니오로 답변해주세요.";
            default:
                return "적절한 내용을 입력해주세요.";
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
                .build();
    }

    /**
     * 재시도 결과 생성
     */
    private ScenarioExecutionResult createRetryResult(ScenarioStep currentStep, String additionalMessage) {
        ScenarioStep retryStep = ScenarioStep.builder()
                .id(currentStep.getId())
                .content(currentStep.getContent() + "\n\n" + additionalMessage)
                .stepType(currentStep.getStepType())
                .inputType(currentStep.getInputType())
                .nextStepId(currentStep.getNextStepId())
                .build();

        return ScenarioExecutionResult.builder()
                .currentStep(retryStep)
                .nextStep(null)
                .context(null)
                .isCompleted(false)
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
                .build();
    }
}