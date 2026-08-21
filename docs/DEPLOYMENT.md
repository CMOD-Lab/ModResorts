# ModResorts Web Application - AWS ECS Fargate Deployment Guide

## Table of Contents
1. [Overview](#overview)
2. [Prerequisites](#prerequisites)
3. [Project Analysis](#project-analysis)
4. [Local Development with Docker Compose](#local-development-with-docker-compose)
5. [Build and Push Docker Image](#build-and-push-docker-image)
6. [AWS ECS Fargate Prerequisites](#aws-ecs-fargate-prerequisites)
7. [ECS Task Definition Explained](#ecs-task-definition-explained)
8. [ECS Service Configuration](#ecs-service-configuration)
9. [ECS Fargate Deployment Walkthrough](#ecs-fargate-deployment-walkthrough)
10. [ECS-Specific Troubleshooting](#ecs-specific-troubleshooting)
11. [ECS Fargate Scaling and Management](#ecs-fargate-scaling-and-management)
12. [Configuration Management](#configuration-management)
13. [Security Considerations](#security-considerations)
14. [Java-Specific Notes](#java-specific-notes)

---

## Overview

**ModResorts** is a Java EE web application (WAR) built with Maven, targeting Java 8 compatibility. It is a resort booking and management web application that exposes servlet-based endpoints for weather, availability checking, and user management.

| Property | Value |
|---|---|
| Application Name | ModResorts |
| Artifact ID | modresorts |
| Version | 2.0.0 |
| Package Type | WAR |
| Java Version | 8 (compiled) / 21 (runtime) |
| Build Tool | Maven |
| Servlet Container | Apache Tomcat 10.1 |
| Application Port | 9080 |
| Context Root | /resorts |
| Health Endpoint | GET /resorts/health |
| Base Image | amazoncorretto:21-alpine-full |

---

## Prerequisites

### Local Development
- **Docker** 20.10+ and **Docker Compose** v2+
- **Java 8+** (for local builds outside Docker)
- **Maven 3.8+** (for local builds outside Docker)
- **AWS CLI v2** (for ECR push and ECS deployment)

### AWS Requirements
- AWS Account with appropriate IAM permissions
- AWS CLI configured: `aws configure`
- VPC with at least 2 subnets (for high availability)
- Security Group allowing inbound TCP on port 9080 (or 80/443 via ALB)
- IAM roles: `ecsTaskExecutionRole` and `ecsTaskRole`

---

## Project Analysis

### Technology Stack
- **Framework**: Java EE (Servlet API 3.1) — NOT Spring Boot
- **Build Tool**: Maven (pom.xml)
- **Packaging**: WAR deployed to Apache Tomcat 10.1
- **Java Compatibility**: Source/Target Java 8, Runtime Java 21 (Amazon Corretto)
- **Key Dependencies**:
  - `javax:javaee-api:7.0` (provided — supplied by Tomcat)
  - `com.google.code.gson:gson:2.10.1`
  - `org.apache.logging.log4j:log4j-core:2.14.1`
  - `org.springframework:spring-webmvc:5.3.17`
  - `com.fasterxml.jackson.core:jackson-databind:2.9.10`

### Application Endpoints
| Endpoint | Servlet | Description |
|---|---|---|
| `GET /resorts/health` | HealthCheckServlet | Health check (returns JSON `{"status":"UP"}`) |
| `GET /resorts/weather` | WeatherServlet | Weather data |
| `GET /resorts/availability` | AvailabilityCheckerServlet | Room availability |
| `GET /resorts/welcome` | WelcomeServlet | Welcome page |
| `GET /resorts/upper` | UpperServlet | Text transformation |

---

## Local Development with Docker Compose

### Quick Start

```bash
# Clone the repository
git clone <repository-url>
cd ModResortsWeb

# Build and start the application
docker compose up --build

# Access the application
open http://localhost:9080/resorts/
```

### Environment Variables

Create a `.env` file in the project root:

```env
# Application
APP_ENV=docker
TZ=UTC

# Database (if applicable)
DB_HOST=your-db-host
DB_PORT=5432
DB_NAME=modresorts
DB_USER=modresorts
DB_PASSWORD=your-password

# Weather API
WEATHER_API_KEY=your-api-key
WEATHER_API_URL=https://api.weather.example.com

# JVM Settings
JAVA_OPTS=-Xmx512m -Xms256m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0
```

### Docker Compose Commands

```bash
# Start in background
docker compose up -d

# View logs
docker compose logs -f modresorts

# Stop
docker compose down

# Rebuild after code changes
docker compose up --build --force-recreate

# Check health
curl http://localhost:9080/resorts/health
```

---

## Build and Push Docker Image

### Linux/macOS

```bash
# Make script executable
chmod +x scripts/build-push.sh

# Run from project root
./scripts/build-push.sh
```

The script will prompt you to:
1. Choose registry (AWS ECR or Docker Hub)
2. Enter registry credentials/details
3. Enter image tag (defaults to `latest`)

### Windows

```cmd
scripts\build-push.bat
```

### Manual Build

```bash
# Build image
docker build -t modresorts:latest .

# Tag for ECR
docker tag modresorts:latest 123456789.dkr.ecr.us-east-1.amazonaws.com/modresorts:latest

# Login to ECR
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin 123456789.dkr.ecr.us-east-1.amazonaws.com

# Push
docker push 123456789.dkr.ecr.us-east-1.amazonaws.com/modresorts:latest
```

---

## AWS ECS Fargate Prerequisites

### 1. IAM Roles

#### ECS Task Execution Role
This role allows ECS to pull images from ECR and write logs to CloudWatch.

```bash
# Create the execution role
aws iam create-role \
  --role-name ecsTaskExecutionRole \
  --assume-role-policy-document '{
    "Version": "2012-10-17",
    "Statement": [{
      "Effect": "Allow",
      "Principal": {"Service": "ecs-tasks.amazonaws.com"},
      "Action": "sts:AssumeRole"
    }]
  }'

# Attach the managed policy
aws iam attach-role-policy \
  --role-name ecsTaskExecutionRole \
  --policy-arn arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy
```

#### ECS Task Role (for application permissions)
```bash
aws iam create-role \
  --role-name ecsTaskRole \
  --assume-role-policy-document '{
    "Version": "2012-10-17",
    "Statement": [{
      "Effect": "Allow",
      "Principal": {"Service": "ecs-tasks.amazonaws.com"},
      "Action": "sts:AssumeRole"
    }]
  }'
```

### 2. VPC and Networking

```bash
# List available VPCs
aws ec2 describe-vpcs --query "Vpcs[*].{VpcId:VpcId,CidrBlock:CidrBlock,IsDefault:IsDefault}" --output table

# List subnets
aws ec2 describe-subnets --query "Subnets[*].{SubnetId:SubnetId,VpcId:VpcId,AvailabilityZone:AvailabilityZone,CidrBlock:CidrBlock}" --output table

# Create security group for ModResorts
aws ec2 create-security-group \
  --group-name modresorts-sg \
  --description "Security group for ModResorts ECS tasks" \
  --vpc-id vpc-xxxxxxxx

# Allow inbound on port 9080
aws ec2 authorize-security-group-ingress \
  --group-id sg-xxxxxxxx \
  --protocol tcp \
  --port 9080 \
  --cidr 0.0.0.0/0

# Allow inbound on port 80 (for ALB)
aws ec2 authorize-security-group-ingress \
  --group-id sg-xxxxxxxx \
  --protocol tcp \
  --port 80 \
  --cidr 0.0.0.0/0
```

### 3. ECR Repository

```bash
# Create ECR repository
aws ecr create-repository \
  --repository-name modresorts \
  --region us-east-1

# Get repository URI
aws ecr describe-repositories \
  --repository-names modresorts \
  --query "repositories[0].repositoryUri" \
  --output text
```

### 4. CloudWatch Log Group

```bash
aws logs create-log-group \
  --log-group-name /ecs/modresorts \
  --region us-east-1

# Set retention (optional)
aws logs put-retention-policy \
  --log-group-name /ecs/modresorts \
  --retention-in-days 30
```

---

## ECS Task Definition Explained

The task definition (`ecs/task-definition.json`) configures how the ModResorts container runs on Fargate:

### Key Fields

| Field | Value | Explanation |
|---|---|---|
| `family` | `modresorts-task` | Task definition family name |
| `requiresCompatibilities` | `["FARGATE"]` | Must be FARGATE for serverless containers |
| `networkMode` | `awsvpc` | Required for Fargate; each task gets its own ENI |
| `cpu` | `"512"` | 0.5 vCPU (valid Fargate unit) |
| `memory` | `"1024"` | 1 GB RAM (valid for 512 CPU) |
| `executionRoleArn` | `ecsTaskExecutionRole` | Allows ECS to pull images and write logs |
| `taskRoleArn` | `ecsTaskRole` | Permissions for the application itself |

### Valid Fargate CPU/Memory Combinations

| CPU | Valid Memory Options |
|---|---|
| 256 (.25 vCPU) | 512, 1024, 2048 MB |
| **512 (.5 vCPU)** | **1024, 2048, 3072, 4096 MB** ← Used |
| 1024 (1 vCPU) | 2048–8192 MB |
| 2048 (2 vCPU) | 4096–16384 MB |
| 4096 (4 vCPU) | 8192–30720 MB |

### Container Definition

```json
{
  "name": "modresorts",
  "image": "{{IMAGE_URI}}",
  "essential": true,
  "portMappings": [{"containerPort": 9080, "protocol": "tcp"}],
  "environment": [
    {"name": "JAVA_OPTS", "value": "-Xmx512m -Xms256m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"},
    {"name": "TZ", "value": "UTC"}
  ],
  "logConfiguration": {
    "logDriver": "awslogs",
    "options": {
      "awslogs-group": "/ecs/modresorts",
      "awslogs-region": "us-east-1",
      "awslogs-stream-prefix": "ecs"
    }
  }
}
```

---

## ECS Service Configuration

The service definition (`ecs/service-definition.json`) manages how many tasks run and how they are networked:

| Field | Value | Explanation |
|---|---|---|
| `launchType` | `FARGATE` | Serverless container execution |
| `desiredCount` | `2` | Run 2 tasks for high availability |
| `networkMode` | `awsvpc` | Each task gets its own IP |
| `assignPublicIp` | `ENABLED` | Tasks get public IPs (use DISABLED with NAT Gateway for private) |
| `maximumPercent` | `200` | Allow up to 4 tasks during rolling deploy |
| `minimumHealthyPercent` | `50` | Keep at least 1 task running during deploy |

---

## ECS Fargate Deployment Walkthrough

### Step 1: Build and Push Image

```bash
# Linux/macOS
chmod +x scripts/build-push.sh
./scripts/build-push.sh

# Windows
scripts\build-push.bat
```

Note the full image URI output (e.g., `123456789.dkr.ecr.us-east-1.amazonaws.com/modresorts:latest`).

### Step 2: Deploy to ECS

```bash
# Linux/macOS
chmod +x scripts/deploy-image.sh
./scripts/deploy-image.sh

# Windows
scripts\deploy-image.bat
```

The deployment script will prompt for:
- AWS Region
- ECS Cluster name
- VPC ID
- Subnet IDs (comma-separated)
- Security Group ID
- ECR Image URI
- Whether to create an Application Load Balancer

### Step 3: Verify Deployment

```bash
# Check service status
aws ecs describe-services \
  --cluster modresorts-cluster \
  --services modresorts-service \
  --region us-east-1

# List running tasks
aws ecs list-tasks \
  --cluster modresorts-cluster \
  --service-name modresorts-service \
  --region us-east-1

# View application logs
aws logs tail /ecs/modresorts --follow --region us-east-1
```

### Step 4: Access the Application

If using ALB:
```
http://<alb-dns-name>/resorts/
http://<alb-dns-name>/resorts/health
```

If accessing tasks directly (public IP):
```bash
# Get task public IP
TASK_ARN=$(aws ecs list-tasks --cluster modresorts-cluster --service-name modresorts-service --query "taskArns[0]" --output text)
ENI_ID=$(aws ecs describe-tasks --cluster modresorts-cluster --tasks $TASK_ARN --query "tasks[0].attachments[0].details[?name=='networkInterfaceId'].value" --output text)
PUBLIC_IP=$(aws ec2 describe-network-interfaces --network-interface-ids $ENI_ID --query "NetworkInterfaces[0].Association.PublicIp" --output text)
echo "http://$PUBLIC_IP:9080/resorts/"
```

---

## ECS-Specific Troubleshooting

### Task Fails to Start

```bash
# Check stopped task reason
aws ecs describe-tasks \
  --cluster modresorts-cluster \
  --tasks <task-arn> \
  --query "tasks[0].{Status:lastStatus,StoppedReason:stoppedReason,Containers:containers[*].{Name:name,Reason:reason,ExitCode:exitCode}}" \
  --output json
```

**Common causes:**
- `CannotPullContainerError`: ECR login failed or image URI incorrect → Check `executionRoleArn` has ECR pull permissions
- `OutOfMemoryError`: Increase task memory in task definition
- `ResourceInitializationError`: Check VPC/subnet/security group configuration

### Container Exits Immediately

```bash
# View container logs
aws logs get-log-events \
  --log-group-name /ecs/modresorts \
  --log-stream-name ecs/modresorts/<task-id> \
  --region us-east-1
```

**Common causes:**
- Tomcat startup failure → Check JAVA_OPTS memory settings
- WAR deployment error → Verify WAR file is valid
- Port conflict → Ensure port 9080 is not blocked

### Network Connectivity Issues

```bash
# Verify security group allows inbound on port 9080
aws ec2 describe-security-groups \
  --group-ids sg-xxxxxxxx \
  --query "SecurityGroups[0].IpPermissions"
```

**Checklist:**
- Security group allows inbound TCP 9080 (or 80 via ALB)
- Subnets have route to internet (via IGW or NAT Gateway)
- `assignPublicIp: ENABLED` if using public subnets

### CPU/Memory Errors

```
InvalidParameterException: Invalid CPU or memory value specified
```

**Fix**: Use valid Fargate combinations. Default: `cpu: "512"`, `memory: "1024"`.

### Service Not Stabilizing

```bash
# Check service events
aws ecs describe-services \
  --cluster modresorts-cluster \
  --services modresorts-service \
  --query "services[0].events[0:5]"
```

---

## ECS Fargate Scaling and Management

### Manual Scaling

```bash
# Scale up to 4 tasks
aws ecs update-service \
  --cluster modresorts-cluster \
  --service modresorts-service \
  --desired-count 4 \
  --region us-east-1
```

### Auto Scaling

```bash
# Register scalable target
aws application-autoscaling register-scalable-target \
  --service-namespace ecs \
  --resource-id service/modresorts-cluster/modresorts-service \
  --scalable-dimension ecs:service:DesiredCount \
  --min-capacity 2 \
  --max-capacity 10

# Create CPU-based scaling policy
aws application-autoscaling put-scaling-policy \
  --service-namespace ecs \
  --resource-id service/modresorts-cluster/modresorts-service \
  --scalable-dimension ecs:service:DesiredCount \
  --policy-name modresorts-cpu-scaling \
  --policy-type TargetTrackingScaling \
  --target-tracking-scaling-policy-configuration '{
    "TargetValue": 70.0,
    "PredefinedMetricSpecification": {
      "PredefinedMetricType": "ECSServiceAverageCPUUtilization"
    },
    "ScaleInCooldown": 300,
    "ScaleOutCooldown": 60
  }'
```

### Blue/Green Deployment

For zero-downtime deployments, use AWS CodeDeploy with ECS:

```bash
# Update service with new image (rolling update)
aws ecs update-service \
  --cluster modresorts-cluster \
  --service modresorts-service \
  --task-definition modresorts-task:<new-revision> \
  --deployment-configuration "maximumPercent=200,minimumHealthyPercent=100" \
  --region us-east-1
```

### Force New Deployment

```bash
aws ecs update-service \
  --cluster modresorts-cluster \
  --service modresorts-service \
  --force-new-deployment \
  --region us-east-1
```

---

## Configuration Management

### Environment Variables

All sensitive configuration should use AWS Secrets Manager or SSM Parameter Store:

```bash
# Store database credentials
aws secretsmanager create-secret \
  --name modresorts/db-credentials \
  --secret-string '{"username":"modresorts","password":"your-password"}'

# Store API key
aws secretsmanager create-secret \
  --name modresorts/weather-api-key \
  --secret-string 'your-api-key'
```

Reference secrets in task definition:
```json
"secrets": [
  {
    "name": "DB_PASSWORD",
    "valueFrom": "arn:aws:secretsmanager:us-east-1:123456789:secret:modresorts/db-credentials:password::"
  }
]
```

### Application Profiles

The application uses `APP_ENV` environment variable to select configuration:
- `docker` — Local Docker Compose
- `production` — AWS ECS Fargate

---

## Security Considerations

### Container Security
- ✅ Non-root user (`appuser`) runs the application
- ✅ No unnecessary packages installed in runtime image
- ✅ Multi-stage build minimizes attack surface
- ✅ Explicit base image version pinned (`amazoncorretto:21-alpine-full`)

### Network Security
- Use private subnets with NAT Gateway for production (set `assignPublicIp: DISABLED`)
- Restrict security group inbound rules to ALB security group only
- Enable VPC Flow Logs for network monitoring

### IAM Security
- Apply least-privilege IAM policies to `ecsTaskRole`
- Use Secrets Manager for all sensitive values (never hardcode in task definitions)
- Enable CloudTrail for API audit logging

### Image Security
- Regularly scan ECR images: `aws ecr start-image-scan --repository-name modresorts --image-id imageTag=latest`
- Enable ECR image scanning on push:
  ```bash
  aws ecr put-image-scanning-configuration \
    --repository-name modresorts \
    --image-scanning-configuration scanOnPush=true
  ```

---

## Java-Specific Notes

### JVM Configuration

The following JVM flags are set via `JAVA_OPTS`:

| Flag | Purpose |
|---|---|
| `-Xmx512m` | Maximum heap size (512 MB) |
| `-Xms256m` | Initial heap size (256 MB) |
| `-XX:+UseContainerSupport` | Enable container-aware JVM (Java 8u191+) |
| `-XX:MaxRAMPercentage=75.0` | Use 75% of container RAM for heap |
| `-XX:+UnlockExperimentalVMOptions` | Enable experimental JVM features |
| `-Djava.security.egd=file:/dev/./urandom` | Faster SecureRandom initialization |

### Tomcat Configuration

- Context root: `/resorts` (configured via WAR file name `resorts.war`)
- Port: `9080` (configured in `server.xml`)
- Session timeout: 30 minutes (from `web.xml`)

### Increasing Memory for Production

If the application requires more memory, update the task definition:

```json
{
  "cpu": "1024",
  "memory": "2048",
  "environment": [
    {"name": "JAVA_OPTS", "value": "-Xmx1536m -Xms512m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"}
  ]
}
```

### Log4j Configuration

The application uses Log4j 2.14.1. Ensure log output goes to stdout/stderr for CloudWatch capture:

```xml
<!-- src/main/resources/log4j2.xml -->
<Configuration>
  <Appenders>
    <Console name="Console" target="SYSTEM_OUT">
      <PatternLayout pattern="%d{ISO8601} [%t] %-5level %logger{36} - %msg%n"/>
    </Console>
  </Appenders>
  <Loggers>
    <Root level="INFO">
      <AppenderRef ref="Console"/>
    </Root>
  </Loggers>
</Configuration>
```

### Health Check

The application exposes a health endpoint at `GET /resorts/health` which returns:
```json
{"status":"UP","application":"ModResorts"}
```

This endpoint is used by:
- ALB target group health checks
- ECS service health monitoring
- Docker Compose `healthcheck`
