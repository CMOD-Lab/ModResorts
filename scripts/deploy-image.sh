#!/usr/bin/env bash
# =============================================================================
# deploy-image.sh – Deploy ModResorts to AWS ECS Fargate
# Usage: ./scripts/deploy-image.sh
# Prerequisites: aws CLI configured, jq installed
# =============================================================================
set -e
set -o pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "${SCRIPT_DIR}")"
ECS_DIR="${PROJECT_ROOT}/ecs"

SERVICE_NAME="modresorts-service"
TASK_FAMILY="modresorts-task"
CONTAINER_NAME="modresorts"
APP_PORT=9080
LOG_GROUP="/ecs/modresorts"

echo "=============================================="
echo "  ModResorts – AWS ECS Fargate Deployment"
echo "=============================================="
echo ""

# ---------------------------------------------------------------------------
# Collect inputs
# ---------------------------------------------------------------------------
read -rp "Enter AWS Region (e.g. us-east-1): "          AWS_REGION
read -rp "Enter ECS Cluster name: "                      CLUSTER_NAME
read -rp "Enter ECR Image URI (repo:tag): "              IMAGE_URI
read -rp "Enter Subnet IDs (comma-separated, min 2): "   SUBNETS_RAW
read -rp "Enter Security Group ID: "                     SECURITY_GROUP

# Parse subnets
SUBNET_1=$(echo "${SUBNETS_RAW}" | cut -d',' -f1 | tr -d ' ')
SUBNET_2=$(echo "${SUBNETS_RAW}" | cut -d',' -f2 | tr -d ' ')
if [ -z "${SUBNET_2}" ]; then
  SUBNET_2="${SUBNET_1}"
fi

# ---------------------------------------------------------------------------
# Resolve AWS Account ID
# ---------------------------------------------------------------------------
echo ""
echo "Resolving AWS Account ID …"
ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
echo "Account ID : ${ACCOUNT_ID}"

# ---------------------------------------------------------------------------
# Ensure CloudWatch log group exists
# ---------------------------------------------------------------------------
echo ""
echo "Ensuring CloudWatch log group '${LOG_GROUP}' exists …"
aws logs create-log-group --log-group-name "${LOG_GROUP}" --region "${AWS_REGION}" 2>/dev/null || true

# ---------------------------------------------------------------------------
# Ensure ECS cluster exists
# ---------------------------------------------------------------------------
echo "Ensuring ECS cluster '${CLUSTER_NAME}' exists …"
CLUSTER_STATUS=$(aws ecs describe-clusters \
  --clusters "${CLUSTER_NAME}" \
  --region "${AWS_REGION}" \
  --query "clusters[0].status" \
  --output text 2>/dev/null || echo "NONE")

if [ "${CLUSTER_STATUS}" != "ACTIVE" ]; then
  echo "Creating ECS cluster '${CLUSTER_NAME}' …"
  aws ecs create-cluster --cluster-name "${CLUSTER_NAME}" --region "${AWS_REGION}"
fi

# ---------------------------------------------------------------------------
# Load balancer (optional)
# ---------------------------------------------------------------------------
echo ""
read -rp "Do you need an Application Load Balancer for this service? (y/n): " NEED_LB

USE_LB=false
TARGET_GROUP_ARN=""
ALB_DNS=""

if [[ "${NEED_LB}" =~ ^[Yy]$ ]]; then
  USE_LB=true
  read -rp "Enter VPC ID for the ALB: " VPC_ID

  ALB_NAME="modresorts-alb"
  TG_NAME="modresorts-tg"

  echo ""
  echo "Creating Application Load Balancer '${ALB_NAME}' …"
  ALB_ARN=$(aws elbv2 create-load-balancer \
    --name "${ALB_NAME}" \
    --subnets "${SUBNET_1}" "${SUBNET_2}" \
    --security-groups "${SECURITY_GROUP}" \
    --scheme internet-facing \
    --type application \
    --region "${AWS_REGION}" \
    --query "LoadBalancers[0].LoadBalancerArn" \
    --output text)

  ALB_DNS=$(aws elbv2 describe-load-balancers \
    --load-balancer-arns "${ALB_ARN}" \
    --region "${AWS_REGION}" \
    --query "LoadBalancers[0].DNSName" \
    --output text)

  echo "Creating Target Group '${TG_NAME}' (target-type: ip) …"
  TARGET_GROUP_ARN=$(aws elbv2 create-target-group \
    --name "${TG_NAME}" \
    --protocol HTTP \
    --port "${APP_PORT}" \
    --vpc-id "${VPC_ID}" \
    --target-type ip \
    --health-check-path "/resorts/health" \
    --health-check-interval-seconds 30 \
    --healthy-threshold-count 2 \
    --unhealthy-threshold-count 3 \
    --region "${AWS_REGION}" \
    --query "TargetGroups[0].TargetGroupArn" \
    --output text)

  echo "Creating ALB Listener on port 80 …"
  aws elbv2 create-listener \
    --load-balancer-arn "${ALB_ARN}" \
    --protocol HTTP \
    --port 80 \
    --default-actions "Type=forward,TargetGroupArn=${TARGET_GROUP_ARN}" \
    --region "${AWS_REGION}" >/dev/null

  echo "ALB DNS  : ${ALB_DNS}"
  echo "TG ARN   : ${TARGET_GROUP_ARN}"
fi

# ---------------------------------------------------------------------------
# Prepare task definition JSON (replace placeholders)
# ---------------------------------------------------------------------------
echo ""
echo "Preparing task definition …"
TASK_DEF_FILE="${ECS_DIR}/task-definition.json"
TASK_DEF_TMP="/tmp/modresorts-task-definition.json"

sed \
  -e "s|{{ACCOUNT_ID}}|${ACCOUNT_ID}|g" \
  -e "s|{{AWS_REGION}}|${AWS_REGION}|g" \
  -e "s|{{IMAGE_URI}}|${IMAGE_URI}|g" \
  "${TASK_DEF_FILE}" > "${TASK_DEF_TMP}"

# ---------------------------------------------------------------------------
# Register task definition
# ---------------------------------------------------------------------------
echo "Registering task definition …"
TASK_DEF_ARN=$(aws ecs register-task-definition \
  --cli-input-json "file://${TASK_DEF_TMP}" \
  --region "${AWS_REGION}" \
  --query "taskDefinition.taskDefinitionArn" \
  --output text)
echo "Task Definition ARN: ${TASK_DEF_ARN}"

# ---------------------------------------------------------------------------
# Prepare service definition JSON (replace placeholders)
# ---------------------------------------------------------------------------
SVC_DEF_FILE="${ECS_DIR}/service-definition.json"
SVC_DEF_TMP="/tmp/modresorts-service-definition.json"

sed \
  -e "s|{{CLUSTER_NAME}}|${CLUSTER_NAME}|g" \
  -e "s|{{SUBNET_1}}|${SUBNET_1}|g" \
  -e "s|{{SUBNET_2}}|${SUBNET_2}|g" \
  -e "s|{{SECURITY_GROUP}}|${SECURITY_GROUP}|g" \
  "${SVC_DEF_FILE}" > "${SVC_DEF_TMP}"

# Inject task definition ARN
python3 -c "
import json, sys
with open('${SVC_DEF_TMP}') as f:
    d = json.load(f)
d['taskDefinition'] = '${TASK_DEF_ARN}'
if ${USE_LB} == True:
    d['loadBalancers'] = [{
        'targetGroupArn': '${TARGET_GROUP_ARN}',
        'containerName': '${CONTAINER_NAME}',
        'containerPort': ${APP_PORT}
    }]
    d['healthCheckGracePeriodSeconds'] = 300
else:
    d.pop('loadBalancers', None)
with open('${SVC_DEF_TMP}', 'w') as f:
    json.dump(d, f, indent=2)
" 2>/dev/null || \
  sed -i "s|\"${TASK_FAMILY}\"|\"${TASK_DEF_ARN}\"|g" "${SVC_DEF_TMP}"

# ---------------------------------------------------------------------------
# Create or update ECS service
# ---------------------------------------------------------------------------
echo ""
EXISTING_SERVICE=$(aws ecs describe-services \
  --cluster "${CLUSTER_NAME}" \
  --services "${SERVICE_NAME}" \
  --region "${AWS_REGION}" \
  --query "services[?status=='ACTIVE'].serviceName" \
  --output text 2>/dev/null || echo "")

if [ -z "${EXISTING_SERVICE}" ] || [ "${EXISTING_SERVICE}" = "None" ]; then
  echo "Creating ECS service '${SERVICE_NAME}' …"
  aws ecs create-service \
    --cli-input-json "file://${SVC_DEF_TMP}" \
    --region "${AWS_REGION}"
else
  echo "Updating existing ECS service '${SERVICE_NAME}' …"
  aws ecs update-service \
    --cluster "${CLUSTER_NAME}" \
    --service "${SERVICE_NAME}" \
    --task-definition "${TASK_DEF_ARN}" \
    --region "${AWS_REGION}"
fi

# ---------------------------------------------------------------------------
# Wait for stability
# ---------------------------------------------------------------------------
echo ""
echo "Waiting for service to stabilise (this may take a few minutes) …"
aws ecs wait services-stable \
  --cluster "${CLUSTER_NAME}" \
  --services "${SERVICE_NAME}" \
  --region "${AWS_REGION}"

# ---------------------------------------------------------------------------
# Summary
# ---------------------------------------------------------------------------
echo ""
echo "=============================================="
echo "  Deployment complete!"
echo "=============================================="
aws ecs describe-services \
  --cluster "${CLUSTER_NAME}" \
  --services "${SERVICE_NAME}" \
  --region "${AWS_REGION}" \
  --query "services[0].{Status:status,Running:runningCount,Desired:desiredCount,TaskDef:taskDefinition}" \
  --output table

echo ""
echo "CloudWatch Logs : ${LOG_GROUP}"
if [ -n "${ALB_DNS}" ]; then
  echo "Application URL : http://${ALB_DNS}/resorts/"
fi
echo ""
echo "Troubleshooting tips:"
echo "  - View tasks  : aws ecs list-tasks --cluster ${CLUSTER_NAME} --service-name ${SERVICE_NAME} --region ${AWS_REGION}"
echo "  - Task logs   : aws logs tail ${LOG_GROUP} --follow --region ${AWS_REGION}"
echo "  - Task details: aws ecs describe-tasks --cluster ${CLUSTER_NAME} --tasks <TASK_ARN> --region ${AWS_REGION}"
