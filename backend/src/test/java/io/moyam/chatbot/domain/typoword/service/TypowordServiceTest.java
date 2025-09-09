package io.moyam.chatbot.domain.typoword.service;

import io.moyam.chatbot.domain.typoword.model.Typoword;
import io.moyam.chatbot.domain.typoword.repository.TypowordMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TypowordService 테스트")
class TypowordServiceTest {

    @Mock
    private TypowordMapper typowordMapper;
    
    @InjectMocks
    private TypowordService typowordService;

    private Typoword sampleTypoword;

    @BeforeEach
    void setUp() {
        sampleTypoword = Typoword.builder()
                .id(1L)
                .incorrectWord("안뇽")
                .correctWord("안녕")
                .description("인사말 오타")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("단일 단어 오타 교정 테스트")
    void testCorrectSingleWord() {
        // Given
        when(typowordMapper.findCorrectWord("안뇽")).thenReturn("안녕");
        
        // When
        String result = typowordService.correctTypo("안뇽");
        
        // Then
        assertThat(result).isEqualTo("안녕");
        verify(typowordMapper).findCorrectWord("안뇽");
    }

    @Test
    @DisplayName("문장 단위 오타 교정 테스트")
    void testCorrectSentence() {
        // Given
        when(typowordMapper.findCorrectWord("안뇽")).thenReturn("안녕");
        when(typowordMapper.findCorrectWord("하세요")).thenReturn(null);
        
        // When
        String result = typowordService.correctTypo("안뇽 하세요");
        
        // Then
        assertThat(result).isEqualTo("안녕 하세요");
        verify(typowordMapper).findCorrectWord("안뇽");
        verify(typowordMapper).findCorrectWord("하세요");
    }

    @Test
    @DisplayName("오타가 없는 경우 원본 반환 테스트")
    void testNoTypoFound() {
        // Given
        when(typowordMapper.findCorrectWord("안녕")).thenReturn(null);
        
        // When
        String result = typowordService.correctTypo("안녕");
        
        // Then
        assertThat(result).isEqualTo("안녕");
    }

    @Test
    @DisplayName("빈 입력 처리 테스트")
    void testEmptyInput() {
        // When & Then
        assertThat(typowordService.correctTypo("")).isEqualTo("");
        assertThat(typowordService.correctTypo(null)).isNull();
        assertThat(typowordService.correctTypo("   ")).isEqualTo("   ");
        
        // DB 조회가 발생하지 않아야 함
        verify(typowordMapper, never()).findCorrectWord(any());
    }

    @Test
    @DisplayName("특수문자 포함 단어 교정 테스트")
    void testSpecialCharacters() {
        // Given
        when(typowordMapper.findCorrectWord("안뇽!")).thenReturn(null);
        when(typowordMapper.findCorrectWord("안뇽")).thenReturn("안녕");
        
        // When
        String result = typowordService.correctTypo("안뇽!");
        
        // Then
        assertThat(result).isEqualTo("안녕");
        verify(typowordMapper).findCorrectWord("안뇽!"); // 첫 번째 시도
        verify(typowordMapper).findCorrectWord("안뇽");  // 특수문자 제거 후 재시도
    }

    @Test
    @DisplayName("캐시 동작 테스트")
    void testCache() {
        // Given
        when(typowordMapper.findCorrectWord("안뇽")).thenReturn("안녕");
        
        // When - 동일한 단어를 두 번 교정
        String result1 = typowordService.correctTypo("안뇽");
        String result2 = typowordService.correctTypo("안뇽");
        
        // Then
        assertThat(result1).isEqualTo("안녕");
        assertThat(result2).isEqualTo("안녕");
        
        // DB는 한 번만 조회되어야 함 (캐시 적용)
        verify(typowordMapper, times(1)).findCorrectWord("안뇽");
    }

    @Test
    @DisplayName("오타 규칙 추가 테스트")
    void testAddTypoRule() {
        // When
        typowordService.addTypoRule("헬로", "hello", "영어 인사말");
        
        // Then
        verify(typowordMapper).insert(any(Typoword.class));
    }

    @Test
    @DisplayName("유효하지 않은 오타 규칙 추가 시 예외 테스트")
    void testAddInvalidTypoRule() {
        // When & Then
        assertThatThrownBy(() -> typowordService.addTypoRule(null, "hello", "설명"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("오타와 교정 단어는 필수입니다");
        
        assertThatThrownBy(() -> typowordService.addTypoRule("hello", null, "설명"))
                .isInstanceOf(IllegalArgumentException.class);
        
        assertThatThrownBy(() -> typowordService.addTypoRule("hello", "hello", "설명"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("유효하지 않은 오타 규칙입니다");
    }

    @Test
    @DisplayName("오타 규칙 수정 테스트")
    void testUpdateTypoRule() {
        // Given
        when(typowordMapper.findById(1L)).thenReturn(sampleTypoword);
        
        // When
        typowordService.updateTypoRule(1L, "안뇽뇽", "안녕하세요", "인사말 오타 수정");
        
        // Then
        verify(typowordMapper).findById(1L);
        verify(typowordMapper).update(any(Typoword.class));
    }

    @Test
    @DisplayName("존재하지 않는 오타 규칙 수정 시 예외 테스트")
    void testUpdateNonExistentTypoRule() {
        // Given
        when(typowordMapper.findById(999L)).thenReturn(null);
        
        // When & Then
        assertThatThrownBy(() -> typowordService.updateTypoRule(999L, "안뇽", "안녕", "설명"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("존재하지 않는 오타 규칙입니다");
    }

    @Test
    @DisplayName("오타 규칙 삭제 테스트")
    void testDeleteTypoRule() {
        // Given
        when(typowordMapper.findById(1L)).thenReturn(sampleTypoword);
        
        // When
        typowordService.deleteTypoRule(1L);
        
        // Then
        verify(typowordMapper).findById(1L);
        verify(typowordMapper).delete(1L);
    }

    @Test
    @DisplayName("전체 오타 규칙 조회 테스트")
    void testGetAllTypoRules() {
        // Given
        List<Typoword> mockRules = List.of(sampleTypoword);
        when(typowordMapper.findAll()).thenReturn(mockRules);
        
        // When
        List<Typoword> result = typowordService.getAllTypoRules();
        
        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getIncorrectWord()).isEqualTo("안뇽");
    }

    @Test
    @DisplayName("특정 올바른 단어의 오타 패턴 조회 테스트")
    void testGetTypoPatterns() {
        // Given
        List<Typoword> mockPatterns = List.of(
            Typoword.builder().incorrectWord("안뇽").correctWord("안녕").build(),
            Typoword.builder().incorrectWord("안넝").correctWord("안녕").build()
        );
        when(typowordMapper.findByCorrectWord("안녕")).thenReturn(mockPatterns);
        
        // When
        List<Typoword> result = typowordService.getTypoPatterns("안녕");
        
        // Then
        assertThat(result).hasSize(2);
        assertThat(result).extracting("correctWord")
                .containsOnly("안녕", "안녕");
    }

    @Test
    @DisplayName("캐시 초기화 테스트")
    void testClearCache() {
        // Given - 캐시에 데이터 추가
        when(typowordMapper.findCorrectWord("안뇽")).thenReturn("안녕");
        typowordService.correctTypo("안뇽");
        
        // When - 캐시 초기화
        typowordService.clearCache();
        
        // 캐시 초기화 후 다시 호출하면 DB 조회가 다시 발생해야 함
        typowordService.correctTypo("안뇽");
        
        // Then
        verify(typowordMapper, times(2)).findCorrectWord("안뇽");
    }

    @Test
    @DisplayName("캐시 크기 확인 테스트")
    void testGetCacheSize() {
        // Given
        when(typowordMapper.findCorrectWord("안뇽")).thenReturn("안녕");
        when(typowordMapper.findCorrectWord("시작해줘")).thenReturn("시작");
        
        // When - 캐시에 항목 추가
        typowordService.correctTypo("안뇽");
        typowordService.correctTypo("시작해줘");
        
        // Then
        assertThat(typowordService.getCacheSize()).isEqualTo(2);
    }

    @Test
    @DisplayName("예외 상황에서 원본 반환 테스트")
    void testExceptionHandling() {
        // Given - DB 조회 시 예외 발생
        when(typowordMapper.findCorrectWord("안뇽")).thenThrow(new RuntimeException("DB 오류"));
        
        // When & Then - 예외가 발생해도 원본 반환
        assertThatNoException().isThrownBy(() -> {
            String result = typowordService.correctTypo("안뇽");
            assertThat(result).isEqualTo("안뇽");
        });
    }
}
