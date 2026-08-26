# =============================================================================
# ModResorts - Multi-Stage Dockerfile
# Build Tool : Maven
# Java Version: 8 (compiled with maven.compiler.source=1.8)
# Package Type: WAR (deployed on Open Liberty / Tomcat)
# Runtime Base: amazoncorretto:21-alpine-full (explicit)
# =============================================================================

# ---------------------------------------------------------------------------
# Stage 1: Builder
# ---------------------------------------------------------------------------
FROM maven:3.9.4-eclipse-temurin-21 AS builder

WORKDIR /workspace

# Copy build descriptor first for dependency layer caching
COPY pom.xml .

# Pre-download all dependencies (cached layer unless pom.xml changes)
RUN mvn dependency:go-offline -B

# Copy full project source
COPY src ./src
COPY WebContent ./WebContent

# Build the WAR, skip tests
RUN mvn clean package -DskipTests -B

# ---------------------------------------------------------------------------
# Stage 2: Runtime
# ---------------------------------------------------------------------------
FROM amazoncorretto:21-alpine-full

# Metadata
LABEL maintainer="ModResorts Team" \
      application="modresorts" \
      version="2.0.0"

# Timezone
ENV TZ=UTC

# Install Open Liberty (lightweight Jakarta EE / Servlet container)
ARG LIBERTY_VERSION=23.0.0.12
RUN apk add --no-cache tar gzip \
    && mkdir -p /opt/ol \
    && wget -q "https://public.dhe.ibm.com/ibmdl/export/pub/software/openliberty/runtime/release/${LIBERTY_VERSION}/openliberty-${LIBERTY_VERSION}.zip" \
         -O /tmp/liberty.zip \
    && unzip -q /tmp/liberty.zip -d /opt/ol \
    && mv /opt/ol/wlp /opt/liberty \
    && rm -rf /tmp/liberty.zip /opt/ol \
    && apk del tar gzip

# Create non-root user
RUN addgroup -S modresorts && adduser -S modresorts -G modresorts

# Liberty server setup
ENV LIBERTY_HOME=/opt/liberty
ENV PATH="${LIBERTY_HOME}/bin:${PATH}"

# Create a Liberty server named 'defaultServer'
RUN ${LIBERTY_HOME}/bin/server create defaultServer

# Copy the WAR into Liberty's dropins directory
COPY --from=builder /workspace/target/modresorts-2.0.0.war \
     ${LIBERTY_HOME}/usr/servers/defaultServer/dropins/modresorts.war

# Minimal server.xml – enables servlet-4.0 feature and listens on 9080
RUN cat > ${LIBERTY_HOME}/usr/servers/defaultServer/server.xml <<'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<server description="ModResorts Server">
    <featureManager>
        <feature>servlet-4.0</feature>
        <feature>jndi-1.0</feature>
        <feature>cdi-2.0</feature>
    </featureManager>

    <httpEndpoint id="defaultHttpEndpoint"
                  host="*"
                  httpPort="9080"
                  httpsPort="-1"/>

    <webApplication location="modresorts.war" contextRoot="/resorts"/>

    <logging consoleLogLevel="INFO"/>
</server>
EOF

# Ownership
RUN chown -R modresorts:modresorts ${LIBERTY_HOME}/usr/servers/defaultServer

# JVM options for container-aware memory management
ENV JAVA_OPTS="-XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75.0 \
               -XX:+UnlockExperimentalVMOptions \
               -Xms256m \
               -Xmx512m \
               -Djava.awt.headless=true \
               -Dfile.encoding=UTF-8 \
               -Duser.timezone=UTC"

ENV JVM_ARGS="${JAVA_OPTS}"

# Application port
EXPOSE 9080

USER modresorts

# Graceful shutdown via SIGTERM
STOPSIGNAL SIGTERM

CMD ["server", "run", "defaultServer"]
