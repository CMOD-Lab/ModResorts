# ============================================================
# Stage 1: Builder
# ============================================================
FROM maven:3.8.6-openjdk-8-slim AS builder

WORKDIR /workspace

# Copy pom.xml first for dependency caching
COPY pom.xml .

# Download dependencies (cached layer)
RUN mvn dependency:go-offline -B

# Copy source code and web content
COPY src/ src/
COPY WebContent/ WebContent/

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
    JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UnlockExperimentalVMOptions" \
    CATALINA_HOME=/opt/tomcat \
    WEATHER_API_KEY="" \
    SERVER_DISPLAY_NAME="modresorts-server" \
    SERVER_FULL_NAME="modresorts-server-full" \
    JNDI_FACTORY="com.sun.jndi.fscontext.RefFSContextFactory" \
    JNDI_PROVIDER_URL=""

# Install Tomcat 9 (supports Servlet 4.0 / Java EE 8)
ENV TOMCAT_VERSION=9.0.82
RUN apt-get update && apt-get install -y --no-install-recommends \
        tzdata \
    && rm -rf /var/lib/apt/lists/* \
    && mkdir -p /opt/tomcat \
    && cd /tmp \
    && apt-get clean

# Download and install Tomcat
RUN apt-get update && apt-get install -y --no-install-recommends wget \
    && wget -q https://archive.apache.org/dist/tomcat/tomcat-9/v${TOMCAT_VERSION}/bin/apache-tomcat-${TOMCAT_VERSION}.tar.gz -O /tmp/tomcat.tar.gz \
    && tar -xzf /tmp/tomcat.tar.gz -C /opt/tomcat --strip-components=1 \
    && rm /tmp/tomcat.tar.gz \
    && apt-get remove -y wget \
    && apt-get autoremove -y \
    && rm -rf /var/lib/apt/lists/*

# Create non-root user for security
RUN groupadd -r modresorts && useradd -r -g modresorts -d /opt/tomcat -s /sbin/nologin modresorts

# Remove default Tomcat webapps
RUN rm -rf /opt/tomcat/webapps/*

# Copy WAR from builder stage
COPY --from=builder /workspace/target/modresorts-2.0.0.war /opt/tomcat/webapps/resorts.war

# Set ownership
RUN chown -R modresorts:modresorts /opt/tomcat

# Switch to non-root user
USER modresorts

# Expose application port
EXPOSE 8080

# Start Tomcat
CMD ["/opt/tomcat/bin/catalina.sh", "run"]
