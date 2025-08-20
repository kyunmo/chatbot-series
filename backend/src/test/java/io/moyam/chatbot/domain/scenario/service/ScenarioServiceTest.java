package io.moyam.chatbot.domain.scenario.service;

import io.moyam.chatbot.domain.scenario.model.ScenarioExecutionResult;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("dev")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ScenarioServiceTest {
    
    @Autowired
    private ScenarioService scenarioService;
    
    @Test
    @Order(1)
    void testStartScenario() {
        // Given: 시나리오가 생성되어 있다고 가정
        Long scenarioId = 1L;
        String sessionId = "test-session-1";
        
        // When: 시나리오 시작
        ScenarioExecutionResult result = scenarioService.startScenario(sessionId, scenarioId);
        
        // Then: 시작 단계가 반환됨
        assertThat(result.getCurrentStep()).isNotNull();
        assertThat(result.getCurrentStep().getContent()).isEqualTo("안녕하세요! 무엇을 도와드릴까요?");
        assertThat(result.getNextStep()).isNotNull();
        assertThat(result.isCompleted()).isFalse();
        
        System.out.println("=== 시나리오 시작 테스트 ===");
        System.out.println("현재 단계: " + result.getCurrentStep().getContent());
        System.out.println("다음 단계: " + result.getNextStep().getContent());
    }
    
    @Test
    @Order(2)
    void testExecuteStep() {
        // Given: 진행 중인 대화
        String sessionId = "test-session-2";
        Long stepId = 2L; // "원하시는 서비스를 선택해주세요" 단계
        String userInput = "1";
        
        // When: 단계 실행
        ScenarioExecutionResult result = scenarioService.executeStep(sessionId, stepId, userInput);
        
        // Then: 다음 단계로 진행
        assertThat(result.getCurrentStep()).isNotNull();
        assertThat(result.getContext().getVariables().get("lastInput")).isEqualTo("1");
        assertThat(result.getNextStep()).isNotNull();
        assertThat(result.getNextStep().getContent()).isEqualTo("일정을 확인해드리겠습니다!");
        
        System.out.println("=== 단계 실행 테스트 ===");
        System.out.println("사용자 입력: " + userInput);
        System.out.println("현재 단계: " + result.getCurrentStep().getContent());
        System.out.println("다음 단계: " + result.getNextStep().getContent());
        System.out.println("컨텍스트 변수: " + result.getContext().getVariables());
    }
    
    @Test
    @Order(3)
    void testCompleteScenario() {
        // Given: 마지막 단계
        String sessionId = "test-session-3";
        Long stepId = 3L; // "일정을 확인해드리겠습니다!" 단계
        
        // When: 마지막 단계 실행
        ScenarioExecutionResult result = scenarioService.executeStep(sessionId, stepId, null);
        
        // Then: 시나리오 완료
        assertThat(result.getCurrentStep()).isNotNull();
        assertThat(result.getNextStep()).isNull();
        assertThat(result.isCompleted()).isTrue();
        
        System.out.println("=== 시나리오 완료 테스트 ===");
        System.out.println("현재 단계: " + result.getCurrentStep().getContent());
        System.out.println("시나리오 완료: " + result.isCompleted());
    }
    
    @Test
    @Order(4)
    void testContextManagement() {
        // Given
        String sessionId = "test-session-4";
        Long scenarioId = 1L;
        
        // When: 시나리오 시작
        ScenarioExecutionResult result1 = scenarioService.startScenario(sessionId, scenarioId);
        
        // Then: 컨텍스트가 생성됨
        assertThat(scenarioService.getContext(sessionId)).isNotNull();
        
        // When: 컨텍스트 초기화
        scenarioService.clearContext(sessionId);
        
        // Then: 컨텍스트가 삭제됨
        assertThat(scenarioService.getContext(sessionId)).isNull();
        
        System.out.println("=== 컨텍스트 관리 테스트 완료 ===");
    }
}
