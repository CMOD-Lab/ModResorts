# ModResorts - Deployment Guide

## Overview

This guide covers the complete deployment process for the **ModResorts** Java EE web application on **AWS EKS (Elastic Kubernetes Service)**. ModResorts is a Java 8 servlet-based web application packaged as a WAR file and deployed on Apache Tomcat 9.

- **Application**: ModResorts v2.0.0
- **Technology**: Java 8, Java EE (Servlet 3.1), Apache Tomcat 9
- **Build Tool**: Maven 3.x
- **Package Type**: WAR
- **Context Root**: `/resorts`
- **Application Port**: 8080
- **Health Endpoint**: `/resorts/health`
- **Target Platform**: AWS EKS (Kubernetes)

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

---

## Prerequisites

### Local Development Tools
- **Docker** 20.10+ and **Docker Compose** 2.x
- **Java 8 JDK** (for local builds)
- **Apache Maven** 3.8+

### AWS & Kubernetes Tools
- **AWS CLI** v2 (`aws --version`)
- **kubectl** 1.27+ (`kubectl version --client`)
- **eksctl** 0.150+ (optional, for cluster creation)
- **AWS IAM permissions**: ECR, EKS, EC2, IAM

### Verify Tools
```bash
docker --version
docker compose version
aws --version
kubectl version --client
```

---

## Project Structure

```
ModResorts_MCon/
├── Dockerfile                    # Multi-stage Docker build
├── docker-compose.yml            # Local development compose file
├── .dockerignore                 # Docker build exclusions
├── pom.xml                       # Maven build configuration
├── src/
│   └── main/
│       ├── java/com/acme/modres/ # Java source files
│       └── resources/            # Application resources (ops.json, reservations.json)
├── WebContent/                   # Web assets (HTML, CSS, JS, JSP, WEB-INF)
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
    └── DEPLOYMENT.md             # This guide
```

---

## Local Development with Docker Compose

### 1. Build and Start the Application

```bash
# From the project root directory
docker compose up --build
```

The application will be available at:
- **Application**: http://localhost:8080/resorts/
- **Health Check**: http://localhost:8080/resorts/health

### 2. Environment Variables for Local Development

Create a `.env` file in the project root:

```env
WEATHER_API_KEY=your_weather_api_key_here
SERVER_DISPLAY_NAME=modresorts-local
SERVER_FULL_NAME=modresorts-local-full
JNDI_FACTORY=com.sun.jndi.fscontext.RefFSContextFactory
JNDI_PROVIDER_URL=
```

### 3. Stop the Application

```bash
docker compose down
```

### 4. View Logs

```bash
docker compose logs -f modresorts
```

---

## Build and Push Docker Image

### Linux/macOS

```bash
# Make the script executable
chmod +x scripts/build-push.sh

# Run from project root
./scripts/build-push.sh
```

### Windows

```cmd
scripts\build-push.bat
```

### Script Prompts

The script will interactively ask for:
1. **Image tag** (default: `latest`)
2. **Registry type**: AWS ECR or Docker Hub
3. **Registry credentials** based on selection

### Manual Docker Build

```bash
# Build the image
docker build -t modresorts:latest .

# Tag for ECR
docker tag modresorts:latest <AWS_ACCOUNT_ID>.dkr.ecr.<REGION>.amazonaws.com/modresorts:latest

# Push to ECR
docker push <AWS_ACCOUNT_ID>.dkr.ecr.<REGION>.amazonaws.com/modresorts:latest
```

---

## AWS EKS Prerequisites

### 1. Configure AWS CLI

```bash
aws configure
# Enter: AWS Access Key ID, Secret Access Key, Region, Output format
```

### 2. Required IAM Permissions

Your IAM user/role needs the following policies:
- `AmazonEKSClusterPolicy`
- `AmazonEKSWorkerNodePolicy`
- `AmazonEC2ContainerRegistryFullAccess`
- `AmazonEKS_CNI_Policy`

### 3. Install AWS Load Balancer Controller

The ingress uses AWS ALB. Install the AWS Load Balancer Controller on your EKS cluster:

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

### Option A: Use Existing Cluster

```bash
# Configure kubectl for your existing cluster
aws eks update-kubeconfig --region <AWS_REGION> --name <CLUSTER_NAME>

# Verify connectivity
kubectl cluster-info
kubectl get nodes
```

### Option B: Create New Cluster with eksctl

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

---

## Kubernetes Deployment

### Automated Deployment (Recommended)

#### Linux/macOS

```bash
chmod +x scripts/deploy-image.sh
./scripts/deploy-image.sh
```

#### Windows

```cmd
scripts\deploy-image.bat
```

The script will prompt for:
- AWS Region
- EKS Cluster Name
- Docker Image URI (full path with tag)
- Optional environment variables (WEATHER_API_KEY, etc.)

### Manual Deployment

#### Step 1: Update the deployment manifest

Edit `kubernetes/deployment.yaml` and replace `{{IMAGE_URI}}` with your actual image URI:

```bash
sed -i 's|{{IMAGE_URI}}|<YOUR_IMAGE_URI>|g' kubernetes/deployment.yaml
sed -i 's|{{WEATHER_API_KEY}}|<YOUR_API_KEY>|g' kubernetes/deployment.yaml
sed -i 's|{{SERVER_DISPLAY_NAME}}|modresorts-server|g' kubernetes/deployment.yaml
sed -i 's|{{SERVER_FULL_NAME}}|modresorts-server-full|g' kubernetes/deployment.yaml
sed -i 's|{{JNDI_FACTORY}}|com.sun.jndi.fscontext.RefFSContextFactory|g' kubernetes/deployment.yaml
sed -i 's|{{JNDI_PROVIDER_URL}}||g' kubernetes/deployment.yaml
```

#### Step 2: Apply manifests in order

```bash
kubectl apply -f kubernetes/namespace.yaml
kubectl apply -f kubernetes/deployment.yaml
kubectl apply -f kubernetes/service.yaml
kubectl apply -f kubernetes/ingress.yaml
```

#### Step 3: Verify deployment

```bash
# Check rollout status
kubectl rollout status deployment/modresorts -n modresorts

# Check all resources
kubectl get pods,svc,ingress -n modresorts

# Get ingress hostname
kubectl get ingress modresorts-ingress -n modresorts
```

---

## Configuration Management

### Environment Variables

| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| `WEATHER_API_KEY` | API key for Wunderground weather service | (empty) | No |
| `SERVER_DISPLAY_NAME` | Server display name for logging | `modresorts-server` | No |
| `SERVER_FULL_NAME` | Full server name for logging | `modresorts-server-full` | No |
| `JNDI_FACTORY` | JNDI initial context factory | `com.sun.jndi.fscontext.RefFSContextFactory` | No |
| `JNDI_PROVIDER_URL` | JNDI provider URL | (empty) | No |
| `JAVA_OPTS` | JVM options | `-Xmx512m -Xms256m -XX:+UseContainerSupport` | No |
| `TZ` | Timezone | `UTC` | No |

### Using Kubernetes Secrets for Sensitive Values

```bash
# Create a secret for the weather API key
kubectl create secret generic modresorts-secrets \
  --from-literal=WEATHER_API_KEY=your_api_key_here \
  -n modresorts
```

Then reference in deployment.yaml:
```yaml
env:
  - name: WEATHER_API_KEY
    valueFrom:
      secretKeyRef:
        name: modresorts-secrets
        key: WEATHER_API_KEY
```

---

## Scaling and Management

### Manual Scaling

```bash
# Scale to 3 replicas
kubectl scale deployment modresorts --replicas=3 -n modresorts
```

### Horizontal Pod Autoscaler (HPA)

```bash
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
  modresorts=<NEW_IMAGE_URI> \
  -n modresorts

# Monitor rollout
kubectl rollout status deployment/modresorts -n modresorts
```

### Rollback

```bash
# Rollback to previous version
kubectl rollout undo deployment/modresorts -n modresorts

# Rollback to specific revision
kubectl rollout history deployment/modresorts -n modresorts
kubectl rollout undo deployment/modresorts --to-revision=2 -n modresorts
```

---

## Troubleshooting

### Pod Not Starting

```bash
# Check pod status
kubectl get pods -n modresorts

# Describe pod for events
kubectl describe pod <POD_NAME> -n modresorts

# Check pod logs
kubectl logs <POD_NAME> -n modresorts
kubectl logs -l app=modresorts -n modresorts --tail=100
```

### Application Not Accessible

```bash
# Check service
kubectl get svc -n modresorts
kubectl describe svc modresorts-service -n modresorts

# Check ingress
kubectl get ingress -n modresorts
kubectl describe ingress modresorts-ingress -n modresorts

# Port-forward for local testing
kubectl port-forward svc/modresorts-service 8080:80 -n modresorts
# Then access: http://localhost:8080/resorts/
```

### Health Check Failures

```bash
# Test health endpoint directly
kubectl exec -it <POD_NAME> -n modresorts -- \
  sh -c 'wget -qO- http://localhost:8080/resorts/health'

# Check Tomcat logs
kubectl logs <POD_NAME> -n modresorts | grep -i error
```

### JVM Memory Issues

```bash
# Check resource usage
kubectl top pods -n modresorts

# Increase memory limits in deployment.yaml
# resources.limits.memory: "2Gi"
# JAVA_OPTS: "-Xmx1536m -Xms512m"
```

### Image Pull Errors

```bash
# Check ECR authentication
aws ecr get-login-password --region <REGION> | \
  docker login --username AWS --password-stdin \
  <ACCOUNT_ID>.dkr.ecr.<REGION>.amazonaws.com

# Verify image exists
aws ecr describe-images --repository-name modresorts --region <REGION>
```

---

## Security Considerations

1. **Non-root Container**: The application runs as a non-root user (`modresorts`) inside the container.

2. **Secrets Management**: Use Kubernetes Secrets or AWS Secrets Manager for sensitive values like `WEATHER_API_KEY`. Never hardcode secrets in manifests.

3. **Network Policies**: Consider adding Kubernetes NetworkPolicies to restrict pod-to-pod communication.

4. **Image Scanning**: Enable ECR image scanning to detect vulnerabilities:
   ```bash
   aws ecr put-image-scanning-configuration \
     --repository-name modresorts \
     --image-scanning-configuration scanOnPush=true \
     --region <REGION>
   ```

5. **RBAC**: Apply least-privilege RBAC policies for the application's service account.

6. **TLS/HTTPS**: Configure HTTPS on the ALB ingress:
   ```yaml
   annotations:
     alb.ingress.kubernetes.io/listen-ports: '[{"HTTPS": 443}]'
     alb.ingress.kubernetes.io/certificate-arn: arn:aws:acm:<REGION>:<ACCOUNT>:certificate/<CERT_ID>
   ```

7. **Application Security**: The `web.xml` has security constraints commented out for demo purposes. Enable them in production by uncommenting the `<security-constraint>` sections.

---

## Java-Specific Notes

### JVM Container Awareness

The Dockerfile sets the following JVM flags for container-aware memory management:
```
-XX:+UseContainerSupport
-XX:MaxRAMPercentage=75.0
-XX:+UnlockExperimentalVMOptions
```

These ensure the JVM respects container memory limits rather than using host memory.

### Tomcat Configuration

The application is deployed on Apache Tomcat 9 with the WAR deployed at context root `/resorts`. The default Tomcat port is 8080.

### Startup Time

Java EE applications on Tomcat typically take 30-60 seconds to start. The Kubernetes probes are configured with:
- **Liveness probe**: `initialDelaySeconds: 60`
- **Readiness probe**: `initialDelaySeconds: 30`

Adjust these values if your environment requires longer startup times.

### Weather API Integration

The application integrates with the Wunderground weather API. Set `WEATHER_API_KEY` environment variable to enable real-time weather data. Without it, the application uses static default weather data from `WebContent/data/`.
