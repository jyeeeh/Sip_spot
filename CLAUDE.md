# CLAUDE.md — Sip Spot 프로젝트 지침

## 스택

| 영역 | 기술 |
|---|---|
| Frontend | Vue 3 + Vite + JavaScript (TypeScript 사용 안 함), Pinia, Vue Router 4 |
| Backend | Java 21, Spring Boot 4.1.0, Spring Framework 7, Gradle 8.14+ |
| Java 패키지 | `com.jyeeeh.sipspot` (언더스코어 사용 금지) |
| DB | PostgreSQL |

---

## 폴더 구조

```
Sip_spot/
├── README.md
├── .gitignore
├── CLAUDE.md
├── frontend/
│   ├── index.html
│   ├── package.json
│   ├── vite.config.js
│   ├── .env.example
│   └── src/
│       ├── main.js
│       ├── App.vue
│       ├── router/index.js
│       ├── stores/health.js
│       └── views/HomeView.vue
└── backend/
    ├── build.gradle
    ├── settings.gradle
    ├── gradlew / gradlew.bat
    ├── gradle/wrapper/
    ├── .env.example
    └── src/main/
        ├── java/com/jyeeeh/sipspot/
        │   ├── SipspotApplication.java
        │   ├── config/CorsConfig.java
        │   └── controller/HealthController.java
        └── resources/
            ├── application.yml
            ├── application-local.yml
            └── application-prod.yml
```

---

## 실행 명령

```bash
# Backend (프로파일 기본값: local)
cd backend && ./gradlew bootRun

# Frontend
cd frontend && npm install && npm run dev
```

---

## 규칙

- **결제 금액은 서버에서만 계산하고 클라이언트 값을 신뢰하지 않는다**
- **시크릿 키를 코드에 하드코딩하지 않는다** — 환경변수 또는 `.env` 파일 사용
- **커밋 메시지 형식**: `feat:` / `fix:` / `chore:`
- **한 번에 한 단계씩만 작업하고, 큰 변경 전에는 계획을 먼저 보여준다**
- `.env`, `application-secret*` 파일은 절대 커밋하지 않는다
- TypeScript는 사용하지 않는다 (frontend 전체 JavaScript)
- WebSocket, 결제 코드는 별도 단계에서 추가한다
- **Boot 3.x 방식으로 작성하지 않는다. 확실하지 않으면 공식 문서를 확인한다**

### Boot 4 / Spring Framework 7 / Jackson 3 주의사항

- **Jackson 3**: 패키지명이 `com.fasterxml.jackson` → `tools.jackson`으로 변경됨
- **테스트 Mock**: `@MockBean` / `@SpyBean` 제거됨 → `@MockitoBean` / `@MockitoSpyBean` 사용
- **`@EntityScan`**: `org.springframework.boot.persistence.autoconfigure.EntityScan`으로 이동
- **자동설정 클래스 패키지 이동** (exclude 등에서 사용 시):
  - JDBC: `org.springframework.boot.autoconfigure.jdbc.*` → `org.springframework.boot.jdbc.autoconfigure.*`
  - JPA/Hibernate: `org.springframework.boot.autoconfigure.orm.jpa.*` → `org.springframework.boot.hibernate.autoconfigure.*`
- **Web 스타터 이름 변경**: `spring-boot-starter-web` → `spring-boot-starter-webmvc`

---

## 환경변수 목록

### Backend
| 변수 | 설명 | 기본값 |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | 활성 프로파일 | `local` |
| `DB_URL` | PostgreSQL JDBC URL | — |
| `DB_USERNAME` | DB 사용자 | — |
| `DB_PASSWORD` | DB 비밀번호 | — |
| `CORS_ALLOWED_ORIGIN` | CORS 허용 도메인 | `http://localhost:5173` |

### Frontend
| 변수 | 설명 | 기본값 |
|---|---|---|
| `VITE_API_BASE_URL` | 백엔드 API 주소 | `http://localhost:8080` |
