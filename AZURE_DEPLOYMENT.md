# Azure Deployment Guide for Lost and Found Backend

This guide provides comprehensive instructions for deploying the Lost and Found Backend application to Microsoft Azure.

## Table of Contents
- [Prerequisites](#prerequisites)
- [Deployment Options](#deployment-options)
- [Option 1: Azure App Service with Container](#option-1-azure-app-service-with-container)
- [Option 2: Azure Container Apps](#option-2-azure-container-apps)
- [Option 3: Using Bicep Infrastructure as Code](#option-3-using-bicep-infrastructure-as-code)
- [Environment Variables](#environment-variables)
- [CI/CD Setup](#cicd-setup)
- [Monitoring and Logging](#monitoring-and-logging)
- [Cost Estimation](#cost-estimation)
- [Troubleshooting](#troubleshooting)

## Prerequisites

### Required Tools
- [Azure CLI](https://docs.microsoft.com/en-us/cli/azure/install-azure-cli) (version 2.40+)
- [Docker](https://www.docker.com/get-started) (for building containers)
- [Java 21 JDK](https://adoptium.net/) (for local testing)
- [Maven](https://maven.apache.org/download.cgi) (version 3.8+)

### Azure Resources
- Active Azure subscription
- Resource Group
- Azure Container Registry (ACR) or Docker Hub account
- Azure Database for PostgreSQL Flexible Server

### Required Secrets
- `JWT_SECRET`: Secret key for JWT token generation
- `DB_USERNAME`: PostgreSQL database username
- `DB_PASSWORD`: PostgreSQL database password
- `MAIL_USERNAME`: Gmail account for sending emails
- `MAIL_PASSWORD`: Gmail app password (not regular password)

## Deployment Options

### Difficulty Assessment
**Overall Difficulty: Medium** ⭐⭐⭐☆☆

The deployment is moderately complex due to:
- ✅ Application is containerized (Docker support added)
- ✅ Spring Boot has excellent Azure integration
- ✅ Clear infrastructure templates provided
- ⚠️ PostgreSQL database needs to be configured
- ⚠️ Multiple environment variables required
- ⚠️ Email service configuration needed

**Estimated Time:**
- First-time setup: 1-2 hours
- Subsequent deployments: 5-10 minutes (with CI/CD)

## Option 1: Azure App Service with Container

### Step 1: Login to Azure
```bash
az login
az account set --subscription "<your-subscription-id>"
```

### Step 2: Create Resource Group
```bash
az group create \
  --name lost-and-found-rg \
  --location eastus
```

### Step 3: Create Azure Container Registry
```bash
az acr create \
  --resource-group lost-and-found-rg \
  --name lostandfoundacr \
  --sku Basic \
  --admin-enabled true
```

### Step 4: Build and Push Docker Image
```bash
# Login to ACR
az acr login --name lostandfoundacr

# Build the Docker image
docker build -t lostandfoundacr.azurecr.io/lost-and-found-backend:latest .

# Push to ACR
docker push lostandfoundacr.azurecr.io/lost-and-found-backend:latest
```

### Step 5: Create Azure Database for PostgreSQL
```bash
az postgres flexible-server create \
  --resource-group lost-and-found-rg \
  --name lost-and-found-db \
  --location eastus \
  --admin-user pgadmin \
  --admin-password "<secure-password>" \
  --sku-name Standard_B1ms \
  --tier Burstable \
  --storage-size 32 \
  --version 16

# Create the database
az postgres flexible-server db create \
  --resource-group lost-and-found-rg \
  --server-name lost-and-found-db \
  --database-name lost-and-found

# Configure firewall to allow Azure services
az postgres flexible-server firewall-rule create \
  --resource-group lost-and-found-rg \
  --name lost-and-found-db \
  --rule-name AllowAzureServices \
  --start-ip-address 0.0.0.0 \
  --end-ip-address 0.0.0.0
```

### Step 6: Create App Service Plan
```bash
az appservice plan create \
  --name lost-and-found-asp \
  --resource-group lost-and-found-rg \
  --is-linux \
  --sku B1
```

### Step 7: Create Web App
```bash
# Get ACR credentials
ACR_USERNAME=$(az acr credential show --name lostandfoundacr --query username -o tsv)
ACR_PASSWORD=$(az acr credential show --name lostandfoundacr --query "passwords[0].value" -o tsv)

# Create web app
az webapp create \
  --resource-group lost-and-found-rg \
  --plan lost-and-found-asp \
  --name lost-and-found-backend \
  --deployment-container-image-name lostandfoundacr.azurecr.io/lost-and-found-backend:latest

# Configure container registry credentials
az webapp config container set \
  --name lost-and-found-backend \
  --resource-group lost-and-found-rg \
  --docker-custom-image-name lostandfoundacr.azurecr.io/lost-and-found-backend:latest \
  --docker-registry-server-url https://lostandfoundacr.azurecr.io \
  --docker-registry-server-user $ACR_USERNAME \
  --docker-registry-server-password $ACR_PASSWORD
```

### Step 8: Configure Environment Variables
```bash
# Get PostgreSQL connection string
PG_HOST=$(az postgres flexible-server show \
  --resource-group lost-and-found-rg \
  --name lost-and-found-db \
  --query fullyQualifiedDomainName -o tsv)

# Set application settings
az webapp config appsettings set \
  --resource-group lost-and-found-rg \
  --name lost-and-found-backend \
  --settings \
    SPRING_PROFILES_ACTIVE=azure \
    SPRING_DATASOURCE_URL="jdbc:postgresql://${PG_HOST}:5432/lost-and-found?sslmode=require" \
    DB_USERNAME="pgadmin" \
    DB_PASSWORD="<your-secure-password>" \
    JWT_SECRET="<your-jwt-secret>" \
    MAIL_USERNAME="<your-email@gmail.com>" \
    MAIL_PASSWORD="<your-gmail-app-password>" \
    WEBSITES_PORT=8082
```

### Step 9: Enable Health Check
```bash
az webapp config set \
  --resource-group lost-and-found-rg \
  --name lost-and-found-backend \
  --health-check-path "/actuator/health"
```

### Step 10: Verify Deployment
```bash
# Get the web app URL
az webapp show \
  --name lost-and-found-backend \
  --resource-group lost-and-found-rg \
  --query defaultHostName -o tsv

# Test the health endpoint
curl https://lost-and-found-backend.azurewebsites.net/actuator/health
```

## Option 2: Azure Container Apps

Azure Container Apps is a more modern, serverless option that scales better:

```bash
# Install the containerapp extension
az extension add --name containerapp --upgrade

# Create Container Apps environment
az containerapp env create \
  --name lost-and-found-env \
  --resource-group lost-and-found-rg \
  --location eastus

# Create the container app
az containerapp create \
  --name lost-and-found-backend \
  --resource-group lost-and-found-rg \
  --environment lost-and-found-env \
  --image lostandfoundacr.azurecr.io/lost-and-found-backend:latest \
  --target-port 8082 \
  --ingress external \
  --registry-server lostandfoundacr.azurecr.io \
  --registry-username $ACR_USERNAME \
  --registry-password $ACR_PASSWORD \
  --env-vars \
    SPRING_PROFILES_ACTIVE=azure \
    SPRING_DATASOURCE_URL="jdbc:postgresql://${PG_HOST}:5432/lost-and-found?sslmode=require" \
    DB_USERNAME="pgadmin" \
    DB_PASSWORD=secretref:db-password \
    JWT_SECRET=secretref:jwt-secret \
    MAIL_USERNAME="your-email@gmail.com" \
    MAIL_PASSWORD=secretref:mail-password \
  --secrets \
    db-password="<your-db-password>" \
    jwt-secret="<your-jwt-secret>" \
    mail-password="<your-mail-password>" \
  --min-replicas 1 \
  --max-replicas 3
```

## Option 3: Using Bicep Infrastructure as Code

The provided Bicep template (`azure/bicep/main.bicep`) deploys all resources at once:

### Step 1: Update Parameters
Edit `azure/bicep/parameters.dev.json` with your values, or create a secure parameter file:

```json
{
  "$schema": "https://schema.management.azure.com/schemas/2019-04-01/deploymentParameters.json#",
  "contentVersion": "1.0.0.0",
  "parameters": {
    "appName": { "value": "lost-and-found" },
    "environment": { "value": "dev" },
    "postgresAdminLogin": { "value": "pgadmin" },
    "postgresAdminPassword": { "value": "YourSecurePassword123!" },
    "jwtSecret": { "value": "your-jwt-secret-min-256-bits" },
    "mailUsername": { "value": "your-email@gmail.com" },
    "mailPassword": { "value": "your-gmail-app-password" },
    "appServicePlanSku": { "value": "B1" }
  }
}
```

### Step 2: Deploy Infrastructure
```bash
# Create resource group
az group create --name lost-and-found-rg --location eastus

# Deploy Bicep template
az deployment group create \
  --resource-group lost-and-found-rg \
  --template-file azure/bicep/main.bicep \
  --parameters azure/bicep/parameters.dev.json

# Get outputs
az deployment group show \
  --resource-group lost-and-found-rg \
  --name main \
  --query properties.outputs
```

### Step 3: Build and Deploy Application
```bash
# Get ACR login server from deployment outputs
ACR_SERVER=$(az deployment group show \
  --resource-group lost-and-found-rg \
  --name main \
  --query properties.outputs.containerRegistryLoginServer.value -o tsv)

# Login to ACR
az acr login --name ${ACR_SERVER%.azurecr.io}

# Build and push
docker build -t $ACR_SERVER/lost-and-found-backend:latest .
docker push $ACR_SERVER/lost-and-found-backend:latest

# Restart web app to pull new image
az webapp restart \
  --name lost-and-found-dev-webapp \
  --resource-group lost-and-found-rg
```

## Environment Variables

Required environment variables for Azure deployment:

| Variable | Description | Example |
|----------|-------------|---------|
| `SPRING_PROFILES_ACTIVE` | Active Spring profile | `azure` |
| `SPRING_DATASOURCE_URL` | PostgreSQL JDBC URL | `jdbc:postgresql://server.postgres.database.azure.com:5432/lost-and-found?sslmode=require` |
| `DB_USERNAME` | Database username | `pgadmin` |
| `DB_PASSWORD` | Database password | (secure password) |
| `JWT_SECRET` | JWT signing secret | (min 256 bits) |
| `MAIL_USERNAME` | Gmail address | `your-email@gmail.com` |
| `MAIL_PASSWORD` | Gmail app password | (app-specific password) |
| `WEBSITES_PORT` | App listening port | `8082` |

## CI/CD Setup

### GitHub Actions

The repository includes a GitHub Actions workflow (`.github/workflows/azure-deploy.yml`).

**Required GitHub Secrets:**
- `AZURE_CONTAINER_REGISTRY_URL`: Your ACR URL (e.g., `lostandfoundacr.azurecr.io`)
- `AZURE_CONTAINER_REGISTRY_USERNAME`: ACR username
- `AZURE_CONTAINER_REGISTRY_PASSWORD`: ACR password
- `AZURE_WEBAPP_PUBLISH_PROFILE`: Download from Azure Portal

To set up:
1. Go to your GitHub repository → Settings → Secrets and variables → Actions
2. Add the required secrets
3. Push to `main` branch to trigger deployment

### Azure DevOps

The repository includes an Azure Pipeline (`azure-pipelines.yml`).

**Setup steps:**
1. Create a new pipeline in Azure DevOps
2. Connect to your GitHub repository
3. Select the `azure-pipelines.yml` file
4. Update variables in the pipeline
5. Create service connections for Azure and ACR

## Monitoring and Logging

### Application Insights
Application Insights is configured in the Bicep template for monitoring:

```bash
# View live metrics
az monitor app-insights component show \
  --app lost-and-found-dev-insights \
  --resource-group lost-and-found-rg
```

### View Logs
```bash
# Stream web app logs
az webapp log tail \
  --name lost-and-found-backend \
  --resource-group lost-and-found-rg

# Download logs
az webapp log download \
  --name lost-and-found-backend \
  --resource-group lost-and-found-rg \
  --log-file logs.zip
```

### Health Checks
The application exposes health endpoints via Spring Actuator:
- Health: `https://your-app.azurewebsites.net/actuator/health`
- Info: `https://your-app.azurewebsites.net/actuator/info`

## Cost Estimation

Estimated monthly costs for a development environment:

| Resource | SKU/Tier | Estimated Cost |
|----------|----------|----------------|
| App Service Plan | B1 (Basic) | ~$13/month |
| PostgreSQL Flexible Server | Standard_B1ms | ~$12/month |
| Container Registry | Basic | ~$5/month |
| Application Insights | Basic | ~$2-5/month |
| **Total** | | **~$32-35/month** |

For production, consider:
- Upgrade to S1 App Service Plan (~$70/month)
- Use Standard_D2s_v3 for PostgreSQL (~$110/month)
- Enable backups and geo-redundancy

## Troubleshooting

### Container fails to start
```bash
# Check container logs
az webapp log tail --name lost-and-found-backend --resource-group lost-and-found-rg

# Common issues:
# 1. Wrong port - ensure WEBSITES_PORT=8082
# 2. Missing environment variables
# 3. Database connection issues
```

### Database connection errors
```bash
# Test database connectivity
az postgres flexible-server connect \
  --name lost-and-found-db \
  --resource-group lost-and-found-rg \
  --admin-user pgadmin \
  --database-name lost-and-found

# Check firewall rules
az postgres flexible-server firewall-rule list \
  --resource-group lost-and-found-rg \
  --name lost-and-found-db
```

### Email sending issues
- Ensure you're using a Gmail **app password**, not your regular password
- Enable "Less secure app access" or use OAuth2 (recommended)
- Check that 2-factor authentication is enabled on your Google account

### Slow startup
- Spring Boot apps can take 60-90 seconds to start
- Increase health check grace period in Azure Portal
- Consider using `--health-check-start-period` parameter

## Best Practices

1. **Use Azure Key Vault** for storing secrets instead of environment variables
2. **Enable HTTPS only** (already configured)
3. **Set up autoscaling** for production workloads
4. **Use managed identities** for ACR authentication
5. **Enable diagnostic logging** to Log Analytics
6. **Set up alerts** for failures and performance issues
7. **Use deployment slots** for zero-downtime deployments
8. **Implement database backups** and disaster recovery

## Next Steps

After deployment:
1. Configure custom domain and SSL certificate
2. Set up Azure Front Door for CDN and WAF
3. Implement backup and disaster recovery strategy
4. Configure autoscaling rules
5. Set up monitoring alerts
6. Document runbooks for common operations

## Support

For issues:
- Check Azure Portal diagnostics
- Review Application Insights logs
- Contact Azure support if infrastructure issues persist

---

**Summary**: The deployment to Azure is **moderately straightforward** with the provided configurations. The main complexity comes from managing multiple services (database, container registry, app service) and configuring environment variables correctly. With the provided Bicep templates and this guide, a first-time deployment should take 1-2 hours, with subsequent deployments automated via CI/CD in under 10 minutes.
