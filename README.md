# 🤖 Spring Boot ChatBot Series

**재미로 해보는 Spring Boot ChatBot 개발 여정**  
Spring Boot 3.x + Vue 3 + PostgreSQL + MyBatis 기반의 **시나리오형 ChatBot** 프로젝트입니다.  

---

## 📊 진행 현황

| 편 | 제목 | 브랜치 | 상태 |
|----|------|--------|------|
| 1  | 프로젝트 시작하기 | `series/01-project-setup` | ✅ 완료 |
| 2  | 데이터베이스 설계 | `series/02-database-design` | ⏳ 진행 예정 |
| 3  | Spring Security 인증/인가 | `series/03-spring-security` | ⏳ 예정 |
| 4  | 개발환경 구축 (Docker) | `series/04-dev-environment` | ⏳ 예정 |
| 5~15 | 핵심 기능 & 운영 고도화 | - | ⏳ 예정 |

> 📌 각 시리즈 완료 시 `series-XX-complete` 태그를 생성하고, Velog에 상세 구현 과정을 게시합니다.

---

## 🛠 기술 스택

**Backend**
- Java 17
- Spring Boot 3.x
- MyBatis
- PostgreSQL 15
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

## 🏗 아키텍처

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

### 2) 백엔드 실행
```bash
./gradlew bootRun
```

### 3) 프론트엔드 실행
```bash
cd frontend
npm install
npm run dev
```

- Backend: [http://localhost:9780](http://localhost:9780)
- Frontend: [http://localhost:5173](http://localhost:5173)

---

## 🌿 브랜치 & 커밋 규칙

- 시리즈별 브랜치: `series/01-project-setup`, `series/02-database-design`, ...
- 커밋 메시지 형식:
  ```
  feat[01]: 프로젝트 초기 설정 추가
  fix[02]: DB 스키마 오류 수정
  docs[01]: README 초기 작성
  ```

---