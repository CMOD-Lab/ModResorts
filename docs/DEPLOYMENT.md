# ModResorts - Deployment Guide

## Overview

This guide covers the complete deployment process for the **ModResorts** Java EE web application on **AWS EKS (Elastic Kubernetes Service)**. ModResorts is a Java 8 WAR-packaged servlet-based web application deployed on Apache Tomcat 9.

---

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Project Structure](#project-structure)
3. [Local Development with Docker Compose](#local-development-with-docker-compose)
4. [Build and Push Docker Image](#build-and-push-docker-image)
5. [AWS EKS Prerequisites](#aws-eks-prerequisites)
6. [EKS Cluster Setup](#eks-cluster-setup)
7. [Kubernetes Deployment Walkthrough](#kubernetes-deployment-walkthrough)
8. [Environment Variables Reference](#environment-variables-reference)
9. [EKS Scaling and Management](#eks-scaling-and-management)
10. [Troubleshooting](#troubleshooting)
11. [Security Considerations](#security-considerations)
12. [Java-Specific Notes](#java-specific-notes)

---

## Prerequisites

### Local Development Tools
- **Docker** 20.10+ and **Docker Compose** v2+
- **Java 8 JDK** (for local builds)
- **Apache Maven** 3.8+

### AWS Deployment Tools
- **AWS CLI** v2 (`aws --version`)
- **kubectl** v1.27+ (`kubectl version --client`)
- **eksctl** v0.150+ (optional, for cluster creation)
- **AWS IAM permissions**: EKS, ECR, EC2, IAM

---

## Project Structure

```
Backend Component/
├── Dockerfile                    # Multi-stage Docker build
├── docker-compose.yml            # Local development compose file
├── .dockerignore                 # Docker build exclusions
├── pom.xml                       # Maven build descriptor
├── src/
│   └── main/
│       ├── java/com/acme/modres/ # Java source files
│       └── resources/            # Application resources (ops.json, reservations.json)
├── WebContent/                   # Web assets (HTML, CSS, JS, WEB-INF)
├── kubernetes/
│   ├── namespace.yaml            # Kubernetes namespace
│   ├── deployment.yaml           # Application deployment
│   ├── service.yaml              # ClusterIP service
│   └── ingress.yaml              # AWS ALB ingress
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
# Weather API (optional - uses default data if not set)
WEATHER_API_KEY=your_weather_api_key_here

# JNDI Configuration (optional)
JNDI_FACTORY=com.sun.jndi.rmi.registry.RegistryContextFactory
JNDI_PROVIDER_URL=rmi://localhost:1099

# Server identification (optional)
SERVER_DISPLAY_NAME=modresorts-local
SERVER_FULL_NAME=modresorts-local-server
```

### 2. Build and Start the Application

```bash
# Build and start
docker-compose up --build

# Start in background
docker-compose up -d --build

# View logs
docker-compose logs -f modresorts

# Stop
docker-compose down
```

### 3. Verify the Application

```bash
# Health check
curl http://localhost:8080/health

# Application home
open http://localhost:8080/
```

Expected health response:
```json
{"status":"UP","application":"modresorts"}
```

---

## Build and Push Docker Image

### Linux / macOS

```bash
# Make script executable
chmod +x scripts/build-push.sh

# Run from project root
./scripts/build-push.sh
```

The script will prompt you to:
1. Enter an image tag (default: `latest`)
2. Select registry type (AWS ECR or Docker Hub)
3. Provide registry credentials and details

### Windows

```cmd
scripts\build-push.bat
```

### Manual Docker Build

```bash
# Build image
docker build -t modresorts:latest .

# Tag for ECR
docker tag modresorts:latest 123456789012.dkr.ecr.us-east-1.amazonaws.com/modresorts:latest

# Push to ECR
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin 123456789012.dkr.ecr.us-east-1.amazonaws.com
docker push 123456789012.dkr.ecr.us-east-1.amazonaws.com/modresorts:latest
```

---

## AWS EKS Prerequisites

### 1. Install Required Tools

```bash
# AWS CLI v2
curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
unzip awscliv2.zip && sudo ./aws/install

# kubectl
curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
chmod +x kubectl && sudo mv kubectl /usr/local/bin/

# eksctl (optional)
curl --silent --location "https://github.com/eksctl-io/eksctl/releases/latest/download/eksctl_$(uname -s)_amd64.tar.gz" | tar xz -C /tmp
sudo mv /tmp/eksctl /usr/local/bin
```

### 2. Configure AWS CLI

```bash
aws configure
# Enter: AWS Access Key ID, Secret Access Key, Region, Output format
```

### 3. Required IAM Permissions

Your IAM user/role needs the following policies:
- `AmazonEKSClusterPolicy`
- `AmazonEKSWorkerNodePolicy`
- `AmazonEC2ContainerRegistryFullAccess`
- `AmazonEKS_CNI_Policy`

---

## EKS Cluster Setup

### Option A: Use Existing Cluster

```bash
# Configure kubectl for your existing cluster
aws eks update-kubeconfig --region us-east-1 --name your-cluster-name

# Verify connectivity
kubectl cluster-info
kubectl get nodes
```

### Option B: Create New EKS Cluster with eksctl

```bash
eksctl create cluster \
  --name modresorts-cluster \
  --region us-east-1 \
  --nodegroup-name standard-workers \
  --node-type t3.medium \
  --nodes 2 \
  --nodes-min 1 \
  --nodes-max 4 \
  --managed
```

### Install AWS Load Balancer Controller (for Ingress)

```bash
# Add IAM policy for ALB controller
curl -O https://raw.githubusercontent.com/kubernetes-sigs/aws-load-balancer-controller/v2.6.0/docs/install/iam_policy.json
aws iam create-policy \
  --policy-name AWSLoadBalancerControllerIAMPolicy \
  --policy-document file://iam_policy.json

# Install via Helm
helm repo add eks https://aws.github.io/eks-charts
helm repo update
helm install aws-load-balancer-controller eks/aws-load-balancer-controller \
  -n kube-system \
  --set clusterName=modresorts-cluster \
  --set serviceAccount.create=true
```

---

## Kubernetes Deployment Walkthrough

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

The script will:
1. Prompt for AWS region and EKS cluster name
2. Prompt for the full Docker image URI
3. Optionally prompt for environment variable values
4. Configure kubectl
5. Apply all Kubernetes manifests in order
6. Wait for deployment rollout
7. Display the application URL

### Manual Deployment

```bash
# 1. Apply namespace
kubectl apply -f kubernetes/namespace.yaml

# 2. Update image URI in deployment.yaml
sed -i 's|{{IMAGE_URI}}|123456789012.dkr.ecr.us-east-1.amazonaws.com/modresorts:latest|g' kubernetes/deployment.yaml

# 3. Update environment variable placeholders (optional)
sed -i 's|{{WEATHER_API_KEY}}|your_api_key|g' kubernetes/deployment.yaml

# 4. Apply deployment
kubectl apply -f kubernetes/deployment.yaml

# 5. Apply service
kubectl apply -f kubernetes/service.yaml

# 6. Apply ingress
kubectl apply -f kubernetes/ingress.yaml

# 7. Wait for rollout
kubectl rollout status deployment/modresorts -n modresorts

# 8. Verify
kubectl get pods,svc,ingress -n modresorts
```

### Kubernetes Manifest Descriptions

| File | Description |
|------|-------------|
| `namespace.yaml` | Creates the `modresorts` namespace |
| `deployment.yaml` | Deploys 2 replicas of the ModResorts container with health probes |
| `service.yaml` | ClusterIP service exposing port 80 → container port 8080 |
| `ingress.yaml` | AWS ALB Ingress routing external traffic to the service |

---

## Environment Variables Reference

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `WEATHER_API_KEY` | No | (empty) | Weather Underground API key for real-time weather data |
| `JNDI_FACTORY` | No | `com.sun.jndi.rmi.registry.RegistryContextFactory` | JNDI initial context factory |
| `JNDI_PROVIDER_URL` | No | `rmi://localhost:1099` | JNDI provider URL |
| `SERVER_DISPLAY_NAME` | No | (empty) | Server display name for identification |
| `SERVER_FULL_NAME` | No | (empty) | Full server name for identification |
| `JAVA_OPTS` | No | `-Xms256m -Xmx512m ...` | JVM options |
| `TZ` | No | `UTC` | Container timezone |

> **Note**: If `WEATHER_API_KEY` is not set, the application uses pre-loaded static weather data for supported cities (Paris, Las Vegas, San Francisco, Miami, Cork, Barcelona).

---

## EKS Scaling and Management

### Horizontal Scaling

```bash
# Scale manually
kubectl scale deployment modresorts --replicas=3 -n modresorts

# Configure Horizontal Pod Autoscaler
kubectl autoscale deployment modresorts \
  --cpu-percent=70 \
  --min=2 \
  --max=10 \
  -n modresorts

# Check HPA status
kubectl get hpa -n modresorts
```

### Rolling Updates

```bash
# Update image
kubectl set image deployment/modresorts \
  modresorts=123456789012.dkr.ecr.us-east-1.amazonaws.com/modresorts:v2.0.1 \
  -n modresorts

# Monitor rollout
kubectl rollout status deployment/modresorts -n modresorts

# View rollout history
kubectl rollout history deployment/modresorts -n modresorts
```

### Rollback

```bash
# Rollback to previous version
kubectl rollout undo deployment/modresorts -n modresorts

# Rollback to specific revision
kubectl rollout undo deployment/modresorts --to-revision=2 -n modresorts
```

---

## Troubleshooting

### Pod Not Starting

```bash
# Check pod status
kubectl get pods -n modresorts

# Describe pod for events
kubectl describe pod <pod-name> -n modresorts

# View pod logs
kubectl logs <pod-name> -n modresorts
kubectl logs -f deployment/modresorts -n modresorts
```

### Health Check Failures

The application exposes a health endpoint at `/health`:
```bash
# Test health endpoint from within cluster
kubectl exec -it <pod-name> -n modresorts -- sh -c 'wget -qO- http://localhost:8080/health'

# Expected response
{"status":"UP","application":"modresorts"}
```

Common causes:
- **Tomcat startup slow**: Increase `initialDelaySeconds` in deployment.yaml (currently 60s for liveness, 30s for readiness)
- **OOM errors**: Increase memory limits in deployment.yaml
- **WAR deployment failure**: Check logs for class loading errors

### Ingress / ALB Issues

```bash
# Check ingress status
kubectl describe ingress modresorts-ingress -n modresorts

# Verify ALB controller is running
kubectl get pods -n kube-system | grep aws-load-balancer

# Check ALB controller logs
kubectl logs -n kube-system deployment/aws-load-balancer-controller
```

### Image Pull Errors

```bash
# Verify ECR access from EKS nodes
# Ensure worker node IAM role has AmazonEC2ContainerRegistryReadOnly policy

# Check image pull secret if using private registry
kubectl get events -n modresorts | grep "Failed to pull"
```

### Resource Constraints

```bash
# Check resource usage
kubectl top pods -n modresorts
kubectl top nodes

# Describe node for capacity
kubectl describe node <node-name>
```

---

## Security Considerations

1. **Non-root container**: The application runs as `appuser` (non-root) inside the container.
2. **Secrets management**: Use Kubernetes Secrets or AWS Secrets Manager for sensitive values like `WEATHER_API_KEY`.
   ```bash
   kubectl create secret generic modresorts-secrets \
     --from-literal=WEATHER_API_KEY=your_api_key \
     -n modresorts
   ```
3. **Network policies**: Consider adding Kubernetes NetworkPolicy to restrict pod-to-pod communication.
4. **Image scanning**: Enable ECR image scanning to detect vulnerabilities.
5. **RBAC**: Apply least-privilege RBAC policies for service accounts.
6. **TLS**: Configure HTTPS on the ALB ingress using AWS Certificate Manager (ACM):
   ```yaml
   annotations:
     alb.ingress.kubernetes.io/certificate-arn: arn:aws:acm:us-east-1:123456789012:certificate/xxx
     alb.ingress.kubernetes.io/listen-ports: '[{"HTTPS":443}]'
   ```

---

## Java-Specific Notes

### JVM Configuration

The application uses the following JVM flags (configurable via `JAVA_OPTS`):

| Flag | Purpose |
|------|---------|
| `-Xms256m` | Initial heap size |
| `-Xmx512m` | Maximum heap size |
| `-XX:+UseContainerSupport` | Enable container-aware JVM |
| `-XX:MaxRAMPercentage=75.0` | Use 75% of container memory for heap |
| `-XX:+UnlockExperimentalVMOptions` | Enable experimental JVM options |

### Application Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/` | GET | Application home page |
| `/health` | GET | Health check (returns `{"status":"UP"}`) |
| `/resorts/weather?selectedCity=Paris` | GET | Weather data for a city |
| `/resorts/availability?date=MM/dd/yyyy` | GET | Check resort availability |
| `/resorts/welcome` | GET | Welcome message |
| `/upper` | GET | Uppercase utility servlet |

### Supported Cities for Weather

Paris, Las_Vegas, San_Francisco, Miami, Cork, Barcelona

### Build Information

- **Build Tool**: Apache Maven 3.8+
- **Java Version**: 1.8 (Java 8)
- **Package Type**: WAR
- **Runtime**: Apache Tomcat 9.x
- **Artifact**: `target/modresorts-2.0.0.war`

### Tomcat Configuration

The WAR is deployed as `ROOT.war` in Tomcat, making it accessible at the root context path `/`. Tomcat listens on port `8080` inside the container.
