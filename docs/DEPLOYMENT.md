# BookingServices - AWS ECS Fargate Deployment Guide

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

**BookingServices** is a Java EE 7 web application (WAR packaging) built with Maven and Java 8. It provides resort booking, weather information, and availability checking services. The application is containerized using Apache Tomcat 9 as the servlet container and deployed to **AWS ECS Fargate** for serverless container orchestration.

| Property | Value |
|---|---|
| Application Name | BookingServices |
| Artifact ID | modresorts |
| Version | 2.0.0 |
| Java Version | 8 (Amazon Corretto 8) |
| Build Tool | Maven |
| Packaging | WAR |
| Servlet Container | Apache Tomcat 9 |
| Application Port | 8080 |
| Context Root | `/resorts` |
| Health Endpoint | `GET /resorts/health` |
| Target Platform | AWS ECS Fargate |

---

## Prerequisites

### Local Development
- **Docker** 20.10+ and **Docker Compose** 2.x
- **Java 8** (Amazon Corretto 8 or OpenJDK 8)
- **Maven** 3.8+
- **Git**

### AWS Deployment
- **AWS CLI** v2 configured with appropriate credentials (`aws configure`)
- **AWS Account** with permissions for:
  - ECS (create clusters, task definitions, services)
  - ECR (create repositories, push images)
  - IAM (create/assign roles)
  - CloudWatch Logs (create log groups)
  - EC2 (VPC, subnets, security groups)
  - Elastic Load Balancing (optional, for ALB)

---

## Project Analysis

### Technology Stack
- **Framework**: Java EE 7 Servlet API (javax.servlet)
- **Build**: Maven 3.x with `maven-war-plugin`
- **Runtime**: Apache Tomcat 9 on Amazon Corretto 8
- **Key Dependencies**:
  - `javax:javaee-api:7.0` (provided scope)
  - `com.google.code.gson:gson:2.10.1`
  - `org.springframework:spring-webmvc:5.3.17`
  - `com.fasterxml.jackson.core:jackson-databind:2.9.10`
  - `org.apache.logging.log4j:log4j-core:2.14.1`

### Application Endpoints
| Endpoint | Servlet | Description |
|---|---|---|
| `GET /resorts/health` | HealthServlet | Health check (returns `{"status":"UP"}`) |
| `GET /resorts/weather` | WeatherServlet | Weather data for resort cities |
| `GET /resorts/availability` | AvailabilityCheckerServlet | Booking availability check |
| `GET /resorts/welcome` | WelcomeServlet | Welcome message |
| `GET /resorts/upper` | UpperServlet | Text transformation |

### Environment Variables
| Variable | Description | Default |
|---|---|---|
| `WEATHER_API_KEY` | Weather Underground API key | (empty - uses mock data) |
| `SERVER_DISPLAY_NAME` | Server display name | `bookingservices` |
| `SERVER_FULL_NAME` | Full server name | `default-cell/default-node/bookingservices` |
| `JNDI_PROVIDER_URL` | JNDI provider URL | (empty) |
| `JAVA_OPTS` | JVM options | See Dockerfile |
| `TZ` | Timezone | `UTC` |

---

## Local Development with Docker Compose

### 1. Build and Start

```bash
# From project root
docker compose up --build
```

### 2. Verify Application

```bash
# Health check
curl http://localhost:8080/resorts/health

# Expected response:
# {"status":"UP","application":"BookingServices"}

# Weather endpoint (uses mock data without API key)
curl "http://localhost:8080/resorts/weather?selectedCity=Paris"

# Availability check
curl "http://localhost:8080/resorts/availability?date=12/25/2025"
```

### 3. Stop Application

```bash
docker compose down
```

### 4. View Logs

```bash
docker compose logs -f bookingservices
```

### 5. Environment Variables for Local Development

Create a `.env` file in the project root:

```env
WEATHER_API_KEY=your_weather_api_key_here
SERVER_DISPLAY_NAME=bookingservices-local
TZ=UTC
```

---

## Build and Push Docker Image

### Linux/macOS

```bash
chmod +x scripts/build-push.sh
./scripts/build-push.sh
```

The script will prompt you to:
1. Enter an image tag (default: `latest`)
2. Select registry type (AWS ECR or Docker Hub)
3. Provide registry credentials

### Windows

```cmd
scripts\build-push.bat
```

### Manual Build

```bash
# Build image
docker build -t bookingservices:latest .

# Tag for ECR
docker tag bookingservices:latest \
  123456789012.dkr.ecr.us-east-1.amazonaws.com/bookingservices:latest

# Login to ECR
aws ecr get-login-password --region us-east-1 | \
  docker login --username AWS --password-stdin \
  123456789012.dkr.ecr.us-east-1.amazonaws.com

# Push
docker push 123456789012.dkr.ecr.us-east-1.amazonaws.com/bookingservices:latest
```

---

## AWS ECS Fargate Prerequisites

### 1. IAM Roles

#### ECS Task Execution Role
This role allows ECS to pull images from ECR and write logs to CloudWatch.

```bash
# Create the role
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

# Attach managed policy
aws iam attach-role-policy \
  --role-name ecsTaskExecutionRole \
  --policy-arn arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy
```

#### ECS Task Role (Optional)
For application-level AWS API access (e.g., S3, DynamoDB):

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

Ensure you have:
- A **VPC** with DNS resolution enabled
- At least **2 public or private subnets** in different Availability Zones
- A **Security Group** allowing:
  - Inbound: TCP port 8080 from ALB security group (or 0.0.0.0/0 for testing)
  - Outbound: All traffic (for ECR image pulls, CloudWatch logs)

```bash
# Example: Create security group
aws ec2 create-security-group \
  --group-name bookingservices-sg \
  --description "BookingServices ECS Security Group" \
  --vpc-id vpc-xxxxxxxx

# Allow inbound on port 8080
aws ec2 authorize-security-group-ingress \
  --group-id sg-xxxxxxxx \
  --protocol tcp \
  --port 8080 \
  --cidr 0.0.0.0/0
```

### 3. CloudWatch Log Group

```bash
aws logs create-log-group \
  --log-group-name /ecs/bookingservices \
  --region us-east-1
```

### 4. ECR Repository

```bash
aws ecr create-repository \
  --repository-name bookingservices \
  --region us-east-1
```

---

## ECS Task Definition Explained

The task definition (`ecs/task-definition.json`) configures how the container runs on Fargate:

```json
{
  "family": "bookingservices-task",
  "requiresCompatibilities": ["FARGATE"],
  "networkMode": "awsvpc",
  "cpu": "512",
  "memory": "1024",
  ...
}
```

### Key Fields

| Field | Value | Explanation |
|---|---|---|
| `requiresCompatibilities` | `["FARGATE"]` | Serverless container execution |
| `networkMode` | `awsvpc` | Required for Fargate; each task gets its own ENI |
| `cpu` | `512` | 0.5 vCPU |
| `memory` | `1024` | 1 GB RAM |
| `executionRoleArn` | `ecsTaskExecutionRole` | Allows ECR pull + CloudWatch logging |

### Valid Fargate CPU/Memory Combinations

| CPU | Valid Memory Options |
|---|---|
| 256 (.25 vCPU) | 512, 1024, 2048 MB |
| **512 (.5 vCPU)** | **1024, 2048, 3072, 4096 MB** ← Used |
| 1024 (1 vCPU) | 2048–8192 MB |
| 2048 (2 vCPU) | 4096–16384 MB |
| 4096 (4 vCPU) | 8192–30720 MB |

### Container Definition

- **Port**: 8080 (Tomcat HTTP)
- **Log Driver**: `awslogs` → CloudWatch log group `/ecs/bookingservices`
- **Environment Variables**: Passed directly to the container

---

## ECS Service Configuration

The service definition (`ecs/service-definition.json`) manages how tasks are scheduled:

```json
{
  "serviceName": "bookingservices-service",
  "launchType": "FARGATE",
  "desiredCount": 2,
  "networkConfiguration": {
    "awsvpcConfiguration": {
      "subnets": ["subnet-xxx", "subnet-yyy"],
      "securityGroups": ["sg-xxx"],
      "assignPublicIp": "ENABLED"
    }
  }
}
```

### Key Settings

| Setting | Value | Explanation |
|---|---|---|
| `desiredCount` | 2 | Run 2 tasks for high availability |
| `assignPublicIp` | `ENABLED` | Required for public subnet Fargate tasks to pull ECR images |
| `maximumPercent` | 200 | Allow up to 4 tasks during rolling deployment |
| `minimumHealthyPercent` | 50 | Keep at least 1 task running during deployment |

---

## ECS Fargate Deployment Walkthrough

### Step 1: Configure AWS CLI

```bash
aws configure
# Enter: AWS Access Key ID, Secret Access Key, Region, Output format
```

### Step 2: Build and Push Image

```bash
./scripts/build-push.sh
# Select: 1 (AWS ECR)
# Enter region, account ID, repository name, tag
```

### Step 3: Deploy to ECS

```bash
chmod +x scripts/deploy-image.sh
./scripts/deploy-image.sh
```

The script will prompt for:
- AWS Region
- ECS Cluster name
- ECR Image URI
- VPC ID
- Subnet IDs (comma-separated)
- Security Group ID
- Whether to create an Application Load Balancer

### Step 4: Verify Deployment

```bash
# Check service status
aws ecs describe-services \
  --cluster bookingservices-cluster \
  --services bookingservices-service \
  --region us-east-1

# List running tasks
aws ecs list-tasks \
  --cluster bookingservices-cluster \
  --service-name bookingservices-service \
  --region us-east-1

# View application logs
aws logs tail /ecs/bookingservices --follow --region us-east-1
```

### Step 5: Test the Application

```bash
# If using ALB (replace with your ALB DNS)
curl http://your-alb-dns.us-east-1.elb.amazonaws.com/resorts/health

# If using task public IP (find from ECS console or CLI)
TASK_ARN=$(aws ecs list-tasks --cluster bookingservices-cluster \
  --service-name bookingservices-service --query "taskArns[0]" --output text)

ENI_ID=$(aws ecs describe-tasks --cluster bookingservices-cluster \
  --tasks $TASK_ARN \
  --query "tasks[0].attachments[0].details[?name=='networkInterfaceId'].value" \
  --output text)

PUBLIC_IP=$(aws ec2 describe-network-interfaces \
  --network-interface-ids $ENI_ID \
  --query "NetworkInterfaces[0].Association.PublicIp" \
  --output text)

curl http://$PUBLIC_IP:8080/resorts/health
```

---

## ECS-Specific Troubleshooting

### Task Fails to Start

```bash
# Check stopped task reason
aws ecs describe-tasks \
  --cluster bookingservices-cluster \
  --tasks <task-arn> \
  --query "tasks[0].stoppedReason"

# Check container exit code
aws ecs describe-tasks \
  --cluster bookingservices-cluster \
  --tasks <task-arn> \
  --query "tasks[0].containers[0].{ExitCode:exitCode,Reason:reason}"
```

**Common causes:**
- `CannotPullContainerError`: ECR permissions issue or image not found
  - Verify `executionRoleArn` has ECR pull permissions
  - Confirm image URI is correct
- `OutOfMemoryError`: Increase task memory (e.g., from 1024 to 2048)
- `ResourceInitializationError`: Fargate agent issue; retry deployment

### Network Issues

```bash
# Verify security group allows port 8080
aws ec2 describe-security-groups --group-ids sg-xxxxxxxx

# Check task ENI
aws ecs describe-tasks \
  --cluster bookingservices-cluster \
  --tasks <task-arn> \
  --query "tasks[0].attachments"
```

**Common causes:**
- Security group not allowing inbound port 8080
- `assignPublicIp: DISABLED` in public subnet (tasks can't pull ECR images)
- Private subnets without NAT Gateway

### CPU/Memory Errors

```
InvalidParameterException: Invalid CPU or memory value specified
```

**Fix**: Use valid Fargate combinations. Default: `cpu: "512"`, `memory: "1024"`.

### Application Not Responding

```bash
# Check Tomcat startup logs
aws logs tail /ecs/bookingservices --follow --region us-east-1

# Common issues:
# - WAR deployment failure: Check for missing dependencies
# - Port conflict: Verify EXPOSE 8080 matches Tomcat configuration
# - JVM OOM: Increase memory or adjust JAVA_OPTS heap settings
```

### Service Not Stabilizing

```bash
# Check service events
aws ecs describe-services \
  --cluster bookingservices-cluster \
  --services bookingservices-service \
  --query "services[0].events[:5]"
```

---

## ECS Fargate Scaling and Management

### Manual Scaling

```bash
# Scale to 4 tasks
aws ecs update-service \
  --cluster bookingservices-cluster \
  --service bookingservices-service \
  --desired-count 4 \
  --region us-east-1
```

### Auto Scaling

```bash
# Register scalable target
aws application-autoscaling register-scalable-target \
  --service-namespace ecs \
  --resource-id service/bookingservices-cluster/bookingservices-service \
  --scalable-dimension ecs:service:DesiredCount \
  --min-capacity 2 \
  --max-capacity 10

# Create CPU-based scaling policy
aws application-autoscaling put-scaling-policy \
  --service-namespace ecs \
  --resource-id service/bookingservices-cluster/bookingservices-service \
  --scalable-dimension ecs:service:DesiredCount \
  --policy-name bookingservices-cpu-scaling \
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
  --cluster bookingservices-cluster \
  --service bookingservices-service \
  --task-definition bookingservices-task:NEW_REVISION \
  --region us-east-1
```

### Force New Deployment

```bash
aws ecs update-service \
  --cluster bookingservices-cluster \
  --service bookingservices-service \
  --force-new-deployment \
  --region us-east-1
```

---

## Configuration Management

### Using AWS Secrets Manager for Sensitive Values

```bash
# Store Weather API key
aws secretsmanager create-secret \
  --name bookingservices/weather-api-key \
  --secret-string "your-api-key-here"
```

Update task definition to use secrets:

```json
"secrets": [
  {
    "name": "WEATHER_API_KEY",
    "valueFrom": "arn:aws:secretsmanager:us-east-1:123456789:secret:bookingservices/weather-api-key"
  }
]
```

### Using AWS Systems Manager Parameter Store

```bash
aws ssm put-parameter \
  --name "/bookingservices/weather-api-key" \
  --value "your-api-key" \
  --type SecureString
```

---

## Security Considerations

1. **Non-root container user**: The Dockerfile creates and uses `appuser` (non-root)
2. **Minimal base image**: Amazon Corretto 8 with only required packages
3. **No sensitive data in image**: All secrets via environment variables or AWS Secrets Manager
4. **Security Group**: Restrict inbound to ALB security group only (not 0.0.0.0/0)
5. **Private subnets**: For production, use private subnets with NAT Gateway
6. **IAM least privilege**: Task role should only have permissions the application needs
7. **ECR image scanning**: Enable ECR vulnerability scanning:
   ```bash
   aws ecr put-image-scanning-configuration \
     --repository-name bookingservices \
     --image-scanning-configuration scanOnPush=true
   ```
8. **HTTPS**: Configure ALB with HTTPS listener and ACM certificate for production
9. **Log retention**: Set CloudWatch log retention policy:
   ```bash
   aws logs put-retention-policy \
     --log-group-name /ecs/bookingservices \
     --retention-in-days 30
   ```

---

## Java-Specific Notes

### JVM Configuration

The container uses these JVM flags (set via `JAVA_OPTS`):

| Flag | Purpose |
|---|---|
| `-Xmx512m` | Maximum heap size |
| `-Xms256m` | Initial heap size |
| `-XX:+UseContainerSupport` | Enables JVM container awareness (Java 8u191+) |
| `-XX:MaxRAMPercentage=75.0` | Use 75% of container RAM for heap |
| `-XX:+UseG1GC` | G1 garbage collector (better for containers) |
| `-Djava.security.egd=file:/dev/./urandom` | Faster random number generation |
| `-Dfile.encoding=UTF-8` | Consistent character encoding |
| `-Duser.timezone=UTC` | Consistent timezone |

### Adjusting Memory for Production

For higher load, update the task definition:

```json
{
  "cpu": "1024",
  "memory": "2048"
}
```

And update `JAVA_OPTS`:
```
-Xmx1536m -Xms512m -XX:MaxRAMPercentage=75.0
```

### Tomcat Configuration

The application deploys as `resorts.war` to Tomcat's webapps directory, making it accessible at `/resorts/*`. To change the context root, rename the WAR file accordingly.

### Monitoring with JMX

The application uses JMX MBeans (`com.acme.modres.mbean:name=appInfo`). For remote JMX monitoring in ECS, add to `JAVA_OPTS`:

```
-Dcom.sun.management.jmxremote
-Dcom.sun.management.jmxremote.port=9090
-Dcom.sun.management.jmxremote.ssl=false
-Dcom.sun.management.jmxremote.authenticate=false
-Djava.rmi.server.hostname=0.0.0.0
```

And expose port 9090 in the task definition.

### Log4j Configuration

The application uses Log4j 2.14.1. For structured JSON logging in CloudWatch, add a `log4j2.xml` to `src/main/resources`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<Configuration status="WARN">
  <Appenders>
    <Console name="Console" target="SYSTEM_OUT">
      <JsonLayout compact="true" eventEol="true"/>
    </Console>
  </Appenders>
  <Loggers>
    <Root level="INFO">
      <AppenderRef ref="Console"/>
    </Root>
  </Loggers>
</Configuration>
```
