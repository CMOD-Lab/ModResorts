# ============================================================
# Stage 1: Builder
# ============================================================
FROM maven:3.8.6-openjdk-8-slim AS builder

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
FROM openjdk:8-jdk

LABEL maintainer="ModResorts Team" \
      application="modresorts" \
      version="2.0.0"

# Set environment variables
ENV TZ=UTC \
    LANG=en_US.UTF-8 \
    LANGUAGE=en_US:en \
    LC_ALL=en_US.UTF-8 \
    CATALINA_HOME=/opt/tomcat \
    JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UnlockExperimentalVMOptions"

# Install Tomcat 9 (supports Servlet 4.0 / Java EE 8)
RUN apt-get update && apt-get install -y --no-install-recommends \
        ca-certificates \
    && rm -rf /var/lib/apt/lists/* \
    && mkdir -p /opt/tomcat \
    && groupadd -r appuser && useradd -r -g appuser -d /opt/tomcat -s /sbin/nologin appuser

# Download and install Apache Tomcat 9
RUN apt-get update && apt-get install -y --no-install-recommends wget \
    && wget -q https://archive.apache.org/dist/tomcat/tomcat-9/v9.0.82/bin/apache-tomcat-9.0.82.tar.gz -O /tmp/tomcat.tar.gz \
    && tar -xzf /tmp/tomcat.tar.gz -C /opt/tomcat --strip-components=1 \
    && rm /tmp/tomcat.tar.gz \
    && apt-get remove -y wget && apt-get autoremove -y \
    && rm -rf /var/lib/apt/lists/* \
    && rm -rf /opt/tomcat/webapps/ROOT \
    && rm -rf /opt/tomcat/webapps/examples \
    && rm -rf /opt/tomcat/webapps/docs \
    && rm -rf /opt/tomcat/webapps/host-manager \
    && rm -rf /opt/tomcat/webapps/manager

# Copy the WAR from builder stage
COPY --from=builder /workspace/target/modresorts-2.0.0.war /opt/tomcat/webapps/ROOT.war

# Set ownership
RUN chown -R appuser:appuser /opt/tomcat

# Switch to non-root user
USER appuser

# Expose application port
EXPOSE 8080

# Start Tomcat
CMD ["/opt/tomcat/bin/catalina.sh", "run"]
