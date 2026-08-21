@echo off
setlocal enabledelayedexpansion

:: =============================================================================
:: ModResorts - AWS ECS Fargate Deployment Script (Windows)
:: =============================================================================

set SERVICE_NAME=modresorts-service
set TASK_FAMILY=modresorts-task
set PROJECT_NAME=modresorts
set LOG_GROUP=/ecs/modresorts
set APP_PORT=9080

echo ==============================================
echo   ModResorts - AWS ECS Fargate Deployment
echo ==============================================
echo.

:: ---- Collect Configuration ----
set /p AWS_REGION="Enter AWS Region [us-east-1]: "
if "!AWS_REGION!"=="" set AWS_REGION=us-east-1

set /p CLUSTER_NAME="Enter ECS Cluster name [modresorts-cluster]: "
if "!CLUSTER_NAME!"=="" set CLUSTER_NAME=modresorts-cluster

set /p VPC_ID="Enter VPC ID (e.g. vpc-xxxxxxxx): "
if "!VPC_ID!"=="" (
    echo ERROR: VPC ID is required.
    exit /b 1
)

set /p SUBNETS_INPUT="Enter Subnet IDs (comma-separated, e.g. subnet-aaa,subnet-bbb): "
if "!SUBNETS_INPUT!"=="" (
    echo ERROR: At least one subnet ID is required.
    exit /b 1
)

set /p SECURITY_GROUP="Enter Security Group ID (e.g. sg-xxxxxxxx): "
if "!SECURITY_GROUP!"=="" (
    echo ERROR: Security Group ID is required.
    exit /b 1
)

set /p IMAGE_URI="Enter full ECR Image URI (e.g. 123456789.dkr.ecr.us-east-1.amazonaws.com/modresorts:latest): "
if "!IMAGE_URI!"=="" (
    echo ERROR: Image URI is required.
    exit /b 1
)

:: Parse subnets
for /f "tokens=1,2 delims=," %%a in ("!SUBNETS_INPUT!") do (
    set SUBNET_1=%%a
    set SUBNET_2=%%b
)
if "!SUBNET_2!"=="" set SUBNET_2=!SUBNET_1!

:: Remove spaces from subnets
set SUBNET_1=!SUBNET_1: =!
set SUBNET_2=!SUBNET_2: =!

:: ---- Get AWS Account ID ----
echo.
echo Retrieving AWS Account ID...
for /f "delims=" %%i in ('aws sts get-caller-identity --query Account --output text 2^>^&1') do set ACCOUNT_ID=%%i
if "!ACCOUNT_ID!"=="" (
    echo ERROR: Could not retrieve AWS Account ID. Ensure AWS CLI is configured.
    exit /b 1
)
echo AWS Account ID: !ACCOUNT_ID!

:: ---- Create CloudWatch Log Group ----
echo.
echo Creating CloudWatch log group: !LOG_GROUP! ...
aws logs create-log-group --log-group-name "!LOG_GROUP!" --region !AWS_REGION! >nul 2>&1
echo Log group ready.

:: ---- Check / Create ECS Cluster ----
echo.
echo Checking ECS cluster: !CLUSTER_NAME! ...
for /f "delims=" %%i in ('aws ecs describe-clusters --clusters !CLUSTER_NAME! --region !AWS_REGION! --query "clusters[0].status" --output text 2^>^&1') do set CLUSTER_STATUS=%%i
if "!CLUSTER_STATUS!" neq "ACTIVE" (
    echo Creating ECS cluster: !CLUSTER_NAME! ...
    aws ecs create-cluster --cluster-name !CLUSTER_NAME! --region !AWS_REGION!
    if !ERRORLEVEL! neq 0 (
        echo ERROR: Failed to create ECS cluster.
        exit /b 1
    )
    echo Cluster created.
) else (
    echo Cluster '!CLUSTER_NAME!' is ACTIVE.
)

:: ---- Load Balancer ----
echo.
set /p NEED_LB="Do you need an Application Load Balancer for this service? (y/n) [n]: "
if "!NEED_LB!"=="" set NEED_LB=n

set TARGET_GROUP_ARN=
set ALB_DNS=

if /i "!NEED_LB!"=="y" (
    echo.
    echo Creating Application Load Balancer...

    set ALB_NAME=!PROJECT_NAME!-alb
    for /f "delims=" %%i in ('aws elbv2 create-load-balancer --name !ALB_NAME! --subnets !SUBNET_1! !SUBNET_2! --security-groups !SECURITY_GROUP! --scheme internet-facing --type application --ip-address-type ipv4 --region !AWS_REGION! --query "LoadBalancers[0].LoadBalancerArn" --output text 2^>^&1') do set ALB_ARN=%%i
    echo ALB created: !ALB_ARN!

    for /f "delims=" %%i in ('aws elbv2 describe-load-balancers --load-balancer-arns !ALB_ARN! --region !AWS_REGION! --query "LoadBalancers[0].DNSName" --output text 2^>^&1') do set ALB_DNS=%%i

    set TG_NAME=!PROJECT_NAME!-tg
    for /f "delims=" %%i in ('aws elbv2 create-target-group --name !TG_NAME! --protocol HTTP --port !APP_PORT! --vpc-id !VPC_ID! --target-type ip --health-check-path "/resorts/health" --health-check-interval-seconds 30 --health-check-timeout-seconds 10 --healthy-threshold-count 2 --unhealthy-threshold-count 3 --region !AWS_REGION! --query "TargetGroups[0].TargetGroupArn" --output text 2^>^&1') do set TARGET_GROUP_ARN=%%i
    echo Target Group created: !TARGET_GROUP_ARN!

    aws elbv2 create-listener --load-balancer-arn !ALB_ARN! --protocol HTTP --port 80 --default-actions "Type=forward,TargetGroupArn=!TARGET_GROUP_ARN!" --region !AWS_REGION! >nul
    echo ALB Listener created on port 80.
)

:: ---- Prepare Task Definition ----
echo.
echo Preparing task definition...
copy /Y ecs\task-definition.json %TEMP%\task-definition-deploy.json >nul

powershell -Command "(Get-Content '%TEMP%\task-definition-deploy.json') -replace '{{IMAGE_URI}}','!IMAGE_URI!' -replace '{{AWS_REGION}}','!AWS_REGION!' -replace '{{ACCOUNT_ID}}','!ACCOUNT_ID!' | Set-Content '%TEMP%\task-definition-deploy.json'"

:: ---- Register Task Definition ----
echo Registering ECS task definition...
for /f "delims=" %%i in ('aws ecs register-task-definition --cli-input-json file://%TEMP%\task-definition-deploy.json --region !AWS_REGION! --query "taskDefinition.taskDefinitionArn" --output text 2^>^&1') do set TASK_DEF_ARN=%%i
if "!TASK_DEF_ARN!"=="" (
    echo ERROR: Failed to register task definition.
    exit /b 1
)
echo Task definition registered: !TASK_DEF_ARN!

:: ---- Prepare Service Definition ----
echo.
echo Preparing service definition...
copy /Y ecs\service-definition.json %TEMP%\service-definition-deploy.json >nul

powershell -Command "(Get-Content '%TEMP%\service-definition-deploy.json') -replace '{{CLUSTER_NAME}}','!CLUSTER_NAME!' -replace '{{SUBNET_1}}','!SUBNET_1!' -replace '{{SUBNET_2}}','!SUBNET_2!' -replace '{{SECURITY_GROUP}}','!SECURITY_GROUP!' | Set-Content '%TEMP%\service-definition-deploy.json'"

:: ---- Create or Update ECS Service ----
echo.
echo Checking if ECS service '!SERVICE_NAME!' exists...
for /f "delims=" %%i in ('aws ecs describe-services --cluster !CLUSTER_NAME! --services !SERVICE_NAME! --region !AWS_REGION! --query "services[?status!='INACTIVE'].serviceName" --output text 2^>^&1') do set EXISTING_SERVICE=%%i

if "!EXISTING_SERVICE!"=="" (
    echo Creating new ECS service: !SERVICE_NAME! ...
    aws ecs create-service --cli-input-json file://%TEMP%\service-definition-deploy.json --region !AWS_REGION!
    if !ERRORLEVEL! neq 0 (
        echo ERROR: Failed to create ECS service.
        exit /b 1
    )
    echo Service created.
) else (
    echo Updating existing ECS service: !SERVICE_NAME! ...
    aws ecs update-service --cluster !CLUSTER_NAME! --service !SERVICE_NAME! --task-definition !TASK_DEF_ARN! --desired-count 2 --region !AWS_REGION!
    if !ERRORLEVEL! neq 0 (
        echo ERROR: Failed to update ECS service.
        exit /b 1
    )
    echo Service updated.
)

:: ---- Wait for Service Stability ----
echo.
echo Waiting for service to become stable (this may take a few minutes)...
aws ecs wait services-stable --cluster !CLUSTER_NAME! --services !SERVICE_NAME! --region !AWS_REGION!
if !ERRORLEVEL! neq 0 (
    echo WARNING: Service did not stabilize within the expected time. Check ECS console.
) else (
    echo Service is stable.
)

:: ---- Verify Deployment ----
echo.
echo ==============================================
echo   Deployment Verification
echo ==============================================
aws ecs describe-services --cluster !CLUSTER_NAME! --services !SERVICE_NAME! --region !AWS_REGION! --query "services[0].{ServiceName:serviceName,Status:status,DesiredCount:desiredCount,RunningCount:runningCount}" --output table

echo.
echo CloudWatch Log Group: !LOG_GROUP!
echo   View logs: aws logs tail !LOG_GROUP! --follow --region !AWS_REGION!

if "!ALB_DNS!" neq "" (
    echo.
    echo Application Load Balancer DNS: http://!ALB_DNS!
    echo Application URL: http://!ALB_DNS!/resorts/
    echo Health Check URL: http://!ALB_DNS!/resorts/health
)

echo.
echo ==============================================
echo   Deployment Complete!
echo ==============================================
echo.
echo Troubleshooting tips:
echo   - View tasks: aws ecs list-tasks --cluster !CLUSTER_NAME! --region !AWS_REGION!
echo   - View logs: aws logs tail !LOG_GROUP! --follow --region !AWS_REGION!

endlocal
