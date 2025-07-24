# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Google Jib MCP (Model Context Protocol) Server built with Spring Boot 3.5.3 and Java 21. The server provides transparent containerization assistance for Java applications in corporate environments, allowing users to make natural requests like "build a container" without needing to explicitly mention or learn Google Jib configurations.

## Development Commands

### Build & Run
```bash
# Build the project
./gradlew build

# Run the MCP server
./gradlew bootRun

# Run tests
./gradlew test

# Run a specific test class
./gradlew test --tests "JlibmcpserverApplicationTests"

# Clean and rebuild
./gradlew clean build

# Create distribution JAR
./gradlew bootJar
```

## Architecture

### Core Components (to be implemented)
The MCP server is designed with these main components as specified in `design.MD`:

1. **Jib Config Manager** - Handles Google Jib plugin configuration for Maven/Gradle projects
2. **Build Manager** - Manages container build processes transparently using Jib
3. **Helm Chart Generator** - Creates Kubernetes deployment charts using in-house plugin
4. **Project Parsers** - Supports both Maven and Gradle project analysis and modification
5. **File System Utils** - Handles file operations and project structure analysis

### MCP Tools Structure
The server implements 15+ MCP tools across 7 categories using Spring AI's `@Tool` annotation:
- **Project Analysis**: `analyze_project`, `detect_main_class`
- **Jib Configuration**: `init_jib_config`, `update_jib_config`, `set_main_class`, `configure_container_settings`
- **Container Operations**: `build_container`, `validate_jib_config`
- **Registry Management**: `configure_registry`
- **Helm Charts**: `generate_helm_chart`, `update_helm_values`
- **Development/Testing**: `deploy_to_k3s`, `get_build_status`
- **Configuration Management**: `export_jib_config`, `import_jib_config`

### Tool Registration
Tools are automatically registered via `McpToolConfiguration` using Spring AI's `MethodToolCallbackProvider`.

### Key Design Principles
- **Transparency**: Users should never need to mention "jib" explicitly - requests like "build a container" should automatically use Jib
- **Corporate Environment**: Designed for firewall-controlled environments with in-house configurations
- **Legacy Support**: Specifically targets containerization of legacy Java applications currently running on VMs
- **IDE Integration**: Built for VSCode GitHub Copilot agent mode with MCP server support

## Current Implementation Status

- **Infrastructure**: ✅ Spring Boot MCP server setup complete
- **Core Application**: ✅ Basic structure in place (`JlibmcpserverApplication.java`)
- **MCP Tools**: ❌ Not implemented - only skeleton application exists
- **Business Logic**: ❌ Requires implementation of all tool handlers
- **Tests**: ✅ Basic test structure exists but needs expansion

## Key Dependencies

- `spring-ai-starter-mcp-server`: Core MCP server functionality
- Spring Boot 3.5.3 with Java 21 toolchain
- Gradle 8.14.3 build system

## Development Notes

- The main application class is `com.example.jlibmcpserver.JlibmcpserverApplication`
- MCP tools are implemented using Spring AI's `@Tool` annotation in `JibMcpController`
- Tool registration is handled automatically via `McpToolConfiguration` using `MethodToolCallbackProvider`
- The server supports both Maven and Gradle projects for Jib configuration
- Focus on transparent operations - users shouldn't need to understand Jib syntax
- Integration with in-house Maven/Gradle plugins for Helm chart generation is implemented