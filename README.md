# Sip Spot

7자리 룸 코드로 친구들끼리 입장해서 내 위치(부엌/거실/침대/밖)를 공유하고,
커피를 후원하면 실시간 알림이 가는 사이드 프로젝트.

---

## 스택

| 영역 | 기술 |
|---|---|
| Frontend | Vue 3 + Vite + JavaScript, Pinia, Vue Router 4 |
| Backend | Java 21, Spring Boot 4.1.0, Spring Framework 7, Gradle 8.14+ |
| DB | PostgreSQL (1단계에서는 미연결) |

---

## 로컬 실행 방법

### 사전 요구사항

- Java 21
- Node.js 20+ / npm

### Backend

```bash
cd backend

# (선택) .env 파일 생성 — DB 연결 전까지는 없어도 됨
cp .env.example .env

# 실행 (프로파일 기본값: local)
./gradlew bootRun
```

서버가 뜨면 `http://localhost:8080/api/health` 에서 `{"status":"ok"}` 를 확인할 수 있습니다.

### Frontend

```bash
cd frontend

# (선택) 환경변수 파일 생성
cp .env.example .env.local

npm install
npm run dev
```

브라우저에서 `http://localhost:5173` 를 열면 health API 호출 결과가 표시됩니다.

### 프로파일 전환

```bash
# 운영 프로파일로 실행
SPRING_PROFILES_ACTIVE=prod ./gradlew bootRun
```

---

## 환경변수

### Backend (`backend/.env`)

| 변수 | 설명 | 기본값 |
|---|---|---|
| `DB_URL` | PostgreSQL JDBC URL | (없으면 DB 비활성화) |
| `DB_USERNAME` | DB 사용자 | — |
| `DB_PASSWORD` | DB 비밀번호 | — |
| `CORS_ALLOWED_ORIGIN` | CORS 허용 도메인 | `http://localhost:5173` |

### Frontend (`frontend/.env.local`)

| 변수 | 설명 | 기본값 |
|---|---|---|
| `VITE_API_BASE_URL` | 백엔드 API 주소 | `http://localhost:8080` |

---

## 개발 규칙

- 결제 금액은 서버에서만 계산한다
- 시크릿 키를 코드에 하드코딩하지 않는다
- 커밋 메시지 형식: `feat:` / `fix:` / `chore:`
- 한 번에 한 단계씩 작업하고, 큰 변경 전에는 계획을 먼저 공유한다
