# Vault Integration Service

Vault Integration Service for DocFlow - HashiCorp Vault integration microservice for secure secrets management.

## Prerequisites

- **Java 21** (JDK 21+)
- **Maven 3.9+**
- **HashiCorp Vault** (running instance, local or remote)

## Tech Stack

| Component | Version |
|-----------|---------|
| Java | 21 |
| Spring Boot | 3.5.0 |
| Spring Vault Core | 3.1.2 |
| SpringDoc OpenAPI | 2.7.0 |
| Lombok | Managed by Spring Boot |

## Project Structure

```
vault/
├── pom.xml
├── README.md
└── src/
    ├── main/
    │   ├── java/com/docflow/vault/
    │   │   ├── VaultApplication.java
    │   │   ├── application/
    │   │   │   ├── dto/
    │   │   │   ├── ports/
    │   │   │   │   ├── input/
    │   │   │   │   └── output/
    │   │   │   └── services/
    │   │   ├── domain/
    │   │   │   ├── exceptions/
    │   │   │   ├── model/
    │   │   │   └── service/
    │   │   └── infrastructure/
    │   │       ├── adapters/
    │   │       │   ├── input/rest/
    │   │       │   │   ├── GlobalExceptionHandler.java
    │   │       │   │   ├── HealthController.java
    │   │       │   │   ├── SecretController.java
    │   │       │   │   └── SecretNotFoundException.java
    │   │       │   └── output/vault/
    │   │       └── config/
    │   │           └── VaultClientConfig.java
    │   └── resources/
    │       └── application.yml
    └── test/
        └── java/com/docflow/vault/
```

## Build

```bash
# Compile and package
mvn clean package

# Skip tests
mvn clean package -DskipTests
```

## Test

```bash
# Run all tests
mvn test
```

## Run

### Option 1: Using Maven

```bash
mvn spring-boot:run
```

### Option 2: Using JAR

```bash
java -jar target/vault-service-0.0.1-SNAPSHOT.jar
```

### With Custom Vault Configuration

```bash
# Using environment variables
export VAULT_URI=http://localhost:8200
export VAULT_TOKEN=your-vault-token
export VAULT_KV_BACKEND=secret
export VAULT_KV_DEFAULT_CONTEXT=docflow
mvn spring-boot:run

# Or using command line arguments
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.cloud.vault.uri=http://localhost:8200 --spring.cloud.vault.token=your-token"
```

## Configuration

The service uses the following configuration properties (can be set via environment variables or application.yml):

| Property | Environment Variable | Default | Description |
|----------|---------------------|---------|-------------|
| `spring.cloud.vault.uri` | `VAULT_URI` | `http://localhost:8200` | HashiCorp Vault server URI |
| `spring.cloud.vault.token` | `VAULT_TOKEN` | `root` | Vault authentication token |
| `spring.cloud.vault.kv.backend` | `VAULT_KV_BACKEND` | `secret` | KV secrets engine backend path |
| `spring.cloud.vault.kv.default-context` | `VAULT_KV_DEFAULT_CONTEXT` | `docflow` | Default context for KV operations |
| `server.port` | - | `8085` | Server port |

## HashiCorp Vault Setup (Development)

### Start Vault in Development Mode

```bash
# Using Docker
docker run --cap-add=IPC_LOCK -d --name=vault -p 8200:8200 hashicorp/vault:latest server -dev -dev-root-token-id=root

# Or using Vault CLI
vault server -dev -dev-root-token-id=root
```

### Create a Test Secret

```bash
# Set Vault address and token
export VAULT_ADDR='http://127.0.0.1:8200'
export VAULT_TOKEN='root'

# Create a secret (KV v2)
vault kv put secret/my-secret value=my-secret-value

# Verify the secret
vault kv get secret/my-secret
```

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/health` | Health check endpoint |
| GET | `/secret/{path}` | Read a secret from Vault |
| GET | `/swagger-ui.html` | Swagger UI |
| GET | `/api-docs` | OpenAPI specification |

## Usage Examples

### Health Check

```bash
curl -X GET http://localhost:8085/health
```

http://localhost:8085/health

**Response:**
```json
{
  "status": "ok"
}
```

### Read a Secret

```bash
# Read secret at path "my-secret"
curl -X GET http://localhost:8085/secret/my-secret
```

http://localhost:8085/secret/my-secret

**Success Response (200):**
```json
{
  "data": "my-secret-value"
}
```

**Not Found Response (404):**
```json
{
  "error": "Secret not found",
  "path": "my-secret"
}
```

### Access Swagger UI

Open in your browser:

http://localhost:8085/swagger-ui.html


## Troubleshooting

### Connection Refused to Vault

1. Ensure Vault is running and accessible at the configured URI
2. Check that the Vault token is valid
3. Verify network connectivity to the Vault server

### Secret Not Found

1. Verify the secret exists in Vault at the specified path
2. Check that the KV backend is configured correctly (v1 vs v2)
3. Ensure the token has read permissions for the secret path

### Logging

Increase logging verbosity by setting:

```yaml
logging:
  level:
    com.docflow.vault: DEBUG
    org.springframework.vault: DEBUG
```

## License

This project is part of DocFlow - Document Management System.
