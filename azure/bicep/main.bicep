// Parameters
@description('The name of the application')
param appName string = 'lost-and-found'

@description('Location for all resources')
param location string = resourceGroup().location

@description('Environment name (dev, staging, prod)')
@allowed([
  'dev'
  'staging'
  'prod'
])
param environment string = 'dev'

@description('PostgreSQL administrator login')
@secure()
param postgresAdminLogin string

@description('PostgreSQL administrator password')
@secure()
param postgresAdminPassword string

@description('JWT Secret')
@secure()
param jwtSecret string

@description('Mail username')
param mailUsername string = ''

@description('Mail password')
@secure()
param mailPassword string = ''

@description('SKU for App Service Plan')
param appServicePlanSku string = 'B1'

// Variables
var resourceNamePrefix = '${appName}-${environment}'
var webAppName = '${resourceNamePrefix}-webapp'
var appServicePlanName = '${resourceNamePrefix}-asp'
var postgresServerName = '${resourceNamePrefix}-postgres'
var containerRegistryName = replace('${resourceNamePrefix}acr', '-', '')
var logAnalyticsName = '${resourceNamePrefix}-logs'
var appInsightsName = '${resourceNamePrefix}-insights'

// Log Analytics Workspace
resource logAnalytics 'Microsoft.OperationalInsights/workspaces@2022-10-01' = {
  name: logAnalyticsName
  location: location
  properties: {
    sku: {
      name: 'PerGB2018'
    }
    retentionInDays: 30
  }
}

// Application Insights
resource appInsights 'Microsoft.Insights/components@2020-02-02' = {
  name: appInsightsName
  location: location
  kind: 'web'
  properties: {
    Application_Type: 'web'
    WorkspaceResourceId: logAnalytics.id
  }
}

// Container Registry
resource containerRegistry 'Microsoft.ContainerRegistry/registries@2023-01-01-preview' = {
  name: containerRegistryName
  location: location
  sku: {
    name: 'Basic'
  }
  properties: {
    adminUserEnabled: true
  }
}

// PostgreSQL Flexible Server
resource postgresServer 'Microsoft.DBforPostgreSQL/flexibleServers@2023-03-01-preview' = {
  name: postgresServerName
  location: location
  sku: {
    name: 'Standard_B1ms'
    tier: 'Burstable'
  }
  properties: {
    administratorLogin: postgresAdminLogin
    administratorLoginPassword: postgresAdminPassword
    version: '16'
    storage: {
      storageSizeGB: 32
    }
    backup: {
      backupRetentionDays: 7
      geoRedundantBackup: 'Disabled'
    }
    highAvailability: {
      mode: 'Disabled'
    }
  }
}

// PostgreSQL Database
resource postgresDatabase 'Microsoft.DBforPostgreSQL/flexibleServers/databases@2023-03-01-preview' = {
  parent: postgresServer
  name: 'lost-and-found'
  properties: {
    charset: 'UTF8'
    collation: 'en_US.utf8'
  }
}

// PostgreSQL Firewall Rule - Allow Azure Services
resource postgresFirewallRule 'Microsoft.DBforPostgreSQL/flexibleServers/firewallRules@2023-03-01-preview' = {
  parent: postgresServer
  name: 'AllowAzureServices'
  properties: {
    startIpAddress: '0.0.0.0'
    endIpAddress: '0.0.0.0'
  }
}

// App Service Plan
resource appServicePlan 'Microsoft.Web/serverfarms@2022-09-01' = {
  name: appServicePlanName
  location: location
  sku: {
    name: appServicePlanSku
  }
  kind: 'linux'
  properties: {
    reserved: true
  }
}

// Web App
resource webApp 'Microsoft.Web/sites@2022-09-01' = {
  name: webAppName
  location: location
  kind: 'app,linux,container'
  properties: {
    serverFarmId: appServicePlan.id
    siteConfig: {
      linuxFxVersion: 'DOCKER|${containerRegistry.properties.loginServer}/lost-and-found-backend:latest'
      appSettings: [
        {
          name: 'SPRING_PROFILES_ACTIVE'
          value: 'azure'
        }
        {
          name: 'SPRING_DATASOURCE_URL'
          value: 'jdbc:postgresql://${postgresServer.properties.fullyQualifiedDomainName}:5432/lost-and-found?sslmode=require'
        }
        {
          name: 'DB_USERNAME'
          value: postgresAdminLogin
        }
        {
          name: 'DB_PASSWORD'
          value: postgresAdminPassword
        }
        {
          name: 'JWT_SECRET'
          value: jwtSecret
        }
        {
          name: 'MAIL_USERNAME'
          value: mailUsername
        }
        {
          name: 'MAIL_PASSWORD'
          value: mailPassword
        }
        {
          name: 'APPLICATIONINSIGHTS_CONNECTION_STRING'
          value: appInsights.properties.ConnectionString
        }
        {
          name: 'DOCKER_REGISTRY_SERVER_URL'
          value: 'https://${containerRegistry.properties.loginServer}'
        }
        {
          name: 'DOCKER_REGISTRY_SERVER_USERNAME'
          value: containerRegistry.listCredentials().username
        }
        {
          name: 'DOCKER_REGISTRY_SERVER_PASSWORD'
          value: containerRegistry.listCredentials().passwords[0].value
        }
        {
          name: 'WEBSITES_PORT'
          value: '8082'
        }
      ]
      healthCheckPath: '/actuator/health'
    }
    httpsOnly: true
  }
}

// Outputs
output webAppUrl string = 'https://${webApp.properties.defaultHostName}'
output containerRegistryLoginServer string = containerRegistry.properties.loginServer
output postgresServerFqdn string = postgresServer.properties.fullyQualifiedDomainName
