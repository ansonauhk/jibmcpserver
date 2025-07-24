package com.example.jlibmcpserver.service;

import com.example.jlibmcpserver.model.JibConfigResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JibConfigurationServiceTest {

    private JibConfigurationService jibConfigurationService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        jibConfigurationService = new JibConfigurationService();
    }

    @Test
    void testInitJibConfig_Maven() throws IOException {
        // Create basic Maven pom.xml
        Path pomFile = tempDir.resolve("pom.xml");
        String pomContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0">
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.example</groupId>
                <artifactId>test-app</artifactId>
                <version>1.0.0</version>
                
                <build>
                    <plugins>
                    </plugins>
                </build>
            </project>
            """;
        Files.writeString(pomFile, pomContent);

        JibConfigResult result = jibConfigurationService.initJibConfig(
            tempDir.toString(), "maven", "eclipse-temurin:21-jre", "my-app:latest"
        );

        assertTrue(result.isSuccess());
        assertEquals(pomFile.toString(), result.getConfigFilePath());
        
        // Verify Jib plugin was added
        String updatedContent = Files.readString(pomFile);
        assertTrue(updatedContent.contains("jib-maven-plugin"));
        assertTrue(updatedContent.contains("eclipse-temurin:21-jre"));
        assertTrue(updatedContent.contains("my-app:latest"));
    }

    @Test
    void testInitJibConfig_Gradle() throws IOException {
        // Create basic Gradle build.gradle
        Path buildFile = tempDir.resolve("build.gradle");
        String buildContent = """
            plugins {
                id 'java'
            }
            
            dependencies {
                implementation 'org.springframework.boot:spring-boot-starter'
            }
            """;
        Files.writeString(buildFile, buildContent);

        JibConfigResult result = jibConfigurationService.initJibConfig(
            tempDir.toString(), "gradle", "eclipse-temurin:17-jre", "my-gradle-app:latest"
        );

        assertTrue(result.isSuccess());
        assertEquals(buildFile.toString(), result.getConfigFilePath());
        
        // Verify Jib plugin was added
        String updatedContent = Files.readString(buildFile);
        assertTrue(updatedContent.contains("com.google.cloud.tools.jib"));
        assertTrue(updatedContent.contains("eclipse-temurin:17-jre"));
        assertTrue(updatedContent.contains("my-gradle-app:latest"));
    }

    @Test
    void testInitJibConfig_AlreadyConfigured() throws IOException {
        // Create Maven pom.xml with existing Jib configuration
        Path pomFile = tempDir.resolve("pom.xml");
        String pomContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0">
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.example</groupId>
                <artifactId>test-app</artifactId>
                <version>1.0.0</version>
                
                <build>
                    <plugins>
                        <plugin>
                            <groupId>com.google.cloud.tools</groupId>
                            <artifactId>jib-maven-plugin</artifactId>
                            <version>3.4.0</version>
                        </plugin>
                    </plugins>
                </build>
            </project>
            """;
        Files.writeString(pomFile, pomContent);

        JibConfigResult result = jibConfigurationService.initJibConfig(
            tempDir.toString(), "maven", "eclipse-temurin:21-jre", "my-app:latest"
        );

        assertTrue(result.isSuccess());
        assertEquals(pomFile.toString(), result.getConfigFilePath());
    }

    @Test
    void testSetMainClass_Maven() throws IOException {
        // Create Maven pom.xml with Jib configuration
        Path pomFile = tempDir.resolve("pom.xml");
        String pomContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0">
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.example</groupId>
                <artifactId>test-app</artifactId>
                <version>1.0.0</version>
                
                <build>
                    <plugins>
                        <plugin>
                            <groupId>com.google.cloud.tools</groupId>
                            <artifactId>jib-maven-plugin</artifactId>
                            <version>3.4.0</version>
                            <configuration>
                                <container>
                                </container>
                            </configuration>
                        </plugin>
                    </plugins>
                </build>
            </project>
            """;
        Files.writeString(pomFile, pomContent);

        JibConfigResult result = jibConfigurationService.setMainClass(
            tempDir.toString(), "com.example.Application"
        );

        assertTrue(result.isSuccess());
        
        // Verify main class was added
        String updatedContent = Files.readString(pomFile);
        assertTrue(updatedContent.contains("<mainClass>com.example.Application</mainClass>"));
    }

    @Test
    void testSetMainClass_Gradle() throws IOException {
        // Create Gradle build.gradle with Jib configuration
        Path buildFile = tempDir.resolve("build.gradle");
        String buildContent = """
            plugins {
                id 'java'
                id 'com.google.cloud.tools.jib' version '3.4.0'
            }
            
            jib {
                from {
                    image = 'eclipse-temurin:17-jre'
                }
                to {
                    image = 'my-app:latest'
                }
                container {
                }
            }
            """;
        Files.writeString(buildFile, buildContent);

        JibConfigResult result = jibConfigurationService.setMainClass(
            tempDir.toString(), "com.example.GradleApplication"
        );

        assertTrue(result.isSuccess());
        
        // Verify main class was added
        String updatedContent = Files.readString(buildFile);
        assertTrue(updatedContent.contains("mainClass = 'com.example.GradleApplication'"));
    }

    @Test
    void testConfigureContainerSettings() throws IOException {
        Path pomFile = tempDir.resolve("pom.xml");
        String pomContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0">
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.example</groupId>
                <artifactId>test-app</artifactId>
                <version>1.0.0</version>
                
                <build>
                    <plugins>
                        <plugin>
                            <groupId>com.google.cloud.tools</groupId>
                            <artifactId>jib-maven-plugin</artifactId>
                            <version>3.4.0</version>
                        </plugin>
                    </plugins>
                </build>
            </project>
            """;
        Files.writeString(pomFile, pomContent);

        List<String> ports = List.of("8080", "8081");
        Map<String, String> envVars = Map.of("ENV", "test", "DEBUG", "true");
        List<String> jvmFlags = List.of("-Xms512m", "-Xmx1024m");

        JibConfigResult result = jibConfigurationService.configureContainerSettings(
            tempDir.toString(), ports, envVars, jvmFlags, "/app"
        );

        assertTrue(result.isSuccess());
    }

    @Test
    void testInitJibConfig_NonExistentPath() {
        JibConfigResult result = jibConfigurationService.initJibConfig(
            "/non/existent/path", "maven", "eclipse-temurin:21-jre", "my-app:latest"
        );

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("does not exist"));
    }

    @Test
    void testInitJibConfig_UnsupportedBuildSystem() throws IOException {
        // Create empty directory
        JibConfigResult result = jibConfigurationService.initJibConfig(
            tempDir.toString(), "ant", "eclipse-temurin:21-jre", "my-app:latest"
        );

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("Unsupported build system"));
    }
}