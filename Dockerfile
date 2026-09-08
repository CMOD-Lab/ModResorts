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
FROM eclipse-temurin:8-jdk

LABEL maintainer="ModResorts Team" \
      app="modresorts" \
      version="2.0.0"

# Install Tomcat 9 (supports Servlet 3.1 / Java EE 7)
ENV CATALINA_HOME=/opt/tomcat
ENV TOMCAT_VERSION=9.0.85

RUN mkdir -p ${CATALINA_HOME} && \
    cd /tmp && \
    wget -q https://archive.apache.org/dist/tomcat/tomcat-9/v${TOMCAT_VERSION}/bin/apache-tomcat-${TOMCAT_VERSION}.tar.gz && \
    tar -xzf apache-tomcat-${TOMCAT_VERSION}.tar.gz -C ${CATALINA_HOME} --strip-components=1 && \
    rm apache-tomcat-${TOMCAT_VERSION}.tar.gz && \
    rm -rf ${CATALINA_HOME}/webapps/ROOT \
           ${CATALINA_HOME}/webapps/examples \
           ${CATALINA_HOME}/webapps/docs \
           ${CATALINA_HOME}/webapps/host-manager \
           ${CATALINA_HOME}/webapps/manager

# Create non-root user for security
RUN groupadd -r appuser && useradd -r -g appuser -d /home/appuser -s /sbin/nologin appuser

# Set timezone
ENV TZ=UTC

# JVM tuning for containers
ENV JAVA_OPTS="-Xmx512m -Xms256m \
  -XX:+UseContainerSupport \
  -XX:MaxRAMPercentage=75.0 \
  -XX:+UnlockExperimentalVMOptions \
  -Djava.security.egd=file:/dev/./urandom \
  -Dfile.encoding=UTF-8 \
  -Duser.timezone=UTC"

# Application environment variables
ENV WEATHER_API_KEY=""
ENV SERVER_DISPLAY_NAME="modresorts"
ENV SERVER_FULL_NAME="modresorts-server"
ENV JNDI_FACTORY="com.sun.jndi.rmi.registry.RegistryContextFactory"
ENV JNDI_PROVIDER_URL="rmi://localhost:1099"

# Copy WAR from builder stage and deploy to Tomcat ROOT context under /resorts
COPY --from=builder /workspace/target/modresorts-2.0.0.war ${CATALINA_HOME}/webapps/resorts.war

# Set ownership
RUN chown -R appuser:appuser ${CATALINA_HOME}

# Expose application port
EXPOSE 8080

USER appuser

# Start Tomcat
CMD ["sh", "-c", "${CATALINA_HOME}/bin/catalina.sh run"]
