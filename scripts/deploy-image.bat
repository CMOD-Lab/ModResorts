@echo off
setlocal enabledelayedexpansion

:: ============================================================
:: deploy-image.bat - Deploy ModResorts to GCP GKE (Windows)
:: ============================================================

echo ==============================================
echo   ModResorts - GKE Deployment Script
echo ==============================================
echo.

:: ---- Collect GCP / GKE configuration ----
set /p GCP_PROJECT="Enter GCP Project ID: "
if "!GCP_PROJECT!"=="" (
    echo ERROR: GCP Project ID is required.
    exit /b 1
)

set /p GCP_ZONE="Enter GCP Zone (e.g. us-central1-a): "
if "!GCP_ZONE!"=="" (
    echo ERROR: GCP Zone is required.
    exit /b 1
)

set /p CLUSTER_NAME="Enter GKE Cluster Name: "
if "!CLUSTER_NAME!"=="" (
    echo ERROR: GKE Cluster Name is required.
    exit /b 1
)

set /p IMAGE_URI="Enter full Docker image URI (e.g. us-central1-docker.pkg.dev/my-project/repo/modresorts:latest): "
if "!IMAGE_URI!"=="" (
    echo ERROR: Docker image URI is required.
    exit /b 1
)

:: ---- Optional application environment variables ----
echo.
echo --- Optional Application Configuration ---
echo (Press Enter to skip any variable)
echo.

set /p WEATHER_API_KEY_VAL="Enter value for WEATHER_API_KEY (Weather Underground API key): "
if "!WEATHER_API_KEY_VAL!"=="" set "WEATHER_API_KEY_VAL="

:: ---- Determine script and project root directories ----
set "SCRIPT_DIR=%~dp0"
set "PROJECT_ROOT=%SCRIPT_DIR%.."
set "K8S_DIR=%PROJECT_ROOT%\kubernetes"

:: ---- Backup and update deployment manifest ----
echo.
echo Updating Kubernetes manifests...

copy /Y "!K8S_DIR!\deployment.yaml" "!K8S_DIR!\deployment.yaml.bak" >nul

:: Use PowerShell to replace placeholders
powershell -Command "(Get-Content '!K8S_DIR!\deployment.yaml') -replace '\{\{IMAGE_URI\}\}', '!IMAGE_URI!' | Set-Content '!K8S_DIR!\deployment.yaml'"
if !ERRORLEVEL! neq 0 (
    echo ERROR: Failed to update IMAGE_URI in deployment.yaml
    exit /b 1
)

powershell -Command "(Get-Content '!K8S_DIR!\deployment.yaml') -replace '\{\{WEATHER_API_KEY\}\}', '!WEATHER_API_KEY_VAL!' | Set-Content '!K8S_DIR!\deployment.yaml'"
if !ERRORLEVEL! neq 0 (
    echo ERROR: Failed to update WEATHER_API_KEY in deployment.yaml
    exit /b 1
)

echo Manifests updated successfully.

:: ---- Configure kubectl for GKE ----
echo.
echo Configuring kubectl for GKE cluster: !CLUSTER_NAME!...
gcloud container clusters get-credentials !CLUSTER_NAME! --zone !GCP_ZONE! --project !GCP_PROJECT!
if !ERRORLEVEL! neq 0 (
    echo ERROR: Failed to configure kubectl for GKE cluster.
    copy /Y "!K8S_DIR!\deployment.yaml.bak" "!K8S_DIR!\deployment.yaml" >nul
    exit /b 1
)

echo.
echo Verifying cluster connectivity...
kubectl cluster-info
if !ERRORLEVEL! neq 0 (
    echo ERROR: Cannot connect to cluster.
    copy /Y "!K8S_DIR!\deployment.yaml.bak" "!K8S_DIR!\deployment.yaml" >nul
    exit /b 1
)

:: ---- Apply Kubernetes manifests ----
echo.
echo Applying Kubernetes manifests...

echo   [1/4] Applying namespace...
kubectl apply -f "!K8S_DIR!\namespace.yaml"
if !ERRORLEVEL! neq 0 (
    echo ERROR: Failed to apply namespace.yaml
    copy /Y "!K8S_DIR!\deployment.yaml.bak" "!K8S_DIR!\deployment.yaml" >nul
    exit /b 1
)

echo   [2/4] Applying deployment...
kubectl apply -f "!K8S_DIR!\deployment.yaml"
if !ERRORLEVEL! neq 0 (
    echo ERROR: Failed to apply deployment.yaml
    copy /Y "!K8S_DIR!\deployment.yaml.bak" "!K8S_DIR!\deployment.yaml" >nul
    exit /b 1
)

echo   [3/4] Applying service...
kubectl apply -f "!K8S_DIR!\service.yaml"
if !ERRORLEVEL! neq 0 (
    echo ERROR: Failed to apply service.yaml
    copy /Y "!K8S_DIR!\deployment.yaml.bak" "!K8S_DIR!\deployment.yaml" >nul
    exit /b 1
)

echo   [4/4] Applying ingress...
kubectl apply -f "!K8S_DIR!\ingress.yaml"
if !ERRORLEVEL! neq 0 (
    echo ERROR: Failed to apply ingress.yaml
    copy /Y "!K8S_DIR!\deployment.yaml.bak" "!K8S_DIR!\deployment.yaml" >nul
    exit /b 1
)

:: ---- Wait for rollout ----
echo.
echo Waiting for deployment rollout...
kubectl rollout status deployment/modresorts -n modresorts --timeout=300s
if !ERRORLEVEL! neq 0 (
    echo.
    echo ERROR: Deployment rollout failed or timed out.
    echo To rollback, run: kubectl rollout undo deployment/modresorts -n modresorts
    copy /Y "!K8S_DIR!\deployment.yaml.bak" "!K8S_DIR!\deployment.yaml" >nul
    exit /b 1
)

:: ---- Restore original manifest ----
copy /Y "!K8S_DIR!\deployment.yaml.bak" "!K8S_DIR!\deployment.yaml" >nul

:: ---- Verify deployment ----
echo.
echo Verifying deployed resources...
kubectl get pods,svc,ingress -n modresorts

echo.
echo ==============================================
echo   Deployment completed successfully!
echo ==============================================
echo.
echo Useful commands:
echo   kubectl get pods -n modresorts
echo   kubectl logs -f deployment/modresorts -n modresorts
echo   kubectl rollout undo deployment/modresorts -n modresorts

endlocal
