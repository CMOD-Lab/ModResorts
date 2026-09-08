@echo off
setlocal enabledelayedexpansion

:: ============================================================
:: build-push.bat - Build and push ModResorts Docker image
:: Target: GCP GKE
:: ============================================================

echo ==============================================
echo   ModResorts - Docker Build ^& Push Script
echo ==============================================
echo.

set "PROJECT_NAME=modresorts"
set "IMAGE_NAME=modresorts"

echo Select container registry:
echo   1. Google Artifact Registry
echo   2. Docker Hub
echo.
set /p REGISTRY_CHOICE="Enter choice [1 or 2]: "

if "!REGISTRY_CHOICE!"=="1" (
    echo.
    echo --- Google Artifact Registry ---
    set /p GCP_PROJECT="Enter GCP Project ID: "
    set /p GCP_REGION="Enter GCP Region (e.g. us-central1): "
    set /p AR_REPO="Enter Artifact Registry repository name (e.g. modresorts-repo): "
    set /p IMAGE_TAG="Enter image tag [default: latest]: "

    if "!IMAGE_TAG!"=="" set "IMAGE_TAG=latest"

    set "FULL_IMAGE_NAME=!GCP_REGION!-docker.pkg.dev/!GCP_PROJECT!/!AR_REPO!/!IMAGE_NAME!:!IMAGE_TAG!"

    echo.
    echo Authenticating with Google Artifact Registry...
    gcloud auth configure-docker !GCP_REGION!-docker.pkg.dev
    if !ERRORLEVEL! neq 0 (
        echo ERROR: Artifact Registry authentication failed.
        exit /b 1
    )
) else if "!REGISTRY_CHOICE!"=="2" (
    echo.
    echo --- Docker Hub ---
    set /p DOCKER_USERNAME="Enter Docker Hub username: "
    set /p DOCKER_PASSWORD="Enter Docker Hub password/token: "
    set /p IMAGE_TAG="Enter image tag [default: latest]: "

    if "!IMAGE_TAG!"=="" set "IMAGE_TAG=latest"

    set "FULL_IMAGE_NAME=!DOCKER_USERNAME!/!IMAGE_NAME!:!IMAGE_TAG!"

    echo.
    echo Authenticating with Docker Hub...
    echo !DOCKER_PASSWORD! | docker login --username !DOCKER_USERNAME! --password-stdin
    if !ERRORLEVEL! neq 0 (
        echo ERROR: Docker Hub authentication failed.
        exit /b 1
    )
) else (
    echo ERROR: Invalid choice. Please enter 1 or 2.
    exit /b 1
)

echo.
echo Building Docker image: !FULL_IMAGE_NAME!
echo Build context: . (repository root)
echo.

docker build -f Dockerfile -t "!FULL_IMAGE_NAME!" .
if !ERRORLEVEL! neq 0 (
    echo ERROR: Docker build failed.
    exit /b 1
)

echo.
echo Pushing image: !FULL_IMAGE_NAME!
docker push "!FULL_IMAGE_NAME!"
if !ERRORLEVEL! neq 0 (
    echo ERROR: Docker push failed.
    exit /b 1
)

echo.
echo ==============================================
echo   Build and push completed successfully!
echo   Image: !FULL_IMAGE_NAME!
echo ==============================================

endlocal
