# ============================================================
# Stage 1: Build
# ============================================================
FROM maven:3.8.6-openjdk-8-slim AS builder

WORKDIR /workspace

# Copy Maven descriptor first for dependency-layer caching
COPY pom.xml .

# Download all dependencies (cached unless pom.xml changes)
RUN mvn dependency:go-offline -B

# Copy the full project source
COPY src ./src
COPY WebContent ./WebContent

# Build the WAR, skipping tests
RUN mvn clean package -DskipTests -B

# ============================================================
# Stage 2: Runtime
# ============================================================
FROM eclipse-temurin:8-jdk

LABEL maintainer="ModResorts Team" \
      application="bookingcomp" \
      version="2.0.0"

# Install Open Liberty runtime
ENV LIBERTY_VERSION=23.0.0.12
RUN apt-get update && apt-get install -y --no-install-recommends \
        ca-certificates \
        unzip \
    && rm -rf /var/lib/apt/lists/*

RUN mkdir -p /opt/ol && \
    apt-get update && apt-get install -y --no-install-recommends curl && \
    curl -fsSL "https://public.dhe.ibm.com/ibmdl/export/pub/software/openliberty/runtime/release/${LIBERTY_VERSION}/openliberty-${LIBERTY_VERSION}.zip" \
         -o /tmp/openliberty.zip && \
    unzip /tmp/openliberty.zip -d /opt/ol && \
    rm /tmp/openliberty.zip && \
    apt-get purge -y curl && \
    apt-get autoremove -y && \
    rm -rf /var/lib/apt/lists/*

ENV LIBERTY_HOME=/opt/ol/wlp
ENV PATH="${LIBERTY_HOME}/bin:${PATH}"

# Create a non-root user
RUN groupadd -r appgroup && useradd -r -g appgroup -d /home/appuser -s /bin/bash appuser

# Create Liberty server
RUN server create modresorts

# Copy server configuration
COPY --chown=appuser:appgroup docker/server.xml ${LIBERTY_HOME}/usr/servers/modresorts/server.xml

# Copy the built WAR from the builder stage
COPY --from=builder --chown=appuser:appgroup \
     /workspace/target/modresorts-2.0.0.war \
     ${LIBERTY_HOME}/usr/servers/modresorts/apps/modresorts.war

# Set ownership
RUN chown -R appuser:appgroup ${LIBERTY_HOME}/usr/servers/modresorts

# JVM options for container awareness
ENV JVM_ARGS="-Xmx512m -Xms256m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
ENV TZ=UTC

# Application port
EXPOSE 9080

USER appuser

CMD ["server", "run", "modresorts"]
