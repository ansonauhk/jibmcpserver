package com.example.jlibmcpserver.service;

import com.example.jlibmcpserver.model.BuildResult;
import com.example.jlibmcpserver.model.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ContainerBuildServiceTest {

    private ContainerBuildService containerBuildService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        containerBuildService = new ContainerBuildService();
    }

    @Test
    void testValidateJibConfig_ValidMavenConfiguration() throws IOException {
        // Create Maven project with valid Jib configuration
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
                                <from>
                                    <image>eclipse-temurin:21-jre</image>
                                </from>
                                <to>
                                    <image>my-app:latest</image>
                                </to>
                                <container>
                                    <mainClass>com.example.Application</mainClass>
                                </container>
                            </configuration>
                        </plugin>
                    </plugins>
                </build>
            </project>
            """;
        Files.writeString(pomFile, pomContent);

        ValidationResult result = containerBuildService.validateJibConfig(tempDir.toString());

        assertTrue(result.isValid());
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    void testValidateJibConfig_ValidGradleConfiguration() throws IOException {
        // Create Gradle project with valid Jib configuration
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
                    image = 'my-gradle-app:latest'
                }
                container {
                    mainClass = 'com.example.GradleApplication'
                }
            }
            """;
        Files.writeString(buildFile, buildContent);

        ValidationResult result = containerBuildService.validateJibConfig(tempDir.toString());

        assertTrue(result.isValid());
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    void testValidateJibConfig_MissingJibPlugin() throws IOException {
        // Create Maven project without Jib plugin
        Path pomFile = tempDir.resolve("pom.xml");
        String pomContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0">
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.example</groupId>
                <artifactId>test-app</artifactId>
                <version>1.0.0</version>
            </project>
            """;
        Files.writeString(pomFile, pomContent);

        ValidationResult result = containerBuildService.validateJibConfig(tempDir.toString());

        assertFalse(result.isValid());
        assertFalse(result.getErrors().isEmpty());
        assertTrue(result.getErrors().get(0).contains("Jib plugin is not configured"));
    }

    @Test
    void testValidateJibConfig_MissingMainClass() throws IOException {
        // Create Maven project with Jib but no main class
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
                                <from>
                                    <image>eclipse-temurin:21-jre</image>
                                </from>
                                <to>
                                    <image>my-app:latest</image>
                                </to>
                            </configuration>
                        </plugin>
                    </plugins>
                </build>
            </project>
            """;
        Files.writeString(pomFile, pomContent);

        ValidationResult result = containerBuildService.validateJibConfig(tempDir.toString());

        assertTrue(result.isValid()); // Valid but with warnings
        assertFalse(result.getWarnings().isEmpty());
        assertTrue(result.getWarnings().stream().anyMatch(w -> w.contains("No main class specified")));
        assertTrue(result.getSuggestions().stream().anyMatch(s -> s.contains("Set main class")));
    }

    @Test
    void testValidateJibConfig_MissingTargetImage() throws IOException {
        // Create Gradle project with Jib but no target image
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
            }
            """;
        Files.writeString(buildFile, buildContent);

        ValidationResult result = containerBuildService.validateJibConfig(tempDir.toString());

        assertTrue(result.isValid()); // Valid but with warnings
        assertFalse(result.getWarnings().isEmpty());
        assertTrue(result.getWarnings().stream().anyMatch(w -> w.contains("Target image not specified")));
    }

    @Test
    void testBuildContainer_NonExistentPath() {
        BuildResult result = containerBuildService.buildContainer(
            "/non/existent/path", "docker", List.of("tag1", "tag2")
        );

        assertFalse(result.isSuccess());
        assertTrue(result.getBuildLog().contains("does not exist"));
        assertEquals(0, result.getExecutionTime());
    }

    @Test
    void testBuildContainer_UnknownBuildSystem() throws IOException {
        // Create directory without pom.xml or build.gradle
        BuildResult result = containerBuildService.buildContainer(
            tempDir.toString(), "docker", null
        );

        assertFalse(result.isSuccess());
        assertTrue(result.getBuildLog().contains("Could not detect build system"));
    }

    @Test
    void testBuildContainer_InvalidConfiguration() throws IOException {
        // Create Maven project without Jib plugin
        Path pomFile = tempDir.resolve("pom.xml");
        String pomContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0">
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.example</groupId>
                <artifactId>test-app</artifactId>
                <version>1.0.0</version>
            </project>
            """;
        Files.writeString(pomFile, pomContent);

        BuildResult result = containerBuildService.buildContainer(
            tempDir.toString(), "docker", null
        );

        assertFalse(result.isSuccess());
        assertTrue(result.getBuildLog().contains("Configuration validation failed"));
    }

    @Test
    void testValidateJibConfig_NonExistentPath() {
        ValidationResult result = containerBuildService.validateJibConfig("/non/existent/path");

        assertFalse(result.isValid());
        assertFalse(result.getErrors().isEmpty());
        assertTrue(result.getErrors().get(0).contains("Could not detect build system"));
    }

    @Test
    void testValidateJibConfig_UnknownBuildSystem() throws IOException {
        // Create directory without build files
        ValidationResult result = containerBuildService.validateJibConfig(tempDir.toString());

        assertFalse(result.isValid());
        assertFalse(result.getErrors().isEmpty());
        assertTrue(result.getErrors().get(0).contains("Could not detect build system"));
    }
}