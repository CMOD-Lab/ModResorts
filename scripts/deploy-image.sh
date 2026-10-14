#!/bin/bash
set -e
set -o pipefail

APP_NAME="bookingcomp"
NAMESPACE="bookingcomp"
K8S_DIR="kubernetes"

echo "============================================"
echo "  ModResorts BookingComp - Deploy to EKS"
echo "============================================"
echo ""

# ---- Collect deployment inputs ----
read -rp "Enter AWS Region (e.g. us-east-1): " AWS_REGION
if [ -z "$AWS_REGION" ]; then
  echo "AWS Region is required. Exiting."
  exit 1
fi

read -rp "Enter EKS Cluster Name: " CLUSTER_NAME
if [ -z "$CLUSTER_NAME" ]; then
  echo "EKS Cluster Name is required. Exiting."
  exit 1
fi

read -rp "Enter full Docker image URI (e.g. 123456789.dkr.ecr.us-east-1.amazonaws.com/bookingcomp:latest): " IMAGE_URI
if [ -z "$IMAGE_URI" ]; then
  echo "Image URI is required. Exiting."
  exit 1
fi

echo ""
echo "--- Optional environment variable configuration ---"
read -rp "Enter REDIS_HOST (or press Enter to skip): " REDIS_HOST_VAL
read -rp "Enter REDIS_PORT [6379] (or press Enter to skip): " REDIS_PORT_VAL
REDIS_PORT_VAL="${REDIS_PORT_VAL:-6379}"
read -rp "Enter WEATHER_SERVICE_URL (or press Enter to skip): " WEATHER_SERVICE_URL_VAL
read -rp "Enter WEATHER_API_KEY (or press Enter to skip): " WEATHER_API_KEY_VAL

# ---- Configure kubectl for EKS ----
echo ""
echo "Configuring kubectl for EKS cluster: $CLUSTER_NAME in $AWS_REGION ..."
aws eks update-kubeconfig --region "$AWS_REGION" --name "$CLUSTER_NAME"

echo "Verifying cluster connectivity..."
kubectl cluster-info || { echo "Cannot connect to EKS cluster. Exiting."; exit 1; }

# ---- Substitute placeholders in manifests ----
echo ""
echo "Updating Kubernetes manifests with deployment values..."

# Work on copies to avoid modifying originals
cp -r "$K8S_DIR" /tmp/bookingcomp-k8s-deploy

sed -i "s|{{IMAGE_URI}}|${IMAGE_URI}|g"                         /tmp/bookingcomp-k8s-deploy/deployment.yaml
sed -i "s|{{REDIS_HOST}}|${REDIS_HOST_VAL}|g"                   /tmp/bookingcomp-k8s-deploy/deployment.yaml
sed -i "s|{{REDIS_PORT}}|${REDIS_PORT_VAL}|g"                   /tmp/bookingcomp-k8s-deploy/deployment.yaml
sed -i "s|{{WEATHER_SERVICE_URL}}|${WEATHER_SERVICE_URL_VAL}|g" /tmp/bookingcomp-k8s-deploy/deployment.yaml
sed -i "s|{{WEATHER_API_KEY}}|${WEATHER_API_KEY_VAL}|g"         /tmp/bookingcomp-k8s-deploy/deployment.yaml

# ---- Apply manifests ----
echo ""
echo "Applying Kubernetes manifests..."
kubectl apply -f /tmp/bookingcomp-k8s-deploy/namespace.yaml
kubectl apply -f /tmp/bookingcomp-k8s-deploy/deployment.yaml
kubectl apply -f /tmp/bookingcomp-k8s-deploy/service.yaml
kubectl apply -f /tmp/bookingcomp-k8s-deploy/ingress.yaml

# ---- Wait for rollout ----
echo ""
echo "Waiting for deployment rollout..."
kubectl rollout status deployment/"$APP_NAME" -n "$NAMESPACE" --timeout=300s

# ---- Verify resources ----
echo ""
echo "Verifying deployed resources..."
kubectl get pods,svc,ingress -n "$NAMESPACE"

# ---- Display application URL ----
echo ""
echo "Fetching application URL from Ingress..."
INGRESS_HOST=$(kubectl get ingress "${APP_NAME}-ingress" -n "$NAMESPACE" \
  -o jsonpath='{.status.loadBalancer.ingress[0].hostname}' 2>/dev/null || echo "pending")
if [ "$INGRESS_HOST" != "pending" ] && [ -n "$INGRESS_HOST" ]; then
  echo "Application is accessible at: http://${INGRESS_HOST}"
else
  echo "Ingress hostname is still provisioning. Run the following to check:"
  echo "  kubectl get ingress ${APP_NAME}-ingress -n ${NAMESPACE}"
fi

# ---- Cleanup temp files ----
rm -rf /tmp/bookingcomp-k8s-deploy

echo ""
echo "============================================"
echo "  Deployment complete!"
echo "  Namespace : $NAMESPACE"
echo "  Image     : $IMAGE_URI"
echo "============================================"
echo ""
echo "Rollback command (if needed):"
echo "  kubectl rollout undo deployment/$APP_NAME -n $NAMESPACE"
