# ModResorts - Deployment Guide

## Overview

This guide covers the complete deployment process for the **ModResorts** application — a Java EE 7 web application (WAR) built with Maven and Java 8. The application provides resort booking, weather information, and availability checking features.

- **Application**: ModResorts v2.0.0
- **Technology**: Java 8, Java EE 7 (Servlet/CDI), Maven
- **Package**: WAR (Web Application Archive)
- **Target Platform**: AWS EKS (Elastic Kubernetes Service)
- **Health Endpoint**: `GET /health`
- **Application Port**: 8080

---

## Prerequisites

### Local Development
- Docker Desktop 20.10+
- Docker Compose 2.x
- Java 8 JDK (for local builds)
- Maven 3.6+

### AWS EKS Deployment
- AWS CLI v2 configured with appropriate IAM permissions
- `kubectl` 1.24+
- `eksctl` (optional, for cluster creation)
- An existing EKS cluster or permissions to create one
- AWS ECR or Docker Hub account for image registry

### Required IAM Permissions
```
ecr:GetAuthorizationToken
ecr:BatchCheckLayerAvailability
ecr:GetDownloadUrlForLayer
ecr:BatchGetImage
ecr:PutImage
ecr:InitiateLayerUpload
ecr:UploadLayerPart
ecr:CompleteLayerUpload
ecr:CreateRepository
ecr:DescribeRepositories
eks:DescribeCluster
eks:ListClusters
```

---

## Project Structure

```
fullapp/
├── Dockerfile                  # Multi-stage Docker build
├── docker-compose.yml          # Local development compose file
├── .dockerignore               # Docker build exclusions
├── pom.xml                     # Maven build configuration
├── src/                        # Java source code
│   └── main/java/com/acme/modres/
│       ├── HealthCheckServlet.java   # GET /health endpoint
│       ├── WeatherServlet.java       # GET /resorts/weather
│       ├── AvailabilityCheckerServlet.java
│       └── ...
├── WebContent/                 # Web resources (HTML, CSS, JS)
├── kubernetes/                 # Kubernetes manifests
│   ├── namespace.yaml
│   ├── deployment.yaml
│   ├── service.yaml
│   └── ingress.yaml
├── scripts/                    # Build and deploy scripts
│   ├── build-push.sh
│   ├── build-push.bat
│   ├── deploy-image.sh
│   └── deploy-image.bat
└── docs/
    └── DEPLOYMENT.md           # This file
```

---

## Local Development with Docker Compose

### Step 1: Build and Start the Application

```bash
# Build and start the application container
docker-compose up --build

# Run in detached mode
docker-compose up --build -d
```

### Step 2: Access the Application

- **Application**: http://localhost:8080
- **Health Check**: http://localhost:8080/health
- **Weather API**: http://localhost:8080/resorts/weather?selectedCity=Paris
- **Availability**: http://localhost:8080/resorts/availability?date=12/25/2024

### Step 3: Configure Environment Variables

Create a `.env` file in the project root:

```env
# Weather API (optional - uses default data if not set)
WEATHER_API_KEY=your_weather_api_key_here

# Server identification
SERVER_DISPLAY_NAME=modresorts-local
SERVER_FULL_NAME=modresorts-local-server

# Database connection (if using customer information feature)
DB_URL=jdbc:postgresql://your-db-host:5432/modresorts
DB_USERNAME=dbuser
DB_PASSWORD=dbpassword
```

### Step 4: Stop the Application

```bash
docker-compose down

# Remove volumes as well
docker-compose down -v
```

---

## Building the Docker Image Manually

```bash
# Build the image
docker build -t modresorts:latest .

# Run the container
docker run -p 8080:8080 \
  -e WEATHER_API_KEY=your_key \
  -e TZ=UTC \
  modresorts:latest

# Verify health
curl http://localhost:8080/health
```

---

## Build and Push to Registry

### Linux/macOS

```bash
# Make the script executable
chmod +x scripts/build-push.sh

# Run the script (from project root)
./scripts/build-push.sh
```

The script will prompt you to:
1. Enter an image tag (defaults to `latest`)
2. Select registry type (AWS ECR or Docker Hub)
3. Provide registry credentials

### Windows

```cmd
# Run from project root
scripts\build-push.bat
```

---

## AWS EKS Deployment

### Step 1: Configure AWS CLI

```bash
aws configure
# Enter: AWS Access Key ID, Secret Access Key, Region, Output format
```

### Step 2: Create or Connect to EKS Cluster

**Create a new cluster (if needed):**
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

**Connect to existing cluster:**
```bash
aws eks update-kubeconfig --region us-east-1 --name your-cluster-name
kubectl cluster-info
```

### Step 3: Install AWS Load Balancer Controller (for Ingress)

```bash
# Add the EKS chart repo
helm repo add eks https://aws.github.io/eks-charts
helm repo update

# Install the controller
helm install aws-load-balancer-controller eks/aws-load-balancer-controller \
  -n kube-system \
  --set clusterName=your-cluster-name \
  --set serviceAccount.create=false \
  --set serviceAccount.name=aws-load-balancer-controller
```

### Step 4: Build and Push the Docker Image

```bash
chmod +x scripts/build-push.sh
./scripts/build-push.sh
# Select option 1 (AWS ECR) and follow prompts
```

### Step 5: Deploy to EKS

**Linux/macOS:**
```bash
chmod +x scripts/deploy-image.sh
./scripts/deploy-image.sh
```

**Windows:**
```cmd
scripts\deploy-image.bat
```

The deploy script will:
1. Prompt for AWS region and EKS cluster name
2. Prompt for the full Docker image URI
3. Optionally prompt for environment variable values
4. Configure `kubectl` for your EKS cluster
5. Apply all Kubernetes manifests in order
6. Wait for the deployment rollout to complete
7. Display the application URL

### Step 6: Verify Deployment

```bash
# Check all resources in the namespace
kubectl get all -n modresorts

# Check pod status
kubectl get pods -n modresorts

# Check pod logs
kubectl logs -f deployment/modresorts -n modresorts

# Check ingress and get URL
kubectl get ingress -n modresorts

# Test health endpoint
kubectl port-forward svc/modresorts-service 8080:80 -n modresorts
curl http://localhost:8080/health
```

---

## Kubernetes Manifest Descriptions

### namespace.yaml
Creates the `modresorts` namespace to isolate all application resources.

### deployment.yaml
Defines the application deployment with:
- **2 replicas** for high availability
- **Resource limits**: CPU 500m, Memory 1Gi
- **Resource requests**: CPU 250m, Memory 512Mi
- **Liveness probe**: `GET /health` (starts after 60s, every 30s)
- **Readiness probe**: `GET /health` (starts after 30s, every 15s)
- **Non-root security context** (UID 1000)
- **Environment variables** for application configuration

### service.yaml
Creates a `ClusterIP` service that routes traffic to port 8080 on the pods.

### ingress.yaml
Creates an AWS ALB Ingress with:
- Internet-facing load balancer
- Health check on `/health`
- Routes all traffic to `modresorts-service`

---

## Configuration Management

### Environment Variables

| Variable | Description | Required | Default |
|----------|-------------|----------|---------|
| `WEATHER_API_KEY` | Weather Underground API key | No | Uses default data |
| `SERVER_DISPLAY_NAME` | Server display name | No | `modresorts` |
| `SERVER_FULL_NAME` | Server full name | No | `modresorts-server` |
| `DB_URL` | Database JDBC URL | No | - |
| `DB_USERNAME` | Database username | No | - |
| `DB_PASSWORD` | Database password | No | - |
| `TZ` | Timezone | No | `UTC` |
| `JAVA_OPTS` | JVM options | No | See Dockerfile |

### JVM Configuration

The application uses the following JVM settings:
```
-Xmx512m              # Maximum heap size
-Xms256m              # Initial heap size
-XX:+UseContainerSupport          # Container-aware memory
-XX:MaxRAMPercentage=75.0         # Use 75% of container RAM
-XX:+UnlockExperimentalVMOptions  # Enable experimental options
```

---

## Scaling and Management

### Horizontal Pod Autoscaling

```bash
# Create HPA based on CPU usage
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
# Update the image
kubectl set image deployment/modresorts \
  modresorts=your-registry/modresorts:new-tag \
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
# Check pod events
kubectl describe pod <pod-name> -n modresorts

# Check pod logs
kubectl logs <pod-name> -n modresorts

# Check previous container logs (if crashed)
kubectl logs <pod-name> -n modresorts --previous
```

### Common Issues

**Issue: ImagePullBackOff**
```bash
# Verify image URI is correct
kubectl describe pod <pod-name> -n modresorts | grep Image

# Check ECR permissions
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin <account>.dkr.ecr.us-east-1.amazonaws.com
```

**Issue: CrashLoopBackOff**
```bash
# Check application logs
kubectl logs <pod-name> -n modresorts --previous

# Common cause: JVM out of memory - increase memory limits in deployment.yaml
```

**Issue: Health Check Failing**
```bash
# Test health endpoint directly
kubectl port-forward <pod-name> 8080:8080 -n modresorts
curl http://localhost:8080/health
# Expected: {"status":"UP","application":"modresorts"}
```

**Issue: Ingress Not Getting External IP**
```bash
# Check AWS Load Balancer Controller is installed
kubectl get pods -n kube-system | grep aws-load-balancer

# Check ingress events
kubectl describe ingress modresorts-ingress -n modresorts
```

**Issue: Service Unavailable**
```bash
# Check service endpoints
kubectl get endpoints modresorts-service -n modresorts

# Verify pod labels match service selector
kubectl get pods -n modresorts --show-labels
```

---

## Security Considerations

1. **Non-root container**: The application runs as UID 1000 (non-root)
2. **Secrets management**: Use Kubernetes Secrets or AWS Secrets Manager for sensitive values (DB passwords, API keys)
3. **Network policies**: Consider adding Kubernetes NetworkPolicies to restrict pod-to-pod communication
4. **Image scanning**: Enable ECR image scanning for vulnerability detection
5. **RBAC**: Apply least-privilege RBAC policies for service accounts
6. **TLS**: Configure HTTPS on the ALB ingress for production deployments

### Using Kubernetes Secrets for Sensitive Data

```bash
# Create a secret for database credentials
kubectl create secret generic modresorts-secrets \
  --from-literal=DB_PASSWORD=your_password \
  --from-literal=WEATHER_API_KEY=your_api_key \
  -n modresorts
```

Then reference in deployment.yaml:
```yaml
env:
  - name: DB_PASSWORD
    valueFrom:
      secretKeyRef:
        name: modresorts-secrets
        key: DB_PASSWORD
```

---

## Java-Specific Notes

- **Java Version**: Java 8 (Amazon Corretto 8)
- **Packaging**: WAR file deployed to embedded servlet container
- **Framework**: Java EE 7 (Servlet 3.1, CDI)
- **Build Tool**: Maven 3.9.4
- **JVM startup time**: Allow 60 seconds for initial liveness probe
- **Garbage Collection**: Container-aware GC enabled via `-XX:+UseContainerSupport`
- **Memory**: Container memory limits are respected via `MaxRAMPercentage`

---

## Support

For issues or questions:
1. Check pod logs: `kubectl logs -f deployment/modresorts -n modresorts`
2. Review Kubernetes events: `kubectl get events -n modresorts --sort-by='.lastTimestamp'`
3. Verify health endpoint: `curl http://<app-url>/health`
