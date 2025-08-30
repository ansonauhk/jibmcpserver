# Jib MCP Server Development Instructions

**ALWAYS follow these instructions exactly and fallback to additional search and context gathering ONLY if the information here is incomplete or found to be in error.**

This is a Google Jib MCP (Model Context Protocol) Server built with Spring Boot 3.5.3 and Java 21. The server provides transparent containerization assistance for Java applications in corporate environments, allowing users to make natural requests like "build a container" without needing to explicitly mention Google Jib configurations.

## Prerequisites and Setup

**CRITICAL**: This project requires Java 21. Always set Java 21 before any commands:
```bash
export JAVA_HOME=/usr/lib/jvm/temurin-21-jdk-amd64
export PATH=/usr/lib/jvm/temurin-21-jdk-amd64/bin:$PATH
```

Verify Java version before proceeding:
```bash
java -version  # Should show "openjdk version 21.0.8"
```

## Building and Testing Commands

### Clean Build
**NEVER CANCEL** - Build completes in 6-7 seconds. Set timeout to 30+ seconds minimum.
```bash
./gradlew clean build
```
- Expected time: 6-7 seconds
- Builds all code, runs 53+ tests
- Creates distribution JAR

### Incremental Build
```bash
./gradlew build
```
- Expected time: 1-2 seconds when no changes
- Use for quick validation after small changes

### Run Tests Only
```bash
./gradlew test
```
- Expected time: 1-4 seconds
- Runs 53+ unit and integration tests
- All tests should pass

### Run Specific Test Class
```bash
./gradlew test --tests "JlibmcpserverApplicationTests"
```
- Expected time: 4 seconds
- Use for targeted testing after changes

### Create Distribution JAR
```bash
./gradlew bootJar
```
- Expected time: 1 second
- Creates executable JAR in build/libs/

## Running the Application

### Start MCP Server
```bash
./gradlew bootRun
```
- Expected startup time: 1.6 seconds
- Runs on port 8080 (HTTP)
- Registers 15 MCP tools automatically
- Look for log message: "Registered tools: 15"
- Press Ctrl+C to stop

### Direct JAR Execution
```bash
java -jar build/libs/jlibmcpserver-0.0.1-SNAPSHOT.jar
```

## Application Architecture

### MCP Tools Available (15 total)
The server implements these tools across 7 categories:

**Project Analysis:**
- `analyze_project` - Analyze Java project structure and detect build system
- `detect_main_class` - Automatically find main application classes

**Jib Configuration:**
- `init_jib_config` - Initialize Jib plugin configuration in Maven/Gradle
- `update_jib_config` - Update specific Jib configuration parameters
- `set_main_class` - Configure main class for containerization
- `configure_container_settings` - Set ports, environment variables, JVM flags

**Container Building:**
- `build_container` - Build container images using Jib
- `validate_jib_config` - Validate Jib configuration before building

**Registry Management:**
- `configure_registry` - Configure container registries (GCR, ECR, Docker Hub, etc.)

**Helm Charts:**
- `generate_helm_chart` - Generate production-ready Helm charts
- `update_helm_values` - Update Helm chart values for different environments

**Development/Testing:**
- `deploy_to_k3s` - Deploy to local k3s for testing
- `get_build_status` - Get status of last container build

**Configuration Management:**
- `export_jib_config` - Export configuration as JSON
- `import_jib_config` - Import configuration from JSON

### Key Design Principles
- **Transparency**: Users never need to mention "jib" explicitly
- **Corporate Environment**: Designed for firewall-controlled environments
- **Legacy Support**: Targets containerization of legacy Java applications
- **MCP Integration**: Built for VSCode GitHub Copilot agent mode

## Validation Scenarios

**ALWAYS test these scenarios after making changes to ensure functionality:**

### Basic MCP Server Validation
1. Start the application: `./gradlew bootRun`
2. Verify startup logs show: "Registered tools: 15"
3. Verify server starts without errors in ~1.6 seconds
4. Verify application runs on port 8080
5. Stop with Ctrl+C

### Build System Validation
1. Run clean build: `./gradlew clean build`
2. Verify build completes in 6-7 seconds
3. Verify all 53+ tests pass
4. Check build/libs/ contains JAR file

### Code Quality Validation
Always run these before committing changes:
```bash
./gradlew build test
```

## Project Structure

### Key Directories
```
src/main/java/com/example/jlibmcpserver/
├── JlibmcpserverApplication.java    # Main Spring Boot application
├── config/McpToolConfiguration.java # MCP tool registration
├── controller/JibMcpController.java # 15 MCP tools implementation
├── service/                         # Business logic services
├── model/                          # Result and data models
└── ...

src/test/java/                      # Test classes (53+ tests)
build.gradle                        # Gradle build configuration
CLAUDE.md                          # Claude development guidance
design.MD                          # Architecture and design docs
```

### Important Files to Check After Changes
- Always check `JibMcpController.java` after modifying MCP tool functionality
- Always verify `build.gradle` after adding dependencies
- Always check test files in `src/test/java/` match your changes

### Configuration Files
- `src/main/resources/application.properties` - Spring Boot configuration
- `build.gradle` - Build system and dependencies
- Gradle Wrapper in root directory (use `./gradlew` not `gradle`)

## Dependencies and Framework

### Core Dependencies
- **Spring Boot 3.5.3** - Application framework
- **Spring AI MCP Server** - MCP protocol implementation
- **Java 21** - Runtime and compilation target
- **Gradle 8.14.3** - Build system

### Build System
- Uses Gradle Wrapper (`./gradlew`) - ALWAYS use wrapper, not system gradle
- Java 21 toolchain configured in build.gradle
- Spring Boot plugin provides bootRun and bootJar tasks

## Troubleshooting

### Common Issues and Solutions

**Build fails with "Unsupported Java version":**
```bash
# Ensure Java 21 is set correctly
export JAVA_HOME=/usr/lib/jvm/temurin-21-jdk-amd64
export PATH=/usr/lib/jvm/temurin-21-jdk-amd64/bin:$PATH
java -version
```

**Application fails to start:**
- Check port 8080 is not in use
- Verify Java 21 is being used
- Check application.properties for configuration issues

**Tests fail:**
- Run `./gradlew clean` first
- Ensure no other instances are running on port 8080
- Check for Java version mismatch

**MCP tools not registering:**
- Verify `@Tool` annotations in JibMcpController.java
- Check Spring component scanning is working
- Look for "Registered tools: 15" in startup logs

## Development Workflow

### Making Changes
1. **Always** set Java 21 environment first
2. Make minimal code changes
3. Run incremental build: `./gradlew build`
4. Run tests: `./gradlew test`
5. Test functionality: `./gradlew bootRun`
6. Verify MCP tools registration in logs

### Before Committing
1. Run full clean build: `./gradlew clean build`
2. Verify all tests pass (53+)
3. Start application and verify 15 tools register
4. Test at least one functional scenario

### Performance Expectations
- **Clean build**: 6-7 seconds (NEVER CANCEL - set 30+ second timeout)
- **Incremental build**: 1-2 seconds
- **Test execution**: 1-4 seconds
- **Application startup**: 1.6 seconds
- **Tool registration**: Should see "Registered tools: 15" in logs

## Documentation Files

### Key Documentation (check these for context)
- `README.md` - Project overview and usage examples
- `CLAUDE.md` - Claude-specific development guidance
- `design.MD` - Architecture and MCP tools design
- `requirement.MD` - Original requirements and context

### Common Commands Reference
```bash
# Setup (run first)
export JAVA_HOME=/usr/lib/jvm/temurin-21-jdk-amd64
export PATH=/usr/lib/jvm/temurin-21-jdk-amd64/bin:$PATH

# Build and test
./gradlew clean build    # Clean build (6-7 seconds)
./gradlew build         # Incremental build (1-2 seconds)
./gradlew test          # Run tests (1-4 seconds)
./gradlew bootRun       # Start MCP server
./gradlew bootJar       # Create distribution JAR

# Validation
java -version           # Verify Java 21
./gradlew --version     # Verify Gradle 8.14.3
```

Remember: This is an MCP server that makes Google Jib containerization transparent for users. The goal is enabling natural language requests like "build a container" without requiring users to learn Jib syntax.