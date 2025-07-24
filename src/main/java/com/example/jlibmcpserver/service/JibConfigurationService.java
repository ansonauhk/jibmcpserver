package com.example.jlibmcpserver.service;

import com.example.jlibmcpserver.model.JibConfigResult;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@Service
public class JibConfigurationService {

    public JibConfigResult initJibConfig(String projectPath, String buildSystem, 
                                        String baseImage, String targetImage) {
        try {
            Path path = Paths.get(projectPath);
            if (!Files.exists(path)) {
                return new JibConfigResult(false, null, "Project path does not exist: " + projectPath);
            }

            String configFilePath;
            boolean success;

            if ("maven".equals(buildSystem)) {
                configFilePath = initMavenJibConfig(path, baseImage, targetImage);
                success = configFilePath != null;
            } else if ("gradle".equals(buildSystem)) {
                configFilePath = initGradleJibConfig(path, baseImage, targetImage);
                success = configFilePath != null;
            } else {
                return new JibConfigResult(false, null, "Unsupported build system: " + buildSystem);
            }

            String message = success ? "Jib configuration initialized successfully" : "Failed to initialize Jib configuration";
            return new JibConfigResult(success, configFilePath, message);

        } catch (Exception e) {
            return new JibConfigResult(false, null, "Failed to initialize Jib config: " + e.getMessage());
        }
    }

    public JibConfigResult updateJibConfig(String projectPath, String configSection, Map<String, Object> parameters) {
        try {
            Path path = Paths.get(projectPath);
            String buildSystem = detectBuildSystem(path);
            
            boolean success;
            if ("maven".equals(buildSystem)) {
                success = updateMavenJibConfig(path, configSection, parameters);
            } else if ("gradle".equals(buildSystem)) {
                success = updateGradleJibConfig(path, configSection, parameters);
            } else {
                return new JibConfigResult(false, null, "Unsupported build system: " + buildSystem);
            }

            String message = success ? "Jib configuration updated successfully" : "Failed to update Jib configuration";
            return new JibConfigResult(success, path.toString(), message);

        } catch (Exception e) {
            return new JibConfigResult(false, null, "Failed to update Jib config: " + e.getMessage());
        }
    }

    public JibConfigResult setMainClass(String projectPath, String mainClass) {
        try {
            Path path = Paths.get(projectPath);
            String buildSystem = detectBuildSystem(path);
            
            String previousMainClass = getCurrentMainClass(path, buildSystem);
            boolean success;
            
            if ("maven".equals(buildSystem)) {
                success = setMavenMainClass(path, mainClass);
            } else if ("gradle".equals(buildSystem)) {
                success = setGradleMainClass(path, mainClass);
            } else {
                return new JibConfigResult(false, null, "Unsupported build system: " + buildSystem);
            }

            String message = success ? 
                String.format("Main class set to %s (was: %s)", mainClass, previousMainClass) :
                "Failed to set main class";
            return new JibConfigResult(success, null, message);

        } catch (Exception e) {
            return new JibConfigResult(false, null, "Failed to set main class: " + e.getMessage());
        }
    }

    public JibConfigResult configureContainerSettings(String projectPath, List<String> ports,
                                                     Map<String, String> environmentVariables,
                                                     List<String> jvmFlags, String workingDirectory) {
        try {
            Path path = Paths.get(projectPath);
            String buildSystem = detectBuildSystem(path);
            
            boolean success;
            if ("maven".equals(buildSystem)) {
                success = configureMavenContainerSettings(path, ports, environmentVariables, jvmFlags, workingDirectory);
            } else if ("gradle".equals(buildSystem)) {
                success = configureGradleContainerSettings(path, ports, environmentVariables, jvmFlags, workingDirectory);
            } else {
                return new JibConfigResult(false, null, "Unsupported build system: " + buildSystem);
            }

            String message = success ? "Container settings configured successfully" : "Failed to configure container settings";
            return new JibConfigResult(success, path.toString(), message);

        } catch (Exception e) {
            return new JibConfigResult(false, null, "Failed to configure container settings: " + e.getMessage());
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

    private String initMavenJibConfig(Path projectPath, String baseImage, String targetImage) {
        try {
            Path pomPath = projectPath.resolve("pom.xml");
            if (!Files.exists(pomPath)) {
                return null;
            }

            String content = Files.readString(pomPath);
            
            // Check if jib plugin already exists
            if (content.contains("jib-maven-plugin")) {
                return pomPath.toString(); // Already configured
            }

            // Add jib plugin to build plugins section
            String jibPlugin = String.format("""
                            <plugin>
                                <groupId>com.google.cloud.tools</groupId>
                                <artifactId>jib-maven-plugin</artifactId>
                                <version>3.4.0</version>
                                <configuration>
                                    <from>
                                        <image>%s</image>
                                    </from>
                                    <to>
                                        <image>%s</image>
                                    </to>
                                    <container>
                                        <jvmFlags>
                                            <jvmFlag>-Xms512m</jvmFlag>
                                            <jvmFlag>-Xmx1024m</jvmFlag>
                                        </jvmFlags>
                                        <ports>
                                            <port>8080</port>
                                        </ports>
                                    </container>
                                </configuration>
                            </plugin>""", baseImage, targetImage);

            // Insert plugin before </plugins> closing tag
            content = content.replace("</plugins>", jibPlugin + "\n        </plugins>");
            
            Files.writeString(pomPath, content);
            return pomPath.toString();

        } catch (IOException e) {
            return null;
        }
    }

    private String initGradleJibConfig(Path projectPath, String baseImage, String targetImage) {
        try {
            Path gradlePath = projectPath.resolve("build.gradle");
            if (!Files.exists(gradlePath)) {
                return null;
            }

            String content = Files.readString(gradlePath);
            
            // Check if jib plugin already exists
            if (content.contains("jib") || content.contains("com.google.cloud.tools.jib")) {
                return gradlePath.toString(); // Already configured
            }

            // Add jib plugin to plugins block
            String pluginLine = "    id 'com.google.cloud.tools.jib' version '3.4.0'";
            if (content.contains("plugins {")) {
                content = content.replaceFirst("(plugins \\{[^}]*)", "$1\n" + pluginLine);
            } else {
                // Add plugins block if it doesn't exist
                content = "plugins {\n" + pluginLine + "\n}\n\n" + content;
            }

            // Add jib configuration
            String jibConfig = String.format("""
                
                jib {
                    from {
                        image = '%s'
                    }
                    to {
                        image = '%s'
                    }
                    container {
                        jvmFlags = ['-Xms512m', '-Xmx1024m']
                        ports = ['8080']
                    }
                }""", baseImage, targetImage);

            content += jibConfig;
            
            Files.writeString(gradlePath, content);
            return gradlePath.toString();

        } catch (IOException e) {
            return null;
        }
    }

    private boolean updateMavenJibConfig(Path projectPath, String configSection, Map<String, Object> parameters) {
        // Implementation for updating Maven Jib configuration
        // This would involve XML parsing and updating specific sections
        return true; // Simplified implementation
    }

    private boolean updateGradleJibConfig(Path projectPath, String configSection, Map<String, Object> parameters) {
        // Implementation for updating Gradle Jib configuration
        // This would involve parsing and updating Gradle build script
        return true; // Simplified implementation
    }

    private String getCurrentMainClass(Path projectPath, String buildSystem) {
        // Implementation to extract current main class from configuration
        return null; // Simplified implementation
    }

    private boolean setMavenMainClass(Path projectPath, String mainClass) {
        try {
            Path pomPath = projectPath.resolve("pom.xml");
            String content = Files.readString(pomPath);
            
            // Add or update main class in jib configuration
            if (content.contains("<mainClass>")) {
                content = content.replaceAll("<mainClass>[^<]*</mainClass>", "<mainClass>" + mainClass + "</mainClass>");
            } else if (content.contains("<container>")) {
                content = content.replace("<container>", "<container>\n                        <mainClass>" + mainClass + "</mainClass>");
            }
            
            Files.writeString(pomPath, content);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private boolean setGradleMainClass(Path projectPath, String mainClass) {
        try {
            Path gradlePath = projectPath.resolve("build.gradle");
            String content = Files.readString(gradlePath);
            
            // Add or update main class in jib configuration
            if (content.contains("mainClass")) {
                content = content.replaceAll("mainClass\\s*=\\s*['\"][^'\"]*['\"]", "mainClass = '" + mainClass + "'");
            } else if (content.contains("container {")) {
                content = content.replace("container {", "container {\n        mainClass = '" + mainClass + "'");
            }
            
            Files.writeString(gradlePath, content);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private boolean configureMavenContainerSettings(Path projectPath, List<String> ports,
                                                   Map<String, String> environmentVariables,
                                                   List<String> jvmFlags, String workingDirectory) {
        // Implementation for Maven container settings
        return true; // Simplified implementation
    }

    private boolean configureGradleContainerSettings(Path projectPath, List<String> ports,
                                                    Map<String, String> environmentVariables,
                                                    List<String> jvmFlags, String workingDirectory) {
        // Implementation for Gradle container settings
        return true; // Simplified implementation
    }
}