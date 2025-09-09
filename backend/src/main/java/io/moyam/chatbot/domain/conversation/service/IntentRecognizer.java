package io.moyam.chatbot.domain.conversation.service;

import io.moyam.chatbot.domain.conversationblock.model.ConversationBlock;
import io.moyam.chatbot.domain.conversationblock.service.ConversationBlockService;
import io.moyam.chatbot.domain.intent.model.IntentAnalysisResult;
import io.moyam.chatbot.domain.typoword.service.TypowordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;

/**
 * 의도분석 엔진 - ChatBot의 핵심 서비스
 * 
 * 사용자 입력의 의도를 분석하고 적절한 응답을 찾는 5단계 처리:
 * 1. 입력 검증 → 2. 텍스트 전처리 → 3. 오타 교정 → 4. 기본대화 매칭 → 5. 폴백 응답
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IntentRecognizer {

    private final TypowordService typowordService;
    private final ConversationBlockService conversationBlockService;
    
    // 유사도 임계값 - 실제 테스트를 통해 0.6이 최적임을 확인
    private static final double SIMILARITY_THRESHOLD = 0.6;

    /**
     * 사용자 입력에 대한 의도 분석 수행 - ChatWebSocketController에서 호출하는 핵심 메서드
     */
    @Cacheable(value = "intentAnalysis", key = "#input")
    public IntentAnalysisResult analyze(String input) {
        // 1단계: 입력 검증
        if (input == null || input.trim().isEmpty()) {
            return IntentAnalysisResult.fallback("메시지를 입력해주세요.");
        }

        log.debug("Analyzing input: {}", input);

        // 2단계: 텍스트 전처리
        String normalizedInput = preprocessInput(input);
        log.debug("Normalized input: {}", normalizedInput);

        // 3단계: 오타 교정
        String correctedInput = typowordService.correctTypo(normalizedInput);
        log.debug("Corrected input: {}", correctedInput);

        // 4단계: 기본 대화 블록 매칭
        IntentAnalysisResult blockResult = findBasicConversationBlock(correctedInput);
        if (blockResult != null) {
            log.debug("Matched basic conversation block: {}", blockResult);
            return blockResult;
        }

        // 5단계: 지식베이스 매칭 (6편에서 구현 예정)
        // TODO: KnowledgeService 연동 예정
        // IntentAnalysisResult knowledgeResult = findKnowledgeMatch(correctedInput);
        
        // 6단계: 폴백 응답 생성
        return generateFallbackResponse(correctedInput);
    }

    /**
     * 텍스트 전처리 - 특수문자 제거, 정규화
     */
    private String preprocessInput(String input) {
        return input
            .replaceAll("[?!.,]", " ")      // 문장부호 제거
            .replaceAll("\\s+", " ")        // 연속된 공백 정리
            .toLowerCase()
            .trim();
    }

    /**
     * 기본 대화 블록에서 매칭되는 응답 찾기
     * 활성화된 모든 블록을 대상으로 키워드 유사도 계산
     */
    private IntentAnalysisResult findBasicConversationBlock(String input) {
        try {
            List<ConversationBlock> blocks = conversationBlockService.getAllActiveBlocks();
            
            double highestSimilarity = 0.0;
            ConversationBlock bestMatch = null;

            // 모든 활성화된 블록과 키워드 매칭 시도
            for (ConversationBlock block : blocks) {
                for (String keyword : block.getKeywordList()) {
                    double similarity = calculateSimpleSimilarity(input, keyword.trim());
                    
                    if (similarity > highestSimilarity && similarity > SIMILARITY_THRESHOLD) {
                        highestSimilarity = similarity;
                        bestMatch = block;
                    }
                }
            }

            if (bestMatch != null) {
                return IntentAnalysisResult.basicConversation(
                    bestMatch.getId(), 
                    highestSimilarity, 
                    bestMatch.getResponseMessage()
                );
            }

        } catch (Exception e) {
            log.error("Error finding basic conversation block", e);
        }

        return null;
    }

    /**
     * 간단한 유사도 계산 - 5편용 (6편에서 TF-IDF로 고도화 예정)
     * 3단계 매칭: 정확일치 → 포함관계 → Levenshtein 거리
     */
    private double calculateSimpleSimilarity(String input, String keyword) {
        // 1. 정확히 일치하는 경우
        if (input.equals(keyword)) {
            return 1.0;
        }
        
        // 2. 포함 관계 확인 (양방향)
        if (input.contains(keyword) || keyword.contains(input)) {
            return 0.8;
        }
        
        // 3. Levenshtein 거리 기반 유사도
        return calculateLevenshteinSimilarity(input, keyword);
    }

    /**
     * Levenshtein 거리 기반 유사도 계산
     * 문자열 편집 거리를 이용해 오타 허용도 계산
     */
    private double calculateLevenshteinSimilarity(String s1, String s2) {
        int maxLength = Math.max(s1.length(), s2.length());
        if (maxLength == 0) return 1.0;
        
        int distance = calculateLevenshteinDistance(s1, s2);
        return 1.0 - ((double) distance / maxLength);
    }

    /**
     * Levenshtein 거리 계산 - 동적 프로그래밍
     */
    private int calculateLevenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];

        for (int i = 0; i <= s1.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= s2.length(); j++) dp[0][j] = j;

        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                if (s1.charAt(i-1) == s2.charAt(j-1)) {
                    dp[i][j] = dp[i-1][j-1];
                } else {
                    dp[i][j] = 1 + Math.min(Math.min(dp[i-1][j], dp[i][j-1]), dp[i-1][j-1]);
                }
            }
        }

        return dp[s1.length()][s2.length()];
    }

    /**
     * 폴백 응답 생성 - 매칭 실패 시 다양한 응답으로 자연스러움 연출
     */
    private IntentAnalysisResult generateFallbackResponse(String input) {
        String[] fallbackMessages = {
            "죄송합니다. 잘 이해하지 못했어요.\n\n이런 것들을 말씀해보세요:\n• '시작' - 시나리오 체험\n• '도움말' - 기능 안내\n• '안녕하세요' - 인사하기",
            "잘 모르겠어요! 😅\n\n다른 방식으로 말씀해주시거나,\n'도움말'이라고 말씀하시면 사용법을 안내해드릴게요!",
            "흠... 이해가 어려워요.\n\n'시작'이라고 말씀하시면 제가 할 수 있는 일들을 보여드릴게요!"
        };
        
        Random random = new Random();
        String message = fallbackMessages[random.nextInt(fallbackMessages.length)];
        
        return IntentAnalysisResult.fallback(message);
    }

    /**
     * 특정 의도 타입으로 강제 분석 (테스트용)
     */
    public IntentAnalysisResult forceAnalyze(String input, String intentType) {
        log.debug("Force analyzing input: {} as {}", input, intentType);
        
        switch (intentType) {
            case "BASIC_CONVERSATION":
                return findBasicConversationBlock(preprocessInput(input));
            case "FALLBACK":
                return generateFallbackResponse(input);
            default:
                return analyze(input);
        }
    }

    /**
     * 캐시 상태 확인 (모니터링용)
     */
    public void logCacheInfo() {
        log.info("Intent analysis cache statistics - Check cache manager for details");
    }

    /**
     * 시나리오 시작 의도 감지
     * ChatWebSocketController에서 시나리오 자동 시작 판단에 사용
     */
    public boolean isStartScenarioIntent(IntentAnalysisResult result) {
        if (!"BASIC_CONVERSATION".equals(result.getIntentType())) {
            return false;
        }

        ConversationBlock block = conversationBlockService.findById(result.getTargetId());
        return block != null && "시작요청".equals(block.getTitle());
    }
}
