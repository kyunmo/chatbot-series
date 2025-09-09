package io.moyam.chatbot.interfaces.api.websocket;

import io.moyam.chatbot.domain.conversation.model.ConversationContext;
import io.moyam.chatbot.domain.conversation.service.IntentRecognizer;
import io.moyam.chatbot.domain.conversationblock.service.ConversationBlockService;
import io.moyam.chatbot.domain.intent.model.IntentAnalysisResult;
import io.moyam.chatbot.domain.scenario.model.ChoiceOption;
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
import java.util.List;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketController {

    private final ScenarioService scenarioService;
    private final IntentRecognizer intentRecognizer;  // 5편 추가: 의도분석 엔진
    private final ConversationBlockService conversationBlockService;  // 5편 추가

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
            
            // 일반 대화 처리 - 5편 개선: 의도분석 엔진 사용
            log.debug("Processing general conversation for session {}", sessionId);
            return handleGeneralConversation(request.getMessage(), sessionId);
            
        } catch (Exception e) {
            log.error("Error processing message for session {}: {}", sessionId, e.getMessage(), e);
            return ChatResponse.error(
                "죄송합니다. 일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요."
            );
        }
    }

    /**
     * 일반 대화 처리 - 5편 핵심 개선: 의도분석 엔진 사용
     * 기존의 단순한 키워드 매칭을 IntentRecognizer로 고도화
     */
    private ChatResponse handleGeneralConversation(String message, String sessionId) {
        try {
            // 의도분석 엔진으로 사용자 입력 분석
            IntentAnalysisResult analysis = intentRecognizer.analyze(message);
            
            log.debug("Intent analysis result for session {}: type={}, confidence={}", 
                     sessionId, analysis.getIntentType(), analysis.getConfidence());

            // 시나리오 시작 요청 감지 시 자동 시작
            if (intentRecognizer.isStartScenarioIntent(analysis)) {
                log.info("Auto-starting scenario for session {} based on intent analysis", sessionId);
                return startDefaultScenario(sessionId);
            }

            // FALLBACK 처리 - BasicBlockService 사용
            if ("FALLBACK".equals(analysis.getIntentType())) {
                log.debug("Processing FALLBACK response for session {}", sessionId);
                return handleFallbackResponse(sessionId, analysis);
            }

            // 일반 응답 생성 (BASIC_CONVERSATION, KNOWLEDGE 등)
            String responseMessage = analysis.getResponseMessage();
            if (responseMessage == null || responseMessage.trim().isEmpty()) {
                log.warn("No response message found for analysis: {}", analysis);
                return handleFallbackResponse(sessionId, analysis);
            }

            return ChatResponse.builder()
                .message(responseMessage)
                .sessionId(sessionId)
                .isFromBot(true)
                .messageType("text")
                .confidence(analysis.getConfidence())  // 5편 추가: 확신도 제공
                .choices(generateSuggestedActions(analysis))
                .timestamp(LocalDateTime.now())
                .build();

        } catch (Exception e) {
            log.error("Error in intent analysis for session {}: {}", sessionId, e.getMessage(), e);
            return ChatResponse.error("죄송합니다. 처리 중 오류가 발생했습니다.");
        }
    }

    /**
     * FALLBACK 응답 처리 - BasicBlockService 사용
     */
    private ChatResponse handleFallbackResponse(String sessionId, IntentAnalysisResult analysis) {
        try {
            // TODO: BasicBlockService 연동으로 DB에서 FALLBACK 메시지 가져오기
            // BasicBlockResponse fallbackBlock = basicBlockService.getRandomBlockByType("FALLBACK");
            // String fallbackMessage = fallbackBlock.getContent();
            
            // 임시: 하드코딩된 FALLBACK 메시지들 중 랜덤 선택
            String[] fallbackMessages = {
                "잘 모르겠어요! 😅\n\n다른 방식으로 말씀해주시거나 아래 버튼을 클릭해보세요!",
                "죄송해요, 정확히 이해하지 못했어요. 🤔\n\n더 간단하게 말씀해주시면 도와드릴게요!",
                "음... 그 말씀은 잘 모르겠네요. 😊\n\n\"시작\"이라고 말씀하시면 기능을 체험해보실 수 있어요!"
            };

            // 랜덤 선택
            int randomIndex = (int) (Math.random() * fallbackMessages.length);
            String fallbackMessage = fallbackMessages[randomIndex];

            log.debug("Generated FALLBACK response for session {}: message length={}", 
                     sessionId, fallbackMessage.length());

            return ChatResponse.builder()
                .message(fallbackMessage)
                .sessionId(sessionId)
                .isFromBot(true)
                .messageType("text")
                .confidence(0.0)  // FALLBACK은 항상 확신도 0.0
                .choices(generateSuggestedActions(analysis))
                .timestamp(LocalDateTime.now())
                .build();

        } catch (Exception e) {
            log.error("Error generating FALLBACK response for session {}: {}", sessionId, e.getMessage(), e);
            // 최후의 수단: 기본 메시지
            return ChatResponse.builder()
                .message("죄송해요, 잘 모르겠습니다. 😅 \"시작\"이라고 말씀해주세요!")
                .sessionId(sessionId)
                .isFromBot(true)
                .messageType("text")
                .confidence(0.0)
                .timestamp(LocalDateTime.now())
                .build();
        }
    }

    /**
     * 의도분석 결과에 따른 제안 액션 생성
     */
    private List<ChoiceOption> generateSuggestedActions(IntentAnalysisResult analysis) {
        // 기본 제안 버튼 (모든 응답에 공통 적용)
        return List.of(
            ChoiceOption.builder()
                .value("start_demo")
                .label("🚀 시나리오 시작")
                .build(),
            ChoiceOption.builder()
                .value("help")
                .label("❓ 도움말")
                .build()
        );
    }

    /**
     * 기본 시나리오 자동 시작
     */
    private ChatResponse startDefaultScenario(String sessionId) {
        try {
            ScenarioExecutionResult result = scenarioService.startScenario(sessionId, 1L);
            return ChatResponse.fromScenarioResult(result, sessionId);
        } catch (Exception e) {
            log.error("Error starting default scenario for session {}: {}", sessionId, e.getMessage(), e);
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
