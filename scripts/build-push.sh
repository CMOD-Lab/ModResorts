#!/usr/bin/env bash
# =============================================================================
# build-push.sh – Build and push the ModResorts Docker image
# Usage: ./scripts/build-push.sh
# Run from the repository root directory.
# =============================================================================
set -e
set -o pipefail

PROJECT_NAME="modresorts"

# ---------------------------------------------------------------------------
# Sanitise image name: lowercase, replace non-alphanumeric with hyphens,
# strip leading/trailing hyphens.
# ---------------------------------------------------------------------------
IMAGE_NAME=$(echo "${PROJECT_NAME}" | tr '[:upper:]' '[:lower:]' | tr -cs 'a-z0-9' '-' | sed 's/^-*//;s/-*$//')

echo "=============================================="
echo "  ModResorts – Docker Build & Push"
echo "=============================================="
echo ""

# ---------------------------------------------------------------------------
# Registry selection
# ---------------------------------------------------------------------------
echo "Select target registry:"
echo "  1) AWS ECR"
echo "  2) Docker Hub"
echo ""
read -rp "Enter choice [1 or 2]: " REGISTRY_CHOICE

# ---------------------------------------------------------------------------
# Image tag
# ---------------------------------------------------------------------------
read -rp "Enter image tag (default: latest): " RAW_TAG
IMAGE_TAG=$(echo "${RAW_TAG}" | tr '[:upper:]' '[:lower:]' | tr -cs 'a-z0-9._-' '-' | sed 's/^-*//;s/-*$//')
if [ -z "${IMAGE_TAG}" ]; then
  IMAGE_TAG="latest"
fi

echo ""
echo "Image name : ${IMAGE_NAME}"
echo "Image tag  : ${IMAGE_TAG}"
echo ""

# ---------------------------------------------------------------------------
# Registry-specific configuration
# ---------------------------------------------------------------------------
if [ "${REGISTRY_CHOICE}" = "1" ]; then
  # ---- AWS ECR ----
  read -rp "Enter AWS Region (e.g. us-east-1): " AWS_REGION
  read -rp "Enter AWS Account ID (12-digit): "   AWS_ACCOUNT_ID
  read -rp "Enter ECR repository name [${IMAGE_NAME}]: " ECR_REPO_INPUT
  ECR_REPO="${ECR_REPO_INPUT:-${IMAGE_NAME}}"

  REGISTRY_URL="${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com"
  FULL_IMAGE_NAME="${REGISTRY_URL}/${ECR_REPO}:${IMAGE_TAG}"

  echo ""
  echo "Authenticating with ECR …"
  aws ecr get-login-password --region "${AWS_REGION}" \
    | docker login --username AWS --password-stdin "${REGISTRY_URL}"

  echo "Checking / creating ECR repository '${ECR_REPO}' …"
  aws ecr describe-repositories --repository-names "${ECR_REPO}" \
      --region "${AWS_REGION}" >/dev/null 2>&1 \
    || aws ecr create-repository --repository-name "${ECR_REPO}" \
         --region "${AWS_REGION}"

elif [ "${REGISTRY_CHOICE}" = "2" ]; then
  # ---- Docker Hub ----
  read -rp "Enter Docker Hub username: "          DOCKER_USERNAME
  read -rsp "Enter Docker Hub password/token: "   DOCKER_PASSWORD
  echo ""
  read -rp "Enter Docker Hub namespace [${DOCKER_USERNAME}]: " DOCKER_NAMESPACE_INPUT
  DOCKER_NAMESPACE="${DOCKER_NAMESPACE_INPUT:-${DOCKER_USERNAME}}"

  FULL_IMAGE_NAME="${DOCKER_NAMESPACE}/${IMAGE_NAME}:${IMAGE_TAG}"

  echo ""
  echo "Authenticating with Docker Hub …"
  echo "${DOCKER_PASSWORD}" | docker login --username "${DOCKER_USERNAME}" --password-stdin

else
  echo "ERROR: Invalid choice '${REGISTRY_CHOICE}'. Exiting."
  exit 1
fi

echo ""
echo "Full image : ${FULL_IMAGE_NAME}"
echo ""

# ---------------------------------------------------------------------------
# Build
# ---------------------------------------------------------------------------
echo "Building Docker image …"
docker build \
  -f Dockerfile \
  -t "${FULL_IMAGE_NAME}" \
  .

echo ""
echo "Tagging as ${IMAGE_NAME}:${IMAGE_TAG} locally …"
docker tag "${FULL_IMAGE_NAME}" "${IMAGE_NAME}:${IMAGE_TAG}"

# ---------------------------------------------------------------------------
# Push
# ---------------------------------------------------------------------------
echo ""
echo "Pushing image to registry …"
docker push "${FULL_IMAGE_NAME}"

echo ""
echo "=============================================="
echo "  Build & Push complete!"
echo "  Image: ${FULL_IMAGE_NAME}"
echo "=============================================="
