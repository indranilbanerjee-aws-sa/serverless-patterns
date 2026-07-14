# Java Lambda Durable Functions - Workshop Examples

This project demonstrates **AWS Lambda Durable Functions** patterns in Java. It provides working examples of all major durable execution patterns including chaining, fan-out/fan-in, human interaction, monitoring/polling, timers, error handling with saga compensation, map processing, and sub-orchestration.

## Architecture

```
SNS Topic (Producer) --> Lambda Durable Functions (Consumers) --> DynamoDB (Results)
```

- **SNS Message Sender**: A Java CLI application that sends messages to SNS to trigger the durable functions
- **Durable Functions**: Eight Lambda functions, each demonstrating a different durable execution pattern
- **Infrastructure**: CloudFormation template for a VPC + EC2 instance with all tools pre-installed

## Patterns Demonstrated

| # | Pattern | Handler | Description |
|---|---------|---------|-------------|
| 1 | **Chaining** | `OrderProcessingHandler` | Sequential steps where each output feeds the next |
| 2 | **Fan-Out/Fan-In** | `ParallelProcessingHandler` | Parallel execution with result aggregation |
| 3 | **Human Interaction** | `ApprovalWorkflowHandler` | Callbacks for external human approval |
| 4 | **Monitoring/Polling** | `JobMonitorHandler` | waitForCondition with exponential backoff |
| 5 | **Timer/Delays** | `ScheduledReminderHandler` | Suspending execution without compute charges |
| 6 | **Error Handling** | `ResilientPaymentHandler` | Retries, at-most-once, and saga compensation |
| 7 | **Map Processing** | `BatchDataProcessorHandler` | Concurrent collection processing with tolerance |
| 8 | **Sub-Orchestration** | `OrderFulfillmentHandler` | Child contexts for workflow composition |

## Prerequisites

- AWS Account with permissions for Lambda, SNS, DynamoDB, ECR, IAM, CloudFormation
- Java 21 (Amazon Corretto recommended)
- Maven 3.8+
- Docker
- AWS CLI v2
- AWS SAM CLI

## Quick Start

You can run this project either from an EC2 instance (Option A) or from your local machine (Option B).

### Option A: Using the EC2 Dev Environment

The CloudFormation template deploys a VPC with a public subnet and an EC2 instance pre-configured with Java 21, Maven, Docker, AWS CLI v2, and SAM CLI. The project code is automatically cloned onto the instance.

**1. Deploy the infrastructure stack:**

```bash
aws cloudformation create-stack \
  --stack-name durable-functions-infra \
  --template-body file://infrastructure/ec2-client-instance.yaml \
  --capabilities CAPABILITY_NAMED_IAM
```

Wait for the stack to complete (~5 minutes):
```bash
aws cloudformation wait stack-create-complete --stack-name durable-functions-infra
```

**2. Connect to the instance via EC2 Instance Connect:**

```bash
INSTANCE_ID=$(aws cloudformation describe-stacks --stack-name durable-functions-infra \
  --query 'Stacks[0].Outputs[?OutputKey==`EC2InstanceId`].OutputValue' --output text)
aws ec2-instance-connect ssh --instance-id $INSTANCE_ID
```

**3. Navigate to the project directory:**

```bash
cd /home/ec2-user/serverless-patterns/lambda-durable-java-sam
```

Then continue from [Build and Deploy](#build-and-deploy) below.

### Option B: Local Development

Ensure you have Java 21+, Maven 3.8+, Docker, AWS CLI v2, and SAM CLI installed.

Clone the project:
```bash
git clone https://github.com/aws-samples/serverless-patterns.git
cd serverless-patterns/lambda-durable-java-sam
```

Then continue from [Build and Deploy](#build-and-deploy) below.

---

## Build and Deploy

### 1. Build the Durable Functions Container Image

```bash
cd durable-functions-sam/durable-functions
mvn clean package -DskipTests
docker build -t durable-functions-java-examples .
```

### 2. Push to ECR

```bash
AWS_ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
AWS_REGION=${AWS_REGION:-$(aws configure get region)}

aws ecr get-login-password --region $AWS_REGION \
  | docker login --username AWS --password-stdin \
    $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com

# Create the repository (first time only)
aws ecr create-repository --repository-name durable-functions-java-examples

docker tag durable-functions-java-examples:latest \
  $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/durable-functions-java-examples:latest

docker push \
  $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/durable-functions-java-examples:latest
```

### 3. Deploy with SAM

```bash
cd /home/ec2-user/serverless-patterns/lambda-durable-java-sam/durable-functions-sam 
(adjust the folder path for local deployment)
sam deploy --guided --capabilities CAPABILITY_NAMED_IAM
```

### 4. Build the SNS Message Sender

```bash
cd ../sns-message-sender
mvn clean package
```

### 5. Trigger a Durable Function

```bash
java -cp target/sns-message-sender-1.0-SNAPSHOT.jar \
  sns.producer.DurableFunctionsTrigger DurableFunctionsSNSTopic chaining
```

## Triggering Each Pattern

```bash
# Pattern 1: Chaining - Sequential order processing
java -cp target/sns-message-sender-1.0-SNAPSHOT.jar \
  sns.producer.DurableFunctionsTrigger DurableFunctionsSNSTopic chaining

# Pattern 2: Fan-Out/Fan-In - Parallel data analysis
java -cp target/sns-message-sender-1.0-SNAPSHOT.jar \
  sns.producer.DurableFunctionsTrigger DurableFunctionsSNSTopic fanout

# Pattern 3: Human Interaction - Approval workflow (requires callback)
java -cp target/sns-message-sender-1.0-SNAPSHOT.jar \
  sns.producer.DurableFunctionsTrigger DurableFunctionsSNSTopic approval

# Pattern 4: Monitoring - Job status polling
java -cp target/sns-message-sender-1.0-SNAPSHOT.jar \
  sns.producer.DurableFunctionsTrigger DurableFunctionsSNSTopic monitoring

# Pattern 5: Timer - Scheduled reminders
java -cp target/sns-message-sender-1.0-SNAPSHOT.jar \
  sns.producer.DurableFunctionsTrigger DurableFunctionsSNSTopic timer

# Pattern 6: Error Handling - Resilient payment with saga
java -cp target/sns-message-sender-1.0-SNAPSHOT.jar \
  sns.producer.DurableFunctionsTrigger DurableFunctionsSNSTopic errorhandling

# Pattern 7: Map Processing - Batch concurrent processing
java -cp target/sns-message-sender-1.0-SNAPSHOT.jar \
  sns.producer.DurableFunctionsTrigger DurableFunctionsSNSTopic mapprocessing

# Pattern 8: Sub-Orchestration - Nested child workflows
java -cp target/sns-message-sender-1.0-SNAPSHOT.jar \
  sns.producer.DurableFunctionsTrigger DurableFunctionsSNSTopic suborchestration
```

## Completing the Human Interaction Callback

After triggering the `approval` pattern, check CloudWatch Logs for the callback ID, then:

```bash
# To approve:
aws lambda send-durable-execution-callback-success \
  --callback-id <CALLBACK_ID_FROM_LOGS> \
  --cli-binary-format raw-in-base64-out \
  --result '{"decision":"APPROVED","approver":"manager@example.com"}'

# To reject:
aws lambda send-durable-execution-callback-failure \
  --callback-id <CALLBACK_ID_FROM_LOGS> \
  --error ErrorType=Rejected,ErrorMessage="Budget exceeded policy limit"
```

## Key Concepts

### DurableHandler
All durable functions extend `DurableHandler<InputType, OutputType>` instead of implementing `RequestHandler`. This provides a `DurableContext` with durable operations.

### Steps
Steps checkpoint their results. On replay after a wait or failure, the SDK returns the stored result without re-executing the step body.

### Waits
`context.wait()` suspends execution without consuming compute. The backend resumes the function when the duration elapses.

### Callbacks
`context.createCallback()` suspends execution and waits for an external system to call `SendDurableExecutionCallbackSuccess`.

### Parallel & Map
`context.parallel()` runs different operations concurrently. `context.map()` applies the same operation to each item in a collection.

### Child Contexts
`context.runInChildContext()` creates isolated sub-workflows with their own operation namespaces.

### Retry Strategies
Configure automatic retries with exponential backoff, jitter, and error filtering using `StepConfig` and `RetryStrategies`.

## Monitoring

View durable execution status:
```bash
aws lambda list-durable-executions --function-name durable-chaining-example:1
aws lambda get-durable-execution --function-name durable-chaining-example:1 --execution-id <ID>
```

## Project Structure

```
lambda-durable-java-sam/
├── README.md
├── USER_GUIDE.md
├── DEVELOPER_GUIDE.md
├── build-and-deploy.sh
├── infrastructure/
│   └── ec2-client-instance.yaml      # VPC + EC2 CloudFormation template
├── durable-functions-sam/
│   ├── template.yaml                  # SAM template for all Lambda functions
│   └── durable-functions/
│       ├── pom.xml
│       ├── Dockerfile
│       └── src/main/java/com/amazonaws/samples/durable/
│           ├── models/                # Shared model classes
│           ├── chaining/              # Pattern 1: Sequential steps
│           ├── fanout/                # Pattern 2: Parallel execution
│           ├── humaninteraction/      # Pattern 3: Callbacks
│           ├── monitoring/            # Pattern 4: Polling
│           ├── timer/                 # Pattern 5: Scheduled delays
│           ├── errorhandling/         # Pattern 6: Retries & compensation
│           ├── mapprocessing/         # Pattern 7: Collection processing
│           └── suborchestration/      # Pattern 8: Child contexts
└── sns-message-sender/
    ├── pom.xml
    └── src/main/java/sns/producer/
        └── DurableFunctionsTrigger.java
```
