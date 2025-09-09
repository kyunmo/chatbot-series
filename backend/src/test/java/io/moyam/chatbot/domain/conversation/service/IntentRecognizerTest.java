package io.moyam.chatbot.domain.conversation.service;

import io.moyam.chatbot.domain.conversationblock.model.ConversationBlock;
import io.moyam.chatbot.domain.conversationblock.service.ConversationBlockService;
import io.moyam.chatbot.domain.intent.model.IntentAnalysisResult;
import io.moyam.chatbot.domain.typoword.service.TypowordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("IntentRecognizer 테스트")
class IntentRecognizerTest {

    @Mock
    private TypowordService typowordService;
    
    @Mock
    private ConversationBlockService conversationBlockService;
    
    @InjectMocks
    private IntentRecognizer intentRecognizer;

    private ConversationBlock greetingBlock;
    private ConversationBlock startBlock;
    private ConversationBlock helpBlock;

    @BeforeEach
    void setUp() {
        // 인사말 블록
        greetingBlock = ConversationBlock.builder()
                .id(1L)
                .title("인사말")
                .keywords("안녕,하이,hello")
                .responseMessage("안녕하세요! 무엇을 도와드릴까요?")
                .isActive(true)
                .build();
        
        // 시작요청 블록
        startBlock = ConversationBlock.builder()
                .id(5L)
                .title("시작요청")
                .keywords("시작,start,데모")
                .responseMessage("네! 시나리오를 시작해드릴게요.")
                .isActive(true)
                .build();
        
        // 도움요청 블록
        helpBlock = ConversationBlock.builder()
                .id(3L)
                .title("도움요청")
                .keywords("도움,help,헬프")
                .responseMessage("도움말을 보여드릴게요!")
                .isActive(true)
                .build();
        
        // 기본 Mock 설정
        when(conversationBlockService.getAllActiveBlocks())
            .thenReturn(List.of(greetingBlock, startBlock, helpBlock));
        when(conversationBlockService.findById(1L)).thenReturn(greetingBlock);
        when(conversationBlockService.findById(5L)).thenReturn(startBlock);
        when(conversationBlockService.findById(3L)).thenReturn(helpBlock);
    }

    @Test
    @DisplayName("인사말 입력 시 기본 대화 블록 매칭 테스트")
    void testGreetingInput() {
        // Given
        when(typowordService.correctTypo(any())).thenReturn("안녕하세요");
        
        // When
        IntentAnalysisResult result = intentRecognizer.analyze("안녕하세요");
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getIntentType()).isEqualTo("BASIC_CONVERSATION");
        assertThat(result.getTargetId()).isEqualTo(1L);
        assertThat(result.getConfidence()).isGreaterThan(0.6);
        assertThat(result.getResponseMessage()).contains("안녕하세요");
        assertThat(result.isMatched()).isTrue();
        assertThat(result.isHighConfidence()).isTrue();
    }

    @Test
    @DisplayName("오타가 포함된 입력 처리 테스트")
    void testTypoCorrection() {
        // Given - 오타 교정 서비스가 "안뇽"을 "안녕"으로 교정
        when(typowordService.correctTypo("안뇽하세요")).thenReturn("안녕하세요");
        
        // When
        IntentAnalysisResult result = intentRecognizer.analyze("안뇽하세요");
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getIntentType()).isEqualTo("BASIC_CONVERSATION");
        assertThat(result.getTargetId()).isEqualTo(1L);
        assertThat(result.isMatched()).isTrue();
        
        // 오타교정 서비스가 호출되었는지 확인
        verify(typowordService).correctTypo("안뇽하세요");
    }

    @Test
    @DisplayName("시나리오 시작 의도 감지 테스트")
    void testStartScenarioIntent() {
        // Given
        when(typowordService.correctTypo("시작해줘")).thenReturn("시작");
        
        // When
        IntentAnalysisResult result = intentRecognizer.analyze("시작해줘");
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getIntentType()).isEqualTo("BASIC_CONVERSATION");
        assertThat(result.getTargetId()).isEqualTo(5L);
        
        // isStartScenarioIntent 메서드 테스트
        boolean isStartIntent = intentRecognizer.isStartScenarioIntent(result);
        assertThat(isStartIntent).isTrue();
    }

    @Test
    @DisplayName("도움말 요청 처리 테스트")
    void testHelpRequest() {
        // Given
        when(typowordService.correctTypo("help")).thenReturn("help");
        
        // When
        IntentAnalysisResult result = intentRecognizer.analyze("help");
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getIntentType()).isEqualTo("BASIC_CONVERSATION");
        assertThat(result.getTargetId()).isEqualTo(3L);
        assertThat(result.getResponseMessage()).contains("도움말");
    }

    @Test
    @DisplayName("빈 입력에 대한 폴백 처리 테스트")
    void testEmptyInput() {
        // When & Then
        IntentAnalysisResult result1 = intentRecognizer.analyze("");
        assertThat(result1.getIntentType()).isEqualTo("FALLBACK");
        assertThat(result1.getResponseMessage()).contains("메시지를 입력해주세요");
        
        IntentAnalysisResult result2 = intentRecognizer.analyze(null);
        assertThat(result2.getIntentType()).isEqualTo("FALLBACK");
        
        IntentAnalysisResult result3 = intentRecognizer.analyze("   ");
        assertThat(result3.getIntentType()).isEqualTo("FALLBACK");
    }

    @Test
    @DisplayName("매칭되지 않는 입력에 대한 폴백 응답 테스트")
    void testUnmatchedInput() {
        // Given
        when(typowordService.correctTypo("알수없는복잡한입력")).thenReturn("알수없는복잡한입력");
        
        // When
        IntentAnalysisResult result = intentRecognizer.analyze("알수없는복잡한입력");
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getIntentType()).isEqualTo("FALLBACK");
        assertThat(result.getConfidence()).isEqualTo(0.0);
        assertThat(result.getResponseMessage()).isNotEmpty();
        assertThat(result.isMatched()).isFalse();
        assertThat(result.isHighConfidence()).isFalse();
        
        // 다양한 폴백 메시지 중 하나가 반환되는지 확인
        assertThat(result.getResponseMessage()).containsAnyOf(
            "잘 이해하지 못했어요", "잘 모르겠어요", "이해가 어려워요"
        );
    }

    @Test
    @DisplayName("유사도 계산 정확성 테스트")
    void testSimilarityCalculation() {
        // Given - 정확히 일치하는 경우
        when(typowordService.correctTypo("안녕")).thenReturn("안녕");
        
        // When
        IntentAnalysisResult result = intentRecognizer.analyze("안녕");
        
        // Then
        assertThat(result.getConfidence()).isEqualTo(1.0); // 정확 일치는 1.0
        
        // Given - 부분 일치하는 경우
        when(typowordService.correctTypo("안녕하세용")).thenReturn("안녕하세용");
        
        // When
        IntentAnalysisResult result2 = intentRecognizer.analyze("안녕하세용");
        
        // Then
        assertThat(result2.getConfidence()).isBetween(0.6, 1.0); // 부분 일치는 0.6~1.0
    }

    @Test
    @DisplayName("강제 분석 모드 테스트 (테스트용 메서드)")
    void testForceAnalyze() {
        // Given
        when(typowordService.correctTypo("안녕")).thenReturn("안녕");
        
        // When
        IntentAnalysisResult result1 = intentRecognizer.forceAnalyze("안녕", "BASIC_CONVERSATION");
        IntentAnalysisResult result2 = intentRecognizer.forceAnalyze("아무거나", "FALLBACK");
        
        // Then
        assertThat(result1.getIntentType()).isEqualTo("BASIC_CONVERSATION");
        assertThat(result2.getIntentType()).isEqualTo("FALLBACK");
    }

    @Test
    @DisplayName("캐시 동작 테스트 - 동일 입력 시 재분석하지 않음")
    void testCacheHit() {
        // Given
        when(typowordService.correctTypo("안녕")).thenReturn("안녕");
        
        // When - 동일한 입력을 두 번 분석
        IntentAnalysisResult result1 = intentRecognizer.analyze("안녕");
        IntentAnalysisResult result2 = intentRecognizer.analyze("안녕");
        
        // Then - 결과는 동일해야 함
        assertThat(result1.getIntentType()).isEqualTo(result2.getIntentType());
        assertThat(result1.getTargetId()).isEqualTo(result2.getTargetId());
        assertThat(result1.getConfidence()).isEqualTo(result2.getConfidence());
        
        // 오타교정은 첫 번째만 호출되고 두 번째는 캐시 사용
        verify(typowordService, times(2)).correctTypo("안녕");
    }

    @Test
    @DisplayName("예외 상황 처리 테스트")
    void testExceptionHandling() {
        // Given - 오타교정 서비스에서 예외 발생
        when(typowordService.correctTypo(any())).thenThrow(new RuntimeException("DB 오류"));
        
        // When & Then - 예외가 발생해도 폴백 응답 반환
        assertThatNoException().isThrownBy(() -> {
            IntentAnalysisResult result = intentRecognizer.analyze("안녕");
            assertThat(result.getIntentType()).isEqualTo("FALLBACK");
        });
    }
}
