#!/bin/bash
set -e
set -o pipefail

# =============================================================================
# build-push.sh – Build and push the BookingComp Docker image
# =============================================================================

PROJECT_NAME="bookingcomp"
DOCKERFILE_PATH="Dockerfile"

echo "=============================================="
echo "  BookingComp – Docker Build & Push Script"
echo "=============================================="
echo ""

# ---------------------------------------------------------------------------
# Sanitize project name: lowercase, replace non-alphanumeric with hyphens,
# trim leading/trailing hyphens
# ---------------------------------------------------------------------------
IMAGE_NAME=$(echo "$PROJECT_NAME" | tr '[:upper:]' '[:lower:]' | tr -cs 'a-z0-9' '-' | sed 's/^-*//;s/-*$//')

# ---------------------------------------------------------------------------
# Prompt for image tag
# ---------------------------------------------------------------------------
read -rp "Enter image tag [latest]: " IMAGE_TAG_INPUT
IMAGE_TAG=$(echo "${IMAGE_TAG_INPUT:-latest}" | tr '[:upper:]' '[:lower:]' | tr -cs 'a-z0-9._-' '-' | sed 's/^-*//;s/-*$//')
if [ -z "$IMAGE_TAG" ]; then
  IMAGE_TAG="latest"
fi
echo "Using image tag: $IMAGE_TAG"
echo ""

# ---------------------------------------------------------------------------
# Registry selection
# ---------------------------------------------------------------------------
echo "Select container registry:"
echo "  1. AWS ECR"
echo "  2. Docker Hub"
read -rp "Enter choice [1 or 2]: " REGISTRY_CHOICE
echo ""

case "$REGISTRY_CHOICE" in
  1)
    echo "--- AWS ECR Configuration ---"
    read -rp "Enter AWS Region (e.g. us-east-1): " AWS_REGION
    read -rp "Enter AWS Account ID: " AWS_ACCOUNT_ID
    read -rp "Enter ECR repository name [$IMAGE_NAME]: " ECR_REPO_INPUT
    ECR_REPO="${ECR_REPO_INPUT:-$IMAGE_NAME}"

    REGISTRY_URL="${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com"
    FULL_IMAGE_NAME="${REGISTRY_URL}/${ECR_REPO}:${IMAGE_TAG}"

    echo ""
    echo "Logging in to AWS ECR..."
    aws ecr get-login-password --region "$AWS_REGION" | \
      docker login --username AWS --password-stdin "$REGISTRY_URL"
    if [ $? -ne 0 ]; then
      echo "ERROR: ECR login failed." >&2
      exit 1
    fi

    echo "Ensuring ECR repository exists..."
    aws ecr describe-repositories --repository-names "$ECR_REPO" --region "$AWS_REGION" >/dev/null 2>&1 || \
      aws ecr create-repository --repository-name "$ECR_REPO" --region "$AWS_REGION"
    ;;

  2)
    echo "--- Docker Hub Configuration ---"
    read -rp "Enter Docker Hub username: " DOCKER_USERNAME
    read -rsp "Enter Docker Hub password/token: " DOCKER_PASSWORD
    echo ""
    read -rp "Enter Docker Hub repository name [$IMAGE_NAME]: " DOCKER_REPO_INPUT
    DOCKER_REPO="${DOCKER_REPO_INPUT:-$IMAGE_NAME}"

    FULL_IMAGE_NAME="${DOCKER_USERNAME}/${DOCKER_REPO}:${IMAGE_TAG}"

    echo ""
    echo "Logging in to Docker Hub..."
    echo "$DOCKER_PASSWORD" | docker login --username "$DOCKER_USERNAME" --password-stdin
    if [ $? -ne 0 ]; then
      echo "ERROR: Docker Hub login failed." >&2
      exit 1
    fi
    ;;

  *)
    echo "ERROR: Invalid choice. Please enter 1 or 2." >&2
    exit 1
    ;;
esac

echo ""
echo "Building Docker image: $FULL_IMAGE_NAME"
echo "Build context: . (repository root)"
docker build -f "$DOCKERFILE_PATH" -t "$FULL_IMAGE_NAME" .
if [ $? -ne 0 ]; then
  echo "ERROR: Docker build failed." >&2
  exit 1
fi

echo ""
echo "Pushing image: $FULL_IMAGE_NAME"
docker push "$FULL_IMAGE_NAME"
if [ $? -ne 0 ]; then
  echo "ERROR: Docker push failed." >&2
  exit 1
fi

echo ""
echo "=============================================="
echo "  SUCCESS: Image pushed successfully!"
echo "  Image: $FULL_IMAGE_NAME"
echo "=============================================="
