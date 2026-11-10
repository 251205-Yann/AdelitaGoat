# =============================================================================
# Stage 1: Build Ktor distribution with Gradle + JDK 21 (when Root Dir = server)
# =============================================================================
FROM gradle:8.11.1-jdk21 AS builder

WORKDIR /workspace/server

COPY build.gradle.kts settings.gradle.kts gradle.properties ./
COPY src ./src

RUN gradle installDist --no-daemon -x test

# =============================================================================
# Stage 2: Lightweight JRE 21 runtime for Render Free
# =============================================================================
FROM eclipse-temurin:21-jre-jammy AS runtime

WORKDIR /app

COPY --from=builder /workspace/server/build/install/adelitagoat-server /app

ENV PORT=8080 \
    DB_ALLOW_EMBEDDED_FALLBACK=false \
    SEED_DEMO_DATA=false \
    DB_MAX_POOL_SIZE=4 \
    JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8080

ENTRYPOINT ["/app/bin/adelitagoat-server"]
