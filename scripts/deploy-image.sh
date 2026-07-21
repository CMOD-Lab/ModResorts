#!/bin/bash
set -e
set -o pipefail

# ============================================================
# deploy-image.sh - Deploy ModResorts to AWS EKS
# ============================================================

APP_NAME="modresorts"
NAMESPACE="modresorts"
K8S_DIR="kubernetes"

echo "=============================================="
echo "  ModResorts - AWS EKS Deployment Script"
echo "=============================================="
echo ""

# ---- Collect AWS / EKS configuration ----
read -p "Enter AWS Region (e.g. us-east-1): " AWS_REGION
if [ -z "$AWS_REGION" ]; then
  echo "ERROR: AWS Region is required."
  exit 1
fi

read -p "Enter EKS Cluster Name: " CLUSTER_NAME
if [ -z "$CLUSTER_NAME" ]; then
  echo "ERROR: EKS Cluster Name is required."
  exit 1
fi

read -p "Enter full Docker image URI (e.g. 123456789.dkr.ecr.us-east-1.amazonaws.com/modresorts:latest): " IMAGE_URI
if [ -z "$IMAGE_URI" ]; then
  echo "ERROR: Docker image URI is required."
  exit 1
fi

echo ""
echo "---- Optional Environment Variables ----"
echo "Press Enter to skip any variable (placeholder will remain in manifest)."
echo ""

read -p "Enter WEATHER_API_KEY value (or press Enter to skip): " WEATHER_API_KEY_VAL
read -p "Enter JNDI_FACTORY value (or press Enter to skip): " JNDI_FACTORY_VAL
read -p "Enter JNDI_PROVIDER_URL value (or press Enter to skip): " JNDI_PROVIDER_URL_VAL
read -p "Enter SERVER_DISPLAY_NAME value (or press Enter to skip): " SERVER_DISPLAY_NAME_VAL
read -p "Enter SERVER_FULL_NAME value (or press Enter to skip): " SERVER_FULL_NAME_VAL

echo ""
echo "---- Configuring kubectl for EKS ----"
aws eks update-kubeconfig --region "$AWS_REGION" --name "$CLUSTER_NAME"
if [ $? -ne 0 ]; then
  echo "ERROR: Failed to update kubeconfig."
  exit 1
fi

echo "Verifying cluster connectivity..."
kubectl cluster-info || { echo "ERROR: Cannot connect to cluster."; exit 1; }

echo ""
echo "---- Updating Kubernetes manifests ----"

# Work on copies to avoid modifying originals
cp -r "$K8S_DIR" /tmp/modresorts-k8s-deploy

# Replace IMAGE_URI placeholder
sed -i 's|{{IMAGE_URI}}|'"$IMAGE_URI"'|g' /tmp/modresorts-k8s-deploy/deployment.yaml

# Replace environment variable placeholders if values were provided
if [ -n "$WEATHER_API_KEY_VAL" ]; then
  sed -i 's|{{WEATHER_API_KEY}}|'"$WEATHER_API_KEY_VAL"'|g' /tmp/modresorts-k8s-deploy/deployment.yaml
fi
if [ -n "$JNDI_FACTORY_VAL" ]; then
  sed -i 's|{{JNDI_FACTORY}}|'"$JNDI_FACTORY_VAL"'|g' /tmp/modresorts-k8s-deploy/deployment.yaml
fi
if [ -n "$JNDI_PROVIDER_URL_VAL" ]; then
  sed -i 's|{{JNDI_PROVIDER_URL}}|'"$JNDI_PROVIDER_URL_VAL"'|g' /tmp/modresorts-k8s-deploy/deployment.yaml
fi
if [ -n "$SERVER_DISPLAY_NAME_VAL" ]; then
  sed -i 's|{{SERVER_DISPLAY_NAME}}|'"$SERVER_DISPLAY_NAME_VAL"'|g' /tmp/modresorts-k8s-deploy/deployment.yaml
fi
if [ -n "$SERVER_FULL_NAME_VAL" ]; then
  sed -i 's|{{SERVER_FULL_NAME}}|'"$SERVER_FULL_NAME_VAL"'|g' /tmp/modresorts-k8s-deploy/deployment.yaml
fi

echo ""
echo "---- Applying Kubernetes manifests ----"

echo "Applying namespace..."
kubectl apply -f /tmp/modresorts-k8s-deploy/namespace.yaml

echo "Applying deployment..."
kubectl apply -f /tmp/modresorts-k8s-deploy/deployment.yaml

echo "Applying service..."
kubectl apply -f /tmp/modresorts-k8s-deploy/service.yaml

echo "Applying ingress..."
kubectl apply -f /tmp/modresorts-k8s-deploy/ingress.yaml

echo ""
echo "---- Waiting for deployment rollout ----"
kubectl rollout status deployment/$APP_NAME -n $NAMESPACE --timeout=300s
if [ $? -ne 0 ]; then
  echo "ERROR: Deployment rollout failed. Running rollback..."
  kubectl rollout undo deployment/$APP_NAME -n $NAMESPACE
  echo "Rollback initiated. Check pod status with: kubectl get pods -n $NAMESPACE"
  exit 1
fi

echo ""
echo "---- Verifying deployed resources ----"
kubectl get pods,svc,ingress -n $NAMESPACE

echo ""
echo "---- Application Access URL ----"
INGRESS_HOST=$(kubectl get ingress modresorts-ingress -n $NAMESPACE -o jsonpath='{.spec.rules[0].host}' 2>/dev/null || echo "modresorts.example.com")
ALB_ADDRESS=$(kubectl get ingress modresorts-ingress -n $NAMESPACE -o jsonpath='{.status.loadBalancer.ingress[0].hostname}' 2>/dev/null || echo "pending")
echo "  Ingress Host : http://$INGRESS_HOST"
echo "  ALB Address  : http://$ALB_ADDRESS"
echo "  Health Check : http://$ALB_ADDRESS/health"

echo ""
echo "=============================================="
echo "  SUCCESS: ModResorts deployed to EKS!"
echo "=============================================="
echo ""
echo "Useful commands:"
echo "  kubectl get pods -n $NAMESPACE"
echo "  kubectl logs -f deployment/$APP_NAME -n $NAMESPACE"
echo "  kubectl rollout undo deployment/$APP_NAME -n $NAMESPACE  # rollback"

# Cleanup temp files
rm -rf /tmp/modresorts-k8s-deploy
