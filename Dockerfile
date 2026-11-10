# =============================================================================
# Stage 1: Build Ktor distribution with Gradle + JDK 21
# =============================================================================
FROM gradle:8.11.1-jdk21 AS builder

WORKDIR /workspace/server

# Copy Gradle configuration first to leverage Docker layer cache
COPY server/build.gradle.kts server/settings.gradle.kts server/gradle.properties ./

# Copy backend source and Flyway SQL migrations
COPY server/src ./src

# Build standalone distribution without running tests during container image build
RUN gradle installDist --no-daemon -x test

# =============================================================================
# Stage 2: Lightweight JRE 21 runtime for Render Free
# =============================================================================
FROM eclipse-temurin:21-jre-jammy AS runtime

WORKDIR /app

# Copy compiled distribution from builder stage
COPY --from=builder /workspace/server/build/install/adelitagoat-server /app

# Safe production defaults for Render Free + Supabase Session Pooler:
# - Never fall back to embedded DB in production (DB_ALLOW_EMBEDDED_FALLBACK=false)
# - Never seed demo data in production (SEED_DEMO_DATA=false)
# - Small HikariCP pool suitable for Supabase Free Session Pooler (DB_MAX_POOL_SIZE=4)
# - Container-aware JVM memory limits for Render Free (512 MB RAM)
# Secrets (DB_URL, DB_USER, DB_PASSWORD) MUST be injected via Render Environment Variables.
ENV PORT=8080 \
    DB_ALLOW_EMBEDDED_FALLBACK=false \
    SEED_DEMO_DATA=false \
    DB_MAX_POOL_SIZE=4 \
    JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8080

ENTRYPOINT ["/app/bin/adelitagoat-server"]
