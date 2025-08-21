package io.moyam.chatbot.interfaces.api.websocket;

import io.moyam.chatbot.domain.scenario.model.ScenarioExecutionResult;
import io.moyam.chatbot.domain.scenario.service.ScenarioService;
import io.moyam.chatbot.interfaces.api.scenario.request.StartScenarioRequest;
import io.moyam.chatbot.interfaces.api.websocket.request.ChatRequest;
import io.moyam.chatbot.interfaces.api.websocket.response.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;

import java.time.LocalDateTime;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketController {

    private final ScenarioService scenarioService;

    @MessageMapping("/chat/{sessionId}")
    @SendTo("/topic/chat/{sessionId}")
    public ChatResponse handleMessage(
            @DestinationVariable String sessionId,
            ChatRequest request) {

        log.info("Received message from session {}: {}", sessionId, request.getMessage());

        try {
            // 시나리오 실행 엔진 호출
            if (request.getStepId() != null) {
                // 시나리오 기반 대화
                ScenarioExecutionResult result = scenarioService.executeStep(
                        sessionId, request.getStepId(), request.getMessage());

                return ChatResponse.fromScenarioResult(result, sessionId);
            } else {
                // 일반 대화
                return ChatResponse.builder()
                        .message("잘 모르겠습니다. 다시 말씀해 주세요.")
                        .sessionId(sessionId)
                        .isFromBot(true)
                        .timestamp(LocalDateTime.now())
                        .build();
            }
        } catch (Exception e) {
            log.error("Error processing message for session {}", sessionId, e);
            return ChatResponse.error("죄송합니다. 오류가 발생했습니다.");
        }
    }

    @MessageMapping("/chat/{sessionId}/start")
    @SendTo("/topic/chat/{sessionId}")
    public ChatResponse startScenario(
            @DestinationVariable String sessionId,
            @RequestBody StartScenarioRequest request) {

        try {
            ScenarioExecutionResult result = scenarioService.startScenario(
                    sessionId, request.getScenarioId());

            return ChatResponse.fromScenarioResult(result, sessionId);
        } catch (Exception e) {
            log.error("Error starting scenario for session {}", sessionId, e);
            return ChatResponse.error("시나리오를 시작할 수 없습니다.");
        }
    }
}