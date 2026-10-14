#!/bin/bash
set -e
set -o pipefail

# =============================================================================
# deploy-image.sh – Deploy BookingComp to AWS EKS
# =============================================================================

APP_NAME="bookingcomp"
NAMESPACE="bookingcomp"
K8S_DIR="kubernetes"

echo "=============================================="
echo "  BookingComp – AWS EKS Deployment Script"
echo "=============================================="
echo ""

# ---------------------------------------------------------------------------
# Collect AWS / EKS configuration
# ---------------------------------------------------------------------------
read -rp "Enter AWS Region (e.g. us-east-1): " AWS_REGION
if [ -z "$AWS_REGION" ]; then
  echo "ERROR: AWS Region is required." >&2
  exit 1
fi

read -rp "Enter EKS Cluster Name: " CLUSTER_NAME
if [ -z "$CLUSTER_NAME" ]; then
  echo "ERROR: EKS Cluster Name is required." >&2
  exit 1
fi

read -rp "Enter full Docker image URI (e.g. 123456789.dkr.ecr.us-east-1.amazonaws.com/bookingcomp:latest): " IMAGE_URI
if [ -z "$IMAGE_URI" ]; then
  echo "ERROR: Docker image URI is required." >&2
  exit 1
fi

echo ""
echo "--- Application Environment Variables ---"
echo "Press Enter to skip any variable (placeholder will remain in manifest)."
echo ""

read -rp "Enter REDIS_HOST (ElastiCache endpoint, e.g. my-cluster.abc.ng.0001.use1.cache.amazonaws.com): " REDIS_HOST_VAL
read -rp "Enter REDIS_PORT [6379]: " REDIS_PORT_VAL
REDIS_PORT_VAL="${REDIS_PORT_VAL:-6379}"

read -rp "Enter WEATHER_API_KEY (Weather Underground API key, or press Enter to skip): " WEATHER_API_KEY_VAL
read -rp "Enter REMOTE_SERVICE_URL (e.g. http://weather-service:8080, or press Enter to skip): " REMOTE_SERVICE_URL_VAL

echo ""
echo "--- Deployment Summary ---"
echo "  AWS Region   : $AWS_REGION"
echo "  EKS Cluster  : $CLUSTER_NAME"
echo "  Image URI    : $IMAGE_URI"
echo "  Namespace    : $NAMESPACE"
echo ""

# ---------------------------------------------------------------------------
# Configure kubectl for EKS
# ---------------------------------------------------------------------------
echo "Configuring kubectl for EKS cluster..."
aws eks update-kubeconfig --region "$AWS_REGION" --name "$CLUSTER_NAME"
if [ $? -ne 0 ]; then
  echo "ERROR: Failed to configure kubectl. Check AWS credentials and cluster name." >&2
  exit 1
fi

echo "Verifying cluster connectivity..."
kubectl cluster-info || { echo "ERROR: Cannot connect to EKS cluster." >&2; exit 1; }
echo ""

# ---------------------------------------------------------------------------
# Substitute placeholders in Kubernetes manifests (working copies)
# ---------------------------------------------------------------------------
echo "Preparing Kubernetes manifests..."

# Create temporary working copies to avoid modifying originals
TMP_DIR=$(mktemp -d)
cp -r "$K8S_DIR"/* "$TMP_DIR"/

DEPLOY_YAML="$TMP_DIR/deployment.yaml"

# Replace image placeholder
sed -i 's|{{IMAGE_URI}}|'"$IMAGE_URI"'|g' "$DEPLOY_YAML"

# Replace environment variable placeholders
if [ -n "$REDIS_HOST_VAL" ]; then
  sed -i 's|{{REDIS_HOST}}|'"$REDIS_HOST_VAL"'|g' "$DEPLOY_YAML"
fi

sed -i 's|{{REDIS_PORT}}|'"$REDIS_PORT_VAL"'|g' "$DEPLOY_YAML"

if [ -n "$WEATHER_API_KEY_VAL" ]; then
  sed -i 's|{{WEATHER_API_KEY}}|'"$WEATHER_API_KEY_VAL"'|g' "$DEPLOY_YAML"
fi

if [ -n "$REMOTE_SERVICE_URL_VAL" ]; then
  sed -i 's|{{REMOTE_SERVICE_URL}}|'"$REMOTE_SERVICE_URL_VAL"'|g' "$DEPLOY_YAML"
fi

# ---------------------------------------------------------------------------
# Apply Kubernetes manifests in order
# ---------------------------------------------------------------------------
echo "Applying Kubernetes manifests..."
echo ""

echo "[1/4] Applying namespace..."
kubectl apply -f "$TMP_DIR/namespace.yaml"

echo "[2/4] Applying deployment..."
kubectl apply -f "$DEPLOY_YAML"

echo "[3/4] Applying service..."
kubectl apply -f "$TMP_DIR/service.yaml"

echo "[4/4] Applying ingress..."
kubectl apply -f "$TMP_DIR/ingress.yaml"

echo ""

# ---------------------------------------------------------------------------
# Wait for rollout
# ---------------------------------------------------------------------------
echo "Waiting for deployment rollout..."
kubectl rollout status deployment/"$APP_NAME" -n "$NAMESPACE" --timeout=300s
if [ $? -ne 0 ]; then
  echo ""
  echo "ERROR: Deployment rollout failed or timed out." >&2
  echo "Run the following to investigate:"
  echo "  kubectl describe pods -n $NAMESPACE"
  echo "  kubectl logs -l app=$APP_NAME -n $NAMESPACE"
  echo ""
  echo "To rollback:"
  echo "  kubectl rollout undo deployment/$APP_NAME -n $NAMESPACE"
  rm -rf "$TMP_DIR"
  exit 1
fi

# ---------------------------------------------------------------------------
# Verify resources
# ---------------------------------------------------------------------------
echo ""
echo "Verifying deployed resources..."
kubectl get pods,svc,ingress -n "$NAMESPACE"

# ---------------------------------------------------------------------------
# Display application URL
# ---------------------------------------------------------------------------
echo ""
echo "Fetching application URL from Ingress..."
INGRESS_HOST=$(kubectl get ingress "${APP_NAME}-ingress" -n "$NAMESPACE" \
  -o jsonpath='{.status.loadBalancer.ingress[0].hostname}' 2>/dev/null || echo "")

if [ -n "$INGRESS_HOST" ]; then
  echo ""
  echo "=============================================="
  echo "  Application is accessible at:"
  echo "  http://$INGRESS_HOST"
  echo "  Health check: http://$INGRESS_HOST/health"
  echo "=============================================="
else
  echo "Ingress hostname not yet assigned. Check again with:"
  echo "  kubectl get ingress -n $NAMESPACE"
fi

# Cleanup temp files
rm -rf "$TMP_DIR"

echo ""
echo "Deployment complete!"
echo ""
echo "Useful commands:"
echo "  kubectl get pods -n $NAMESPACE"
echo "  kubectl logs -l app=$APP_NAME -n $NAMESPACE"
echo "  kubectl rollout undo deployment/$APP_NAME -n $NAMESPACE  # rollback"
