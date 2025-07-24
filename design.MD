# Google Jib MCP Server Design

## Overview

This MCP (Model Context Protocol) Server provides transparent, intelligent assistance for containerization workflows in corporate Java development environments. It automatically handles all container operations using Google Jib Maven/Gradle plugins without requiring users to explicitly mention "jib" or learn complex Jib configurations. Users can make natural requests like "build a container" or "deploy my application" and the MCP server will transparently use Jib to fulfill these requests.

## Architecture

### Core Components

```
┌─────────────────────────────────────────────────────────────┐
│                    MCP Server                               │
├─────────────────────────────────────────────────────────────┤
│  ┌─────────────────┐  ┌─────────────────┐  ┌──────────────┐ │
│  │   Jib Config    │  │  Build Manager  │  │ Helm Chart   │ │
│  │   Manager       │  │                 │  │ Generator    │ │
│  └─────────────────┘  └─────────────────┘  └──────────────┘ │
├─────────────────────────────────────────────────────────────┤
│  ┌─────────────────┐  ┌─────────────────┐  ┌──────────────┐ │
│  │ Maven Project   │  │ Gradle Project  │  │ File System  │ │
│  │ Parser          │  │ Parser          │  │ Utils        │ │
│  └─────────────────┘  └─────────────────┘  └──────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

### Technology Stack
- **Framework**: Spring Boot 3.5.3 with Spring AI MCP Server
- **Java Version**: 21
- **Build System**: Gradle
- **MCP Protocol**: JSON-RPC based communication

## MCP Tools Definition

### 1. Project Analysis Tools

#### `analyze_project`
**Purpose**: Analyze the current Java project structure and detect build system (Maven/Gradle)

**Parameters**:
```json
{
  "project_path": {
    "type": "string",
    "description": "Path to the Java project root directory"
  }
}
```

**Returns**:
```json
{
  "build_system": "maven|gradle",
  "project_type": "single-module|multi-module",
  "main_class": "string|null",
  "existing_jib_config": "boolean",
  "dependencies": ["string"],
  "java_version": "string"
}
```

#### `detect_main_class`
**Purpose**: Automatically detect the main class in the Java project

**Parameters**:
```json
{
  "project_path": {
    "type": "string",
    "description": "Path to the Java project root directory"
  }
}
```

**Returns**:
```json
{
  "main_classes": ["string"],
  "recommended_main_class": "string|null"
}
```

### 2. Jib Configuration Tools

#### `init_jib_config`
**Purpose**: Initialize Jib plugin configuration transparently in Maven pom.xml or Gradle build.gradle (triggered by containerization requests without explicit "jib" mention)

**Parameters**:
```json
{
  "project_path": {
    "type": "string",
    "description": "Path to the Java project root directory"
  },
  "build_system": {
    "type": "string",
    "enum": ["maven", "gradle"],
    "description": "Build system to configure"
  },
  "base_image": {
    "type": "string",
    "default": "eclipse-temurin:21-jre",
    "description": "Base container image"
  },
  "target_image": {
    "type": "string",
    "description": "Target container image name"
  }
}
```

**Returns**:
```json
{
  "success": "boolean",
  "config_file_path": "string",
  "message": "string"
}
```

#### `update_jib_config`
**Purpose**: Update specific Jib configuration parameters

**Parameters**:
```json
{
  "project_path": {
    "type": "string",
    "description": "Path to the Java project root directory"
  },
  "config_section": {
    "type": "string",
    "enum": ["from", "to", "container", "extraDirectories"],
    "description": "Configuration section to update"
  },
  "parameters": {
    "type": "object",
    "description": "Key-value pairs of configuration parameters"
  }
}
```

**Returns**:
```json
{
  "success": "boolean",
  "updated_config": "object",
  "message": "string"
}
```

#### `set_main_class`
**Purpose**: Set the main class for the containerized application

**Parameters**:
```json
{
  "project_path": {
    "type": "string",
    "description": "Path to the Java project root directory"
  },
  "main_class": {
    "type": "string",
    "description": "Fully qualified main class name (e.g., com.abc.Main)"
  }
}
```

**Returns**:
```json
{
  "success": "boolean",
  "previous_main_class": "string|null",
  "message": "string"
}
```

#### `configure_container_settings`
**Purpose**: Configure container runtime settings (ports, environment variables, JVM flags)

**Parameters**:
```json
{
  "project_path": {
    "type": "string",
    "description": "Path to the Java project root directory"
  },
  "ports": {
    "type": "array",
    "items": {"type": "string"},
    "description": "Exposed container ports"
  },
  "environment_variables": {
    "type": "object",
    "description": "Environment variables as key-value pairs"
  },
  "jvm_flags": {
    "type": "array",
    "items": {"type": "string"},
    "description": "JVM runtime flags"
  },
  "working_directory": {
    "type": "string",
    "description": "Container working directory"
  }
}
```

**Returns**:
```json
{
  "success": "boolean",
  "applied_settings": "object",
  "message": "string"
}
```

### 3. Container Build Tools

#### `build_container`
**Purpose**: Build container image transparently using Jib (triggered by user requests like "build a container", "build my application", etc.)

**Parameters**:
```json
{
  "project_path": {
    "type": "string",
    "description": "Path to the Java project root directory"
  },
  "build_target": {
    "type": "string",
    "enum": ["registry", "docker", "tar"],
    "default": "docker",
    "description": "Build target: push to registry, build to Docker daemon, or create tar file"
  },
  "tags": {
    "type": "array",
    "items": {"type": "string"},
    "description": "Additional image tags"
  }
}
```

**Returns**:
```json
{
  "success": "boolean",
  "image_id": "string",
  "build_log": "string",
  "execution_time": "number"
}
```

#### `validate_jib_config`
**Purpose**: Validate current Jib configuration before building

**Parameters**:
```json
{
  "project_path": {
    "type": "string",
    "description": "Path to the Java project root directory"
  }
}
```

**Returns**:
```json
{
  "valid": "boolean",
  "errors": ["string"],
  "warnings": ["string"],
  "suggestions": ["string"]
}
```

### 4. Registry and Authentication Tools

#### `configure_registry`
**Purpose**: Configure container registry settings and authentication

**Parameters**:
```json
{
  "project_path": {
    "type": "string",
    "description": "Path to the Java project root directory"
  },
  "registry_type": {
    "type": "string",
    "enum": ["gcr", "ecr", "docker-hub", "jfrog", "azure"],
    "description": "Container registry type"
  },
  "registry_url": {
    "type": "string",
    "description": "Registry URL"
  },
  "auth_method": {
    "type": "string",
    "enum": ["credential-helper", "username-password", "service-account"],
    "description": "Authentication method"
  }
}
```

**Returns**:
```json
{
  "success": "boolean",
  "auth_configured": "boolean",
  "message": "string"
}
```

### 5. Helm Chart Generation Tools

#### `generate_helm_chart`
**Purpose**: Generate Helm chart templates for the containerized application

**Parameters**:
```json
{
  "project_path": {
    "type": "string",
    "description": "Path to the Java project root directory"
  },
  "chart_name": {
    "type": "string",
    "description": "Helm chart name"
  },
  "chart_version": {
    "type": "string",
    "default": "0.1.0",
    "description": "Helm chart version"
  },
  "app_version": {
    "type": "string",
    "description": "Application version"
  },
  "namespace": {
    "type": "string",
    "default": "default",
    "description": "Kubernetes namespace"
  }
}
```

**Returns**:
```json
{
  "success": "boolean",
  "chart_path": "string",
  "generated_files": ["string"],
  "message": "string"
}
```

#### `update_helm_values`
**Purpose**: Update Helm chart values for deployment configuration

**Parameters**:
```json
{
  "project_path": {
    "type": "string",
    "description": "Path to the Java project root directory"
  },
  "values": {
    "type": "object",
    "description": "Helm values to update"
  }
}
```

**Returns**:
```json
{
  "success": "boolean",
  "updated_values": "object",
  "message": "string"
}
```

### 6. Development and Testing Tools

#### `deploy_to_k3s`
**Purpose**: Deploy the containerized application to local k3s for testing (automatically containerizes using Jib if needed)

**Parameters**:
```json
{
  "project_path": {
    "type": "string",
    "description": "Path to the Java project root directory"
  },
  "namespace": {
    "type": "string",
    "default": "default",
    "description": "Kubernetes namespace for deployment"
  },
  "wait_for_ready": {
    "type": "boolean",
    "default": true,
    "description": "Wait for deployment to be ready"
  }
}
```

**Returns**:
```json
{
  "success": "boolean",
  "deployment_name": "string",
  "service_url": "string|null",
  "status": "string",
  "message": "string"
}
```

#### `get_build_status`
**Purpose**: Get the status of the last container build

**Parameters**:
```json
{
  "project_path": {
    "type": "string",
    "description": "Path to the Java project root directory"
  }
}
```

**Returns**:
```json
{
  "last_build_time": "string",
  "last_build_status": "success|failed|in-progress",
  "image_id": "string|null",
  "error_message": "string|null"
}
```

### 7. Configuration Management Tools

#### `export_jib_config`
**Purpose**: Export current Jib configuration as JSON for backup or sharing

**Parameters**:
```json
{
  "project_path": {
    "type": "string",
    "description": "Path to the Java project root directory"
  }
}
```

**Returns**:
```json
{
  "config": "object",
  "build_system": "string",
  "export_timestamp": "string"
}
```

#### `import_jib_config`
**Purpose**: Import Jib configuration from JSON

**Parameters**:
```json
{
  "project_path": {
    "type": "string",
    "description": "Path to the Java project root directory"
  },
  "config": {
    "type": "object",
    "description": "Jib configuration to import"
  }
}
```

**Returns**:
```json
{
  "success": "boolean",
  "applied_config": "object",
  "message": "string"
}
```

## Use Case Examples

### Example 1: Initial Project Setup
```
User: "Set up containerization for my Spring Boot project with main class com.example.MyApp"

MCP Flow:
1. analyze_project → detect Maven/Gradle
2. init_jib_config → add Jib plugin transparently
3. set_main_class → configure main class
4. configure_container_settings → set default ports (8080)
```

### Example 2: Container Build and Deploy
```
User: "Build a container and deploy it to k3s for testing"

MCP Flow:
1. validate_jib_config → check configuration (transparent)
2. build_container → build using Jib to Docker daemon
3. generate_helm_chart → create Helm chart
4. deploy_to_k3s → deploy to local cluster
```

### Example 3: Registry Configuration
```
User: "Push my container to Google Container Registry"

MCP Flow:
1. configure_registry → set GCR settings transparently
2. update_jib_config → update target image using Jib
3. validate_jib_config → verify setup
4. build_container → build and push to registry using Jib
```

## Security Considerations

- **Credential Management**: Secure handling of registry credentials
- **Firewall Compliance**: Respect corporate network restrictions
- **Build Isolation**: Ensure build processes don't interfere with each other
- **Resource Limits**: Implement resource constraints for container builds

## Error Handling

- **Validation Errors**: Comprehensive validation with actionable error messages
- **Build Failures**: Detailed build logs and suggested fixes
- **Configuration Conflicts**: Detection and resolution of conflicting settings
- **Network Issues**: Graceful handling of registry connectivity problems

## Integration Points

- **IDE Integration**: VSCode GitHub Copilot agent mode support
- **CI/CD Integration**: Support for automated build pipelines
- **Monitoring**: Build metrics and performance tracking
- **Logging**: Comprehensive logging for debugging and audit trails