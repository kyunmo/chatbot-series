package io.moyam.chatbot.domain.conversationblock.service;

import io.moyam.chatbot.domain.conversationblock.model.ConversationBlock;
import io.moyam.chatbot.domain.conversationblock.repository.ConversationBlockMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 기본 대화 블록 서비스
 * 
 * 인사말, 감사인사, 도움요청 등 일반적인 대화 패턴을 관리하는 서비스입니다.
 * IntentRecognizer에서 기본 대화 매칭 시 사용됩니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ConversationBlockService {

    private final ConversationBlockMapper conversationBlockMapper;

    /**
     * 활성화된 모든 대화 블록 조회 - IntentRecognizer에서 사용하는 핵심 메서드
     * 캐시 적용으로 성능 최적화
     */
    @Cacheable(value = "conversationBlocks", key = "'active'")
    public List<ConversationBlock> getAllActiveBlocks() {
        List<ConversationBlock> blocks = conversationBlockMapper.findAllActive();
        log.debug("Loaded {} active conversation blocks", blocks.size());
        return blocks;
    }
    
    /**
     * ID로 특정 대화 블록 조회
     */
    @Cacheable(value = "conversationBlocks", key = "#id")
    public ConversationBlock findById(Long id) {
        return conversationBlockMapper.findById(id);
    }

    /**
     * 전체 대화 블록 조회 (관리자용)
     */
    public List<ConversationBlock> getAllBlocks() {
        return conversationBlockMapper.findAll();
    }

    /**
     * 제목으로 대화 블록 검색
     */
    public List<ConversationBlock> searchByTitle(String title) {
        return conversationBlockMapper.findByTitle(title);
    }

    /**
     * 새로운 대화 블록 생성
     */
    @CacheEvict(value = "conversationBlocks", allEntries = true)
    public ConversationBlock createBlock(String title, String keywords, String responseMessage) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("대화 블록 제목은 필수입니다");
        }
        if (keywords == null || keywords.trim().isEmpty()) {
            throw new IllegalArgumentException("키워드는 필수입니다");
        }
        if (responseMessage == null || responseMessage.trim().isEmpty()) {
            throw new IllegalArgumentException("응답 메시지는 필수입니다");
        }

        ConversationBlock block = ConversationBlock.builder()
                .title(title.trim())
                .keywords(keywords.trim())
                .responseMessage(responseMessage.trim())
                .isActive(true)
                .build();

        conversationBlockMapper.insert(block);
        
        log.info("Created new conversation block: {} with keywords: {}", title, keywords);
        return block;
    }

    /**
     * 대화 블록 수정
     */
    @CacheEvict(value = "conversationBlocks", allEntries = true)
    public void updateBlock(Long id, String title, String keywords, String responseMessage, Boolean isActive) {
        ConversationBlock existing = conversationBlockMapper.findById(id);
        if (existing == null) {
            throw new IllegalArgumentException("존재하지 않는 대화 블록입니다");
        }

        existing.setTitle(title != null ? title.trim() : existing.getTitle());
        existing.setKeywords(keywords != null ? keywords.trim() : existing.getKeywords());
        existing.setResponseMessage(responseMessage != null ? responseMessage.trim() : existing.getResponseMessage());
        existing.setIsActive(isActive != null ? isActive : existing.getIsActive());

        conversationBlockMapper.update(existing);
        
        log.info("Updated conversation block: {}", title);
    }

    /**
     * 대화 블록 활성화/비활성화
     */
    @CacheEvict(value = "conversationBlocks", allEntries = true)
    public void toggleBlockStatus(Long id, Boolean isActive) {
        ConversationBlock existing = conversationBlockMapper.findById(id);
        if (existing == null) {
            throw new IllegalArgumentException("존재하지 않는 대화 블록입니다");
        }

        conversationBlockMapper.updateActiveStatus(id, isActive);
        
        log.info("Changed block status: {} -> {}", existing.getTitle(), isActive ? "활성화" : "비활성화");
    }

    /**
     * 대화 블록 삭제
     */
    @CacheEvict(value = "conversationBlocks", allEntries = true)
    public void deleteBlock(Long id) {
        ConversationBlock existing = conversationBlockMapper.findById(id);
        if (existing != null) {
            conversationBlockMapper.delete(id);
            log.info("Deleted conversation block: {}", existing.getTitle());
        }
    }

    /**
     * 특정 키워드가 포함된 블록 찾기 (디버깅용)
     */
    public List<ConversationBlock> findBlocksWithKeyword(String keyword) {
        return getAllActiveBlocks().stream()
                .filter(block -> block.containsKeyword(keyword))
                .toList();
    }

    /**
     * 대화 블록에 새 키워드 추가
     */
    @CacheEvict(value = "conversationBlocks", allEntries = true)
    public void addKeywordToBlock(Long id, String newKeyword) {
        ConversationBlock block = conversationBlockMapper.findById(id);
        if (block == null) {
            throw new IllegalArgumentException("존재하지 않는 대화 블록입니다");
        }

        if (block.containsKeyword(newKeyword)) {
            log.warn("Keyword '{}' already exists in block '{}'", newKeyword, block.getTitle());
            return;
        }

        block.addKeyword(newKeyword);
        conversationBlockMapper.update(block);
        
        log.info("Added keyword '{}' to block '{}'", newKeyword, block.getTitle());
    }

    /**
     * 대화 블록 통계 정보
     */
    public BlockStatistics getStatistics() {
        List<ConversationBlock> allBlocks = conversationBlockMapper.findAll();
        long activeCount = allBlocks.stream().filter(ConversationBlock::getIsActive).count();
        long inactiveCount = allBlocks.size() - activeCount;
        
        return new BlockStatistics(allBlocks.size(), activeCount, inactiveCount);
    }

    /**
     * 대화 블록 통계 정보를 담는 내부 클래스
     */
    public record BlockStatistics(long total, long active, long inactive) {}
}
