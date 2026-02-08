# Quick Azure Deployment

This directory contains scripts and configuration for deploying the Lost and Found Backend to Microsoft Azure.

## 🚀 Quick Start

### Prerequisites
- Azure CLI installed
- Docker installed
- Active Azure subscription

### One-Command Deployment

```bash
./scripts/deploy-to-azure.sh
```

The script will:
1. ✅ Create all required Azure resources
2. ✅ Build and push Docker image
3. ✅ Configure PostgreSQL database
4. ✅ Deploy the application
5. ✅ Set up health checks

**Estimated time:** 10-15 minutes

## 📋 Manual Deployment

For step-by-step instructions, see [AZURE_DEPLOYMENT.md](../AZURE_DEPLOYMENT.md)

## 🏗️ Infrastructure as Code

Use Bicep templates for reproducible deployments:

```bash
cd azure/bicep
az deployment group create \
  --resource-group lost-and-found-rg \
  --template-file main.bicep \
  --parameters parameters.dev.json
```

## 📊 Deployment Options Comparison

| Option | Complexity | Cost | Scalability | Best For |
|--------|-----------|------|-------------|----------|
| App Service | ⭐⭐⭐ | $ | ⭐⭐⭐ | Simple deployments |
| Container Apps | ⭐⭐ | $$ | ⭐⭐⭐⭐⭐ | Microservices |
| Bicep IaC | ⭐⭐⭐⭐ | $ | ⭐⭐⭐ | Production |

## 🔧 Post-Deployment

After deployment, configure:
- Custom domain
- SSL certificate
- Autoscaling rules
- Backup policies
- Monitoring alerts

## 📖 Documentation

- [Complete Azure Deployment Guide](../AZURE_DEPLOYMENT.md)
- [GitHub Actions Setup](.github/workflows/azure-deploy.yml)
- [Azure DevOps Pipeline](../azure-pipelines.yml)

## 💰 Cost Estimate

**Development:** ~$32-35/month
**Production:** ~$200-300/month

See [AZURE_DEPLOYMENT.md](../AZURE_DEPLOYMENT.md#cost-estimation) for details.

## 🆘 Troubleshooting

### Common Issues

**Container won't start:**
```bash
az webapp log tail --name your-app-name --resource-group your-rg
```

**Database connection issues:**
- Check firewall rules
- Verify credentials
- Ensure SSL mode is enabled

**Email not sending:**
- Use Gmail app password (not regular password)
- Enable 2FA on Google account

See full troubleshooting guide in [AZURE_DEPLOYMENT.md](../AZURE_DEPLOYMENT.md#troubleshooting)

## 📞 Support

For deployment issues, check:
1. Azure Portal diagnostics
2. Application logs
3. Health check endpoint: `/actuator/health`
