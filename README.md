# Lost and Found Backend

A Spring Boot backend application for a lost and found system.

## 🚀 Quick Start

### Local Development

```bash
# Clone the repository
git clone <repository-url>
cd lost-and-found-backend

# Run with Maven
./mvnw spring-boot:run

# Or with Docker Compose (includes PostgreSQL)
docker-compose up
```

The application will be available at http://localhost:8082

### API Documentation

Access Swagger UI at: http://localhost:8082/swagger-ui.html

## 🏗️ Technology Stack

- **Java 21** - Programming language
- **Spring Boot 4.0.1** - Application framework
- **PostgreSQL 16** - Database
- **Spring Security** - Authentication & Authorization
- **JWT** - Token-based authentication
- **Flyway** - Database migrations
- **Spring Mail** - Email notifications
- **Swagger/OpenAPI** - API documentation
- **Lombok** - Reduce boilerplate code

## ☁️ Azure Deployment

**Difficulty: Medium** ⭐⭐⭐☆☆ | **Time: 1-2 hours (first deployment)**

### Quick Deploy to Azure

```bash
./scripts/deploy-to-azure.sh
```

### Deployment Options

1. **Azure App Service** - Recommended for most use cases
2. **Azure Container Apps** - For microservices architectures
3. **Bicep Templates** - Infrastructure as Code approach

**Estimated Monthly Cost:** $32-35 (development) | $200-300 (production)

📖 **Complete guide:** [AZURE_DEPLOYMENT.md](AZURE_DEPLOYMENT.md)

### What's Included

✅ Dockerfile for containerization  
✅ Azure Bicep templates for infrastructure  
✅ GitHub Actions workflow for CI/CD  
✅ Azure DevOps pipeline configuration  
✅ Azure-specific application properties  
✅ Health checks with Spring Actuator  
✅ Comprehensive deployment documentation

## 📋 Prerequisites

- Java 21 JDK
- Maven 3.8+
- PostgreSQL 16
- Docker (optional)

## 🔧 Configuration

### Environment Variables

Create a `.env` file or set environment variables:

```bash
# Database
DB_USERNAME=postgres
DB_PASSWORD=admin

# JWT
JWT_SECRET=your-secret-key

# Email
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password
```

### Application Properties

- `application.properties` - Local development
- `application-azure.properties` - Azure deployment

## 🧪 Testing

```bash
# Run tests
./mvnw test

# Run with coverage
./mvnw test jacoco:report
```

## 📦 Building

```bash
# Build JAR
./mvnw clean package

# Build Docker image
docker build -t lost-and-found-backend .
```

## 🐳 Docker

### Local Development with Docker Compose

```bash
docker-compose up -d
```

This starts:
- PostgreSQL database on port 5432
- Backend application on port 8082

### Docker Commands

```bash
# Build image
docker build -t lost-and-found-backend .

# Run container
docker run -p 8082:8082 \
  -e DB_USERNAME=postgres \
  -e DB_PASSWORD=admin \
  -e JWT_SECRET=your-secret \
  -e MAIL_USERNAME=email@gmail.com \
  -e MAIL_PASSWORD=app-password \
  lost-and-found-backend
```

## 📚 Project Structure

```
lost-and-found-backend/
├── src/
│   ├── main/
│   │   ├── java/com/example/demo/
│   │   │   ├── model/          # Domain models
│   │   │   ├── repository/     # Data access layer
│   │   │   ├── service/        # Business logic
│   │   │   └── controller/     # REST endpoints
│   │   └── resources/
│   │       ├── application.properties
│   │       └── application-azure.properties
│   └── test/                   # Tests
├── azure/
│   ├── bicep/                  # Azure infrastructure templates
│   └── README.md               # Azure quick start
├── scripts/
│   └── deploy-to-azure.sh      # Deployment automation
├── Dockerfile                   # Container definition
├── docker-compose.yml          # Local development
├── pom.xml                     # Maven configuration
└── AZURE_DEPLOYMENT.md         # Complete Azure guide
```

## 🔐 Security

- JWT-based authentication
- Password encryption with BCrypt
- HTTPS enforced in production
- SQL injection prevention with JPA
- CORS configuration
- Security headers

## 📈 Monitoring

### Health Checks

- `/actuator/health` - Application health status
- `/actuator/info` - Application information

### Azure Monitoring

When deployed to Azure:
- Application Insights for performance monitoring
- Log Analytics for centralized logging
- Health check probes for availability

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License.

## 🆘 Support

- **Deployment Issues:** See [AZURE_DEPLOYMENT.md](AZURE_DEPLOYMENT.md#troubleshooting)
- **Application Issues:** Check logs at `/actuator/health`
- **Azure Issues:** Review Azure Portal diagnostics

## 📞 Contact

For questions or support, please open an issue in the repository.

---

Made with ❤️ using Spring Boot and Azure
