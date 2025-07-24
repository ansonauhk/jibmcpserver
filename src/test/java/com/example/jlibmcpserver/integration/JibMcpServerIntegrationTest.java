package com.example.jlibmcpserver.integration;

import com.example.jlibmcpserver.controller.JibMcpController;
import com.example.jlibmcpserver.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test that demonstrates end-to-end functionality of the Jib MCP Server.
 * This test simulates a typical workflow:
 * 1. Analyze an existing Java project
 * 2. Initialize Jib configuration transparently
 * 3. Configure container settings
 * 4. Generate Helm chart for deployment
 * 5. Validate the complete setup
 */
@SpringBootTest
class JibMcpServerIntegrationTest {

    @Autowired
    private JibMcpController jibMcpController;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        // Create a sample Maven project structure for testing
        createSampleMavenProject();
    }

    @Test
    void testCompleteJibWorkflow() throws IOException {
        String projectPath = tempDir.toString();

        // Step 1: Analyze the project
        ProjectAnalysisResult analysisResult = jibMcpController.analyzeProject(projectPath);
        
        assertNotNull(analysisResult);
        assertEquals("maven", analysisResult.getBuildSystem());
        assertEquals("single-module", analysisResult.getProjectType());
        assertEquals("com.example.demo.DemoApplication", analysisResult.getMainClass());
        assertFalse(analysisResult.isExistingJibConfig());

        // Step 2: Initialize Jib configuration transparently
        JibConfigResult initResult = jibMcpController.initJibConfig(
            projectPath, "maven", "eclipse-temurin:21-jre", "my-demo-app:latest"
        );
        
        assertTrue(initResult.isSuccess());
        assertNotNull(initResult.getConfigFilePath());

        // Step 3: Set main class
        JibConfigResult mainClassResult = jibMcpController.setMainClass(
            projectPath, "com.example.demo.DemoApplication"
        );
        
        assertTrue(mainClassResult.isSuccess());

        // Step 4: Configure container settings
        JibConfigResult containerResult = jibMcpController.configureContainerSettings(
            projectPath, 
            List.of("8080"), 
            Map.of("SPRING_PROFILES_ACTIVE", "production"),
            List.of("-Xms512m", "-Xmx1024m"),
            "/app"
        );
        
        assertTrue(containerResult.isSuccess());

        // Step 5: Validate Jib configuration
        ValidationResult validationResult = jibMcpController.validateJibConfig(projectPath);
        
        assertTrue(validationResult.isValid());
        assertTrue(validationResult.getErrors().isEmpty());

        // Step 6: Generate Helm chart
        HelmChartResult helmResult = jibMcpController.generateHelmChart(
            projectPath, "demo-app", "1.0.0", "1.0.0", "production"
        );
        
        assertTrue(helmResult.isSuccess());
        assertNotNull(helmResult.getChartPath());
        assertFalse(helmResult.getGeneratedFiles().isEmpty());

        // Step 7: Update Helm values
        JibConfigResult helmUpdateResult = jibMcpController.updateHelmValues(
            projectPath, 
            Map.of(
                "replicaCount", 2,
                "image", Map.of("tag", "v1.0.0"),
                "resources", Map.of(
                    "limits", Map.of("memory", "1Gi", "cpu", "500m")
                )
            )
        );
        
        assertTrue(helmUpdateResult.isSuccess());

        // Step 8: Export configuration for backup
        Map<String, Object> exportedConfig = jibMcpController.exportJibConfig(projectPath);
        
        assertNotNull(exportedConfig);
        assertTrue(exportedConfig.containsKey("config"));
        assertTrue(exportedConfig.containsKey("build_system"));
        assertEquals("maven", exportedConfig.get("build_system"));

        // Step 9: Verify that the pom.xml was modified correctly
        Path pomPath = tempDir.resolve("pom.xml");
        String pomContent = Files.readString(pomPath);
        assertTrue(pomContent.contains("jib-maven-plugin"));
        assertTrue(pomContent.contains("eclipse-temurin:21-jre"));
        assertTrue(pomContent.contains("my-demo-app:latest"));
        assertTrue(pomContent.contains("com.example.demo.DemoApplication"));

        // Step 10: Verify Helm chart files were created
        Path chartPath = Path.of(helmResult.getChartPath());
        assertTrue(Files.exists(chartPath.resolve("Chart.yaml")));
        assertTrue(Files.exists(chartPath.resolve("values.yaml")));
        assertTrue(Files.exists(chartPath.resolve("templates/deployment.yaml")));
        assertTrue(Files.exists(chartPath.resolve("templates/service.yaml")));

        // Verify Helm chart values were updated
        String valuesContent = Files.readString(chartPath.resolve("values.yaml"));
        assertTrue(valuesContent.contains("replicaCount: 2"));
        assertTrue(valuesContent.contains("v1.0.0"));
        assertTrue(valuesContent.contains("1Gi"));
    }

    @Test
    void testTransparentContainerOperations() throws IOException {
        String projectPath = tempDir.toString();

        // Simulate a user request like "build a container" - should transparently use Jib
        // First need to set up the project
        jibMcpController.analyzeProject(projectPath);
        jibMcpController.initJibConfig(projectPath, "maven", null, null);
        jibMcpController.setMainClass(projectPath, "com.example.demo.DemoApplication");

        // Now validate that we can build (validation will work, actual build would need Maven/Jib)
        ValidationResult validation = jibMcpController.validateJibConfig(projectPath);
        assertTrue(validation.isValid());

        // Test that configuration registry works
        JibConfigResult registryResult = jibMcpController.configureRegistry(
            projectPath, "gcr", "gcr.io/my-project", "service-account"
        );
        assertTrue(registryResult.isSuccess());

        // Test build status retrieval
        Map<String, Object> buildStatus = jibMcpController.getBuildStatus(projectPath);
        assertNotNull(buildStatus);
        assertTrue(buildStatus.containsKey("status_message"));
    }

    @Test
    void testMainClassDetection() throws IOException {
        String projectPath = tempDir.toString();

        // Test automatic main class detection
        MainClassDetectionResult mainClassResult = jibMcpController.detectMainClass(projectPath);
        
        assertNotNull(mainClassResult);
        assertFalse(mainClassResult.getMainClasses().isEmpty());
        assertTrue(mainClassResult.getMainClasses().contains("com.example.demo.DemoApplication"));
        assertEquals("com.example.demo.DemoApplication", mainClassResult.getRecommendedMainClass());
    }

    private void createSampleMavenProject() throws IOException {
        // Create pom.xml
        Path pomPath = tempDir.resolve("pom.xml");
        String pomContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0"
                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                <modelVersion>4.0.0</modelVersion>
                
                <groupId>com.example</groupId>
                <artifactId>demo</artifactId>
                <version>1.0.0</version>
                <packaging>jar</packaging>
                
                <properties>
                    <maven.compiler.source>21</maven.compiler.source>
                    <maven.compiler.target>21</maven.compiler.target>
                    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
                </properties>
                
                <dependencies>
                    <dependency>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-starter</artifactId>
                        <version>3.2.0</version>
                    </dependency>
                    <dependency>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-starter-web</artifactId>
                        <version>3.2.0</version>
                    </dependency>
                </dependencies>
                
                <build>
                    <plugins>
                        <plugin>
                            <groupId>org.springframework.boot</groupId>
                            <artifactId>spring-boot-maven-plugin</artifactId>
                            <version>3.2.0</version>
                        </plugin>
                    </plugins>
                </build>
            </project>
            """;
        Files.writeString(pomPath, pomContent);

        // Create source directory structure
        Path srcMainJava = tempDir.resolve("src/main/java/com/example/demo");
        Files.createDirectories(srcMainJava);

        // Create main application class
        Path mainClassPath = srcMainJava.resolve("DemoApplication.java");
        String mainClassContent = """
            package com.example.demo;
            
            import org.springframework.boot.SpringApplication;
            import org.springframework.boot.autoconfigure.SpringBootApplication;
            
            @SpringBootApplication
            public class DemoApplication {
                public static void main(String[] args) {
                    SpringApplication.run(DemoApplication.class, args);
                }
            }
            """;
        Files.writeString(mainClassPath, mainClassContent);

        // Create a controller class
        Path controllerPath = srcMainJava.resolve("HelloController.java");
        String controllerContent = """
            package com.example.demo;
            
            import org.springframework.web.bind.annotation.GetMapping;
            import org.springframework.web.bind.annotation.RestController;
            
            @RestController
            public class HelloController {
                @GetMapping("/hello")
                public String hello() {
                    return "Hello, World!";
                }
            }
            """;
        Files.writeString(controllerPath, controllerContent);

        // Create resources directory
        Path srcMainResources = tempDir.resolve("src/main/resources");
        Files.createDirectories(srcMainResources);

        // Create application.properties
        Path propertiesPath = srcMainResources.resolve("application.properties");
        String propertiesContent = """
            server.port=8080
            spring.application.name=demo
            logging.level.com.example=DEBUG
            """;
        Files.writeString(propertiesPath, propertiesContent);
    }
}