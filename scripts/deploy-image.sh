#!/bin/bash
set -e
set -o pipefail

# ============================================================
# ModResorts - Deploy to AWS EKS Script
# ============================================================

echo "============================================"
echo "  ModResorts - Deploy to AWS EKS"
echo "============================================"
echo ""

# Prompt for AWS region
read -p "Enter AWS Region (e.g. us-east-1): " AWS_REGION
if [ -z "$AWS_REGION" ]; then
  echo "ERROR: AWS Region cannot be empty."
  exit 1
fi

# Prompt for EKS cluster name
read -p "Enter EKS Cluster Name: " CLUSTER_NAME
if [ -z "$CLUSTER_NAME" ]; then
  echo "ERROR: EKS Cluster Name cannot be empty."
  exit 1
fi

# Prompt for Docker image URI
read -p "Enter full Docker image URI (e.g. 123456789.dkr.ecr.us-east-1.amazonaws.com/modresorts:latest): " IMAGE_URI
if [ -z "$IMAGE_URI" ]; then
  echo "ERROR: Docker image URI cannot be empty."
  exit 1
fi

echo ""
echo "--------------------------------------------"
echo "  Optional: Application Environment Variables"
echo "  (Press Enter to skip any variable)"
echo "--------------------------------------------"

read -p "Enter WEATHER_API_KEY value (or press Enter to skip): " WEATHER_API_KEY_VAL
read -p "Enter DB_URL value (or press Enter to skip): " DB_URL_VAL
read -p "Enter DB_USERNAME value (or press Enter to skip): " DB_USERNAME_VAL
read -sp "Enter DB_PASSWORD value (or press Enter to skip): " DB_PASSWORD_VAL
echo ""

echo ""
echo "--------------------------------------------"
echo "  Configuring kubectl for EKS cluster..."
echo "--------------------------------------------"
aws eks update-kubeconfig --region "$AWS_REGION" --name "$CLUSTER_NAME"
if [ $? -ne 0 ]; then
  echo "ERROR: Failed to configure kubectl for EKS cluster."
  exit 1
fi

echo "Verifying cluster connectivity..."
kubectl cluster-info || { echo "ERROR: Cannot connect to Kubernetes cluster."; exit 1; }

echo ""
echo "--------------------------------------------"
echo "  Updating Kubernetes manifests..."
echo "--------------------------------------------"

# Replace IMAGE_URI placeholder
sed -i 's|{{IMAGE_URI}}|'"$IMAGE_URI"'|g' kubernetes/deployment.yaml

# Replace environment variable placeholders
if [ -n "$WEATHER_API_KEY_VAL" ]; then
  sed -i 's|{{WEATHER_API_KEY}}|'"$WEATHER_API_KEY_VAL"'|g' kubernetes/deployment.yaml
else
  sed -i 's|{{WEATHER_API_KEY}}||g' kubernetes/deployment.yaml
fi

if [ -n "$DB_URL_VAL" ]; then
  sed -i 's|{{DB_URL}}|'"$DB_URL_VAL"'|g' kubernetes/deployment.yaml
else
  sed -i 's|{{DB_URL}}||g' kubernetes/deployment.yaml
fi

if [ -n "$DB_USERNAME_VAL" ]; then
  sed -i 's|{{DB_USERNAME}}|'"$DB_USERNAME_VAL"'|g' kubernetes/deployment.yaml
else
  sed -i 's|{{DB_USERNAME}}||g' kubernetes/deployment.yaml
fi

if [ -n "$DB_PASSWORD_VAL" ]; then
  sed -i 's|{{DB_PASSWORD}}|'"$DB_PASSWORD_VAL"'|g' kubernetes/deployment.yaml
else
  sed -i 's|{{DB_PASSWORD}}||g' kubernetes/deployment.yaml
fi

echo ""
echo "--------------------------------------------"
echo "  Applying Kubernetes manifests..."
echo "--------------------------------------------"

echo "Applying namespace..."
kubectl apply -f kubernetes/namespace.yaml

echo "Applying deployment..."
kubectl apply -f kubernetes/deployment.yaml

echo "Applying service..."
kubectl apply -f kubernetes/service.yaml

echo "Applying ingress..."
kubectl apply -f kubernetes/ingress.yaml

echo ""
echo "--------------------------------------------"
echo "  Waiting for deployment rollout..."
echo "--------------------------------------------"
kubectl rollout status deployment/modresorts -n modresorts --timeout=300s
if [ $? -ne 0 ]; then
  echo "ERROR: Deployment rollout failed. Rolling back..."
  kubectl rollout undo deployment/modresorts -n modresorts
  echo "Rollback initiated. Check pod status with: kubectl get pods -n modresorts"
  exit 1
fi

echo ""
echo "--------------------------------------------"
echo "  Verifying deployed resources..."
echo "--------------------------------------------"
kubectl get pods,svc,ingress -n modresorts

echo ""
echo "--------------------------------------------"
echo "  Retrieving application URL..."
echo "--------------------------------------------"
INGRESS_HOST=$(kubectl get ingress modresorts-ingress -n modresorts -o jsonpath='{.status.loadBalancer.ingress[0].hostname}' 2>/dev/null || echo "pending")
if [ "$INGRESS_HOST" = "pending" ] || [ -z "$INGRESS_HOST" ]; then
  echo "Ingress hostname is still being provisioned. Check later with:"
  echo "  kubectl get ingress modresorts-ingress -n modresorts"
else
  echo "Application URL: http://$INGRESS_HOST"
fi

echo ""
echo "============================================"
echo "  SUCCESS: ModResorts deployed to EKS!"
echo "============================================"
echo ""
echo "Useful commands:"
echo "  kubectl get pods -n modresorts"
echo "  kubectl logs -f deployment/modresorts -n modresorts"
echo "  kubectl describe deployment modresorts -n modresorts"
echo "  kubectl rollout undo deployment/modresorts -n modresorts  # Rollback"
