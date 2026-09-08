#!/bin/bash
set -e
set -o pipefail

# ============================================================
# deploy-image.sh - Deploy ModResorts to GCP GKE
# ============================================================

echo "=============================================="
echo "  ModResorts - GKE Deployment Script"
echo "=============================================="
echo ""

# ---- Collect GCP / GKE configuration ----
read -rp "Enter GCP Project ID: " GCP_PROJECT
if [ -z "$GCP_PROJECT" ]; then
  echo "ERROR: GCP Project ID is required."
  exit 1
fi

read -rp "Enter GCP Zone (e.g. us-central1-a): " GCP_ZONE
if [ -z "$GCP_ZONE" ]; then
  echo "ERROR: GCP Zone is required."
  exit 1
fi

read -rp "Enter GKE Cluster Name: " CLUSTER_NAME
if [ -z "$CLUSTER_NAME" ]; then
  echo "ERROR: GKE Cluster Name is required."
  exit 1
fi

read -rp "Enter full Docker image URI (e.g. us-central1-docker.pkg.dev/my-project/repo/modresorts:latest): " IMAGE_URI
if [ -z "$IMAGE_URI" ]; then
  echo "ERROR: Docker image URI is required."
  exit 1
fi

# ---- Optional application environment variables ----
echo ""
echo "--- Optional Application Configuration ---"
echo "(Press Enter to skip any variable)"
echo ""

read -rp "Enter value for WEATHER_API_KEY (Weather Underground API key): " WEATHER_API_KEY_VAL
WEATHER_API_KEY_VAL="${WEATHER_API_KEY_VAL:-}"

# ---- Update Kubernetes manifests with actual values ----
echo ""
echo "Updating Kubernetes manifests..."

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
K8S_DIR="${PROJECT_ROOT}/kubernetes"

# Create working copies of manifests
cp "${K8S_DIR}/deployment.yaml" "${K8S_DIR}/deployment.yaml.bak"

# Replace placeholders using pipe delimiter
sed -i 's|{{IMAGE_URI}}|'"${IMAGE_URI}"'|g' "${K8S_DIR}/deployment.yaml"
sed -i 's|{{WEATHER_API_KEY}}|'"${WEATHER_API_KEY_VAL}"'|g' "${K8S_DIR}/deployment.yaml"

echo "Manifests updated successfully."

# ---- Configure kubectl for GKE ----
echo ""
echo "Configuring kubectl for GKE cluster: ${CLUSTER_NAME}..."
gcloud container clusters get-credentials "${CLUSTER_NAME}" \
  --zone "${GCP_ZONE}" \
  --project "${GCP_PROJECT}"

echo ""
echo "Verifying cluster connectivity..."
kubectl cluster-info || { echo "ERROR: Cannot connect to cluster."; exit 1; }

# ---- Apply Kubernetes manifests ----
echo ""
echo "Applying Kubernetes manifests..."

echo "  [1/4] Applying namespace..."
kubectl apply -f "${K8S_DIR}/namespace.yaml"

echo "  [2/4] Applying deployment..."
kubectl apply -f "${K8S_DIR}/deployment.yaml"

echo "  [3/4] Applying service..."
kubectl apply -f "${K8S_DIR}/service.yaml"

echo "  [4/4] Applying ingress..."
kubectl apply -f "${K8S_DIR}/ingress.yaml"

# ---- Wait for rollout ----
echo ""
echo "Waiting for deployment rollout..."
kubectl rollout status deployment/modresorts -n modresorts --timeout=300s || {
  echo ""
  echo "ERROR: Deployment rollout failed or timed out."
  echo "To rollback, run: kubectl rollout undo deployment/modresorts -n modresorts"
  # Restore original manifest
  mv "${K8S_DIR}/deployment.yaml.bak" "${K8S_DIR}/deployment.yaml"
  exit 1
}

# ---- Restore original manifest (remove substituted values) ----
mv "${K8S_DIR}/deployment.yaml.bak" "${K8S_DIR}/deployment.yaml"

# ---- Verify deployment ----
echo ""
echo "Verifying deployed resources..."
kubectl get pods,svc,ingress -n modresorts

# ---- Display access URL ----
echo ""
echo "Fetching application ingress URL..."
INGRESS_IP=$(kubectl get ingress modresorts-ingress -n modresorts \
  -o jsonpath='{.status.loadBalancer.ingress[0].ip}' 2>/dev/null || echo "")

if [ -n "$INGRESS_IP" ]; then
  echo ""
  echo "=============================================="
  echo "  Application is accessible at:"
  echo "  http://${INGRESS_IP}/resorts/"
  echo "  Health check: http://${INGRESS_IP}/resorts/health"
  echo "=============================================="
else
  echo ""
  echo "Ingress IP not yet assigned. Check status with:"
  echo "  kubectl get ingress modresorts-ingress -n modresorts"
fi

echo ""
echo "Deployment completed successfully!"
echo ""
echo "Useful commands:"
echo "  kubectl get pods -n modresorts"
echo "  kubectl logs -f deployment/modresorts -n modresorts"
echo "  kubectl rollout undo deployment/modresorts -n modresorts  # rollback"
