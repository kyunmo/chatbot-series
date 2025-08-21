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
            // 입력값 검증
            if (request.getMessage() == null || request.getMessage().trim().isEmpty()) {
                return ChatResponse.builder()
                        .message("메시지를 입력해주세요.")
                        .sessionId(sessionId)
                        .isFromBot(true)
                        .timestamp(LocalDateTime.now())
                        .build();
            }

            // 시나리오 실행 엔진 호출
            if (request.getStepId() != null && request.getStepId() > 0) {
                // 시나리오 기반 대화
                ScenarioExecutionResult result = scenarioService.executeStep(
                        sessionId, request.getStepId(), request.getMessage());

                return ChatResponse.fromScenarioResult(result, sessionId);
            } else {
                // 일반 대화 - 간단한 키워드 매칭 추가
                String response = generateSmartResponse(request.getMessage());

                return ChatResponse.builder()
                        .message(response)
                        .sessionId(sessionId)
                        .isFromBot(true)
                        .timestamp(LocalDateTime.now())
                        .build();
            }
        } catch (Exception e) {
            log.error("Error processing message for session {}", sessionId, e);
            return ChatResponse.builder()
                    .message("죄송합니다. 일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요.")
                    .sessionId(sessionId)
                    .isFromBot(true)
                    .timestamp(LocalDateTime.now())
                    .build();
        }
    }

    // 간단한 키워드 기반 응답 생성
    private String generateSmartResponse(String message) {
        String lowerMessage = message.toLowerCase().trim();

        // 인사말 처리
        if (lowerMessage.contains("안녕") || lowerMessage.contains("hi") || lowerMessage.contains("hello")) {
            return "안녕하세요! 😊 저는 개인 비서 ChatBot입니다.\n'시나리오 데모 시작' 버튼을 눌러서 대화를 시작해보세요!";
        }

        // 도움 요청
        if (lowerMessage.contains("도움") || lowerMessage.contains("help")) {
            return "도움이 필요하시군요! 💡\n\n이 ChatBot은 다음과 같은 서비스를 제공합니다:\n• 일정 관리\n• 메모 작성\n• 간단한 계산\n\n'시나리오 데모 시작' 버튼을 눌러보세요!";
        }

        // 감사 인사
        if (lowerMessage.contains("고마") || lowerMessage.contains("감사") || lowerMessage.contains("thank")) {
            return "천만에요! 😊 언제든 도움이 필요하시면 말씀해주세요.";
        }

        // 기본 응답
        return "잘 모르겠습니다. 😅\n\n구체적인 도움을 받으시려면 '시나리오 데모 시작' 버튼을 눌러서 대화를 시작해보세요!\n\n또는 다음과 같이 말씀해주세요:\n• '도움말'\n• '안녕하세요'\n• '고마워'";
    }

    @MessageMapping("/chat/{sessionId}/start")
    @SendTo("/topic/chat/{sessionId}")
    public ChatResponse startScenario(
            @DestinationVariable String sessionId,
            StartScenarioRequest request) {

        try {
            ScenarioExecutionResult result = scenarioService.startScenario(
                    sessionId, request.getScenarioId());

            return ChatResponse.fromScenarioResult(result, sessionId);
        } catch (Exception e) {
            log.error("Error starting scenario for session {}", sessionId, e);
            return ChatResponse.builder()
                    .message("시나리오를 시작할 수 없습니다.")
                    .sessionId(sessionId)
                    .isFromBot(true)
                    .timestamp(LocalDateTime.now())
                    .build();
        }
    }
}