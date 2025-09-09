package io.moyam.chatbot.domain.typoword.repository;

import io.moyam.chatbot.domain.typoword.model.Typoword;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 오타 교정 데이터 MyBatis 매퍼
 */
@Mapper
public interface TypowordMapper {
    
    /**
     * 오타 교정: 잘못된 단어에 대응하는 올바른 단어 조회
     * IntentRecognizer에서 가장 많이 사용되는 핵심 메서드
     */
    String findCorrectWord(@Param("incorrectWord") String incorrectWord);
    
    /**
     * 전체 오타 교정 데이터 조회
     */
    List<Typoword> findAll();
    
    /**
     * ID로 특정 오타 규칙 조회
     */
    Typoword findById(@Param("id") Long id);
    
    /**
     * 새로운 오타 교정 규칙 추가
     */
    void insert(Typoword typoword);
    
    /**
     * 오타 교정 규칙 수정
     */
    void update(Typoword typoword);
    
    /**
     * 오타 교정 규칙 삭제
     */
    void delete(@Param("id") Long id);
    
    /**
     * 특정 올바른 단어로 매핑되는 모든 오타들 조회
     * 관리자가 특정 단어의 오타 패턴을 확인할 때 사용
     */
    List<Typoword> findByCorrectWord(@Param("correctWord") String correctWord);
}
