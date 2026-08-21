#!/bin/bash
# =============================================================================
# ModResorts - AWS ECS Fargate Deployment Script
# Deploys the ModResorts application to AWS ECS Fargate
# =============================================================================
set -e
set -o pipefail

SERVICE_NAME="modresorts-service"
TASK_FAMILY="modresorts-task"
PROJECT_NAME="modresorts"
LOG_GROUP="/ecs/modresorts"
APP_PORT=9080

echo "=============================================="
echo "  ModResorts - AWS ECS Fargate Deployment"
echo "=============================================="
echo ""

# ---- Collect Configuration ----
read -p "Enter AWS Region [us-east-1]: " AWS_REGION
AWS_REGION="${AWS_REGION:-us-east-1}"

read -p "Enter ECS Cluster name [modresorts-cluster]: " CLUSTER_NAME
CLUSTER_NAME="${CLUSTER_NAME:-modresorts-cluster}"

read -p "Enter VPC ID (e.g. vpc-xxxxxxxx): " VPC_ID
if [ -z "$VPC_ID" ]; then
  echo "ERROR: VPC ID is required."
  exit 1
fi

read -p "Enter Subnet IDs (comma-separated, e.g. subnet-aaa,subnet-bbb): " SUBNETS_INPUT
if [ -z "$SUBNETS_INPUT" ]; then
  echo "ERROR: At least one subnet ID is required."
  exit 1
fi

read -p "Enter Security Group ID (e.g. sg-xxxxxxxx): " SECURITY_GROUP
if [ -z "$SECURITY_GROUP" ]; then
  echo "ERROR: Security Group ID is required."
  exit 1
fi

read -p "Enter full ECR Image URI (e.g. 123456789.dkr.ecr.us-east-1.amazonaws.com/modresorts:latest): " IMAGE_URI
if [ -z "$IMAGE_URI" ]; then
  echo "ERROR: Image URI is required."
  exit 1
fi

# Parse subnets into JSON array
SUBNET_1=$(echo "$SUBNETS_INPUT" | cut -d',' -f1 | tr -d ' ')
SUBNET_2=$(echo "$SUBNETS_INPUT" | cut -d',' -f2 | tr -d ' ')
if [ -z "$SUBNET_2" ]; then
  SUBNET_2="$SUBNET_1"
fi

# ---- Get AWS Account ID ----
echo ""
echo "Retrieving AWS Account ID..."
ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
if [ -z "$ACCOUNT_ID" ]; then
  echo "ERROR: Could not retrieve AWS Account ID. Ensure AWS CLI is configured."
  exit 1
fi
echo "AWS Account ID: $ACCOUNT_ID"

# ---- Create CloudWatch Log Group ----
echo ""
echo "Creating CloudWatch log group: $LOG_GROUP ..."
aws logs create-log-group --log-group-name "$LOG_GROUP" --region "$AWS_REGION" 2>/dev/null || echo "Log group already exists."

# ---- Check / Create ECS Cluster ----
echo ""
echo "Checking ECS cluster: $CLUSTER_NAME ..."
CLUSTER_STATUS=$(aws ecs describe-clusters --clusters "$CLUSTER_NAME" --region "$AWS_REGION" \
  --query "clusters[0].status" --output text 2>/dev/null || echo "MISSING")

if [ "$CLUSTER_STATUS" != "ACTIVE" ]; then
  echo "Creating ECS cluster: $CLUSTER_NAME ..."
  aws ecs create-cluster --cluster-name "$CLUSTER_NAME" --region "$AWS_REGION"
  echo "Cluster created."
else
  echo "Cluster '$CLUSTER_NAME' is ACTIVE."
fi

# ---- Load Balancer ----
echo ""
read -p "Do you need an Application Load Balancer for this service? (y/n) [n]: " NEED_LB
NEED_LB="${NEED_LB:-n}"

TARGET_GROUP_ARN=""
ALB_DNS=""

if [ "$NEED_LB" = "y" ] || [ "$NEED_LB" = "Y" ]; then
  echo ""
  echo "Creating Application Load Balancer..."

  # Build subnet list for ALB
  SUBNET_LIST=$(echo "$SUBNETS_INPUT" | tr ',' ' ')

  ALB_NAME="${PROJECT_NAME}-alb"
  ALB_ARN=$(aws elbv2 create-load-balancer \
    --name "$ALB_NAME" \
    --subnets $SUBNET_LIST \
    --security-groups "$SECURITY_GROUP" \
    --scheme internet-facing \
    --type application \
    --ip-address-type ipv4 \
    --region "$AWS_REGION" \
    --query "LoadBalancers[0].LoadBalancerArn" \
    --output text)
  echo "ALB created: $ALB_ARN"

  ALB_DNS=$(aws elbv2 describe-load-balancers \
    --load-balancer-arns "$ALB_ARN" \
    --region "$AWS_REGION" \
    --query "LoadBalancers[0].DNSName" \
    --output text)

  # Create Target Group (target-type ip required for Fargate awsvpc)
  TG_NAME="${PROJECT_NAME}-tg"
  TARGET_GROUP_ARN=$(aws elbv2 create-target-group \
    --name "$TG_NAME" \
    --protocol HTTP \
    --port "$APP_PORT" \
    --vpc-id "$VPC_ID" \
    --target-type ip \
    --health-check-path "/resorts/health" \
    --health-check-interval-seconds 30 \
    --health-check-timeout-seconds 10 \
    --healthy-threshold-count 2 \
    --unhealthy-threshold-count 3 \
    --region "$AWS_REGION" \
    --query "TargetGroups[0].TargetGroupArn" \
    --output text)
  echo "Target Group created: $TARGET_GROUP_ARN"

  # Create ALB Listener
  aws elbv2 create-listener \
    --load-balancer-arn "$ALB_ARN" \
    --protocol HTTP \
    --port 80 \
    --default-actions "Type=forward,TargetGroupArn=$TARGET_GROUP_ARN" \
    --region "$AWS_REGION" >/dev/null
  echo "ALB Listener created on port 80."
fi

# ---- Prepare Task Definition ----
echo ""
echo "Preparing task definition..."
cp ecs/task-definition.json /tmp/task-definition-deploy.json

sed -i "s|{{IMAGE_URI}}|$IMAGE_URI|g" /tmp/task-definition-deploy.json
sed -i "s|{{AWS_REGION}}|$AWS_REGION|g" /tmp/task-definition-deploy.json
sed -i "s|{{ACCOUNT_ID}}|$ACCOUNT_ID|g" /tmp/task-definition-deploy.json

# ---- Register Task Definition ----
echo "Registering ECS task definition..."
TASK_DEF_ARN=$(aws ecs register-task-definition \
  --cli-input-json file:///tmp/task-definition-deploy.json \
  --region "$AWS_REGION" \
  --query "taskDefinition.taskDefinitionArn" \
  --output text)
echo "Task definition registered: $TASK_DEF_ARN"

# ---- Prepare Service Definition ----
echo ""
echo "Preparing service definition..."
cp ecs/service-definition.json /tmp/service-definition-deploy.json

sed -i "s|{{CLUSTER_NAME}}|$CLUSTER_NAME|g" /tmp/service-definition-deploy.json
sed -i "s|{{SUBNET_1}}|$SUBNET_1|g" /tmp/service-definition-deploy.json
sed -i "s|{{SUBNET_2}}|$SUBNET_2|g" /tmp/service-definition-deploy.json
sed -i "s|{{SECURITY_GROUP}}|$SECURITY_GROUP|g" /tmp/service-definition-deploy.json

# Add load balancer config if needed
if [ -n "$TARGET_GROUP_ARN" ]; then
  # Inject loadBalancers and healthCheckGracePeriodSeconds into service definition
  python3 -c "
import json, sys
with open('/tmp/service-definition-deploy.json') as f:
    svc = json.load(f)
svc['loadBalancers'] = [{
    'targetGroupArn': '$TARGET_GROUP_ARN',
    'containerName': 'modresorts',
    'containerPort': $APP_PORT
}]
svc['healthCheckGracePeriodSeconds'] = 300
with open('/tmp/service-definition-deploy.json', 'w') as f:
    json.dump(svc, f, indent=2)
print('Load balancer config injected.')
" 2>/dev/null || \
  echo "Note: python3 not available; load balancer config not auto-injected. Add manually if needed."
fi

# ---- Create or Update ECS Service ----
echo ""
echo "Checking if ECS service '$SERVICE_NAME' exists..."
EXISTING_SERVICE=$(aws ecs describe-services \
  --cluster "$CLUSTER_NAME" \
  --services "$SERVICE_NAME" \
  --region "$AWS_REGION" \
  --query "services[?status!='INACTIVE'].serviceName" \
  --output text 2>/dev/null || echo "")

if [ -z "$EXISTING_SERVICE" ] || [ "$EXISTING_SERVICE" = "None" ]; then
  echo "Creating new ECS service: $SERVICE_NAME ..."
  aws ecs create-service \
    --cli-input-json file:///tmp/service-definition-deploy.json \
    --region "$AWS_REGION"
  echo "Service created."
else
  echo "Updating existing ECS service: $SERVICE_NAME ..."
  aws ecs update-service \
    --cluster "$CLUSTER_NAME" \
    --service "$SERVICE_NAME" \
    --task-definition "$TASK_DEF_ARN" \
    --desired-count 2 \
    --region "$AWS_REGION"
  echo "Service updated."
fi

# ---- Wait for Service Stability ----
echo ""
echo "Waiting for service to become stable (this may take a few minutes)..."
aws ecs wait services-stable \
  --cluster "$CLUSTER_NAME" \
  --services "$SERVICE_NAME" \
  --region "$AWS_REGION"
echo "Service is stable."

# ---- Verify Deployment ----
echo ""
echo "=============================================="
echo "  Deployment Verification"
echo "=============================================="
aws ecs describe-services \
  --cluster "$CLUSTER_NAME" \
  --services "$SERVICE_NAME" \
  --region "$AWS_REGION" \
  --query "services[0].{ServiceName:serviceName,Status:status,DesiredCount:desiredCount,RunningCount:runningCount,PendingCount:pendingCount}" \
  --output table

echo ""
echo "CloudWatch Log Group: $LOG_GROUP"
echo "  View logs: aws logs tail $LOG_GROUP --follow --region $AWS_REGION"

if [ -n "$ALB_DNS" ]; then
  echo ""
  echo "Application Load Balancer DNS: http://$ALB_DNS"
  echo "Application URL: http://$ALB_DNS/resorts/"
  echo "Health Check URL: http://$ALB_DNS/resorts/health"
fi

echo ""
echo "=============================================="
echo "  Deployment Complete!"
echo "=============================================="
echo ""
echo "Troubleshooting tips:"
echo "  - View tasks: aws ecs list-tasks --cluster $CLUSTER_NAME --region $AWS_REGION"
echo "  - Describe task: aws ecs describe-tasks --cluster $CLUSTER_NAME --tasks <task-arn> --region $AWS_REGION"
echo "  - View logs: aws logs tail $LOG_GROUP --follow --region $AWS_REGION"
