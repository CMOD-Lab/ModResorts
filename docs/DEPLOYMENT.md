# ModResorts – AWS ECS Fargate Deployment Guide

## Table of Contents
1. [Overview](#overview)
2. [Prerequisites](#prerequisites)
3. [Project Structure](#project-structure)
4. [Local Development with Docker Compose](#local-development-with-docker-compose)
5. [Build & Push Docker Image](#build--push-docker-image)
6. [AWS ECS Fargate Prerequisites](#aws-ecs-fargate-prerequisites)
7. [ECS Task Definition Explained](#ecs-task-definition-explained)
8. [ECS Service Configuration](#ecs-service-configuration)
9. [ECS Fargate Deployment Walkthrough](#ecs-fargate-deployment-walkthrough)
10. [ECS-Specific Troubleshooting](#ecs-specific-troubleshooting)
11. [Scaling and Management](#scaling-and-management)
12. [Configuration Management](#configuration-management)
13. [Security Considerations](#security-considerations)
14. [Java-Specific Notes](#java-specific-notes)

---

## Overview

**ModResorts** is a Java EE 7 web application (WAR) that provides resort booking, weather information, and availability checking services. It is packaged as a WAR file and deployed on **Open Liberty** (a lightweight Jakarta EE / Servlet container).

| Property | Value |
|---|---|
| Build Tool | Maven 3.x |
| Java Version | 8 (compiled) / 21 (runtime via Amazon Corretto) |
| Package Type | WAR |
| Runtime | Open Liberty 23.x |
| Application Port | **9080** |
| Context Root | `/resorts` |
| Health Endpoint | `GET /resorts/health` |
| Base Image | `amazoncorretto:21-alpine-full` |

---

## Prerequisites

### Local Development
- Docker Desktop 24+ (or Docker Engine 24+)
- Docker Compose v2+
- Java 8+ (for local IDE development only)
- Maven 3.8+ (for local builds only)

### AWS Deployment
- AWS CLI v2 configured (`aws configure`)
- IAM permissions: ECS, ECR, ELB, CloudWatch Logs, IAM (read)
- Python 3 (optional – used by deploy script for JSON patching)

---

## Project Structure

```
ModsResort/
├── Dockerfile                  # Multi-stage build (Maven builder + Corretto runtime)
├── docker-compose.yml          # Local development compose file
├── .dockerignore               # Excludes build artifacts and wrapper scripts
├── pom.xml                     # Maven build descriptor
├── src/
│   └── main/
│       ├── java/com/acme/modres/   # Application source code
│       └── resources/              # ops.json, reservations.json
├── WebContent/                 # Static assets, JSPs, WEB-INF
├── ecs/
│   ├── task-definition.json    # ECS Fargate task definition
│   └── service-definition.json # ECS service definition
├── scripts/
│   ├── build-push.sh           # Linux/macOS: build & push to ECR or Docker Hub
│   ├── build-push.bat          # Windows: build & push
│   ├── deploy-image.sh         # Linux/macOS: deploy to ECS Fargate
│   └── deploy-image.bat        # Windows: deploy to ECS Fargate
└── docs/
    └── DEPLOYMENT.md           # This file
```

---

## Local Development with Docker Compose

### 1. Build and start the application

```bash
# From the repository root
docker compose up --build
```

### 2. Access the application

| URL | Description |
|---|---|
| http://localhost:9080/resorts/ | Application home |
| http://localhost:9080/resorts/health | Health check endpoint |
| http://localhost:9080/resorts/weather?selectedCity=Paris | Weather API |
| http://localhost:9080/resorts/availability?date=2024-08-15 | Availability API |

### 3. Environment variables (optional)

Create a `.env` file in the project root:

```env
WEATHER_API_KEY=your_wunderground_api_key
SERVER_DISPLAY_NAME=ModResorts-Dev
SERVER_FULL_NAME=ModResorts/defaultServer
```

### 4. Stop the application

```bash
docker compose down
```

---

## Build & Push Docker Image

### Linux / macOS

```bash
chmod +x scripts/build-push.sh
./scripts/build-push.sh
```

### Windows

```cmd
scripts\build-push.bat
```

The script will prompt you to:
1. Choose registry: **AWS ECR** or **Docker Hub**
2. Enter registry credentials / details
3. Enter an image tag (defaults to `latest`)

The image name is automatically sanitised to lowercase with hyphens: `modresorts`.

---

## AWS ECS Fargate Prerequisites

### 1. AWS CLI

```bash
aws configure
# Enter: Access Key ID, Secret Access Key, Region, Output format (json)
```

### 2. VPC and Networking

You need:
- A VPC with at least **2 public or private subnets** in different AZs
- A **Security Group** that allows:
  - Inbound TCP on port **9080** (from ALB or 0.0.0.0/0 for testing)
  - Outbound all traffic (for ECR image pull, CloudWatch logs)

```bash
# Example: create a security group
aws ec2 create-security-group \
  --group-name modresorts-sg \
  --description "ModResorts ECS Security Group" \
  --vpc-id vpc-xxxxxxxxx

aws ec2 authorize-security-group-ingress \
  --group-id sg-xxxxxxxxx \
  --protocol tcp --port 9080 --cidr 0.0.0.0/0
```

### 3. IAM Roles

#### ecsTaskExecutionRole (required)
Allows ECS to pull images from ECR and write logs to CloudWatch.

```bash
# Create the role (if it doesn't exist)
aws iam create-role \
  --role-name ecsTaskExecutionRole \
  --assume-role-policy-document '{
    "Version":"2012-10-17",
    "Statement":[{
      "Effect":"Allow",
      "Principal":{"Service":"ecs-tasks.amazonaws.com"},
      "Action":"sts:AssumeRole"
    }]
  }'

aws iam attach-role-policy \
  --role-name ecsTaskExecutionRole \
  --policy-arn arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy
```

#### ecsTaskRole (optional)
Grants the running container permissions to call AWS services (e.g., S3, Secrets Manager).

```bash
aws iam create-role \
  --role-name ecsTaskRole \
  --assume-role-policy-document '{
    "Version":"2012-10-17",
    "Statement":[{
      "Effect":"Allow",
      "Principal":{"Service":"ecs-tasks.amazonaws.com"},
      "Action":"sts:AssumeRole"
    }]
  }'
```

### 4. ECR Repository

```bash
aws ecr create-repository \
  --repository-name modresorts \
  --region us-east-1
```

### 5. CloudWatch Log Group

```bash
aws logs create-log-group \
  --log-group-name /ecs/modresorts \
  --region us-east-1
```

---

## ECS Task Definition Explained

File: `ecs/task-definition.json`

| Field | Value | Notes |
|---|---|---|
| `family` | `modresorts-task` | Task definition family name |
| `requiresCompatibilities` | `["FARGATE"]` | Fargate launch type |
| `networkMode` | `awsvpc` | Required for Fargate |
| `cpu` | `"512"` | 0.5 vCPU |
| `memory` | `"1024"` | 1 GB RAM |
| `executionRoleArn` | `ecsTaskExecutionRole` | ECR pull + CloudWatch logs |
| `containerPort` | `9080` | Open Liberty HTTP port |
| `logDriver` | `awslogs` | CloudWatch Logs |

### Valid Fargate CPU/Memory Combinations

| CPU | Memory Options |
|---|---|
| 256 (.25 vCPU) | 512, 1024, 2048 MB |
| **512 (.5 vCPU)** | **1024**, 2048, 3072, 4096 MB |
| 1024 (1 vCPU) | 2048–8192 MB |
| 2048 (2 vCPU) | 4096–16384 MB |

---

## ECS Service Configuration

File: `ecs/service-definition.json`

| Field | Value | Notes |
|---|---|---|
| `serviceName` | `modresorts-service` | ECS service name |
| `launchType` | `FARGATE` | Serverless containers |
| `desiredCount` | `2` | Two running tasks for HA |
| `networkMode` | `awsvpc` | Each task gets its own ENI |
| `assignPublicIp` | `ENABLED` | Required for public subnets |
| `maximumPercent` | `200` | Rolling deploy: up to 2x tasks |
| `minimumHealthyPercent` | `50` | At least 1 task always running |

---

## ECS Fargate Deployment Walkthrough

### Step 1: Build and push the image

```bash
./scripts/build-push.sh
# Select: 1 (AWS ECR)
# Enter: region, account ID, repo name, tag
```

### Step 2: Deploy to ECS

```bash
chmod +x scripts/deploy-image.sh
./scripts/deploy-image.sh
```

You will be prompted for:
- AWS Region
- ECS Cluster name (created automatically if it doesn't exist)
- ECR Image URI (e.g., `123456789012.dkr.ecr.us-east-1.amazonaws.com/modresorts:latest`)
- Subnet IDs (comma-separated)
- Security Group ID
- Whether to create an Application Load Balancer

### Step 3: Verify deployment

```bash
# Check service status
aws ecs describe-services \
  --cluster my-cluster \
  --services modresorts-service \
  --region us-east-1

# List running tasks
aws ecs list-tasks \
  --cluster my-cluster \
  --service-name modresorts-service \
  --region us-east-1

# Tail application logs
aws logs tail /ecs/modresorts --follow --region us-east-1
```

### Step 4: Access the application

- **With ALB**: `http://<ALB-DNS>/resorts/`
- **Direct task IP**: `http://<TASK-PUBLIC-IP>:9080/resorts/`

---

## ECS-Specific Troubleshooting

### Task fails to start

```bash
# Describe stopped tasks for failure reason
aws ecs describe-tasks \
  --cluster my-cluster \
  --tasks <TASK_ARN> \
  --region us-east-1 \
  --query "tasks[0].{Status:lastStatus,StopCode:stopCode,StopReason:stoppedReason}"
```

Common causes:
- **ImagePullFailure**: ECR repository doesn't exist or `ecsTaskExecutionRole` lacks ECR permissions
- **ResourceInitializationError**: Fargate agent can't reach ECR/CloudWatch endpoints (check VPC endpoints or NAT gateway)
- **OutOfMemory**: Increase `memory` in task definition (use valid Fargate combinations)

### Container exits immediately

```bash
# Check CloudWatch logs
aws logs get-log-events \
  --log-group-name /ecs/modresorts \
  --log-stream-name ecs/modresorts/<TASK_ID> \
  --region us-east-1
```

### Health check failures

The application health endpoint is `GET /resorts/health`. Verify:
1. Security group allows inbound on port 9080
2. ALB target group health check path is `/resorts/health`
3. Open Liberty started successfully (check logs)

### Network connectivity issues

```bash
# Verify task has public IP (for public subnets)
aws ecs describe-tasks \
  --cluster my-cluster \
  --tasks <TASK_ARN> \
  --region us-east-1 \
  --query "tasks[0].attachments[0].details"
```

---

## Scaling and Management

### Manual scaling

```bash
aws ecs update-service \
  --cluster my-cluster \
  --service modresorts-service \
  --desired-count 4 \
  --region us-east-1
```

### Auto Scaling

```bash
# Register scalable target
aws application-autoscaling register-scalable-target \
  --service-namespace ecs \
  --resource-id service/my-cluster/modresorts-service \
  --scalable-dimension ecs:service:DesiredCount \
  --min-capacity 2 \
  --max-capacity 10

# CPU-based scaling policy
aws application-autoscaling put-scaling-policy \
  --service-namespace ecs \
  --resource-id service/my-cluster/modresorts-service \
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

### Blue/Green Deployment (CodeDeploy)

For zero-downtime deployments, configure AWS CodeDeploy with ECS:
1. Create a CodeDeploy application with `ECS` compute platform
2. Create a deployment group linked to the ECS service
3. Use `appspec.yaml` to define the deployment lifecycle

---

## Configuration Management

### Environment Variables

| Variable | Description | Default |
|---|---|---|
| `WEATHER_API_KEY` | Wunderground API key for real-time weather | (empty – uses default data) |
| `JNDI_FACTORY` | JNDI initial context factory | (empty) |
| `JNDI_PROVIDER_URL` | JNDI provider URL | (empty) |
| `SERVER_DISPLAY_NAME` | Server display name | `ModResorts` |
| `SERVER_FULL_NAME` | Server full name | `ModResorts/defaultServer` |
| `JAVA_OPTS` | JVM options | See Dockerfile |
| `TZ` | Timezone | `UTC` |

### AWS Secrets Manager (recommended for production)

```bash
# Store the weather API key
aws secretsmanager create-secret \
  --name modresorts/weather-api-key \
  --secret-string "your-api-key-here"
```

Reference in task definition:
```json
"secrets": [
  {
    "name": "WEATHER_API_KEY",
    "valueFrom": "arn:aws:secretsmanager:us-east-1:123456789012:secret:modresorts/weather-api-key"
  }
]
```

---

## Security Considerations

1. **Non-root container**: The application runs as the `modresorts` user (UID non-root)
2. **No sensitive data in images**: Use AWS Secrets Manager or SSM Parameter Store
3. **Security Groups**: Restrict inbound to ALB only (not 0.0.0.0/0) in production
4. **ECR image scanning**: Enable ECR image scanning on push
5. **Task role least privilege**: Grant only required AWS permissions to `ecsTaskRole`
6. **VPC endpoints**: Use VPC endpoints for ECR and CloudWatch to avoid public internet traffic
7. **HTTPS**: Terminate TLS at the ALB (ACM certificate) – do not expose port 9080 directly

```bash
# Enable ECR image scanning
aws ecr put-image-scanning-configuration \
  --repository-name modresorts \
  --image-scanning-configuration scanOnPush=true \
  --region us-east-1
```

---

## Java-Specific Notes

### JVM Memory Settings

The container is configured with:
```
-XX:+UseContainerSupport      # JVM respects cgroup memory limits
-XX:MaxRAMPercentage=75.0     # Use 75% of container memory for heap
-Xms256m                      # Initial heap size
-Xmx512m                      # Maximum heap size
-Dfile.encoding=UTF-8
-Duser.timezone=UTC
```

With 1024 MB Fargate memory:
- JVM heap max: ~768 MB (75%)
- Non-heap (Metaspace, threads, etc.): ~256 MB

### Open Liberty Configuration

The application runs on **Open Liberty** with:
- `servlet-4.0` feature (Java EE 7 Servlet 3.1)
- `jndi-1.0` feature (JNDI support)
- `cdi-2.0` feature (CDI injection)
- HTTP on port **9080** (HTTPS disabled)
- Context root: `/resorts`

### Increasing Resources

If the application needs more memory (e.g., under heavy load):

```json
// ecs/task-definition.json
"cpu": "1024",
"memory": "2048"
```

Update `JAVA_OPTS` accordingly:
```
-Xms512m -Xmx1536m -XX:MaxRAMPercentage=75.0
```

### Monitoring with CloudWatch

Application logs are streamed to CloudWatch Logs at `/ecs/modresorts`.

Create a CloudWatch dashboard:
```bash
aws cloudwatch put-dashboard \
  --dashboard-name ModResorts \
  --dashboard-body file://cloudwatch-dashboard.json
```

Key metrics to monitor:
- `ECS/ContainerInsights` – CPUUtilization, MemoryUtilization
- `ApplicationELB` – RequestCount, TargetResponseTime, HTTPCode_Target_5XX_Count
