#!/bin/bash
set -e
set -o pipefail

# ============================================================
# build-push.sh - Build and push ModResorts Docker image
# Target: GCP GKE
# ============================================================

echo "=============================================="
echo "  ModResorts - Docker Build & Push Script"
echo "=============================================="
echo ""

PROJECT_NAME="modresorts"

# Sanitize image name: lowercase, replace non-alphanumeric with hyphens, trim hyphens
IMAGE_NAME=$(echo "$PROJECT_NAME" | tr '[:upper:]' '[:lower:]' | tr -cs 'a-z0-9' '-' | sed 's/^-*//;s/-*$//')

echo "Select container registry:"
echo "  1. Google Artifact Registry"
echo "  2. Docker Hub"
echo ""
read -rp "Enter choice [1 or 2]: " REGISTRY_CHOICE

case "$REGISTRY_CHOICE" in
  1)
    echo ""
    echo "--- Google Artifact Registry ---"
    read -rp "Enter GCP Project ID: " GCP_PROJECT
    read -rp "Enter GCP Region (e.g. us-central1): " GCP_REGION
    read -rp "Enter Artifact Registry repository name (e.g. modresorts-repo): " AR_REPO
    read -rp "Enter image tag [default: latest]: " IMAGE_TAG
    IMAGE_TAG=$(echo "${IMAGE_TAG:-latest}" | tr '[:upper:]' '[:lower:]' | tr -cs 'a-z0-9._-' '-' | sed 's/^-*//;s/-*$//')
    IMAGE_TAG="${IMAGE_TAG:-latest}"

    FULL_IMAGE_NAME="${GCP_REGION}-docker.pkg.dev/${GCP_PROJECT}/${AR_REPO}/${IMAGE_NAME}:${IMAGE_TAG}"

    echo ""
    echo "Authenticating with Google Artifact Registry..."
    gcloud auth configure-docker "${GCP_REGION}-docker.pkg.dev" --quiet
    ;;
  2)
    echo ""
    echo "--- Docker Hub ---"
    read -rp "Enter Docker Hub username: " DOCKER_USERNAME
    read -rsp "Enter Docker Hub password/token: " DOCKER_PASSWORD
    echo ""
    read -rp "Enter image tag [default: latest]: " IMAGE_TAG
    IMAGE_TAG=$(echo "${IMAGE_TAG:-latest}" | tr '[:upper:]' '[:lower:]' | tr -cs 'a-z0-9._-' '-' | sed 's/^-*//;s/-*$//')
    IMAGE_TAG="${IMAGE_TAG:-latest}"

    FULL_IMAGE_NAME="${DOCKER_USERNAME}/${IMAGE_NAME}:${IMAGE_TAG}"

    echo ""
    echo "Authenticating with Docker Hub..."
    echo "${DOCKER_PASSWORD}" | docker login --username "${DOCKER_USERNAME}" --password-stdin
    ;;
  *)
    echo "ERROR: Invalid choice. Please enter 1 or 2."
    exit 1
    ;;
esac

echo ""
echo "Building Docker image: ${FULL_IMAGE_NAME}"
echo "Build context: . (repository root)"
echo ""

docker build -f Dockerfile -t "${FULL_IMAGE_NAME}" .

echo ""
echo "Pushing image: ${FULL_IMAGE_NAME}"
docker push "${FULL_IMAGE_NAME}"

echo ""
echo "=============================================="
echo "  Build and push completed successfully!"
echo "  Image: ${FULL_IMAGE_NAME}"
echo "=============================================="
