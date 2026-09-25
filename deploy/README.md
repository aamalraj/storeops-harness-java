# ECS Fargate deployment (Option A — direct public IP)

`ecs-task-definition.json` matches [Dockerfile](../Dockerfile): container port `8080`, and a
health check hitting the same `/actuator/health` endpoint the Docker image's own `HEALTHCHECK`
uses.

## Placeholders to replace before registering

| Placeholder | Replace with |
|---|---|
| `<account-id>` | your AWS account id (appears in `executionRoleArn` and `image`) |
| `<region>` | the AWS region you're deploying to, e.g. `us-east-1` |

The `executionRoleArn` assumes an `ecsTaskExecutionRole` already exists with the
`AmazonECSTaskExecutionRolePolicy` managed policy attached (needed to pull from ECR and write to
CloudWatch Logs). Create it once if it doesn't exist:

```bash
aws iam create-role --role-name ecsTaskExecutionRole \
  --assume-role-policy-document '{
    "Version": "2012-10-17",
    "Statement": [{"Effect": "Allow", "Principal": {"Service": "ecs-tasks.amazonaws.com"},
                    "Action": "sts:AssumeRole"}]
  }'
aws iam attach-role-policy --role-name ecsTaskExecutionRole \
  --policy-arn arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy
```

## 1. Push the image (see main README/response for the full ECR push sequence)

```bash
docker build -t storeops-api .
docker tag storeops-api:latest <account-id>.dkr.ecr.<region>.amazonaws.com/storeops-api:latest
docker push <account-id>.dkr.ecr.<region>.amazonaws.com/storeops-api:latest
```

## 2. Register the task definition

```bash
aws ecs register-task-definition --cli-input-json file://deploy/ecs-task-definition.json --region <region>
```

## 3. Create the cluster (if you don't already have one) and the service with a public IP

```bash
aws ecs create-cluster --cluster-name storeops-cluster --region <region>

aws ecs create-service \
  --cluster storeops-cluster \
  --service-name storeops-api \
  --task-definition storeops-api \
  --desired-count 1 \
  --launch-type FARGATE \
  --network-configuration '{
    "awsvpcConfiguration": {
      "subnets": ["<public-subnet-id>"],
      "securityGroups": ["<security-group-id>"],
      "assignPublicIp": "ENABLED"
    }
  }' \
  --region <region>
```

`<security-group-id>` must allow inbound `TCP 8080` from `0.0.0.0/0` (or a narrower range for a
private demo). `<public-subnet-id>` must be a subnet with a route to an Internet Gateway.

## 4. Find the task's public IP and verify

```bash
TASK_ARN=$(aws ecs list-tasks --cluster storeops-cluster --service-name storeops-api \
  --region <region> --query 'taskArns[0]' --output text)

ENI_ID=$(aws ecs describe-tasks --cluster storeops-cluster --tasks "$TASK_ARN" --region <region> \
  --query 'tasks[0].attachments[0].details[?name==`networkInterfaceId`].value' --output text)

PUBLIC_IP=$(aws ec2 describe-network-interfaces --network-interface-ids "$ENI_ID" --region <region> \
  --query 'NetworkInterfaces[0].Association.PublicIp' --output text)

echo "$PUBLIC_IP"
curl "http://$PUBLIC_IP:8080/api/activities" -H "X-User-Id: staff-3" -H "X-Store-Id: store-1"
```

This IP changes on every task restart/redeploy — fine for the capstone's DEPLOYMENT.md
screenshot, not something to hardcode anywhere durable. See the main conversation for Option B
(Application Load Balancer) if a stable URL is needed later.
