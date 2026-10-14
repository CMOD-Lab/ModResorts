# BookingComp (ModResorts) – Deployment Guide

## Overview

**Application**: ModResorts BookingComp  
**Version**: 2.0.0  
**Technology**: Java 8, Java EE (Servlet 3.1), Open Liberty, Maven  
**Package**: WAR  
**Target Platform**: AWS EKS (Elastic Kubernetes Service)  
**Application Port**: 9080  
**Health Endpoint**: `/health`

---

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Project Structure](#project-structure)
3. [Local Development with Docker Compose](#local-development-with-docker-compose)
4. [Build and Push Docker Image](#build-and-push-docker-image)
5. [AWS EKS Prerequisites](#aws-eks-prerequisites)
6. [EKS Cluster Setup](#eks-cluster-setup)
7. [Kubernetes Deployment](#kubernetes-deployment)
8. [Configuration Management](#configuration-management)
9. [Scaling and Management](#scaling-and-management)
10. [Troubleshooting](#troubleshooting)
11. [Security Considerations](#security-considerations)
12. [Java-Specific Notes](#java-specific-notes)

---

## Prerequisites

### Local Development
- Docker Desktop 24.x or later
- Docker Compose v2.x or later
- Java 8 JDK (for local builds outside Docker)
- Apache Maven 3.8.x or later

### AWS EKS Deployment
- AWS CLI v2.x configured with appropriate IAM permissions
- `kubectl` v1.28 or later
- `eksctl` v0.160 or later (for cluster creation)
- AWS IAM permissions:
  - `eks:*`
  - `ecr:*`
  - `ec2:*` (for node groups)
  - `iam:PassRole`

---

## Project Structure

```
BookingComp/
├── Dockerfile                  # Multi-stage build (Maven builder + Liberty runtime)
├── docker-compose.yml          # Local development compose file
├── .dockerignore               # Docker build exclusions
├── docker/
│   └── server.xml              # Open Liberty server configuration
├── kubernetes/
│   ├── namespace.yaml          # Kubernetes namespace
│   ├── deployment.yaml         # Application deployment (2 replicas)
│   ├── service.yaml            # ClusterIP service (port 80 → 9080)
│   └── ingress.yaml            # AWS ALB Ingress
├── scripts/
│   ├── build-push.sh           # Linux/macOS build & push script
│   ├── build-push.bat          # Windows build & push script
│   ├── deploy-image.sh         # Linux/macOS EKS deploy script
│   └── deploy-image.bat        # Windows EKS deploy script
├── src/                        # Java source code
├── WebContent/                 # Web resources (HTML, CSS, JSP)
└── pom.xml                     # Maven build descriptor
```

---

## Local Development with Docker Compose

### 1. Configure Environment Variables

Create a `.env` file in the project root:

```env
REDIS_HOST=localhost
REDIS_PORT=6379
WEATHER_SERVICE_URL=
WEATHER_API_KEY=
```

> **Note**: `WEATHER_SERVICE_URL` is optional. When unset, the application falls back to bundled default weather data.

### 2. Start the Application

```bash
docker-compose up --build
```

The application will be available at: `http://localhost:9080`

### 3. Verify Health

```bash
curl http://localhost:9080/health
# Expected: {"status":"UP"}
```

### 4. Stop the Application

```bash
docker-compose down
```

---

## Build and Push Docker Image

### Linux / macOS

```bash
chmod +x scripts/build-push.sh
./scripts/build-push.sh
```

The script will prompt you to:
1. Enter an image tag (default: `latest`)
2. Select a registry (AWS ECR or Docker Hub)
3. Provide registry credentials

### Windows

```cmd
scripts\build-push.bat
```

### Manual Build (AWS ECR)

```bash
# Set variables
AWS_REGION=us-east-1
AWS_ACCOUNT_ID=123456789012
IMAGE_TAG=2.0.0

# Authenticate to ECR
aws ecr get-login-password --region $AWS_REGION | \
  docker login --username AWS --password-stdin \
  ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com

# Create repository (first time only)
aws ecr create-repository --repository-name bookingcomp --region $AWS_REGION

# Build and push
docker build -t ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/bookingcomp:${IMAGE_TAG} .
docker push ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/bookingcomp:${IMAGE_TAG}
```

---

## AWS EKS Prerequisites

### 1. Install Required Tools

```bash
# AWS CLI
curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
unzip awscliv2.zip && sudo ./aws/install

# kubectl
curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
chmod +x kubectl && sudo mv kubectl /usr/local/bin/

# eksctl
curl --silent --location "https://github.com/eksctl-io/eksctl/releases/latest/download/eksctl_$(uname -s)_amd64.tar.gz" | tar xz -C /tmp
sudo mv /tmp/eksctl /usr/local/bin
```

### 2. Configure AWS CLI

```bash
aws configure
# Enter: AWS Access Key ID, Secret Access Key, Region, Output format
```

### 3. Install AWS Load Balancer Controller

The Ingress resource uses the AWS Load Balancer Controller. Install it on your EKS cluster:

```bash
# Add the EKS chart repo
helm repo add eks https://aws.github.io/eks-charts
helm repo update

# Install the controller
helm install aws-load-balancer-controller eks/aws-load-balancer-controller \
  -n kube-system \
  --set clusterName=<YOUR_CLUSTER_NAME> \
  --set serviceAccount.create=false \
  --set serviceAccount.name=aws-load-balancer-controller
```

---

## EKS Cluster Setup

### Create a New EKS Cluster (if needed)

```bash
eksctl create cluster \
  --name bookingcomp-cluster \
  --region us-east-1 \
  --nodegroup-name standard-workers \
  --node-type t3.medium \
  --nodes 2 \
  --nodes-min 1 \
  --nodes-max 4 \
  --managed
```

### Configure kubectl

```bash
aws eks update-kubeconfig --region us-east-1 --name bookingcomp-cluster
kubectl cluster-info
```

---

## Kubernetes Deployment

### Automated Deployment

#### Linux / macOS

```bash
chmod +x scripts/deploy-image.sh
./scripts/deploy-image.sh
```

The script will prompt for:
- AWS Region
- EKS Cluster Name
- Full Docker image URI
- Optional: REDIS_HOST, REDIS_PORT, WEATHER_SERVICE_URL, WEATHER_API_KEY

#### Windows

```cmd
scripts\deploy-image.bat
```

### Manual Deployment

```bash
# Set the image URI
IMAGE_URI="123456789012.dkr.ecr.us-east-1.amazonaws.com/bookingcomp:2.0.0"

# Substitute placeholders
sed -i "s|{{IMAGE_URI}}|${IMAGE_URI}|g"                         kubernetes/deployment.yaml
sed -i "s|{{REDIS_HOST}}|your-elasticache-endpoint|g"           kubernetes/deployment.yaml
sed -i "s|{{REDIS_PORT}}|6379|g"                                kubernetes/deployment.yaml
sed -i "s|{{WEATHER_SERVICE_URL}}||g"                           kubernetes/deployment.yaml
sed -i "s|{{WEATHER_API_KEY}}||g"                               kubernetes/deployment.yaml

# Apply manifests in order
kubectl apply -f kubernetes/namespace.yaml
kubectl apply -f kubernetes/deployment.yaml
kubectl apply -f kubernetes/service.yaml
kubectl apply -f kubernetes/ingress.yaml

# Wait for rollout
kubectl rollout status deployment/bookingcomp -n bookingcomp

# Verify
kubectl get pods,svc,ingress -n bookingcomp
```

### Verify Deployment

```bash
# Check pod status
kubectl get pods -n bookingcomp

# Check pod logs
kubectl logs -l app=bookingcomp -n bookingcomp

# Check service
kubectl get svc -n bookingcomp

# Get ingress URL
kubectl get ingress bookingcomp-ingress -n bookingcomp
```

---

## Configuration Management

### Environment Variables

| Variable | Description | Default | Required |
|---|---|---|---|
| `REDIS_HOST` | Amazon ElastiCache primary endpoint | `localhost` | Yes (production) |
| `REDIS_PORT` | ElastiCache port | `6379` | No |
| `WEATHER_SERVICE_URL` | External weather microservice base URL | _(empty)_ | No |
| `WEATHER_API_KEY` | Weather API authentication key | _(empty)_ | No |
| `JVM_ARGS` | JVM startup arguments | `-Xmx512m -Xms256m ...` | No |
| `TZ` | Container timezone | `UTC` | No |

### Using Kubernetes Secrets for Sensitive Values

```bash
# Create a secret for sensitive configuration
kubectl create secret generic bookingcomp-secrets \
  --from-literal=WEATHER_API_KEY=your-api-key \
  -n bookingcomp
```

Then reference in `deployment.yaml`:

```yaml
env:
  - name: WEATHER_API_KEY
    valueFrom:
      secretKeyRef:
        name: bookingcomp-secrets
        key: WEATHER_API_KEY
```

---

## Scaling and Management

### Manual Scaling

```bash
kubectl scale deployment bookingcomp --replicas=4 -n bookingcomp
```

### Horizontal Pod Autoscaler (HPA)

```bash
kubectl autoscale deployment bookingcomp \
  --cpu-percent=70 \
  --min=2 \
  --max=10 \
  -n bookingcomp
```

### Rolling Update

```bash
# Update the image
kubectl set image deployment/bookingcomp \
  bookingcomp=123456789012.dkr.ecr.us-east-1.amazonaws.com/bookingcomp:2.1.0 \
  -n bookingcomp

# Monitor rollout
kubectl rollout status deployment/bookingcomp -n bookingcomp
```

### Rollback

```bash
# Rollback to previous version
kubectl rollout undo deployment/bookingcomp -n bookingcomp

# Rollback to specific revision
kubectl rollout history deployment/bookingcomp -n bookingcomp
kubectl rollout undo deployment/bookingcomp --to-revision=2 -n bookingcomp
```

---

## Troubleshooting

### Pod Not Starting

```bash
# Check pod events
kubectl describe pod -l app=bookingcomp -n bookingcomp

# Check pod logs
kubectl logs -l app=bookingcomp -n bookingcomp --previous
```

### Health Check Failures

The application exposes `/health` on port 9080. If health checks fail:

```bash
# Port-forward to test locally
kubectl port-forward svc/bookingcomp-service 9080:80 -n bookingcomp

# Test health endpoint
curl http://localhost:9080/health
# Expected: {"status":"UP"}
```

**Common causes**:
- Liberty server startup takes 30–60 seconds (JVM warm-up). Increase `initialDelaySeconds` if needed.
- Redis connection failure: verify `REDIS_HOST` and `REDIS_PORT` are correct.

### Ingress Not Accessible

```bash
# Check ALB controller logs
kubectl logs -n kube-system -l app.kubernetes.io/name=aws-load-balancer-controller

# Check ingress events
kubectl describe ingress bookingcomp-ingress -n bookingcomp
```

Ensure the AWS Load Balancer Controller is installed and the IAM role has the required permissions.

### OOMKilled Pods

Increase memory limits in `kubernetes/deployment.yaml`:

```yaml
resources:
  limits:
    memory: "2Gi"
```

Also adjust JVM heap:

```yaml
- name: JVM_ARGS
  value: "-Xmx1g -Xms512m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
```

### Redis Connection Issues

```bash
# Verify Redis connectivity from within a pod
kubectl exec -it $(kubectl get pod -l app=bookingcomp -n bookingcomp -o jsonpath='{.items[0].metadata.name}') \
  -n bookingcomp -- bash -c "echo PING | nc $REDIS_HOST $REDIS_PORT"
```

---

## Security Considerations

1. **Non-root container**: The application runs as `appuser` (non-root) inside the container.
2. **Secrets management**: Store sensitive values (API keys, passwords) in Kubernetes Secrets or AWS Secrets Manager, not in environment variables directly.
3. **Network policies**: Consider adding Kubernetes NetworkPolicies to restrict pod-to-pod communication.
4. **Image scanning**: Enable ECR image scanning to detect vulnerabilities in the container image.
5. **RBAC**: Apply least-privilege IAM roles to EKS node groups and service accounts.
6. **TLS**: Configure HTTPS on the ALB Ingress using ACM certificates:
   ```yaml
   annotations:
     alb.ingress.kubernetes.io/certificate-arn: arn:aws:acm:us-east-1:123456789012:certificate/xxx
     alb.ingress.kubernetes.io/listen-ports: '[{"HTTPS":443}]'
   ```

---

## Java-Specific Notes

### JVM Container Awareness

The Dockerfile sets the following JVM flags for optimal container performance:

```
-Xmx512m -Xms256m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0
```

- `-XX:+UseContainerSupport`: Enables JVM to respect container memory/CPU limits (Java 8u191+).
- `-XX:MaxRAMPercentage=75.0`: Limits heap to 75% of container memory.

### Open Liberty Server

The application runs on Open Liberty with the following features enabled:
- `servlet-3.1` – Servlet container
- `cdi-1.2` – Dependency injection
- `jaxrs-2.0` – REST support
- `managedBeans-1.0` – Managed beans

Server configuration is in `docker/server.xml`.

### Startup Time

Open Liberty with Java 8 typically starts in 10–30 seconds. The Kubernetes probes are configured with:
- `initialDelaySeconds: 60` (liveness) – allows full JVM + Liberty startup
- `initialDelaySeconds: 30` (readiness) – marks pod ready once health endpoint responds

Adjust these values based on observed startup times in your environment.

### Logging

Liberty logs are written to stdout/stderr and are captured by Kubernetes. View them with:

```bash
kubectl logs -l app=bookingcomp -n bookingcomp -f
```
