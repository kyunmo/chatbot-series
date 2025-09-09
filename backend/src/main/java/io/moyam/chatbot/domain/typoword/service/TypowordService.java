package io.moyam.chatbot.domain.typoword.service;

import io.moyam.chatbot.domain.typoword.model.Typoword;
import io.moyam.chatbot.domain.typoword.repository.TypowordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 오타 교정 서비스
 * 
 * 사용자 입력의 오타나 줄임말을 올바른 단어로 교정하는 핵심 서비스입니다.
 * IntentRecognizer에서 텍스트 전처리 단계에서 사용됩니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TypowordService {

    private final TypowordMapper typowordMapper;
    
    // 메모리 캐시 (간단한 구현)
    private final ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();

    /**
     * 문장 단위 오타 교정 - IntentRecognizer에서 사용하는 핵심 메서드
     * 각 단어별로 교정 후 재조합
     */
    @Cacheable(value = "typoCorrection", key = "#input")
    public String correctTypo(String input) {
        log.debug("Starting typo correction for: {}", input);

        if (input == null || input.trim().isEmpty()) {
            return input;
        }

        // 단어 단위로 분리하여 처리
        String[] words = input.trim().split("\\s+");
        List<String> correctedWords = new ArrayList<>();

        for (String word : words) {
            String corrected = cache.computeIfAbsent(word.toLowerCase(), this::correctSingleWord);
            correctedWords.add(corrected);
        }

        String result = String.join(" ", correctedWords);
        log.debug("Typo correction result: {} -> {}", input, result);
        
        return result;
    }

    /**
     * 단일 단어 교정 - 캐시 적용으로 성능 최적화
     */
    private String correctSingleWord(String word) {
        try {
            // 1. DB에서 오타 교정 테이블 확인
            String corrected = typowordMapper.findCorrectWord(word.toLowerCase());
            if (corrected != null) {
                log.debug("Corrected by typo table: {} -> {}", word, corrected);
                return corrected;
            }

            // 2. 특수문자 정리 후 재시도
            String cleaned = word.replaceAll("[^a-zA-Z0-9가-힣]", "");
            if (!cleaned.equals(word) && !cleaned.isEmpty()) {
                corrected = typowordMapper.findCorrectWord(cleaned.toLowerCase());
                if (corrected != null) {
                    log.debug("Corrected after cleaning: {} -> {}", word, corrected);
                    return corrected;
                }
            }

            // 3. 교정 실패 시 원본 반환
            return word;
            
        } catch (Exception e) {
            log.error("Error during typo correction for word: {}", word, e);
            return word;
        }
    }

    /**
     * 새로운 오타 규칙 추가
     */
    public void addTypoRule(String incorrectWord, String correctWord, String description) {
        if (incorrectWord == null || correctWord == null) {
            throw new IllegalArgumentException("오타와 교정 단어는 필수입니다");
        }

        Typoword typoword = Typoword.builder()
                .incorrectWord(incorrectWord.toLowerCase().trim())
                .correctWord(correctWord.trim())
                .description(description)
                .build();

        if (!typoword.isValid()) {
            throw new IllegalArgumentException("유효하지 않은 오타 규칙입니다");
        }

        typowordMapper.insert(typoword);
        
        // 캐시 무효화
        cache.remove(incorrectWord.toLowerCase());
        
        log.info("Added new typo rule: {} -> {}", incorrectWord, correctWord);
    }

    /**
     * 오타 규칙 수정
     */
    public void updateTypoRule(Long id, String incorrectWord, String correctWord, String description) {
        Typoword existing = typowordMapper.findById(id);
        if (existing == null) {
            throw new IllegalArgumentException("존재하지 않는 오타 규칙입니다");
        }

        // 기존 캐시 무효화
        cache.remove(existing.getIncorrectWord());

        existing.setIncorrectWord(incorrectWord.toLowerCase().trim());
        existing.setCorrectWord(correctWord.trim());
        existing.setDescription(description);

        if (!existing.isValid()) {
            throw new IllegalArgumentException("유효하지 않은 오타 규칙입니다");
        }

        typowordMapper.update(existing);
        
        log.info("Updated typo rule: {} -> {}", incorrectWord, correctWord);
    }

    /**
     * 오타 규칙 삭제
     */
    public void deleteTypoRule(Long id) {
        Typoword existing = typowordMapper.findById(id);
        if (existing != null) {
            cache.remove(existing.getIncorrectWord());
            typowordMapper.delete(id);
            log.info("Deleted typo rule: {}", existing.getIncorrectWord());
        }
    }

    /**
     * 전체 오타 규칙 조회 (관리자용)
     */
    public List<Typoword> getAllTypoRules() {
        return typowordMapper.findAll();
    }

    /**
     * 특정 올바른 단어의 모든 오타 패턴 조회
     */
    public List<Typoword> getTypoPatterns(String correctWord) {
        return typowordMapper.findByCorrectWord(correctWord);
    }

    /**
     * 캐시 초기화 (시스템 운영 중 필요 시)
     */
    public void clearCache() {
        cache.clear();
        log.info("Typo correction cache cleared");
    }

    /**
     * 캐시 상태 확인
     */
    public int getCacheSize() {
        return cache.size();
    }
}
