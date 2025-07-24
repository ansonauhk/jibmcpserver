package com.example.jlibmcpserver.service;

import com.example.jlibmcpserver.model.MainClassDetectionResult;
import com.example.jlibmcpserver.model.ProjectAnalysisResult;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Service
public class ProjectAnalysisService {

    public ProjectAnalysisResult analyzeProject(String projectPath) {
        try {
            Path path = Paths.get(projectPath);
            if (!Files.exists(path)) {
                throw new IllegalArgumentException("Project path does not exist: " + projectPath);
            }

            String buildSystem = detectBuildSystem(path);
            String projectType = detectProjectType(path);
            String mainClass = detectMainClass(path);
            boolean existingJibConfig = hasExistingJibConfig(path, buildSystem);
            List<String> dependencies = extractDependencies(path, buildSystem);
            String javaVersion = detectJavaVersion(path, buildSystem);

            return new ProjectAnalysisResult(buildSystem, projectType, mainClass, 
                                           existingJibConfig, dependencies, javaVersion);
        } catch (Exception e) {
            throw new RuntimeException("Failed to analyze project: " + e.getMessage(), e);
        }
    }

    public MainClassDetectionResult detectMainClasses(String projectPath) {
        try {
            Path path = Paths.get(projectPath);
            List<String> mainClasses = findMainClasses(path);
            String recommended = mainClasses.isEmpty() ? null : mainClasses.get(0);
            
            return new MainClassDetectionResult(mainClasses, recommended);
        } catch (Exception e) {
            throw new RuntimeException("Failed to detect main classes: " + e.getMessage(), e);
        }
    }

    private String detectBuildSystem(Path projectPath) throws IOException {
        if (Files.exists(projectPath.resolve("pom.xml"))) {
            return "maven";
        } else if (Files.exists(projectPath.resolve("build.gradle")) || 
                   Files.exists(projectPath.resolve("build.gradle.kts"))) {
            return "gradle";
        }
        return "unknown";
    }

    private String detectProjectType(Path projectPath) throws IOException {
        // Check for multi-module structure
        try (Stream<Path> paths = Files.walk(projectPath, 2)) {
            long subModules = paths
                .filter(Files::isDirectory)
                .filter(p -> !p.equals(projectPath))
                .filter(p -> Files.exists(p.resolve("pom.xml")) || 
                           Files.exists(p.resolve("build.gradle")) ||
                           Files.exists(p.resolve("build.gradle.kts")))
                .count();
            
            return subModules > 0 ? "multi-module" : "single-module";
        }
    }

    private String detectMainClass(Path projectPath) {
        List<String> mainClasses = findMainClasses(projectPath);
        return mainClasses.isEmpty() ? null : mainClasses.get(0);
    }

    private List<String> findMainClasses(Path projectPath) {
        List<String> mainClasses = new ArrayList<>();
        
        try {
            // Look for Java files with main method
            Files.walk(projectPath)
                .filter(path -> path.toString().endsWith(".java"))
                .forEach(javaFile -> {
                    try {
                        String content = Files.readString(javaFile);
                        if (content.contains("public static void main(String")) {
                            String className = extractClassName(javaFile, content);
                            if (className != null) {
                                mainClasses.add(className);
                            }
                        }
                    } catch (IOException e) {
                        // Ignore files that can't be read
                    }
                });
        } catch (IOException e) {
            // Return empty list if there's an error
        }
        
        return mainClasses;
    }

    private String extractClassName(Path javaFile, String content) {
        // Extract package name
        Pattern packagePattern = Pattern.compile("package\\s+([^;]+);");
        Matcher packageMatcher = packagePattern.matcher(content);
        String packageName = packageMatcher.find() ? packageMatcher.group(1).trim() : "";
        
        // Extract class name from file name
        String fileName = javaFile.getFileName().toString();
        String className = fileName.substring(0, fileName.lastIndexOf(".java"));
        
        return packageName.isEmpty() ? className : packageName + "." + className;
    }

    private boolean hasExistingJibConfig(Path projectPath, String buildSystem) {
        try {
            if ("maven".equals(buildSystem)) {
                Path pomPath = projectPath.resolve("pom.xml");
                if (Files.exists(pomPath)) {
                    String content = Files.readString(pomPath);
                    return content.contains("jib-maven-plugin");
                }
            } else if ("gradle".equals(buildSystem)) {
                Path gradlePath = projectPath.resolve("build.gradle");
                Path gradleKtsPath = projectPath.resolve("build.gradle.kts");
                
                if (Files.exists(gradlePath)) {
                    String content = Files.readString(gradlePath);
                    return content.contains("jib") || content.contains("com.google.cloud.tools.jib");
                }
                if (Files.exists(gradleKtsPath)) {
                    String content = Files.readString(gradleKtsPath);
                    return content.contains("jib") || content.contains("com.google.cloud.tools.jib");
                }
            }
        } catch (IOException e) {
            // Ignore and return false
        }
        return false;
    }

    private List<String> extractDependencies(Path projectPath, String buildSystem) {
        List<String> dependencies = new ArrayList<>();
        
        try {
            if ("maven".equals(buildSystem)) {
                Path pomPath = projectPath.resolve("pom.xml");
                if (Files.exists(pomPath)) {
                    String content = Files.readString(pomPath);
                    // Simple regex to extract artifact IDs
                    Pattern pattern = Pattern.compile("<artifactId>([^<]+)</artifactId>");
                    Matcher matcher = pattern.matcher(content);
                    while (matcher.find()) {
                        dependencies.add(matcher.group(1));
                    }
                }
            } else if ("gradle".equals(buildSystem)) {
                Path gradlePath = projectPath.resolve("build.gradle");
                if (Files.exists(gradlePath)) {
                    String content = Files.readString(gradlePath);
                    // Simple regex to extract dependencies
                    Pattern pattern = Pattern.compile("implementation\\s+['\"]([^'\"]+)['\"]");
                    Matcher matcher = pattern.matcher(content);
                    while (matcher.find()) {
                        dependencies.add(matcher.group(1));
                    }
                }
            }
        } catch (IOException e) {
            // Ignore and return empty list
        }
        
        return dependencies;
    }

    private String detectJavaVersion(Path projectPath, String buildSystem) {
        try {
            if ("maven".equals(buildSystem)) {
                Path pomPath = projectPath.resolve("pom.xml");
                if (Files.exists(pomPath)) {
                    String content = Files.readString(pomPath);
                    // Look for maven.compiler.source or maven.compiler.target
                    Pattern pattern = Pattern.compile("<maven\\.compiler\\.(?:source|target)>([^<]+)</maven\\.compiler\\.(?:source|target)>");
                    Matcher matcher = pattern.matcher(content);
                    if (matcher.find()) {
                        return matcher.group(1);
                    }
                }
            } else if ("gradle".equals(buildSystem)) {
                Path gradlePath = projectPath.resolve("build.gradle");
                if (Files.exists(gradlePath)) {
                    String content = Files.readString(gradlePath);
                    // Look for targetCompatibility or sourceCompatibility
                    Pattern pattern = Pattern.compile("(?:target|source)Compatibility\\s*=\\s*['\"]?([^'\"\\s]+)['\"]?");
                    Matcher matcher = pattern.matcher(content);
                    if (matcher.find()) {
                        return matcher.group(1);
                    }
                    
                    // Look for toolchain
                    Pattern toolchainPattern = Pattern.compile("languageVersion\\s*=\\s*JavaLanguageVersion\\.of\\((\\d+)\\)");
                    Matcher toolchainMatcher = toolchainPattern.matcher(content);
                    if (toolchainMatcher.find()) {
                        return toolchainMatcher.group(1);
                    }
                }
            }
        } catch (IOException e) {
            // Ignore and return default
        }
        
        return "11"; // Default Java version
    }
}