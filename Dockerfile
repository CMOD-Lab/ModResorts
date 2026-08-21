# =============================================================================
# ModResorts Web Application - Multi-Stage Dockerfile
# Build Tool: Maven | Java Version: 8 | Package: WAR
# Runtime: amazoncorretto:21-alpine-full (explicit base image)
# =============================================================================

# ---- Stage 1: Builder ----
FROM maven:3.9.4-eclipse-temurin-21 AS builder

LABEL maintainer="ModResorts Team"
LABEL description="ModResorts Web Application - Builder Stage"

WORKDIR /workspace

# Copy Maven build descriptor first for dependency caching
COPY pom.xml .

# Download all dependencies (cached layer if pom.xml unchanged)
RUN mvn dependency:go-offline -B

# Copy the full project source
COPY src/ src/
COPY WebContent/ WebContent/

# Build the WAR (skip tests for Docker build)
RUN mvn clean package -DskipTests -B

# ---- Stage 2: Runtime ----
FROM amazoncorretto:21-alpine-full

LABEL maintainer="ModResorts Team"
LABEL description="ModResorts Web Application - Runtime Stage"
LABEL version="2.0.0"
LABEL application="modresorts"

# Set environment variables
ENV TZ=UTC \
    LANG=en_US.UTF-8 \
    LANGUAGE=en_US:en \
    LC_ALL=en_US.UTF-8 \
    JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UnlockExperimentalVMOptions -Djava.security.egd=file:/dev/./urandom" \
    APP_PORT=9080

# Create non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Install Tomcat (servlet container for WAR deployment)
ENV CATALINA_HOME=/opt/tomcat
ENV CATALINA_BASE=/opt/tomcat
ENV PATH=$CATALINA_HOME/bin:$PATH

RUN apk add --no-cache wget tar && \
    wget -q https://archive.apache.org/dist/tomcat/tomcat-10/v10.1.18/bin/apache-tomcat-10.1.18.tar.gz -O /tmp/tomcat.tar.gz && \
    mkdir -p /opt/tomcat && \
    tar -xzf /tmp/tomcat.tar.gz -C /opt/tomcat --strip-components=1 && \
    rm /tmp/tomcat.tar.gz && \
    rm -rf /opt/tomcat/webapps/ROOT \
           /opt/tomcat/webapps/examples \
           /opt/tomcat/webapps/docs \
           /opt/tomcat/webapps/host-manager \
           /opt/tomcat/webapps/manager && \
    apk del wget tar && \
    chown -R appuser:appgroup /opt/tomcat && \
    chmod -R 755 /opt/tomcat

# Configure Tomcat port
RUN sed -i 's/port="8080"/port="9080"/' /opt/tomcat/conf/server.xml

# Create application directories
RUN mkdir -p /app/logs /app/config && \
    chown -R appuser:appgroup /app

# Copy WAR from builder stage
COPY --from=builder /workspace/target/modresorts-2.0.0.war /opt/tomcat/webapps/resorts.war

# Set ownership
RUN chown appuser:appgroup /opt/tomcat/webapps/resorts.war

# Switch to non-root user
USER appuser

# Expose application port
EXPOSE 9080

# Graceful shutdown support
STOPSIGNAL SIGTERM

# Start Tomcat
CMD ["sh", "-c", "exec $CATALINA_HOME/bin/catalina.sh run"]
