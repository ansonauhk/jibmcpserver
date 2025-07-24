package com.example.jlibmcpserver.service;

import com.example.jlibmcpserver.model.MainClassDetectionResult;
import com.example.jlibmcpserver.model.ProjectAnalysisResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ProjectAnalysisServiceTest {

    private ProjectAnalysisService projectAnalysisService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        projectAnalysisService = new ProjectAnalysisService();
    }

    @Test
    void testAnalyzeProject_MavenProject() throws IOException {
        // Create a Maven project structure
        Path pomFile = tempDir.resolve("pom.xml");
        String pomContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0">
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.example</groupId>
                <artifactId>test-app</artifactId>
                <version>1.0.0</version>
                
                <properties>
                    <maven.compiler.source>11</maven.compiler.source>
                    <maven.compiler.target>11</maven.compiler.target>
                </properties>
                
                <dependencies>
                    <dependency>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-starter</artifactId>
                        <version>3.0.0</version>
                    </dependency>
                </dependencies>
            </project>
            """;
        Files.writeString(pomFile, pomContent);

        // Create main class
        Path srcDir = tempDir.resolve("src/main/java/com/example");
        Files.createDirectories(srcDir);
        Path mainClass = srcDir.resolve("Application.java");
        String mainClassContent = """
            package com.example;
            
            public class Application {
                public static void main(String[] args) {
                    System.out.println("Hello World");
                }
            }
            """;
        Files.writeString(mainClass, mainClassContent);

        ProjectAnalysisResult result = projectAnalysisService.analyzeProject(tempDir.toString());

        assertNotNull(result);
        assertEquals("maven", result.getBuildSystem());
        assertEquals("single-module", result.getProjectType());
        assertEquals("com.example.Application", result.getMainClass());
        assertEquals("11", result.getJavaVersion());
        assertFalse(result.isExistingJibConfig());
        assertTrue(result.getDependencies().contains("spring-boot-starter"));
    }

    @Test
    void testAnalyzeProject_GradleProject() throws IOException {
        // Create a Gradle project structure
        Path buildFile = tempDir.resolve("build.gradle");
        String buildContent = """
            plugins {
                id 'java'
                id 'org.springframework.boot' version '3.0.0'
            }
            
            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(17)
                }
            }
            
            dependencies {
                implementation 'org.springframework.boot:spring-boot-starter'
                implementation 'org.springframework.boot:spring-boot-starter-web'
            }
            """;
        Files.writeString(buildFile, buildContent);

        ProjectAnalysisResult result = projectAnalysisService.analyzeProject(tempDir.toString());

        assertNotNull(result);
        assertEquals("gradle", result.getBuildSystem());
        assertEquals("single-module", result.getProjectType());
        assertEquals("17", result.getJavaVersion());
        assertFalse(result.isExistingJibConfig());
    }

    @Test
    void testAnalyzeProject_WithJibConfiguration() throws IOException {
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

        ProjectAnalysisResult result = projectAnalysisService.analyzeProject(tempDir.toString());

        assertNotNull(result);
        assertEquals("maven", result.getBuildSystem());
        assertTrue(result.isExistingJibConfig());
    }

    @Test
    void testDetectMainClasses_MultipleClasses() throws IOException {
        // Create multiple classes with main methods
        Path srcDir = tempDir.resolve("src/main/java/com/example");
        Files.createDirectories(srcDir);

        Path mainClass1 = srcDir.resolve("Application.java");
        String mainClass1Content = """
            package com.example;
            
            public class Application {
                public static void main(String[] args) {
                    System.out.println("Main Application");
                }
            }
            """;
        Files.writeString(mainClass1, mainClass1Content);

        Path mainClass2 = srcDir.resolve("TestRunner.java");
        String mainClass2Content = """
            package com.example;
            
            public class TestRunner {
                public static void main(String[] args) {
                    System.out.println("Test Runner");
                }
            }
            """;
        Files.writeString(mainClass2, mainClass2Content);

        MainClassDetectionResult result = projectAnalysisService.detectMainClasses(tempDir.toString());

        assertNotNull(result);
        assertEquals(2, result.getMainClasses().size());
        assertTrue(result.getMainClasses().contains("com.example.Application"));
        assertTrue(result.getMainClasses().contains("com.example.TestRunner"));
        assertNotNull(result.getRecommendedMainClass());
    }

    @Test
    void testAnalyzeProject_NonExistentPath() {
        assertThrows(RuntimeException.class, () -> {
            projectAnalysisService.analyzeProject("/non/existent/path");
        });
    }

    @Test
    void testDetectMainClasses_NoMainMethods() throws IOException {
        // Create class without main method
        Path srcDir = tempDir.resolve("src/main/java/com/example");
        Files.createDirectories(srcDir);

        Path utilClass = srcDir.resolve("Utils.java");
        String utilClassContent = """
            package com.example;
            
            public class Utils {
                public void doSomething() {
                    System.out.println("Utility method");
                }
            }
            """;
        Files.writeString(utilClass, utilClassContent);

        MainClassDetectionResult result = projectAnalysisService.detectMainClasses(tempDir.toString());

        assertNotNull(result);
        assertTrue(result.getMainClasses().isEmpty());
        assertNull(result.getRecommendedMainClass());
    }
}