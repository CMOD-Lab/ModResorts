# BookingComp – Deployment Guide

## Overview

**Application**: ModResorts BookingComp  
**Version**: 2.0.0  
**Technology**: Java 8, Java EE 7 (Servlet 3.1), Apache Tomcat 9  
**Build Tool**: Maven 3.9.x  
**Package Type**: WAR  
**Target Platform**: AWS EKS (Elastic Kubernetes Service)  
**Health Endpoint**: `GET /health`  
**Application Port**: 8080  

---

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Project Structure](#project-structure)
3. [Local Development with Docker Compose](#local-development-with-docker-compose)
4. [Build and Push Docker Image](#build-and-push-docker-image)
5. [AWS EKS Prerequisites](#aws-eks-prerequisites)
6. [EKS Cluster Setup](#eks-cluster-setup)
7. [Kubernetes Deployment](#kubernetes-deployment)
8. [Environment Variables Reference](#environment-variables-reference)
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
- Maven 3.9.x (for local builds outside Docker)

### AWS EKS Deployment
- AWS CLI v2.x configured with appropriate IAM permissions
- `kubectl` v1.28 or later
- `eksctl` v0.170 or later (for cluster creation)
- AWS IAM permissions:
  - `eks:*`
  - `ec2:*` (for node groups)
  - `ecr:*` (for container registry)
  - `elasticloadbalancing:*` (for ALB Ingress)
  - `iam:CreateRole`, `iam:AttachRolePolicy` (for service accounts)

---

## Project Structure

```
BookingComp/
├── Dockerfile                    # Multi-stage build (Maven builder + Tomcat runtime)
├── .dockerignore                 # Excludes build artifacts and wrapper files
├── docker-compose.yml            # Local development compose file
├── pom.xml                       # Maven build descriptor
├── src/
│   └── main/
│       ├── java/com/acme/modres/ # Application source code
│       └── resources/            # JSON data files (ops.json, reservations.json)
├── WebContent/                   # Static web assets, JSP, WEB-INF
├── kubernetes/
│   ├── namespace.yaml            # Kubernetes namespace
│   ├── deployment.yaml           # Application deployment (2 replicas)
│   ├── service.yaml              # ClusterIP service
│   └── ingress.yaml              # AWS ALB Ingress
├── scripts/
│   ├── build-push.sh             # Linux/macOS build & push script
│   ├── build-push.bat            # Windows build & push script
│   ├── deploy-image.sh           # Linux/macOS EKS deploy script
│   └── deploy-image.bat          # Windows EKS deploy script
└── docs/
    └── DEPLOYMENT.md             # This file
```

---

## Local Development with Docker Compose

### 1. Configure Environment Variables

Create a `.env` file in the project root:

```bash
# Redis / ElastiCache (use a local Redis for development)
REDIS_HOST=redis
REDIS_PORT=6379

# Weather Underground API key (optional – uses mock data if not set)
WEATHER_API_KEY=your_api_key_here

# Remote service discovery URL (optional)
REMOTE_SERVICE_URL=http://localhost:8081
```

### 2. Start the Application

```bash
# Build and start
docker compose up --build

# Start in background
docker compose up -d --build

# View logs
docker compose logs -f bookingcomp

# Stop
docker compose down
```

### 3. Verify the Application

```bash
# Health check
curl http://localhost:8080/health

# Expected response:
# {"status":"UP","application":"modresorts"}

# Access the application
open http://localhost:8080
```

---

## Build and Push Docker Image

### Linux / macOS

```bash
# Make the script executable
chmod +x scripts/build-push.sh

# Run from the project root
./scripts/build-push.sh
```

The script will prompt you to:
1. Enter an image tag (default: `latest`)
2. Select a registry: **AWS ECR** or **Docker Hub**
3. Provide registry credentials and repository details

### Windows

```cmd
scripts\build-push.bat
```

### Manual Docker Build

```bash
# Build the image
docker build -t bookingcomp:latest .

# Tag for ECR
docker tag bookingcomp:latest <AWS_ACCOUNT_ID>.dkr.ecr.<REGION>.amazonaws.com/bookingcomp:latest

# Push to ECR
aws ecr get-login-password --region <REGION> | \
  docker login --username AWS --password-stdin <AWS_ACCOUNT_ID>.dkr.ecr.<REGION>.amazonaws.com

docker push <AWS_ACCOUNT_ID>.dkr.ecr.<REGION>.amazonaws.com/bookingcomp:latest
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
# Create IAM policy
curl -O https://raw.githubusercontent.com/kubernetes-sigs/aws-load-balancer-controller/v2.7.1/docs/install/iam_policy.json
aws iam create-policy \
  --policy-name AWSLoadBalancerControllerIAMPolicy \
  --policy-document file://iam_policy.json

# Create service account
eksctl create iamserviceaccount \
  --cluster=<CLUSTER_NAME> \
  --namespace=kube-system \
  --name=aws-load-balancer-controller \
  --role-name AmazonEKSLoadBalancerControllerRole \
  --attach-policy-arn=arn:aws:iam::<AWS_ACCOUNT_ID>:policy/AWSLoadBalancerControllerIAMPolicy \
  --approve

# Install via Helm
helm repo add eks https://aws.github.io/eks-charts
helm repo update eks
helm install aws-load-balancer-controller eks/aws-load-balancer-controller \
  -n kube-system \
  --set clusterName=<CLUSTER_NAME> \
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

### Automated Deployment (Recommended)

#### Linux / macOS

```bash
chmod +x scripts/deploy-image.sh
./scripts/deploy-image.sh
```

#### Windows

```cmd
scripts\deploy-image.bat
```

The script will prompt for:
- AWS Region and EKS cluster name
- Full Docker image URI (with tag)
- Application environment variables (REDIS_HOST, REDIS_PORT, WEATHER_API_KEY, REMOTE_SERVICE_URL)

### Manual Deployment

```bash
# 1. Update the image URI in deployment.yaml
sed -i 's|{{IMAGE_URI}}|<YOUR_IMAGE_URI>|g' kubernetes/deployment.yaml
sed -i 's|{{REDIS_HOST}}|<YOUR_REDIS_HOST>|g' kubernetes/deployment.yaml
sed -i 's|{{REDIS_PORT}}|6379|g' kubernetes/deployment.yaml
sed -i 's|{{WEATHER_API_KEY}}|<YOUR_API_KEY>|g' kubernetes/deployment.yaml
sed -i 's|{{REMOTE_SERVICE_URL}}|<YOUR_SERVICE_URL>|g' kubernetes/deployment.yaml

# 2. Apply manifests in order
kubectl apply -f kubernetes/namespace.yaml
kubectl apply -f kubernetes/deployment.yaml
kubectl apply -f kubernetes/service.yaml
kubectl apply -f kubernetes/ingress.yaml

# 3. Wait for rollout
kubectl rollout status deployment/bookingcomp -n bookingcomp

# 4. Verify
kubectl get pods,svc,ingress -n bookingcomp
```

### Verify Deployment

```bash
# Check pod status
kubectl get pods -n bookingcomp

# Check pod logs
kubectl logs -l app=bookingcomp -n bookingcomp

# Check health endpoint via port-forward
kubectl port-forward svc/bookingcomp-service 8080:80 -n bookingcomp
curl http://localhost:8080/health

# Get ingress URL
kubectl get ingress -n bookingcomp
```

---

## Environment Variables Reference

| Variable | Required | Default | Description |
|---|---|---|---|
| `REDIS_HOST` | Yes | `localhost` | Amazon ElastiCache Redis primary endpoint |
| `REDIS_PORT` | No | `6379` | Redis port |
| `WEATHER_API_KEY` | No | _(empty)_ | Weather Underground API key. If not set, mock data is used |
| `REMOTE_SERVICE_URL` | No | _(empty)_ | Remote service base URL for Kubernetes DNS-based discovery |
| `JAVA_OPTS` | No | See Dockerfile | JVM tuning flags |
| `TZ` | No | `UTC` | Container timezone |

### Using Kubernetes Secrets for Sensitive Values

```bash
# Create a secret for sensitive environment variables
kubectl create secret generic bookingcomp-secrets \
  --from-literal=WEATHER_API_KEY=<your_api_key> \
  --from-literal=REDIS_HOST=<your_redis_host> \
  -n bookingcomp

# Reference in deployment.yaml (update env section):
# - name: WEATHER_API_KEY
#   valueFrom:
#     secretKeyRef:
#       name: bookingcomp-secrets
#       key: WEATHER_API_KEY
```

---

## Scaling and Management

### Horizontal Scaling

```bash
# Scale manually
kubectl scale deployment bookingcomp --replicas=4 -n bookingcomp

# Configure Horizontal Pod Autoscaler
kubectl autoscale deployment bookingcomp \
  --cpu-percent=70 \
  --min=2 \
  --max=10 \
  -n bookingcomp

# Check HPA status
kubectl get hpa -n bookingcomp
```

### Rolling Updates

```bash
# Update image
kubectl set image deployment/bookingcomp \
  bookingcomp=<NEW_IMAGE_URI> \
  -n bookingcomp

# Monitor rollout
kubectl rollout status deployment/bookingcomp -n bookingcomp

# Rollback if needed
kubectl rollout undo deployment/bookingcomp -n bookingcomp

# View rollout history
kubectl rollout history deployment/bookingcomp -n bookingcomp
```

### Resource Management

```bash
# View resource usage
kubectl top pods -n bookingcomp

# Describe deployment
kubectl describe deployment bookingcomp -n bookingcomp
```

---

## Troubleshooting

### Pod Not Starting

```bash
# Check pod events
kubectl describe pod -l app=bookingcomp -n bookingcomp

# Check logs
kubectl logs -l app=bookingcomp -n bookingcomp --previous

# Common causes:
# - Image pull errors: verify ECR permissions and image URI
# - OOMKilled: increase memory limits in deployment.yaml
# - CrashLoopBackOff: check application logs for startup errors
```

### Health Check Failures

```bash
# Port-forward and test health endpoint directly
kubectl port-forward svc/bookingcomp-service 8080:80 -n bookingcomp
curl -v http://localhost:8080/health

# Expected: HTTP 200 {"status":"UP","application":"modresorts"}

# If Tomcat is slow to start, increase initialDelaySeconds in deployment.yaml
# livenessProbe.initialDelaySeconds: 90  (increase from 60)
# readinessProbe.initialDelaySeconds: 45 (increase from 30)
```

### Ingress / ALB Issues

```bash
# Check ingress status
kubectl describe ingress bookingcomp-ingress -n bookingcomp

# Check AWS Load Balancer Controller logs
kubectl logs -n kube-system -l app.kubernetes.io/name=aws-load-balancer-controller

# Verify ALB is created in AWS Console:
# EC2 → Load Balancers → look for bookingcomp ALB
```

### Redis Connection Issues

```bash
# Verify REDIS_HOST is set correctly
kubectl exec -it <pod-name> -n bookingcomp -- env | grep REDIS

# Test Redis connectivity from pod
kubectl exec -it <pod-name> -n bookingcomp -- \
  bash -c "echo 'PING' | nc -w 2 $REDIS_HOST $REDIS_PORT"
```

### Image Pull Errors

```bash
# Verify ECR login
aws ecr get-login-password --region <REGION> | \
  docker login --username AWS --password-stdin <ACCOUNT_ID>.dkr.ecr.<REGION>.amazonaws.com

# Check node IAM role has ECR pull permissions
# Attach policy: AmazonEC2ContainerRegistryReadOnly to the EKS node role
```

---

## Security Considerations

1. **Non-root container**: The application runs as `appuser` (non-root) inside the container.
2. **Secrets management**: Use Kubernetes Secrets or AWS Secrets Manager for sensitive values (API keys, Redis credentials).
3. **Network policies**: Consider adding Kubernetes NetworkPolicies to restrict pod-to-pod communication.
4. **Image scanning**: Enable ECR image scanning to detect vulnerabilities in the container image.
5. **RBAC**: Apply least-privilege RBAC policies for the application's service account.
6. **TLS**: Configure HTTPS on the ALB Ingress using ACM certificates:
   ```yaml
   annotations:
     alb.ingress.kubernetes.io/listen-ports: '[{"HTTPS": 443}]'
     alb.ingress.kubernetes.io/certificate-arn: arn:aws:acm:<REGION>:<ACCOUNT>:certificate/<CERT_ID>
   ```
7. **Security groups**: Restrict ALB security group to allow only necessary inbound traffic.

---

## Java-Specific Notes

### JVM Memory Configuration

The container is configured with:
- `-Xms256m` – Initial heap size
- `-Xmx512m` – Maximum heap size
- `-XX:+UseContainerSupport` – Enables JVM container awareness
- `-XX:MaxRAMPercentage=75.0` – JVM uses up to 75% of container memory

Adjust `JAVA_OPTS` in `kubernetes/deployment.yaml` if the application requires more memory. Ensure the Kubernetes memory limit is at least 25% higher than `-Xmx`.

### Tomcat Configuration

The WAR is deployed to Tomcat 9's ROOT context (`/`). Servlet URL mappings from `web.xml`:
- `GET /health` → HealthServlet (liveness/readiness probe)
- `GET /resorts/weather` → WeatherServlet
- `GET /resorts/availability` → AvailabilityCheckerServlet
- `GET /resorts/welcome` → WelcomeServlet
- `GET /upper` → UpperServlet

### Startup Time

Tomcat + Java 8 typically takes 20–40 seconds to start. The Kubernetes probes are configured with:
- `initialDelaySeconds: 60` for liveness (allows full startup)
- `initialDelaySeconds: 30` for readiness

Increase these values if pods are being killed before startup completes.

### Base Image

The runtime image uses `amazoncorretto:8` (Amazon Corretto JDK 8), which is Amazon's production-ready OpenJDK distribution with long-term support.
