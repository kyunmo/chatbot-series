package io.moyam.chatbot.domain.conversationblock.repository;

import io.moyam.chatbot.domain.conversationblock.model.ConversationBlock;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 기본 대화 블록 MyBatis 매퍼
 */
@Mapper
public interface ConversationBlockMapper {
    
    /**
     * 활성화된 모든 대화 블록 조회
     * IntentRecognizer에서 매칭 시 사용되는 핵심 메서드
     */
    List<ConversationBlock> findAllActive();
    
    /**
     * ID로 특정 대화 블록 조회
     */
    ConversationBlock findById(@Param("id") Long id);
    
    /**
     * 전체 대화 블록 조회 (관리자용)
     */
    List<ConversationBlock> findAll();
    
    /**
     * 제목으로 대화 블록 검색
     */
    List<ConversationBlock> findByTitle(@Param("title") String title);
    
    /**
     * 새로운 대화 블록 추가
     */
    void insert(ConversationBlock block);
    
    /**
     * 대화 블록 정보 수정
     */
    void update(ConversationBlock block);
    
    /**
     * 대화 블록 활성화/비활성화
     */
    void updateActiveStatus(@Param("id") Long id, @Param("isActive") Boolean isActive);
    
    /**
     * 대화 블록 완전 삭제
     */
    void delete(@Param("id") Long id);
}
