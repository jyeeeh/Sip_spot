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
- **커밋 메시지 형식**: 타입 접두사(`feat` / `fix` / `chore` / `test` / `docs`)는 영어, 제목과 본문은 한국어
- **한 번에 한 단계씩만 작업하고, 큰 변경 전에는 계획을 먼저 보여준다**
- `.env`, `application-secret*` 파일은 절대 커밋하지 않는다
- TypeScript는 사용하지 않는다 (frontend 전체 JavaScript)
- WebSocket, 결제 코드는 별도 단계에서 추가한다
- **Boot 3.x 방식으로 작성하지 않는다. 확실하지 않으면 공식 문서를 확인한다**
- **프론트엔드에서 `v-html` 사용 금지. 사용자 입력은 항상 텍스트 바인딩(`{{ }}`)으로 출력한다**
- **방 코드는 만료되지 않는다. 멤버 인증은 코드가 아니라 토큰으로 한다. 나중에 코드 갱신을 붙일 수 있도록 코드에 의존한 로직을 만들지 않는다**

### 방 코드 규칙

- charset: `ABCDEFGHJKLMNPQRSTUVWXYZ23456789` (32자, 혼동 문자 0·1·I·O 제외)
- `SecureRandom`으로 7자 생성
- DB unique 충돌 시 최대 5회 재생성 (트랜잭션 밖 루프)
- 입력 코드는 항상 대문자로 정규화 후 처리
- **방 코드는 항상 대문자로 정규화해서 저장·비교한다**

### 토큰 규칙

- `SecureRandom(32 bytes)` → base64url(패딩 없음) → 클라이언트 응답
- DB에는 SHA-256 hex(64자)만 저장, 원문 토큰은 로그에 남기지 않는다
- 프론트엔드는 토큰을 `localStorage`에 방 코드별(`sipspot_token_{CODE}`)로 저장
- memberId도 `sipspot_member_id_{CODE}` 키로 저장 (위치 UI, 아바타 구분에 사용)

### WebSocket 규칙

- **토큰 전달**: STOMP CONNECT 프레임의 `Authorization: Bearer <token>` 헤더로만 전달. URL 쿼리 파라미터 사용 금지
- **구독 권한**: `/topic/rooms/{code}`는 해당 방의 멤버만 구독 가능. 다른 방 또는 허용되지 않은 destination 구독 거부
- **Principal 사용**: `memberId`는 반드시 `Principal`(`MemberPrincipal`)에서만 추출. 페이로드 값 신뢰 금지
- **SEND 검증**: `@MessageMapping` 핸들러에서 URL `{code}`(대문자 정규화)와 `principal.roomCode()` 일치 여부 반드시 확인
- **브로드캐스트**: `tokenHash` 등 민감 필드를 이벤트 메시지에 절대 포함하지 않는다
- **오프라인 지연**: Presence 오프라인 처리는 3초 지연 (새로고침 깜빡임 방지)

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
