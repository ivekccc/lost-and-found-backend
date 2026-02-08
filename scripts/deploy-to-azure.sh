#!/bin/bash

# Azure Quick Deploy Script for Lost and Found Backend
# This script automates the deployment process to Azure

set -e  # Exit on error

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored output
print_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if Azure CLI is installed
if ! command -v az &> /dev/null; then
    print_error "Azure CLI is not installed. Please install it from https://docs.microsoft.com/en-us/cli/azure/install-azure-cli"
    exit 1
fi

print_info "Azure CLI found. Version: $(az version --query '\"azure-cli\"' -o tsv)"

# Configuration
read -p "Enter Resource Group Name [lost-and-found-rg]: " RESOURCE_GROUP
RESOURCE_GROUP=${RESOURCE_GROUP:-lost-and-found-rg}

read -p "Enter Location [eastus]: " LOCATION
LOCATION=${LOCATION:-eastus}

read -p "Enter App Name [lost-and-found-backend]: " APP_NAME
APP_NAME=${APP_NAME:-lost-and-found-backend}

read -p "Enter Container Registry Name [lostandfoundacr]: " ACR_NAME
ACR_NAME=${ACR_NAME:-lostandfoundacr}

read -p "Enter PostgreSQL Server Name [lost-and-found-db]: " PG_SERVER
PG_SERVER=${PG_SERVER:-lost-and-found-db}

read -p "Enter PostgreSQL Admin Username [pgadmin]: " PG_ADMIN
PG_ADMIN=${PG_ADMIN:-pgadmin}

read -sp "Enter PostgreSQL Admin Password: " PG_PASSWORD
echo

read -sp "Enter JWT Secret: " JWT_SECRET
echo

read -p "Enter Mail Username: " MAIL_USERNAME

read -sp "Enter Mail Password: " MAIL_PASSWORD
echo

print_info "Starting deployment to Azure..."

# Login check
print_info "Checking Azure login status..."
if ! az account show &> /dev/null; then
    print_warning "Not logged in to Azure. Initiating login..."
    az login
fi

# Create resource group
print_info "Creating resource group: $RESOURCE_GROUP"
az group create --name "$RESOURCE_GROUP" --location "$LOCATION"

# Create Container Registry
print_info "Creating Azure Container Registry: $ACR_NAME"
az acr create \
    --resource-group "$RESOURCE_GROUP" \
    --name "$ACR_NAME" \
    --sku Basic \
    --admin-enabled true

# Build and push Docker image
print_info "Building Docker image..."
docker build -t "${ACR_NAME}.azurecr.io/$APP_NAME:latest" .

print_info "Pushing image to ACR..."
az acr login --name "$ACR_NAME"
docker push "${ACR_NAME}.azurecr.io/$APP_NAME:latest"

# Create PostgreSQL server
print_info "Creating PostgreSQL Flexible Server: $PG_SERVER"
az postgres flexible-server create \
    --resource-group "$RESOURCE_GROUP" \
    --name "$PG_SERVER" \
    --location "$LOCATION" \
    --admin-user "$PG_ADMIN" \
    --admin-password "$PG_PASSWORD" \
    --sku-name Standard_B1ms \
    --tier Burstable \
    --storage-size 32 \
    --version 16 \
    --public-access 0.0.0.0

# Create database
print_info "Creating database: lost-and-found"
az postgres flexible-server db create \
    --resource-group "$RESOURCE_GROUP" \
    --server-name "$PG_SERVER" \
    --database-name lost-and-found

# Allow Azure services
print_info "Configuring firewall rules..."
az postgres flexible-server firewall-rule create \
    --resource-group "$RESOURCE_GROUP" \
    --name "$PG_SERVER" \
    --rule-name AllowAzureServices \
    --start-ip-address 0.0.0.0 \
    --end-ip-address 0.0.0.0

# Create App Service Plan
print_info "Creating App Service Plan..."
az appservice plan create \
    --name "${APP_NAME}-plan" \
    --resource-group "$RESOURCE_GROUP" \
    --is-linux \
    --sku B1

# Get ACR credentials
ACR_USERNAME=$(az acr credential show --name "$ACR_NAME" --query username -o tsv)
ACR_PASSWORD=$(az acr credential show --name "$ACR_NAME" --query "passwords[0].value" -o tsv)

# Create Web App
print_info "Creating Web App: $APP_NAME"
az webapp create \
    --resource-group "$RESOURCE_GROUP" \
    --plan "${APP_NAME}-plan" \
    --name "$APP_NAME" \
    --deployment-container-image-name "${ACR_NAME}.azurecr.io/$APP_NAME:latest"

# Configure container registry
print_info "Configuring container registry credentials..."
az webapp config container set \
    --name "$APP_NAME" \
    --resource-group "$RESOURCE_GROUP" \
    --docker-custom-image-name "${ACR_NAME}.azurecr.io/$APP_NAME:latest" \
    --docker-registry-server-url "https://${ACR_NAME}.azurecr.io" \
    --docker-registry-server-user "$ACR_USERNAME" \
    --docker-registry-server-password "$ACR_PASSWORD"

# Get PostgreSQL host
PG_HOST=$(az postgres flexible-server show \
    --resource-group "$RESOURCE_GROUP" \
    --name "$PG_SERVER" \
    --query fullyQualifiedDomainName -o tsv)

# Configure application settings
print_info "Configuring application settings..."
az webapp config appsettings set \
    --resource-group "$RESOURCE_GROUP" \
    --name "$APP_NAME" \
    --settings \
        SPRING_PROFILES_ACTIVE=azure \
        SPRING_DATASOURCE_URL="jdbc:postgresql://${PG_HOST}:5432/lost-and-found?sslmode=require" \
        DB_USERNAME="$PG_ADMIN" \
        DB_PASSWORD="$PG_PASSWORD" \
        JWT_SECRET="$JWT_SECRET" \
        MAIL_USERNAME="$MAIL_USERNAME" \
        MAIL_PASSWORD="$MAIL_PASSWORD" \
        WEBSITES_PORT=8082

# Enable health check
print_info "Enabling health check..."
az webapp config set \
    --resource-group "$RESOURCE_GROUP" \
    --name "$APP_NAME" \
    --health-check-path "/actuator/health"

# Get web app URL
WEB_APP_URL=$(az webapp show \
    --name "$APP_NAME" \
    --resource-group "$RESOURCE_GROUP" \
    --query defaultHostName -o tsv)

print_info "Deployment completed successfully!"
print_info "Application URL: https://$WEB_APP_URL"
print_info "Health Check: https://$WEB_APP_URL/actuator/health"
print_info "API Documentation: https://$WEB_APP_URL/swagger-ui.html"

print_warning "Note: The application may take 60-90 seconds to start. Please wait before accessing."

# Test health endpoint after a delay
print_info "Waiting 90 seconds for application to start..."
sleep 90

print_info "Testing health endpoint..."
if curl -f -s "https://$WEB_APP_URL/actuator/health" > /dev/null; then
    print_info "Health check passed! Application is running."
else
    print_warning "Health check failed. Application may still be starting. Check logs with:"
    echo "    az webapp log tail --name $APP_NAME --resource-group $RESOURCE_GROUP"
fi

print_info "To view logs, run:"
echo "    az webapp log tail --name $APP_NAME --resource-group $RESOURCE_GROUP"

print_info "To delete all resources, run:"
echo "    az group delete --name $RESOURCE_GROUP --yes --no-wait"
