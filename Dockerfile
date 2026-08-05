# =============================================================================
# Stage 1: Builder
# =============================================================================
FROM maven:3.9.4-eclipse-temurin-8 AS builder

WORKDIR /workspace

# Copy build descriptor first for dependency caching
COPY pom.xml .

# Download dependencies (cached layer)
RUN mvn dependency:go-offline -B

# Copy source code and web content
COPY src/ src/
COPY WebContent/ WebContent/

# Build the WAR artifact (skip tests for Docker build)
RUN mvn clean package -DskipTests -B

# =============================================================================
# Stage 2: Runtime
# =============================================================================
FROM amazoncorretto:8

LABEL maintainer="BookingServices Team" \
      application="bookingservices" \
      version="2.0.0"

# Install Apache Tomcat 9 (supports Servlet 3.1 / Java EE 7)
ENV CATALINA_HOME=/opt/tomcat
ENV TOMCAT_VERSION=9.0.85

RUN yum install -y tar gzip && \
    curl -fsSL "https://archive.apache.org/dist/tomcat/tomcat-9/v${TOMCAT_VERSION}/bin/apache-tomcat-${TOMCAT_VERSION}.tar.gz" \
         -o /tmp/tomcat.tar.gz && \
    mkdir -p ${CATALINA_HOME} && \
    tar -xzf /tmp/tomcat.tar.gz -C ${CATALINA_HOME} --strip-components=1 && \
    rm -f /tmp/tomcat.tar.gz && \
    yum clean all

# Create non-root user for security
RUN groupadd -r appgroup && useradd -r -g appgroup -d /home/appuser -s /sbin/nologin appuser

# Set timezone
ENV TZ=UTC

# JVM tuning for containerized environments
ENV JAVA_OPTS="-Xmx512m -Xms256m \
    -XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:+UseG1GC \
    -Djava.security.egd=file:/dev/./urandom \
    -Dfile.encoding=UTF-8 \
    -Duser.timezone=UTC"

# Remove default Tomcat webapps
RUN rm -rf ${CATALINA_HOME}/webapps/ROOT \
           ${CATALINA_HOME}/webapps/examples \
           ${CATALINA_HOME}/webapps/docs \
           ${CATALINA_HOME}/webapps/host-manager \
           ${CATALINA_HOME}/webapps/manager

# Copy WAR from builder stage
COPY --from=builder /workspace/target/modresorts-2.0.0.war ${CATALINA_HOME}/webapps/resorts.war

# Set ownership
RUN chown -R appuser:appgroup ${CATALINA_HOME}

# Create log directory
RUN mkdir -p /app/logs && chown -R appuser:appgroup /app/logs

# Expose application port
EXPOSE 8080

USER appuser

# Graceful shutdown support
STOPSIGNAL SIGTERM

# Start Tomcat
CMD ["sh", "-c", "${CATALINA_HOME}/bin/catalina.sh run"]
