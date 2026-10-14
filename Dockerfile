# ============================================================
# Stage 1: Builder
# ============================================================
FROM maven:3.9.4-eclipse-temurin-8 AS builder

WORKDIR /workspace

# Copy dependency descriptor first for layer caching
COPY pom.xml .

# Download all dependencies (cached layer)
RUN mvn dependency:go-offline -B

# Copy full project source
COPY src ./src
COPY WebContent ./WebContent

# Build the WAR (skip tests for Docker build)
RUN mvn clean package -DskipTests -B

# ============================================================
# Stage 2: Runtime
# ============================================================
FROM amazoncorretto:8

LABEL maintainer="ModResorts Team" \
      application="modresorts" \
      version="2.0.0"

# Install Tomcat 9 (Java EE 7 / Servlet 3.1 compatible)
ENV CATALINA_HOME=/opt/tomcat
ENV PATH=$CATALINA_HOME/bin:$PATH
ENV TOMCAT_VERSION=9.0.89

RUN yum install -y tar gzip && \
    curl -fsSL "https://archive.apache.org/dist/tomcat/tomcat-9/v${TOMCAT_VERSION}/bin/apache-tomcat-${TOMCAT_VERSION}.tar.gz" \
         -o /tmp/tomcat.tar.gz && \
    mkdir -p ${CATALINA_HOME} && \
    tar -xzf /tmp/tomcat.tar.gz -C ${CATALINA_HOME} --strip-components=1 && \
    rm /tmp/tomcat.tar.gz && \
    yum clean all

# Create non-root user for security
RUN groupadd -r appgroup && useradd -r -g appgroup -d /home/appuser -s /sbin/nologin appuser

# Remove default Tomcat webapps
RUN rm -rf ${CATALINA_HOME}/webapps/*

# Copy the built WAR into Tomcat's ROOT context
COPY --from=builder /workspace/target/modresorts-2.0.0.war ${CATALINA_HOME}/webapps/ROOT.war

# Set ownership
RUN chown -R appuser:appgroup ${CATALINA_HOME}

# JVM tuning for containers
ENV JAVA_OPTS="-Xms256m -Xmx512m \
  -XX:+UseContainerSupport \
  -XX:MaxRAMPercentage=75.0 \
  -Djava.security.egd=file:/dev/./urandom \
  -Dfile.encoding=UTF-8 \
  -Duser.timezone=UTC"

ENV TZ=UTC

# Application port
EXPOSE 8080

USER appuser

# Start Tomcat
CMD ["catalina.sh", "run"]
