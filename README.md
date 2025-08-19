# 🤖 Spring Boot ChatBot Series

**재미로 해보는 Spring Boot ChatBot 개발 여정**  
Spring Boot 3.x + Vue 3 + PostgreSQL + MyBatis 기반의 **시나리오형 ChatBot** 프로젝트입니다.  

---

## 📊 진행 현황

| 편 | 제목 | 브랜치 | 상태 |
|----|------|--------|------|
| 1  | 프로젝트 시작하기 | `series/01-project-setup` | ✅ 완료 |
| 2  | 데이터베이스 설계 | `series/02-database-design` | ✅ 완료 |
| 3  | 시나리오 관리 시스템 | `series/03-scenario-system` | ⏳ 진행 중 |
| 4  | 실시간 채팅 (WebSocket) | `series/04-websocket-chat` | ⏳ 예정 |

> 📌 각 시리즈 완료 시 `series-XX-complete` 태그를 생성하고, Velog에 상세 구현 과정을 게시합니다.

---

## 완성된 기능들

### ✅ 1편: 프로젝트 기초 설정
- Spring Boot 3.x + Gradle(Kotlin DSL) 멀티모듈 구조
- PostgreSQL 연동 및 기본 환경 설정
- Vue.js 3 프론트엔드 통합 빌드 시스템

### ✅ 2편: 견고한 데이터베이스 설계
- **시나리오 기반 대화 흐름** 테이블 설계
- JSONB를 활용한 **조건부 분기** 시스템
- Self-Referencing으로 단계별 연결 관리
- **9개 도메인 모델** 및 MyBatis 매퍼 완성

---

## 기술 스택

**Backend**
- Java 17
- Spring Boot 3.x
- MyBatis
- PostgreSQL 15 (JSONB 활용)
- Gradle (Kotlin DSL)
- JUnit 5 + MockMvc

**Frontend**
- Vue.js 3
- Vite
- Pinia
- Node.js 20.x

**DevOps**
- Docker / Docker Compose
- GitHub Actions (CI/CD)

---

## 아키텍처

```
┌───────────────────────┐
│      Frontend (Vue)    │
│ - Vite Build           │
│ - Pinia 상태관리       │
└───────────▲───────────┘
            │ REST API
            ▼
┌───────────────────────┐
│   Backend (Spring)     │
│ - MyBatis Persistence  │
│ - REST API Controller  │
└───────────▲───────────┘
            │ SQL Query
            ▼
┌───────────────────────┐
│   PostgreSQL Database  │
└───────────────────────┘
```

---

## 🚀 실행 방법

### 1) 저장소 클론
```bash
git clone https://github.com/kyunmo/chatbot-series.git
cd chatbot-series
```

### 2) PostgreSQL 실행 (Docker)
```bash
docker run --name chatbot-postgres \
    -e POSTGRES_DB=chatbot_dev \
    -e POSTGRES_USER=chatbot \
    -e POSTGRES_PASSWORD=chatbot2025@ \
    -p 5432:5432 -d postgres:15
```

### 3) 백엔드 실행
```bash
./gradlew bootRun
```

### 4) 프론트엔드 실행
```bash
cd frontend
npm install
npm run dev
```

**접속 주소**
- Backend: [http://localhost:9780](http://localhost:9780)
- Frontend: [http://localhost:5173](http://localhost:5173)

---

## 프로젝트 구조

```
chatbot-series/
├── backend/                    # Spring Boot 백엔드
│   ├── src/main/java/io/moyam/chatbot/
│   │   ├── domain/            # 9개 도메인 모델
│   │   ├── mapper/            # MyBatis 매퍼 인터페이스
│   │   ├── service/           # 비즈니스 로직
│   │   └── config/            # 설정 (JsonTypeHandler 등)
│   └── src/main/resources/
│       ├── mybatis/mapper/    # XML 매퍼 파일들
│       └── application-*.yml  # 환경별 설정
├── frontend/                   # Vue.js 프론트엔드
│   ├── src/
│   └── package.json
├── docs/                      # 시리즈 문서
└── scripts/                   # 유틸리티 스크립트
```

---

## 🌿 브랜치 & 커밋 규칙

### 브랜치 구조
- `main`: 안정된 버전
- `series/XX-[feature-name]`: 각 시리즈별 브랜치

### 커밋 메시지 형식
```
feat[02]: JSONB 타입 핸들러 추가
fix[02]: 순환 참조 DDL 순서 수정
test[02]: 도메인 매퍼 테스트 완성
docs[02]: 데이터베이스 설계 문서 업데이트
```

---

**📖 현재 진행 상황**: 2편 완료, 3편 진행 중  
**🎯 다음 목표**: 시나리오 실행 엔진 완성 및 실제 대화 테스트
