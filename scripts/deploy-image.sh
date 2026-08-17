#!/bin/bash
set -e
set -o pipefail

# ============================================================
# deploy-image.sh - Deploy ModResorts to AWS EKS
# ============================================================

APP_NAME="modresorts"
NAMESPACE="modresorts"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

echo "============================================"
echo "  ModResorts - AWS EKS Deployment Script"
echo "============================================"
echo ""

# Prompt for AWS region
read -p "Enter AWS Region (e.g. us-east-1): " AWS_REGION
if [ -z "$AWS_REGION" ]; then
  echo "ERROR: AWS Region is required."
  exit 1
fi

# Prompt for EKS cluster name
read -p "Enter EKS Cluster Name: " CLUSTER_NAME
if [ -z "$CLUSTER_NAME" ]; then
  echo "ERROR: EKS Cluster Name is required."
  exit 1
fi

# Prompt for Docker image URI
read -p "Enter full Docker image URI (e.g. 123456789.dkr.ecr.us-east-1.amazonaws.com/modresorts:latest): " IMAGE_URI
if [ -z "$IMAGE_URI" ]; then
  echo "ERROR: Docker image URI is required."
  exit 1
fi

echo ""
echo "--- Optional Application Environment Variables ---"
echo "(Press Enter to skip any variable)"
echo ""

read -p "Enter WEATHER_API_KEY value (or press Enter to skip): " WEATHER_API_KEY_VAL
read -p "Enter SERVER_DISPLAY_NAME value (or press Enter to skip): " SERVER_DISPLAY_NAME_VAL
read -p "Enter SERVER_FULL_NAME value (or press Enter to skip): " SERVER_FULL_NAME_VAL
read -p "Enter JNDI_FACTORY value (or press Enter to skip): " JNDI_FACTORY_VAL
read -p "Enter JNDI_PROVIDER_URL value (or press Enter to skip): " JNDI_PROVIDER_URL_VAL

# Set defaults if not provided
WEATHER_API_KEY_VAL="${WEATHER_API_KEY_VAL:-}"
SERVER_DISPLAY_NAME_VAL="${SERVER_DISPLAY_NAME_VAL:-modresorts-server}"
SERVER_FULL_NAME_VAL="${SERVER_FULL_NAME_VAL:-modresorts-server-full}"
JNDI_FACTORY_VAL="${JNDI_FACTORY_VAL:-com.sun.jndi.fscontext.RefFSContextFactory}"
JNDI_PROVIDER_URL_VAL="${JNDI_PROVIDER_URL_VAL:-}"

echo ""
echo "--- Configuring kubectl for EKS ---"
aws eks update-kubeconfig --region "$AWS_REGION" --name "$CLUSTER_NAME"
if [ $? -ne 0 ]; then
  echo "ERROR: Failed to update kubeconfig. Check your AWS credentials and cluster name."
  exit 1
fi

echo ""
echo "--- Verifying cluster connectivity ---"
kubectl cluster-info || { echo "ERROR: Cannot connect to Kubernetes cluster."; exit 1; }

echo ""
echo "--- Preparing Kubernetes manifests ---"

# Create working copies of manifests
WORK_DIR=$(mktemp -d)
cp -r "$PROJECT_ROOT/kubernetes/"* "$WORK_DIR/"

# Replace placeholders in deployment.yaml using pipe delimiter
sed -i 's|{{IMAGE_URI}}|'"$IMAGE_URI"'|g' "$WORK_DIR/deployment.yaml"
sed -i 's|{{WEATHER_API_KEY}}|'"$WEATHER_API_KEY_VAL"'|g' "$WORK_DIR/deployment.yaml"
sed -i 's|{{SERVER_DISPLAY_NAME}}|'"$SERVER_DISPLAY_NAME_VAL"'|g' "$WORK_DIR/deployment.yaml"
sed -i 's|{{SERVER_FULL_NAME}}|'"$SERVER_FULL_NAME_VAL"'|g' "$WORK_DIR/deployment.yaml"
sed -i 's|{{JNDI_FACTORY}}|'"$JNDI_FACTORY_VAL"'|g' "$WORK_DIR/deployment.yaml"
sed -i 's|{{JNDI_PROVIDER_URL}}|'"$JNDI_PROVIDER_URL_VAL"'|g' "$WORK_DIR/deployment.yaml"

echo ""
echo "--- Applying Kubernetes manifests ---"

echo "Applying namespace..."
kubectl apply -f "$WORK_DIR/namespace.yaml"

echo "Applying deployment..."
kubectl apply -f "$WORK_DIR/deployment.yaml"

echo "Applying service..."
kubectl apply -f "$WORK_DIR/service.yaml"

echo "Applying ingress..."
kubectl apply -f "$WORK_DIR/ingress.yaml"

echo ""
echo "--- Waiting for deployment rollout ---"
kubectl rollout status deployment/"$APP_NAME" -n "$NAMESPACE" --timeout=300s
if [ $? -ne 0 ]; then
  echo "ERROR: Deployment rollout failed or timed out."
  echo ""
  echo "--- Rollback Instructions ---"
  echo "To rollback: kubectl rollout undo deployment/$APP_NAME -n $NAMESPACE"
  echo "To check logs: kubectl logs -l app=$APP_NAME -n $NAMESPACE"
  rm -rf "$WORK_DIR"
  exit 1
fi

echo ""
echo "--- Verifying deployed resources ---"
kubectl get pods,svc,ingress -n "$NAMESPACE"

echo ""
echo "--- Application Access ---"
INGRESS_HOST=$(kubectl get ingress modresorts-ingress -n "$NAMESPACE" -o jsonpath='{.status.loadBalancer.ingress[0].hostname}' 2>/dev/null || echo "pending")
if [ "$INGRESS_HOST" != "pending" ] && [ -n "$INGRESS_HOST" ]; then
  echo "Application URL: http://$INGRESS_HOST/resorts/"
  echo "Health Check:    http://$INGRESS_HOST/resorts/health"
else
  echo "Ingress hostname is still provisioning. Run the following to check:"
  echo "  kubectl get ingress modresorts-ingress -n $NAMESPACE"
fi

# Cleanup temp directory
rm -rf "$WORK_DIR"

echo ""
echo "============================================"
echo "  SUCCESS: ModResorts deployed to EKS!"
echo "  Namespace: $NAMESPACE"
echo "  Image: $IMAGE_URI"
echo "============================================"
