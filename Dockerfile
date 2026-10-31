# Stage 1: 프론트엔드 빌드 및 테스트
FROM node:22-alpine AS frontend-builder
WORKDIR /workspace/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend ./
RUN npm run test:games && npm run build

# Stage 2: 백엔드 빌드 (스프링 부트 jar 생성)
FROM eclipse-temurin:21-jdk-alpine AS backend-builder
WORKDIR /workspace
COPY gradlew settings.gradle build.gradle ./
COPY gradle/ ./gradle/
COPY backend/build.gradle ./backend/
COPY --from=frontend-builder /workspace/frontend/dist ./frontend/dist
COPY --from=frontend-builder /workspace/frontend/package.json ./frontend/package.json
COPY backend/src ./backend/src
RUN chmod +x gradlew && ./gradlew :backend:bootJar -x buildFrontend --no-daemon

# Stage 3: 경량 런타임 이미지 (Hugging Face Spaces 호환)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Hugging Face Spaces는 UID 1000(non-root) 실행을 요구합니다.
RUN adduser -D -u 1000 appuser && chown -R appuser:appuser /app
USER appuser

COPY --from=backend-builder --chown=appuser:appuser /workspace/backend/build/libs/*.jar app.jar

ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-Dfile.encoding=UTF-8", "-jar", "app.jar"]
