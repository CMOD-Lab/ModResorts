@echo off
setlocal enabledelayedexpansion

:: =============================================================================
:: deploy-image.bat - Deploy BookingServices to AWS ECS Fargate (Windows)
:: =============================================================================

set PROJECT_NAME=bookingservices
set SERVICE_NAME=%PROJECT_NAME%-service
set TASK_FAMILY=%PROJECT_NAME%-task
set LOG_GROUP=/ecs/%PROJECT_NAME%
set TASK_DEF_FILE=ecs\task-definition.json
set SERVICE_DEF_FILE=ecs\service-definition.json

echo ==============================================
echo   BookingServices - AWS ECS Fargate Deploy
echo ==============================================
echo.

:: -------------------------------------------------------
:: Collect configuration
:: -------------------------------------------------------
set /p AWS_REGION="Enter AWS Region (e.g. us-east-1): "
set /p CLUSTER_NAME="Enter ECS Cluster name [bookingservices-cluster]: "
if "!CLUSTER_NAME!"=="" set CLUSTER_NAME=bookingservices-cluster

set /p IMAGE_URI="Enter ECR Image URI (e.g. 123456789.dkr.ecr.us-east-1.amazonaws.com/bookingservices:latest): "
set /p VPC_ID="Enter VPC ID (e.g. vpc-xxxxxxxx): "
set /p SUBNETS_INPUT="Enter Subnet IDs (comma-separated, e.g. subnet-aaa,subnet-bbb): "
set /p SECURITY_GROUP="Enter Security Group ID (e.g. sg-xxxxxxxx): "

:: Parse subnets using PowerShell
for /f "delims=" %%i in ('powershell -Command "\"!SUBNETS_INPUT!\".Split(',')[0].Trim()"') do set SUBNET_1=%%i
for /f "delims=" %%i in ('powershell -Command "\"!SUBNETS_INPUT!\".Split(',') | Select-Object -Index 1 | ForEach-Object { $_.Trim() }"') do set SUBNET_2=%%i
if "!SUBNET_2!"=="" set SUBNET_2=!SUBNET_1!

:: -------------------------------------------------------
:: Get AWS Account ID
:: -------------------------------------------------------
echo.
echo Retrieving AWS Account ID...
for /f "delims=" %%i in ('aws sts get-caller-identity --query Account --output text') do set ACCOUNT_ID=%%i
echo Account ID: !ACCOUNT_ID!

:: -------------------------------------------------------
:: Ensure CloudWatch log group exists
:: -------------------------------------------------------
echo.
echo Ensuring CloudWatch log group exists: %LOG_GROUP% ...
aws logs create-log-group --log-group-name "%LOG_GROUP%" --region !AWS_REGION! >nul 2>&1

:: -------------------------------------------------------
:: Check/create ECS cluster
:: -------------------------------------------------------
echo Checking ECS cluster: !CLUSTER_NAME! ...
for /f "delims=" %%i in ('aws ecs describe-clusters --clusters !CLUSTER_NAME! --region !AWS_REGION! --query "clusters[0].status" --output text 2^>nul') do set CLUSTER_STATUS=%%i

if not "!CLUSTER_STATUS!"=="ACTIVE" (
    echo Creating ECS cluster: !CLUSTER_NAME! ...
    aws ecs create-cluster --cluster-name !CLUSTER_NAME! --region !AWS_REGION!
    if !ERRORLEVEL! neq 0 (
        echo ERROR: Failed to create ECS cluster.
        exit /b 1
    )
)

:: -------------------------------------------------------
:: Load balancer prompt
:: -------------------------------------------------------
echo.
set /p NEED_LB="Do you need an Application Load Balancer for this service? (y/n) [n]: "
if "!NEED_LB!"=="" set NEED_LB=n

set TARGET_GROUP_ARN=
set ALB_DNS=

if /i "!NEED_LB!"=="y" (
    echo.
    echo Creating Application Load Balancer...

    set ALB_NAME=%PROJECT_NAME%-alb
    for /f "delims=" %%i in ('aws elbv2 create-load-balancer --name !ALB_NAME! --subnets !SUBNET_1! !SUBNET_2! --security-groups !SECURITY_GROUP! --scheme internet-facing --type application --region !AWS_REGION! --query "LoadBalancers[0].LoadBalancerArn" --output text') do set ALB_ARN=%%i
    echo ALB ARN: !ALB_ARN!

    for /f "delims=" %%i in ('aws elbv2 describe-load-balancers --load-balancer-arns !ALB_ARN! --region !AWS_REGION! --query "LoadBalancers[0].DNSName" --output text') do set ALB_DNS=%%i

    set TG_NAME=%PROJECT_NAME%-tg
    for /f "delims=" %%i in ('aws elbv2 create-target-group --name !TG_NAME! --protocol HTTP --port 8080 --vpc-id !VPC_ID! --target-type ip --health-check-path "/resorts/health" --health-check-interval-seconds 30 --healthy-threshold-count 2 --unhealthy-threshold-count 3 --region !AWS_REGION! --query "TargetGroups[0].TargetGroupArn" --output text') do set TARGET_GROUP_ARN=%%i
    echo Target Group ARN: !TARGET_GROUP_ARN!

    aws elbv2 create-listener --load-balancer-arn !ALB_ARN! --protocol HTTP --port 80 --default-actions Type=forward,TargetGroupArn=!TARGET_GROUP_ARN! --region !AWS_REGION! >nul
    echo ALB Listener created on port 80.
)

:: -------------------------------------------------------
:: Prepare task definition (replace placeholders via PowerShell)
:: -------------------------------------------------------
echo.
echo Preparing task definition...
copy /Y "%TASK_DEF_FILE%" "%TEMP%\task-definition-deploy.json" >nul

powershell -Command "(Get-Content '%TEMP%\task-definition-deploy.json') -replace '{{IMAGE_URI}}','!IMAGE_URI!' -replace '{{AWS_REGION}}','!AWS_REGION!' -replace '{{ACCOUNT_ID}}','!ACCOUNT_ID!' | Set-Content '%TEMP%\task-definition-deploy.json'"

:: -------------------------------------------------------
:: Register task definition
:: -------------------------------------------------------
echo Registering ECS task definition...
for /f "delims=" %%i in ('aws ecs register-task-definition --cli-input-json file://%TEMP%\task-definition-deploy.json --region !AWS_REGION! --query "taskDefinition.taskDefinitionArn" --output text') do set TASK_DEF_ARN=%%i
echo Task Definition ARN: !TASK_DEF_ARN!

:: -------------------------------------------------------
:: Prepare service definition (replace placeholders via PowerShell)
:: -------------------------------------------------------
echo Preparing service definition...
copy /Y "%SERVICE_DEF_FILE%" "%TEMP%\service-definition-deploy.json" >nul

powershell -Command "(Get-Content '%TEMP%\service-definition-deploy.json') -replace '{{CLUSTER_NAME}}','!CLUSTER_NAME!' -replace '{{SUBNET_1}}','!SUBNET_1!' -replace '{{SUBNET_2}}','!SUBNET_2!' -replace '{{SECURITY_GROUP}}','!SECURITY_GROUP!' | Set-Content '%TEMP%\service-definition-deploy.json'"

:: Handle load balancer in service JSON via PowerShell
if /i "!NEED_LB!"=="y" (
    powershell -Command "$svc = Get-Content '%TEMP%\service-definition-deploy.json' | ConvertFrom-Json; $lb = @{targetGroupArn='!TARGET_GROUP_ARN!'; containerName='bookingservices'; containerPort=8080}; $svc | Add-Member -NotePropertyName 'loadBalancers' -NotePropertyValue @($lb) -Force; $svc | Add-Member -NotePropertyName 'healthCheckGracePeriodSeconds' -NotePropertyValue 300 -Force; $svc | ConvertTo-Json -Depth 10 | Set-Content '%TEMP%\service-definition-deploy.json'"
) else (
    powershell -Command "$svc = Get-Content '%TEMP%\service-definition-deploy.json' | ConvertFrom-Json; $svc.PSObject.Properties.Remove('loadBalancers'); $svc.PSObject.Properties.Remove('healthCheckGracePeriodSeconds'); $svc | ConvertTo-Json -Depth 10 | Set-Content '%TEMP%\service-definition-deploy.json'"
)

:: -------------------------------------------------------
:: Create or update ECS service
:: -------------------------------------------------------
echo.
echo Checking if ECS service exists: !SERVICE_NAME! ...
for /f "delims=" %%i in ('aws ecs describe-services --cluster !CLUSTER_NAME! --services !SERVICE_NAME! --region !AWS_REGION! --query "services[?status==''ACTIVE''].serviceName" --output text 2^>nul') do set EXISTING_SERVICE=%%i

if "!EXISTING_SERVICE!"=="" (
    echo Creating new ECS service: !SERVICE_NAME! ...
    aws ecs create-service --cli-input-json file://%TEMP%\service-definition-deploy.json --region !AWS_REGION!
    if !ERRORLEVEL! neq 0 (
        echo ERROR: Failed to create ECS service.
        exit /b 1
    )
) else (
    echo Updating existing ECS service: !SERVICE_NAME! ...
    aws ecs update-service --cluster !CLUSTER_NAME! --service !SERVICE_NAME! --task-definition !TASK_DEF_ARN! --region !AWS_REGION!
    if !ERRORLEVEL! neq 0 (
        echo ERROR: Failed to update ECS service.
        exit /b 1
    )
)

:: -------------------------------------------------------
:: Wait for service stability
:: -------------------------------------------------------
echo.
echo Waiting for service to stabilize (this may take a few minutes)...
aws ecs wait services-stable --cluster !CLUSTER_NAME! --services !SERVICE_NAME! --region !AWS_REGION!
if !ERRORLEVEL! neq 0 (
    echo WARNING: Service did not stabilize within the expected time. Check ECS console.
)

:: -------------------------------------------------------
:: Verify deployment
:: -------------------------------------------------------
echo.
echo Verifying deployment...
aws ecs describe-services --cluster !CLUSTER_NAME! --services !SERVICE_NAME! --region !AWS_REGION! --query "services[0].{ServiceName:serviceName,Status:status,DesiredCount:desiredCount,RunningCount:runningCount,PendingCount:pendingCount}" --output table

echo.
echo ==============================================
echo   DEPLOYMENT COMPLETE
echo ==============================================
echo   Cluster:      !CLUSTER_NAME!
echo   Service:      !SERVICE_NAME!
echo   Task Def ARN: !TASK_DEF_ARN!
echo   CloudWatch:   %LOG_GROUP%
if /i "!NEED_LB!"=="y" (
    echo   ALB DNS:      http://!ALB_DNS!
)
echo.
echo Troubleshooting:
echo   View logs:  aws logs tail %LOG_GROUP% --follow --region !AWS_REGION!
echo   List tasks: aws ecs list-tasks --cluster !CLUSTER_NAME! --service-name !SERVICE_NAME! --region !AWS_REGION!
echo ==============================================

endlocal
