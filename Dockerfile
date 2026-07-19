# ============================================================
# Stage 1: Builder
# ============================================================
FROM maven:3.9.4-eclipse-temurin-8 AS builder

WORKDIR /workspace

# Copy pom.xml first for dependency caching
COPY pom.xml .

# Download dependencies (layer cache)
RUN mvn dependency:go-offline -B

# Copy source code and web content
COPY src ./src
COPY WebContent ./WebContent

# Build the WAR artifact
RUN mvn clean package -DskipTests -B

# ============================================================
# Stage 2: Runtime
# ============================================================
FROM amazoncorretto:8

LABEL maintainer="ModResorts Team" \
      application="modresorts" \
      version="2.0.0"

# Set environment variables
ENV TZ=UTC \
    JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UnlockExperimentalVMOptions" \
    WEATHER_API_KEY="" \
    SERVER_DISPLAY_NAME="modresorts" \
    SERVER_FULL_NAME="modresorts-server"

# Create non-root user for security
RUN groupadd -r appgroup && useradd -r -g appgroup -d /opt/app -s /sbin/nologin appuser

# Create application directory
RUN mkdir -p /opt/app && chown -R appuser:appgroup /opt/app

WORKDIR /opt/app

# Copy the WAR from builder stage
COPY --from=builder /workspace/target/modresorts-2.0.0.war app.war

# Change ownership
RUN chown -R appuser:appgroup /opt/app

# Switch to non-root user
USER appuser

# Expose application port
EXPOSE 8080

# Start the application using an embedded servlet container
ENTRYPOINT ["java", "-jar", "app.war"]
