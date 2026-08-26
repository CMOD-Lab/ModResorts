@echo off
setlocal enabledelayedexpansion

:: =============================================================================
:: build-push.bat – Build and push the ModResorts Docker image (Windows)
:: Usage: scripts\build-push.bat
:: Run from the repository root directory.
:: =============================================================================

set "PROJECT_NAME=modresorts"

echo ==============================================
echo   ModResorts - Docker Build ^& Push
echo ==============================================
echo.

:: ---------------------------------------------------------------------------
:: Registry selection
:: ---------------------------------------------------------------------------
echo Select target registry:
echo   1) AWS ECR
echo   2) Docker Hub
echo.
set /p "REGISTRY_CHOICE=Enter choice [1 or 2]: "

:: ---------------------------------------------------------------------------
:: Image tag
:: ---------------------------------------------------------------------------
set /p "RAW_TAG=Enter image tag (default: latest): "
if "!RAW_TAG!"=="" (
    set "IMAGE_TAG=latest"
) else (
    set "IMAGE_TAG=!RAW_TAG!"
)

echo.
echo Image name : !PROJECT_NAME!
echo Image tag  : !IMAGE_TAG!
echo.

:: ---------------------------------------------------------------------------
:: Registry-specific configuration
:: ---------------------------------------------------------------------------
if "!REGISTRY_CHOICE!"=="1" (
    :: ---- AWS ECR ----
    set /p "AWS_REGION=Enter AWS Region (e.g. us-east-1): "
    set /p "AWS_ACCOUNT_ID=Enter AWS Account ID (12-digit): "
    set /p "ECR_REPO_INPUT=Enter ECR repository name [!PROJECT_NAME!]: "
    if "!ECR_REPO_INPUT!"=="" (
        set "ECR_REPO=!PROJECT_NAME!"
    ) else (
        set "ECR_REPO=!ECR_REPO_INPUT!"
    )

    set "REGISTRY_URL=!AWS_ACCOUNT_ID!.dkr.ecr.!AWS_REGION!.amazonaws.com"
    set "FULL_IMAGE_NAME=!REGISTRY_URL!/!ECR_REPO!:!IMAGE_TAG!"

    echo.
    echo Authenticating with ECR ...
    aws ecr get-login-password --region !AWS_REGION! | docker login --username AWS --password-stdin !REGISTRY_URL!
    if !ERRORLEVEL! neq 0 (
        echo ERROR: ECR login failed.
        exit /b 1
    )

    echo Checking / creating ECR repository '!ECR_REPO!' ...
    aws ecr describe-repositories --repository-names !ECR_REPO! --region !AWS_REGION! >nul 2>&1
    if !ERRORLEVEL! neq 0 (
        echo Creating ECR repository ...
        aws ecr create-repository --repository-name !ECR_REPO! --region !AWS_REGION!
        if !ERRORLEVEL! neq 0 (
            echo ERROR: Failed to create ECR repository.
            exit /b 1
        )
    )

) else if "!REGISTRY_CHOICE!"=="2" (
    :: ---- Docker Hub ----
    set /p "DOCKER_USERNAME=Enter Docker Hub username: "
    set /p "DOCKER_PASSWORD=Enter Docker Hub password/token: "
    set /p "DOCKER_NAMESPACE_INPUT=Enter Docker Hub namespace [!DOCKER_USERNAME!]: "
    if "!DOCKER_NAMESPACE_INPUT!"=="" (
        set "DOCKER_NAMESPACE=!DOCKER_USERNAME!"
    ) else (
        set "DOCKER_NAMESPACE=!DOCKER_NAMESPACE_INPUT!"
    )

    set "FULL_IMAGE_NAME=!DOCKER_NAMESPACE!/!PROJECT_NAME!:!IMAGE_TAG!"

    echo.
    echo Authenticating with Docker Hub ...
    echo !DOCKER_PASSWORD! | docker login --username !DOCKER_USERNAME! --password-stdin
    if !ERRORLEVEL! neq 0 (
        echo ERROR: Docker Hub login failed.
        exit /b 1
    )

) else (
    echo ERROR: Invalid choice '!REGISTRY_CHOICE!'. Exiting.
    exit /b 1
)

echo.
echo Full image : !FULL_IMAGE_NAME!
echo.

:: ---------------------------------------------------------------------------
:: Build
:: ---------------------------------------------------------------------------
echo Building Docker image ...
docker build -f Dockerfile -t "!FULL_IMAGE_NAME!" .
if !ERRORLEVEL! neq 0 (
    echo ERROR: Docker build failed.
    exit /b 1
)

echo.
echo Tagging as !PROJECT_NAME!:!IMAGE_TAG! locally ...
docker tag "!FULL_IMAGE_NAME!" "!PROJECT_NAME!:!IMAGE_TAG!"

:: ---------------------------------------------------------------------------
:: Push
:: ---------------------------------------------------------------------------
echo.
echo Pushing image to registry ...
docker push "!FULL_IMAGE_NAME!"
if !ERRORLEVEL! neq 0 (
    echo ERROR: Docker push failed.
    exit /b 1
)

echo.
echo ==============================================
echo   Build ^& Push complete!
echo   Image: !FULL_IMAGE_NAME!
echo ==============================================

endlocal
