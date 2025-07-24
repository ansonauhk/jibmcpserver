# Jib MCP Server

A Model Context Protocol (MCP) server that provides transparent Java application containerization using Google Jib. This server enables seamless container operations without requiring users to explicitly mention "Jib" - simply request container operations like "build a container" and the server handles the rest.

## Features

### 🚀 Transparent Container Operations
- **No Jib Knowledge Required**: Users can request container operations naturally without mentioning "Jib"
- **Automatic Detection**: Automatically detects Maven/Gradle projects and configures Jib accordingly
- **Smart Defaults**: Provides sensible defaults for base images, target images, and container settings

### 🛠️ Comprehensive Tool Suite

#### Project Analysis
- **analyze_project**: Analyze Java project structure and detect build system
- **detect_main_class**: Automatically find main application classes

#### Container Configuration
- **init_jib_config**: Initialize Jib plugin configuration in Maven/Gradle
- **update_jib_config**: Update specific Jib configuration parameters
- **set_main_class**: Configure the main class for containerization
- **configure_container_settings**: Set ports, environment variables, JVM flags

#### Container Building
- **build_container**: Build container images (Docker/registry targets)
- **validate_jib_config**: Validate Jib configuration before building

#### Registry Support
- **configure_registry**: Configure container registries (GCR, ECR, Docker Hub, etc.)

#### Kubernetes Deployment
- **generate_helm_chart**: Generate production-ready Helm charts
- **update_helm_values**: Update Helm chart values for different environments
- **deploy_to_k3s**: Deploy to local k3s for testing

#### Configuration Management
- **export_jib_config**: Export configuration for backup/sharing
- **import_jib_config**: Import configuration from JSON

## Quick Start

### Prerequisites
- Java 21+
- Maven 3.6+ or Gradle 7+
- Spring AI MCP framework
- (Optional) Docker for local container testing
- (Optional) k3s for local Kubernetes testing

### Installation

1. **Clone and Build**
   ```bash
   git clone <repository-url>
   cd jlibmcpserver
   ./gradlew build
   ```

2. **Run the Server**
   ```bash
   ./gradlew bootRun
   ```

3. **Connect via MCP Client**
   The server runs on standard I/O by default, compatible with any MCP client.

### Basic Usage

#### Simple Container Build
```
User: "Build a container for my Spring Boot application"
Server: → Analyzes project
        → Initializes Jib configuration
        → Detects main class
        → Builds container image
        → Returns build results
```

#### Production Deployment
```
User: "Create a Helm chart and deploy to k3s"
Server: → Generates Helm chart with best practices
        → Configures production-ready values
        → Deploys to local k3s
        → Returns deployment status
```

## Configuration

### Application Properties
```yaml
spring:
  ai:
    mcp:
      server:
        enabled: true
        name: "Jib MCP Server"
        version: "1.0.0"
        type: SYNC
        capabilities:
          tool: true
```

### Environment Variables
- `JIBMCP_DEFAULT_BASE_IMAGE`: Default base image (default: `eclipse-temurin:21-jre`)
- `JIBMCP_DEFAULT_REGISTRY`: Default container registry
- `JIBMCP_K3S_ENABLED`: Enable k3s deployment features (default: `true`)

## Supported Build Systems

### Maven Projects
- Automatically adds Jib Maven plugin to `pom.xml`
- Configures plugin with sensible defaults
- Supports multi-module projects

### Gradle Projects  
- Automatically adds Jib Gradle plugin to `build.gradle`
- Configures plugin with Kotlin DSL support
- Handles composite builds

## Container Registry Support

### Supported Registries
- **Google Container Registry (GCR)**: `gcr.io/project-id`
- **Amazon Elastic Container Registry (ECR)**: `account.dkr.ecr.region.amazonaws.com`
- **Docker Hub**: `docker.io/username`
- **Azure Container Registry**: `registry.azurecr.io`
- **Private Registries**: Custom registry URLs

### Authentication Methods
- **Service Account**: JSON key file authentication
- **IAM Roles**: AWS IAM role-based authentication
- **Username/Password**: Basic authentication
- **Token**: Bearer token authentication

## Helm Chart Generation

### Generated Files
```
charts/
├── Chart.yaml              # Chart metadata
├── values.yaml             # Configuration values
└── templates/
    ├── deployment.yaml     # Kubernetes deployment
    ├── service.yaml        # Kubernetes service
    ├── ingress.yaml        # Ingress configuration
    ├── serviceaccount.yaml # Service account
    └── NOTES.txt          # Post-install notes
```

### Features
- **Production Ready**: Includes health checks, resource limits, security contexts
- **Configurable**: Extensive values.yaml for customization
- **Best Practices**: Follows Helm and Kubernetes best practices
- **Multi-Environment**: Support for dev/staging/production configurations

## Local Development with k3s

### k3s Integration
- **Automatic Detection**: Detects existing k3s installation
- **Image Loading**: Loads built images directly into k3s
- **Namespace Management**: Creates and manages Kubernetes namespaces
- **Service Discovery**: Provides access URLs for deployed applications

### Testing Workflow
1. Build container image
2. Generate Helm chart
3. Deploy to k3s
4. Access application via generated URLs
5. Iterate and redeploy

## Examples

### Example 1: Spring Boot Application
```bash
# In your Spring Boot project directory
User: "I need to containerize this application and deploy it"

Server Response:
✅ Analyzed project: Maven, Spring Boot 3.2.0
✅ Detected main class: com.example.demo.DemoApplication
✅ Initialized Jib configuration
✅ Built container: demo:latest
✅ Generated Helm chart: ./charts/demo/
✅ Deployed to k3s namespace: default
🌐 Application URL: http://localhost:30080
```

### Example 2: Multi-Module Maven Project
```bash
User: "Build containers for all modules in this Maven project"

Server Response:
✅ Analyzed project: Multi-module Maven project
✅ Found 3 modules with main classes
✅ Configured Jib for each module
✅ Built containers:
   - auth-service:latest
   - user-service:latest  
   - api-gateway:latest
✅ Generated individual Helm charts
```

### Example 3: Custom Registry
```bash
User: "Build and push to my private registry at registry.company.com"

Server Response:
✅ Configured registry: registry.company.com
✅ Set up authentication
✅ Built and pushed: registry.company.com/myapp:v1.0.0
✅ Updated Helm chart with registry URL
```

## Architecture

### Core Components
- **JibMcpController**: Main MCP tool controller with 15 tools
- **Service Layer**: Specialized services for each operation category
- **Model Classes**: Type-safe result objects for all operations
- **Configuration Management**: Spring Boot auto-configuration

### Tool Categories
1. **Project Analysis**: Understanding project structure
2. **Jib Configuration**: Setting up containerization
3. **Container Building**: Creating container images
4. **Registry Operations**: Managing container registries
5. **Helm Charts**: Kubernetes deployment templates
6. **Development Tools**: Local testing and deployment
7. **Configuration Management**: Import/export capabilities

## Troubleshooting

### Common Issues

#### Build Failures
```bash
# Check project structure
User: "analyze_project /path/to/project"

# Validate configuration
User: "validate_jib_config /path/to/project" 
```

#### Registry Authentication
```bash
# Configure registry with authentication
User: "configure_registry with service account authentication for GCR"
```

#### k3s Deployment Issues
```bash
# Check deployment status
User: "get_build_status /path/to/project"

# Redeploy with fresh configuration
User: "deploy_to_k3s with wait for ready"
```

### Logging
Enable debug logging to see detailed operations:
```yaml
logging:
  level:
    com.example.jlibmcpserver: DEBUG
```

## Contributing

### Development Setup
1. Fork the repository
2. Create a feature branch
3. Run tests: `./gradlew test`
4. Submit pull request

### Testing
```bash
# Run all tests
./gradlew test

# Run integration tests
./gradlew integrationTest

# Run specific test class
./gradlew test --tests "JibMcpServerIntegrationTest"
```

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Support

- **Issues**: Report bugs and feature requests via GitHub Issues
- **Documentation**: Additional documentation in the `/docs` directory
- **Examples**: Sample projects in the `/examples` directory

## Acknowledgments

- **Google Jib**: Container image building without Docker
- **Spring AI**: MCP server framework
- **Spring Boot**: Application framework
- **Helm**: Kubernetes package manager
- **k3s**: Lightweight Kubernetes distribution