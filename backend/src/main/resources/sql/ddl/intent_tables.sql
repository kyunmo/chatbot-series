-- ===== 1. 기본 시스템 블록 테이블 =====
-- 시스템 메시지용 (FALLBACK, WELCOME, ERROR 등)
CREATE TABLE IF NOT EXISTS basic_blocks (
                                            id BIGSERIAL PRIMARY KEY,
                                            type VARCHAR(50) NOT NULL,                    -- FALLBACK, WELCOME, ERROR, TIMEOUT 등
    content TEXT NOT NULL,                        -- 표시할 메시지 내용
    is_active BOOLEAN DEFAULT true,               -- 활성화 여부
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

-- 인덱스
CREATE INDEX IF NOT EXISTS idx_basic_blocks_type ON basic_blocks(type);
CREATE INDEX IF NOT EXISTS idx_basic_blocks_active ON basic_blocks(is_active);

-- ===== 2. 오타 교정 규칙 테이블 =====
CREATE TABLE IF NOT EXISTS typo_words (
                                          id BIGSERIAL PRIMARY KEY,
                                          incorrect_word VARCHAR(100) NOT NULL UNIQUE,  -- 잘못된 단어 (입력)
    correct_word VARCHAR(100) NOT NULL,           -- 올바른 단어 (교정 결과)
    description TEXT,                             -- 설명 (선택적)
    usage_count INTEGER DEFAULT 0,               -- 사용 횟수 (통계용)
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

-- 인덱스
CREATE INDEX IF NOT EXISTS idx_typo_words_incorrect ON typo_words(incorrect_word);
CREATE INDEX IF NOT EXISTS idx_typo_words_correct ON typo_words(correct_word);

-- ===== 3. 기본 대화 블록 테이블 =====
-- 일반적인 대화 (인사, 감사, 도움 요청 등)
CREATE TABLE IF NOT EXISTS conversation_blocks (
                                                   id BIGSERIAL PRIMARY KEY,
                                                   title VARCHAR(100) NOT NULL,                  -- 블록 제목 (관리용)
    keywords TEXT NOT NULL,                       -- 매칭 키워드들 (쉼표 구분)
    response_message TEXT NOT NULL,               -- 응답 메시지
    similarity_threshold DECIMAL(3,2) DEFAULT 0.7, -- 유사도 임계값
    priority INTEGER DEFAULT 1,                  -- 우선순위 (낮을수록 우선)
    is_active BOOLEAN DEFAULT true,               -- 활성화 여부
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

-- 인덱스
CREATE INDEX IF NOT EXISTS idx_conversation_blocks_active ON conversation_blocks(is_active);
CREATE INDEX IF NOT EXISTS idx_conversation_blocks_priority ON conversation_blocks(priority);

-- ===== 4. 대화 히스토리 확장 =====
-- 기존 conversations 테이블 확장 (컬럼 추가)
-- similarity_score: 의도분석 결과 유사도
-- intent_type: 분석된 의도 타입
-- reference_id: 참조된 블록/지식 ID

ALTER TABLE conversations
    ADD COLUMN IF NOT EXISTS similarity_score DECIMAL(3,2) DEFAULT 0.0,
    ADD COLUMN IF NOT EXISTS intent_type VARCHAR(50),
    ADD COLUMN IF NOT EXISTS reference_id BIGINT;

-- 새 인덱스
CREATE INDEX IF NOT EXISTS idx_conversations_intent_type ON conversations(intent_type);
CREATE INDEX IF NOT EXISTS idx_conversations_similarity ON conversations(similarity_score);

-- ===== 5. 피드백 학습 테이블 =====
-- 사용자 피드백 기반 의도분석 성능 개선
CREATE TABLE IF NOT EXISTS conversation_feedback (
                                                     id BIGSERIAL PRIMARY KEY,
                                                     conversation_id BIGINT,                       -- 원본 대화 ID
                                                     user_input TEXT NOT NULL,                     -- 사용자 입력
                                                     predicted_intent VARCHAR(50),                 -- 예측된 의도
    actual_intent VARCHAR(50),                    -- 실제 의도 (피드백)
    feedback_type VARCHAR(50) NOT NULL,           -- BASIC_CONVERSATION, KNOWLEDGE, FALLBACK
    target_id BIGINT,                            -- 대상 블록/지식 ID
    similarity_score DECIMAL(3,2),               -- 당시 유사도 점수
    feedback_score INTEGER CHECK (feedback_score BETWEEN 1 AND 5), -- 사용자 만족도
    is_approved BOOLEAN DEFAULT false,            -- 관리자 승인 여부
    feedback_status VARCHAR(20) DEFAULT 'PENDING', -- PENDING, APPROVED, REJECTED
    user_session_id VARCHAR(100),                -- 세션 추적용
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

-- 인덱스
CREATE INDEX IF NOT EXISTS idx_conversation_feedback_approved ON conversation_feedback(is_approved);
CREATE INDEX IF NOT EXISTS idx_conversation_feedback_status ON conversation_feedback(feedback_status);
CREATE INDEX IF NOT EXISTS idx_conversation_feedback_type ON conversation_feedback(feedback_type);
CREATE INDEX IF NOT EXISTS idx_conversation_feedback_session ON conversation_feedback(user_session_id);

-- ===== 6. 지식베이스 기본 구조 (6편 준비) =====
-- 향후 TF-IDF 구현을 위한 기본 테이블

CREATE TABLE IF NOT EXISTS knowledge_base (
                                              id BIGSERIAL PRIMARY KEY,
                                              question TEXT NOT NULL,                       -- 질문
                                              answer TEXT NOT NULL,                         -- 답변
                                              category VARCHAR(100),                        -- 카테고리
    keywords TEXT,                               -- 추출된 키워드들
    tf_idf_vector TEXT,                          -- TF-IDF 벡터 (JSON 형태)
    view_count INTEGER DEFAULT 0,               -- 조회수 (인기도)
    last_matched_at TIMESTAMP,                   -- 마지막 매칭 시간
    is_active BOOLEAN DEFAULT true,              -- 활성화 여부
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

-- 인덱스
CREATE INDEX IF NOT EXISTS idx_knowledge_base_active ON knowledge_base(is_active);
CREATE INDEX IF NOT EXISTS idx_knowledge_base_category ON knowledge_base(category);
CREATE INDEX IF NOT EXISTS idx_knowledge_base_view_count ON knowledge_base(view_count DESC);



-- ===== 8. 초기 데이터 검증 쿼리 =====
-- 개발 시 확인용 (주석 해제 후 실행)

/*
-- 테이블 생성 확인
SELECT
    table_name,
    table_comment
FROM information_schema.tables
WHERE table_schema = 'public'
    AND table_name IN ('basic_blocks', 'typo_words', 'conversation_blocks', 'conversation_feedback', 'knowledge_base')
ORDER BY table_name;

-- 인덱스 생성 확인
SELECT
    indexname,
    tablename
FROM pg_indexes
WHERE tablename IN ('basic_blocks', 'typo_words', 'conversation_blocks', 'conversation_feedback', 'knowledge_base')
ORDER BY tablename, indexname;
*/