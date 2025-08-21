package io.moyam.chatbot.interfaces.api.scenario.request;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * 시나리오 시작 요청
 * WebSocket과 REST API에서 공통으로 사용 가능
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StartScenarioRequest {

    /**
     * 시작할 시나리오 ID
     */
    @NotNull(message = "시나리오 ID는 필수입니다")
    @Positive(message = "시나리오 ID는 양수여야 합니다")
    private Long scenarioId;

    /**
     * 세션 ID (WebSocket에서는 경로에서, REST에서는 바디에서)
     */
    private String sessionId;

    /**
     * 시나리오 시작 시 추가 파라미터 (선택적)
     */
    private String initialMessage;
}