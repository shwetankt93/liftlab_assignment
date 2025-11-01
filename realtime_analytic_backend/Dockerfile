# Multi-stage build for Spring Boot application
# Stage 1: Build the application
FROM maven:3.8.6-openjdk-11-slim AS build

WORKDIR /app

# Copy pom.xml and download dependencies (for better caching)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM openjdk:11-jre-slim

# Add curl for healthcheck
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy JAR from build stage
COPY --from=build /app/target/realtime-analytics-backend-*.jar app.jar

# Create non-root user for security
RUN groupadd -r appuser && useradd -r -g appuser appuser
RUN chown -R appuser:appuser /app
USER appuser

EXPOSE 8080

# Environment variables with defaults (can be overridden via docker-compose or docker run)
# Redis Configuration
ENV REDIS_HOST=redis
ENV REDIS_PORT=6379

# Kafka Configuration (internal Docker network - use port 29092)
ENV KAFKA_BOOTSTRAP_SERVERS=kafka:29092
ENV KAFKA_CONSUMER_GROUP_ID=analytics-consumer-group
ENV KAFKA_AUTO_OFFSET_RESET=earliest
ENV KAFKA_ENABLE_AUTO_COMMIT=true
ENV KAFKA_CONCURRENCY=2

# Server Configuration
ENV SERVER_PORT=8080

# CORS Configuration
ENV CORS_ALLOWED_ORIGINS=http://localhost:5173,http://localhost:3000,http://localhost:8081

# Rate Limiting Configuration
ENV RATE_LIMIT_PER_SECOND=100
ENV RATE_LIMIT_REFRESH_PERIOD=1s
ENV RATE_LIMIT_TIMEOUT_DURATION=100ms

# Logging Configuration
ENV LOGGING_LEVEL_ROOT=INFO
ENV LOGGING_LEVEL_COM_LIFTLAB_ANALYTICS=DEBUG
ENV LOGGING_LEVEL_SPRING_KAFKA=INFO
ENV LOGGING_LEVEL_SPRING_DATA_REDIS=INFO

# JVM Options
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar --spring.profiles.active=docker"]
