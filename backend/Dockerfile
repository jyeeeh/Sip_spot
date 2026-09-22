# ── 빌드 스테이지 ──────────────────────────────────────────────────────────
FROM gradle:8.14-jdk21 AS builder
WORKDIR /app

# 의존성 캐시 레이어: 설정 파일만 먼저 복사
COPY --chown=gradle:gradle build.gradle settings.gradle ./
COPY --chown=gradle:gradle gradle gradle
COPY --chown=gradle:gradle gradlew ./
RUN ./gradlew dependencies --no-daemon

# 소스 복사 후 jar 빌드
COPY --chown=gradle:gradle src src
RUN ./gradlew bootJar --no-daemon

# ── 런타임 스테이지 ────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN adduser -D -u 1001 appuser

COPY --from=builder --chown=appuser:appuser \
     /app/build/libs/sipspot-0.1.0-SNAPSHOT.jar app.jar

USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
