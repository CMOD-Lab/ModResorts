@echo off
setlocal enabledelayedexpansion

:: =============================================================================
:: deploy-image.bat – Deploy ModResorts to AWS ECS Fargate (Windows)
:: Usage: scripts\deploy-image.bat
:: Prerequisites: AWS CLI configured, Python 3 (optional for JSON patching)
:: =============================================================================

set "SERVICE_NAME=modresorts-service"
set "TASK_FAMILY=modresorts-task"
set "CONTAINER_NAME=modresorts"
set "APP_PORT=9080"
set "LOG_GROUP=/ecs/modresorts"

echo ==============================================
echo   ModResorts - AWS ECS Fargate Deployment
echo ==============================================
echo.

:: ---------------------------------------------------------------------------
:: Collect inputs
:: ---------------------------------------------------------------------------
set /p "AWS_REGION=Enter AWS Region (e.g. us-east-1): "
set /p "CLUSTER_NAME=Enter ECS Cluster name: "
set /p "IMAGE_URI=Enter ECR Image URI (repo:tag): "
set /p "SUBNETS_RAW=Enter Subnet IDs (comma-separated, min 2): "
set /p "SECURITY_GROUP=Enter Security Group ID: "

:: Parse subnets (first two)
for /f "tokens=1,2 delims=," %%A in ("!SUBNETS_RAW!") do (
    set "SUBNET_1=%%A"
    set "SUBNET_2=%%B"
)
if "!SUBNET_2!"=="" set "SUBNET_2=!SUBNET_1!"

:: Strip spaces
set "SUBNET_1=!SUBNET_1: =!"
set "SUBNET_2=!SUBNET_2: =!"

:: ---------------------------------------------------------------------------
:: Resolve AWS Account ID
:: ---------------------------------------------------------------------------
echo.
echo Resolving AWS Account ID ...
for /f "delims=" %%I in ('aws sts get-caller-identity --query Account --output text') do set "ACCOUNT_ID=%%I"
echo Account ID : !ACCOUNT_ID!

:: ---------------------------------------------------------------------------
:: Ensure CloudWatch log group exists
:: ---------------------------------------------------------------------------
echo.
echo Ensuring CloudWatch log group '!LOG_GROUP!' exists ...
aws logs create-log-group --log-group-name "!LOG_GROUP!" --region "!AWS_REGION!" >nul 2>&1

:: ---------------------------------------------------------------------------
:: Ensure ECS cluster exists
:: ---------------------------------------------------------------------------
echo Ensuring ECS cluster '!CLUSTER_NAME!' exists ...
for /f "delims=" %%S in ('aws ecs describe-clusters --clusters "!CLUSTER_NAME!" --region "!AWS_REGION!" --query "clusters[0].status" --output text 2^>nul') do set "CLUSTER_STATUS=%%S"
if not "!CLUSTER_STATUS!"=="ACTIVE" (
    echo Creating ECS cluster '!CLUSTER_NAME!' ...
    aws ecs create-cluster --cluster-name "!CLUSTER_NAME!" --region "!AWS_REGION!"
    if !ERRORLEVEL! neq 0 (
        echo ERROR: Failed to create ECS cluster.
        exit /b 1
    )
)

:: ---------------------------------------------------------------------------
:: Load balancer (optional)
:: ---------------------------------------------------------------------------
echo.
set /p "NEED_LB=Do you need an Application Load Balancer for this service? (y/n): "
set "USE_LB=false"
set "TARGET_GROUP_ARN="
set "ALB_DNS="

if /i "!NEED_LB!"=="y" (
    set "USE_LB=true"
    set /p "VPC_ID=Enter VPC ID for the ALB: "

    set "ALB_NAME=modresorts-alb"
    set "TG_NAME=modresorts-tg"

    echo.
    echo Creating Application Load Balancer '!ALB_NAME!' ...
    for /f "delims=" %%A in ('aws elbv2 create-load-balancer --name "!ALB_NAME!" --subnets "!SUBNET_1!" "!SUBNET_2!" --security-groups "!SECURITY_GROUP!" --scheme internet-facing --type application --region "!AWS_REGION!" --query "LoadBalancers[0].LoadBalancerArn" --output text') do set "ALB_ARN=%%A"
    if !ERRORLEVEL! neq 0 (
        echo ERROR: Failed to create ALB.
        exit /b 1
    )

    for /f "delims=" %%D in ('aws elbv2 describe-load-balancers --load-balancer-arns "!ALB_ARN!" --region "!AWS_REGION!" --query "LoadBalancers[0].DNSName" --output text') do set "ALB_DNS=%%D"

    echo Creating Target Group '!TG_NAME!' ...
    for /f "delims=" %%T in ('aws elbv2 create-target-group --name "!TG_NAME!" --protocol HTTP --port "!APP_PORT!" --vpc-id "!VPC_ID!" --target-type ip --health-check-path "/resorts/health" --health-check-interval-seconds 30 --healthy-threshold-count 2 --unhealthy-threshold-count 3 --region "!AWS_REGION!" --query "TargetGroups[0].TargetGroupArn" --output text') do set "TARGET_GROUP_ARN=%%T"
    if !ERRORLEVEL! neq 0 (
        echo ERROR: Failed to create Target Group.
        exit /b 1
    )

    echo Creating ALB Listener on port 80 ...
    aws elbv2 create-listener --load-balancer-arn "!ALB_ARN!" --protocol HTTP --port 80 --default-actions "Type=forward,TargetGroupArn=!TARGET_GROUP_ARN!" --region "!AWS_REGION!" >nul
    echo ALB DNS  : !ALB_DNS!
    echo TG ARN   : !TARGET_GROUP_ARN!
)

:: ---------------------------------------------------------------------------
:: Prepare task definition (replace placeholders)
:: ---------------------------------------------------------------------------
echo.
echo Preparing task definition ...
set "TASK_DEF_SRC=%~dp0..\ecs\task-definition.json"
set "TASK_DEF_TMP=%TEMP%\modresorts-task-definition.json"

powershell -Command "(Get-Content '!TASK_DEF_SRC!') -replace '{{ACCOUNT_ID}}','!ACCOUNT_ID!' -replace '{{AWS_REGION}}','!AWS_REGION!' -replace '{{IMAGE_URI}}','!IMAGE_URI!' | Set-Content '!TASK_DEF_TMP!'"
if !ERRORLEVEL! neq 0 (
    echo ERROR: Failed to prepare task definition.
    exit /b 1
)

:: ---------------------------------------------------------------------------
:: Register task definition
:: ---------------------------------------------------------------------------
echo Registering task definition ...
for /f "delims=" %%R in ('aws ecs register-task-definition --cli-input-json "file://!TASK_DEF_TMP!" --region "!AWS_REGION!" --query "taskDefinition.taskDefinitionArn" --output text') do set "TASK_DEF_ARN=%%R"
if !ERRORLEVEL! neq 0 (
    echo ERROR: Failed to register task definition.
    exit /b 1
)
echo Task Definition ARN: !TASK_DEF_ARN!

:: ---------------------------------------------------------------------------
:: Prepare service definition (replace placeholders)
:: ---------------------------------------------------------------------------
set "SVC_DEF_SRC=%~dp0..\ecs\service-definition.json"
set "SVC_DEF_TMP=%TEMP%\modresorts-service-definition.json"

powershell -Command "(Get-Content '!SVC_DEF_SRC!') -replace '{{CLUSTER_NAME}}','!CLUSTER_NAME!' -replace '{{SUBNET_1}}','!SUBNET_1!' -replace '{{SUBNET_2}}','!SUBNET_2!' -replace '{{SECURITY_GROUP}}','!SECURITY_GROUP!' -replace '\"!TASK_FAMILY!\"','\"!TASK_DEF_ARN!\"' | Set-Content '!SVC_DEF_TMP!'"
if !ERRORLEVEL! neq 0 (
    echo ERROR: Failed to prepare service definition.
    exit /b 1
)

:: ---------------------------------------------------------------------------
:: Create or update ECS service
:: ---------------------------------------------------------------------------
echo.
for /f "delims=" %%E in ('aws ecs describe-services --cluster "!CLUSTER_NAME!" --services "!SERVICE_NAME!" --region "!AWS_REGION!" --query "services[?status==''ACTIVE''].serviceName" --output text 2^>nul') do set "EXISTING_SERVICE=%%E"

if "!EXISTING_SERVICE!"=="" (
    echo Creating ECS service '!SERVICE_NAME!' ...
    aws ecs create-service --cli-input-json "file://!SVC_DEF_TMP!" --region "!AWS_REGION!"
    if !ERRORLEVEL! neq 0 (
        echo ERROR: Failed to create ECS service.
        exit /b 1
    )
) else (
    echo Updating existing ECS service '!SERVICE_NAME!' ...
    aws ecs update-service --cluster "!CLUSTER_NAME!" --service "!SERVICE_NAME!" --task-definition "!TASK_DEF_ARN!" --region "!AWS_REGION!"
    if !ERRORLEVEL! neq 0 (
        echo ERROR: Failed to update ECS service.
        exit /b 1
    )
)

:: ---------------------------------------------------------------------------
:: Wait for stability
:: ---------------------------------------------------------------------------
echo.
echo Waiting for service to stabilise (this may take a few minutes) ...
aws ecs wait services-stable --cluster "!CLUSTER_NAME!" --services "!SERVICE_NAME!" --region "!AWS_REGION!"
if !ERRORLEVEL! neq 0 (
    echo WARNING: Service did not stabilise within the expected time. Check ECS console.
)

:: ---------------------------------------------------------------------------
:: Summary
:: ---------------------------------------------------------------------------
echo.
echo ==============================================
echo   Deployment complete!
echo ==============================================
aws ecs describe-services --cluster "!CLUSTER_NAME!" --services "!SERVICE_NAME!" --region "!AWS_REGION!" --query "services[0].{Status:status,Running:runningCount,Desired:desiredCount}" --output table

echo.
echo CloudWatch Logs : !LOG_GROUP!
if not "!ALB_DNS!"=="" (
    echo Application URL : http://!ALB_DNS!/resorts/
)
echo.
echo Troubleshooting tips:
echo   - View tasks  : aws ecs list-tasks --cluster !CLUSTER_NAME! --service-name !SERVICE_NAME! --region !AWS_REGION!
echo   - Task logs   : aws logs tail !LOG_GROUP! --follow --region !AWS_REGION!

endlocal
