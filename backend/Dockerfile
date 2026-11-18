# ==============================================================================
# Stage 1: Build stage with Maven and Java 21
# ==============================================================================
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# Copy Maven project descriptor
COPY pom.xml .

# Copy backend source code (excluding frontend code entirely)
COPY src ./src

# Build production executable JAR skipping both test compilation and test execution.
# Deterministic POSIX glob selection: find regular *.jar under target/, exclude *.jar.original, and fail unless exactly one exists.
RUN mvn clean package -Dmaven.test.skip=true -B && \
    set -- && \
    for f in target/*.jar; do \
        [ -f "$f" ] || continue; \
        case "$f" in \
            *.jar.original) continue ;; \
            *) set -- "$@" "$f" ;; \
        esac; \
    done && \
    if [ "$#" -ne 1 ]; then \
        echo "ERROR: Expected exactly 1 executable JAR in target/, found $#: $*" >&2; \
        exit 1; \
    fi && \
    cp "$1" /build/app.jar

# ==============================================================================
# Stage 2: Production runtime with lightweight Java 21 JRE (Alpine)
# ==============================================================================
FROM eclipse-temurin:21-jre-alpine AS runner

WORKDIR /app

# Create dedicated non-root user and group
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy built executable JAR from builder stage with proper non-root ownership
COPY --from=builder --chown=appuser:appgroup /build/app.jar /app/app.jar

# Render dynamic port binding: defaults to 8080 if PORT is unset
ENV PORT=8080
EXPOSE 8080

# Production JVM container ergonomics: respects cgroup limits, fails fast on OOM
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

# Switch to non-root execution
USER appuser:appgroup

# Launch application passing Render-injected PORT to server.port with exec for signal propagation
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -Dserver.port=${PORT:-8080} -jar /app/app.jar"]
