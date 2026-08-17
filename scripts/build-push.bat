@echo off
setlocal enabledelayedexpansion

rem ============================================================
rem build-push.bat - Build and push ModResorts Docker image
rem ============================================================

set PROJECT_NAME=modresorts
set IMAGE_NAME=modresorts

echo ============================================
echo   ModResorts - Docker Build and Push Script
echo ============================================
echo.

rem Prompt for image tag
set /p IMAGE_TAG_INPUT="Enter image tag (press Enter for 'latest'): "
if "!IMAGE_TAG_INPUT!"=="" (
    set IMAGE_TAG=latest
) else (
    set IMAGE_TAG=!IMAGE_TAG_INPUT!
)
echo Using image tag: !IMAGE_TAG!
echo.

rem Prompt for registry type
echo Select container registry:
echo   1. AWS ECR (Elastic Container Registry)
echo   2. Docker Hub
set /p REGISTRY_CHOICE="Enter choice [1-2]: "

if "!REGISTRY_CHOICE!"=="1" goto ECR_SETUP
if "!REGISTRY_CHOICE!"=="2" goto DOCKERHUB_SETUP
echo ERROR: Invalid registry choice. Please enter 1 or 2.
exit /b 1

:ECR_SETUP
echo.
echo --- AWS ECR Configuration ---
set /p AWS_REGION="Enter AWS Region (e.g. us-east-1): "
set /p AWS_ACCOUNT_ID="Enter AWS Account ID: "

set ECR_REPO=!IMAGE_NAME!
set REGISTRY_URL=!AWS_ACCOUNT_ID!.dkr.ecr.!AWS_REGION!.amazonaws.com
set FULL_IMAGE_NAME=!REGISTRY_URL!/!ECR_REPO!:!IMAGE_TAG!

echo.
echo Logging in to AWS ECR...
aws ecr get-login-password --region !AWS_REGION! | docker login --username AWS --password-stdin !REGISTRY_URL!
if !ERRORLEVEL! neq 0 (
    echo ERROR: ECR login failed.
    exit /b 1
)

echo Checking/creating ECR repository: !ECR_REPO! ...
aws ecr describe-repositories --repository-names !ECR_REPO! --region !AWS_REGION! >nul 2>&1
if !ERRORLEVEL! neq 0 (
    echo Creating ECR repository...
    aws ecr create-repository --repository-name !ECR_REPO! --region !AWS_REGION!
    if !ERRORLEVEL! neq 0 (
        echo ERROR: Failed to create ECR repository.
        exit /b 1
    )
)
goto BUILD_IMAGE

:DOCKERHUB_SETUP
echo.
echo --- Docker Hub Configuration ---
set /p DOCKER_USERNAME="Enter Docker Hub username: "
set /p DOCKER_PASSWORD="Enter Docker Hub password/token: "
set /p DOCKER_REPO="Enter Docker Hub repository (e.g. myorg/modresorts): "

set FULL_IMAGE_NAME=!DOCKER_REPO!:!IMAGE_TAG!

echo.
echo Logging in to Docker Hub...
echo !DOCKER_PASSWORD! | docker login --username !DOCKER_USERNAME! --password-stdin
if !ERRORLEVEL! neq 0 (
    echo ERROR: Docker Hub login failed.
    exit /b 1
)
goto BUILD_IMAGE

:BUILD_IMAGE
echo.
echo Building Docker image: !FULL_IMAGE_NAME!
echo Build context: . (project root)
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
echo ============================================
echo   SUCCESS: Image pushed successfully!
echo   Image: !FULL_IMAGE_NAME!
echo ============================================

endlocal
exit /b 0
