package io.moyam.chatbot.interfaces.api.websocket;

import io.moyam.chatbot.domain.conversation.model.ConversationContext;
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
                        .messageType("error")
                        .timestamp(LocalDateTime.now())
                        .build();
            }

            // 현재 컨텍스트 확인
            ConversationContext existingContext = scenarioService.getContext(sessionId);
            
            // 시나리오 진행 중인 경우
            if (existingContext != null && existingContext.getCurrentStepId() != null) {
                log.info("Processing with existing context step {} for session {}", 
                         existingContext.getCurrentStepId(), sessionId);
                
                ScenarioExecutionResult result = scenarioService.executeStep(
                        sessionId, existingContext.getCurrentStepId(), request.getMessage());
                
                return ChatResponse.fromScenarioResult(result, sessionId);
            }
            
            // 새로운 시나리오 시작 요청
            if (request.getStepId() != null && request.getStepId() > 0) {
                log.debug("Starting new scenario with step {} for session {}", request.getStepId(), sessionId);
                
                ScenarioExecutionResult result = scenarioService.executeStep(
                        sessionId, request.getStepId(), request.getMessage());
                return ChatResponse.fromScenarioResult(result, sessionId);
            }
            
            // 일반 대화 처리
            log.debug("Processing general conversation for session {}", sessionId);
            return generateSmartChatResponse(request.getMessage(), sessionId);
            
        } catch (Exception e) {
            log.error("Error processing message for session {}: {}", sessionId, e.getMessage(), e);
            return ChatResponse.error(
                "죄송합니다. 일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요."
            );
        }
    }

    /**
     * 응답 생성
     */
    private ChatResponse generateSmartChatResponse(String message, String sessionId) {
        String lowerMessage = message.toLowerCase().trim();
        
        // 시나리오 시작 요청
        if (lowerMessage.contains("시작") || lowerMessage.contains("start") || 
            lowerMessage.contains("데모") || lowerMessage.equals("1")) {
            return startDefaultScenario(sessionId);
        }

        // 인사말 처리 (개인화된 응답)
        if (lowerMessage.contains("안녕") || lowerMessage.contains("hi") || 
            lowerMessage.contains("hello") || lowerMessage.contains("하이")) {
            
            return ChatResponse.builder()
                .message("안녕하세요! 저는 AI 개인비서입니다.\n\n무엇을 도와드릴까요?\n\n아래 버튼을 클릭하거나 '시작'이라고 말씀해주세요!")
                .sessionId(sessionId)
                .isFromBot(true)
                .messageType("info")
                .choices(java.util.Arrays.asList(
                    io.moyam.chatbot.domain.scenario.model.ChoiceOption.builder()
                        .value("start_demo")
                        .label("시나리오 시작하기")
                        .build(),
                    io.moyam.chatbot.domain.scenario.model.ChoiceOption.builder()
                        .value("help")
                        .label("도움말 보기")
                        .build()
                ))
                .timestamp(LocalDateTime.now())
                .build();
        }

        // 도움 요청
        if (lowerMessage.contains("도움") || lowerMessage.contains("help") || 
            lowerMessage.contains("헬프") || lowerMessage.equals("?")) {
            
            return ChatResponse.info(
                "도움말\n\n" +
                "이 ChatBot은 다음과 같은 서비스를 제공합니다:\n" +
                "- 일정 관리: 오늘/내일 일정 확인 및 추가\n" +
                "- 메모 작성: 간단한 메모 저장\n" +
                "- 계산기: 사칙연산 계산\n" +
                "- 설정: 봇 개인화 설정\n\n" +
                "사용법: '시작'이라고 입력하거나 아래 버튼을 클릭하세요!",
                sessionId
            );
        }

        // 감사 인사
        if (lowerMessage.contains("고마") || lowerMessage.contains("감사") || 
            lowerMessage.contains("thank") || lowerMessage.contains("굿")) {
            
            return ChatResponse.info(
                "천만에요! 언제든 도움이 필요하시면 말씀해주세요.\n\n" +
                "다른 기능을 사용해보시겠어요?",
                sessionId
            );
        }

        // 종료 관련
        if (lowerMessage.contains("종료") || lowerMessage.contains("끝") || 
            lowerMessage.contains("bye") || lowerMessage.contains("바이")) {
            
            return ChatResponse.info(
                "안녕히 가세요! 언제든 다시 찾아주세요.\n\n" +
                "새로운 대화를 시작하시려면 '시작'이라고 말씀해주세요.",
                sessionId
            );
        }

        // 기본 응답
        return ChatResponse.builder()
            .message("잘 모르겠습니다.\n\n" +
                    "이런 것들을 시도해보세요:\n" +
                    "• '시작' - 시나리오 데모 시작\n" +
                    "• '도움말' - 사용 가능한 기능 보기\n" +
                    "• '안녕하세요' - 인사하기")
            .sessionId(sessionId)
            .isFromBot(true)
            .messageType("text")
            .choices(java.util.Arrays.asList(
                io.moyam.chatbot.domain.scenario.model.ChoiceOption.builder()
                    .value("start_demo")
                    .label("시나리오 시작")
                    .build(),
                io.moyam.chatbot.domain.scenario.model.ChoiceOption.builder()
                    .value("help")
                    .label("도움말")
                    .build()
            ))
            .timestamp(LocalDateTime.now())
            .build();
    }

    /**
     * 기본 시나리오 자동 시작
     */
    private ChatResponse startDefaultScenario(String sessionId) {
        try {
            ScenarioExecutionResult result = scenarioService.startScenario(sessionId, 1L);
            return ChatResponse.fromScenarioResult(result, sessionId);
        } catch (Exception e) {
            log.error("Error starting default scenario for session {}", sessionId, e);
            return ChatResponse.error("시나리오를 시작할 수 없습니다. 잠시 후 다시 시도해주세요.");
        }
    }

    @MessageMapping("/chat/{sessionId}/start")
    @SendTo("/topic/chat/{sessionId}")
    public ChatResponse startScenario(
            @DestinationVariable String sessionId,
            StartScenarioRequest request) {

        try {
            log.info("Starting scenario {} for session {}", request.getScenarioId(), sessionId);
            
            ScenarioExecutionResult result = scenarioService.startScenario(
                    sessionId, request.getScenarioId());

            return ChatResponse.fromScenarioResult(result, sessionId);
            
        } catch (Exception e) {
            log.error("Error starting scenario {} for session {}: {}", 
                     request.getScenarioId(), sessionId, e.getMessage(), e);
            return ChatResponse.error("시나리오를 시작할 수 없습니다. 잠시 후 다시 시도해주세요.");
        }
    }
}
