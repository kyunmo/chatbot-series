package io.moyam.chatbot.interfaces.api.scenario;

import io.moyam.chatbot.domain.scenario.model.Scenario;
import io.moyam.chatbot.domain.scenario.model.ScenarioExecutionResult;
import io.moyam.chatbot.domain.scenario.service.ScenarioService;
import io.moyam.chatbot.interfaces.api.scenario.request.ScenarioCreateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/scenarios")
@RequiredArgsConstructor
public class ScenarioController {
    
    private final ScenarioService scenarioService;
    
    // 시나리오 목록 조회
    @GetMapping
    public List<Scenario> getScenarios(@RequestParam Long botId) {
        return scenarioService.getScenariosByBot(botId);
    }
    
    // 시나리오 상세 조회
    @GetMapping("/{id}")
    public Scenario getScenario(@PathVariable Long id) {
        return scenarioService.getScenario(id);
    }
    
    // 시나리오 생성
    @PostMapping
    public Scenario createScenario(@RequestBody ScenarioCreateRequest request) {
        Scenario scenario = Scenario.builder()
            .botId(request.getBotId())
            .name(request.getName())
            .description(request.getDescription())
            .isDefault(false)
            .build();
        
        return scenarioService.createScenario(scenario);
    }

/*    @PostMapping("/{scenarioId}/start")
    public ScenarioExecutionResult startScenario(
            @PathVariable Long scenarioId,
            @RequestParam String sessionId) {
        
        return scenarioService.startScenario(sessionId, scenarioId);
    }*/
    
    @PostMapping("/steps/{stepId}/execute")
    public ScenarioExecutionResult executeStep(
            @PathVariable Long stepId,
            @RequestParam String sessionId,
            @RequestBody(required = false) Map<String, String> request) {
        
        String userInput = request != null ? request.get("input") : null;
        return scenarioService.executeStep(sessionId, stepId, userInput);
    }
    
    // 컨텍스트 조회 (디버깅용)
    @GetMapping("/context/{sessionId}")
    public Map<String, Object> getContext(@PathVariable String sessionId) {
        return Map.of("context", scenarioService.getContext(sessionId));
    }
    
    // 컨텍스트 초기화
    @DeleteMapping("/context/{sessionId}")
    public void clearContext(@PathVariable String sessionId) {
        scenarioService.clearContext(sessionId);
    }
}
