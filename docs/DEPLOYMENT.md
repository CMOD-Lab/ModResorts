# ModResorts - GCP GKE Deployment Guide

## Overview

This guide covers building, containerizing, and deploying the **ModResorts** Java EE web application to **Google Kubernetes Engine (GKE)**. ModResorts is a Java 8 servlet-based web application packaged as a WAR file and deployed on Apache Tomcat 9.

---

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Project Structure](#project-structure)
3. [Local Development with Docker Compose](#local-development-with-docker-compose)
4. [Build and Push Docker Image](#build-and-push-docker-image)
5. [GCP GKE Prerequisites](#gcp-gke-prerequisites)
6. [GKE Cluster Setup](#gke-cluster-setup)
7. [Kubernetes Deployment](#kubernetes-deployment)
8. [Configuration Management](#configuration-management)
9. [Scaling and Management](#scaling-and-management)
10. [Troubleshooting](#troubleshooting)
11. [Security Considerations](#security-considerations)
12. [Java-Specific Notes](#java-specific-notes)

---

## Prerequisites

### Local Development Tools
- **Docker** 20.10+ and **Docker Compose** 2.x
- **Java 8 JDK** (for local builds)
- **Apache Maven 3.9+**
- **Git**

### GCP Deployment Tools
- **Google Cloud SDK (gcloud CLI)** - [Install Guide](https://cloud.google.com/sdk/docs/install)
- **kubectl** - Install via `gcloud components install kubectl`
- **GCP Account** with billing enabled
- **GCP Project** with the following APIs enabled:
  - Kubernetes Engine API
  - Artifact Registry API (if using Google Artifact Registry)
  - Container Registry API

---

## Project Structure

```
ModResorts/
├── Dockerfile                    # Multi-stage Docker build
├── docker-compose.yml            # Local development compose file
├── .dockerignore                 # Docker build exclusions
├── pom.xml                       # Maven build configuration
├── src/                          # Java source code
│   └── main/java/com/acme/modres/
│       ├── HealthServlet.java    # Health check endpoint: GET /resorts/health
│       ├── WeatherServlet.java   # Weather data endpoint
│       ├── WelcomeServlet.java   # Welcome endpoint
│       └── ...
├── WebContent/                   # Web resources (HTML, CSS, JS, WEB-INF)
├── kubernetes/                   # Kubernetes manifests
│   ├── namespace.yaml
│   ├── deployment.yaml
│   ├── service.yaml
│   └── ingress.yaml
├── scripts/                      # Build and deployment scripts
│   ├── build-push.sh
│   ├── build-push.bat
│   ├── deploy-image.sh
│   └── deploy-image.bat
└── docs/
    └── DEPLOYMENT.md             # This file
```

---

## Local Development with Docker Compose

### 1. Build and Start the Application

```bash
# From the project root directory
docker-compose up --build
```

### 2. Access the Application

| Endpoint | URL |
|----------|-----|
| Application Home | http://localhost:8080/resorts/ |
| Health Check | http://localhost:8080/resorts/health |
| Weather API | http://localhost:8080/resorts/weather?selectedCity=Paris |
| Availability | http://localhost:8080/resorts/availability |

### 3. Configure Environment Variables

Create a `.env` file in the project root:

```env
WEATHER_API_KEY=your_weather_api_key_here
SERVER_DISPLAY_NAME=modresorts-local
SERVER_FULL_NAME=modresorts-local-server
JNDI_FACTORY=com.sun.jndi.rmi.registry.RegistryContextFactory
JNDI_PROVIDER_URL=rmi://localhost:1099
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
1. Select registry type (Google Artifact Registry or Docker Hub)
2. Enter registry credentials and details
3. Enter an image tag (defaults to `latest`)

### Windows

```cmd
scripts\build-push.bat
```

### Manual Build

```bash
# Build the image
docker build -t modresorts:latest .

# Tag for Google Artifact Registry
docker tag modresorts:latest us-central1-docker.pkg.dev/YOUR_PROJECT/YOUR_REPO/modresorts:latest

# Push
docker push us-central1-docker.pkg.dev/YOUR_PROJECT/YOUR_REPO/modresorts:latest
```

---

## GCP GKE Prerequisites

### 1. Install and Configure gcloud CLI

```bash
# Install gcloud CLI (if not already installed)
# https://cloud.google.com/sdk/docs/install

# Authenticate
gcloud auth login

# Set your project
gcloud config set project YOUR_GCP_PROJECT_ID

# Install kubectl
gcloud components install kubectl
```

### 2. Enable Required APIs

```bash
gcloud services enable container.googleapis.com
gcloud services enable artifactregistry.googleapis.com
```

### 3. Create Artifact Registry Repository (Optional)

```bash
gcloud artifacts repositories create modresorts-repo \
  --repository-format=docker \
  --location=us-central1 \
  --description="ModResorts Docker images"
```

---

## GKE Cluster Setup

### 1. Create a GKE Cluster

```bash
gcloud container clusters create modresorts-cluster \
  --zone us-central1-a \
  --num-nodes 2 \
  --machine-type e2-standard-2 \
  --enable-autoscaling \
  --min-nodes 1 \
  --max-nodes 5
```

### 2. Configure kubectl

```bash
gcloud container clusters get-credentials modresorts-cluster \
  --zone us-central1-a \
  --project YOUR_GCP_PROJECT_ID
```

### 3. Verify Connectivity

```bash
kubectl cluster-info
kubectl get nodes
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
- GCP Project ID
- GCP Zone
- GKE Cluster Name
- Full Docker image URI
- Optional: `WEATHER_API_KEY`

### Manual Deployment

#### 1. Update the Deployment Manifest

Edit `kubernetes/deployment.yaml` and replace `{{IMAGE_URI}}` with your actual image URI:

```bash
sed -i 's|{{IMAGE_URI}}|us-central1-docker.pkg.dev/YOUR_PROJECT/YOUR_REPO/modresorts:latest|g' kubernetes/deployment.yaml
sed -i 's|{{WEATHER_API_KEY}}|your_api_key_here|g' kubernetes/deployment.yaml
```

#### 2. Apply Manifests in Order

```bash
kubectl apply -f kubernetes/namespace.yaml
kubectl apply -f kubernetes/deployment.yaml
kubectl apply -f kubernetes/service.yaml
kubectl apply -f kubernetes/ingress.yaml
```

#### 3. Monitor Rollout

```bash
kubectl rollout status deployment/modresorts -n modresorts
```

#### 4. Verify Resources

```bash
kubectl get pods,svc,ingress -n modresorts
```

### Kubernetes Manifest Descriptions

| File | Description |
|------|-------------|
| `namespace.yaml` | Creates the `modresorts` namespace |
| `deployment.yaml` | Deploys 2 replicas with liveness/readiness probes on `/resorts/health` |
| `service.yaml` | ClusterIP service exposing port 80 → 8080 |
| `ingress.yaml` | GKE Ingress (GCE) routing traffic to the service |

---

## Configuration Management

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `WEATHER_API_KEY` | Weather Underground API key | *(empty - uses mock data)* |
| `SERVER_DISPLAY_NAME` | Server display name for identification | `modresorts` |
| `SERVER_FULL_NAME` | Full server name | `modresorts-server` |
| `JNDI_FACTORY` | JNDI initial context factory | `com.sun.jndi.rmi.registry.RegistryContextFactory` |
| `JNDI_PROVIDER_URL` | JNDI provider URL | `rmi://localhost:1099` |
| `JAVA_OPTS` | JVM options | See Dockerfile |
| `TZ` | Timezone | `UTC` |

### Using Kubernetes Secrets for Sensitive Values

```bash
# Create a secret for the Weather API key
kubectl create secret generic modresorts-secrets \
  --from-literal=WEATHER_API_KEY=your_api_key_here \
  -n modresorts
```

Then update `kubernetes/deployment.yaml` to reference the secret:

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
kubectl scale deployment modresorts --replicas=3 -n modresorts
```

### Horizontal Pod Autoscaler (HPA)

```bash
kubectl autoscale deployment modresorts \
  --cpu-percent=70 \
  --min=2 \
  --max=10 \
  -n modresorts
```

### Rolling Update

```bash
# Update the image
kubectl set image deployment/modresorts \
  modresorts=us-central1-docker.pkg.dev/YOUR_PROJECT/YOUR_REPO/modresorts:v2.0.1 \
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
kubectl describe pod <pod-name> -n modresorts

# Check logs
kubectl logs <pod-name> -n modresorts
kubectl logs -f deployment/modresorts -n modresorts
```

### Common Issues

#### 1. ImagePullBackOff
- Verify the image URI is correct
- Ensure GKE has permission to pull from Artifact Registry:
  ```bash
  gcloud projects add-iam-policy-binding YOUR_PROJECT \
    --member="serviceAccount:YOUR_GKE_SA@YOUR_PROJECT.iam.gserviceaccount.com" \
    --role="roles/artifactregistry.reader"
  ```

#### 2. CrashLoopBackOff
- Check application logs: `kubectl logs <pod-name> -n modresorts`
- Verify Tomcat started correctly
- Check JVM memory settings (increase if OOMKilled)

#### 3. Health Check Failures
- The health endpoint is at `/resorts/health`
- Tomcat startup can take 30-60 seconds; `initialDelaySeconds: 60` is set for liveness probe
- Check if WAR deployed correctly: look for `INFO: Deployment of web application archive` in logs

#### 4. Ingress Not Getting External IP
- GKE Ingress (GCE) can take 5-10 minutes to provision
- Check: `kubectl describe ingress modresorts-ingress -n modresorts`
- Ensure the GKE cluster has HTTP load balancing enabled

#### 5. OOMKilled (Out of Memory)
```bash
# Increase memory limits in deployment.yaml
resources:
  limits:
    memory: "2Gi"
```

### Useful Diagnostic Commands

```bash
# Get all resources in namespace
kubectl get all -n modresorts

# Check ingress details
kubectl describe ingress modresorts-ingress -n modresorts

# Execute shell in running pod
kubectl exec -it <pod-name> -n modresorts -- /bin/bash

# Check resource usage
kubectl top pods -n modresorts
kubectl top nodes
```

---

## Security Considerations

1. **Non-root User**: The container runs as a non-root user (`appuser`) for security.
2. **Secrets Management**: Use Kubernetes Secrets (or GCP Secret Manager) for sensitive values like `WEATHER_API_KEY`.
3. **Network Policies**: Consider adding Kubernetes NetworkPolicies to restrict pod-to-pod communication.
4. **Image Scanning**: Enable Artifact Registry vulnerability scanning for your images.
5. **RBAC**: Apply least-privilege RBAC policies for the application's service account.
6. **TLS/HTTPS**: Configure GKE Managed Certificates for HTTPS:
   ```bash
   gcloud compute ssl-certificates create modresorts-cert \
     --domains=modresorts.example.com
   ```
7. **Security Context**: The deployment enforces `runAsNonRoot: true` and `runAsUser: 1000`.

---

## Java-Specific Notes

### JVM Configuration

The application uses the following JVM flags (set via `JAVA_OPTS`):

| Flag | Purpose |
|------|---------|
| `-Xmx512m` | Maximum heap size |
| `-Xms256m` | Initial heap size |
| `-XX:+UseContainerSupport` | Enable container-aware JVM |
| `-XX:MaxRAMPercentage=75.0` | Use 75% of container RAM for heap |
| `-XX:+UnlockExperimentalVMOptions` | Enable experimental JVM options |
| `-Djava.security.egd=file:/dev/./urandom` | Faster random number generation |
| `-Dfile.encoding=UTF-8` | UTF-8 encoding |
| `-Duser.timezone=UTC` | UTC timezone |

### Application Context

The application is deployed at context root `/resorts` (configured in `ibm-web-ext.xml`).

| Endpoint | Path |
|----------|------|
| Home | `/resorts/` |
| Health | `/resorts/health` |
| Weather | `/resorts/weather?selectedCity=Paris` |
| Availability | `/resorts/availability` |
| Welcome | `/resorts/welcome` |

### Weather API

The application supports real-time weather data via the Weather Underground API. Set `WEATHER_API_KEY` to enable it. Without the key, the application uses static mock weather data from `WebContent/data/`.

### Build Details

- **Build Tool**: Apache Maven 3.9.4
- **Java Version**: 8 (eclipse-temurin)
- **Packaging**: WAR (`modresorts-2.0.0.war`)
- **Runtime**: Apache Tomcat 9 (Servlet 3.1 / Java EE 7 compatible)
- **Builder Image**: `maven:3.9.4-eclipse-temurin-8`
- **Runtime Image**: `eclipse-temurin:8-jdk` (explicit)

---

*Generated for ModResorts v2.0.0 | Target Platform: GCP GKE*
