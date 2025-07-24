package com.example.jlibmcpserver.service;

import com.example.jlibmcpserver.model.JibConfigResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ConfigurationManagementService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public Map<String, Object> exportJibConfig(String projectPath) {
        try {
            Path path = Paths.get(projectPath);
            if (!Files.exists(path)) {
                throw new IllegalArgumentException("Project path does not exist: " + projectPath);
            }

            String buildSystem = detectBuildSystem(path);
            Map<String, Object> config = new HashMap<>();
            
            if ("maven".equals(buildSystem)) {
                config = exportMavenJibConfig(path);
            } else if ("gradle".equals(buildSystem)) {
                config = exportGradleJibConfig(path);
            } else {
                throw new IllegalArgumentException("Unsupported build system: " + buildSystem);
            }

            // Add metadata
            Map<String, Object> result = new HashMap<>();
            result.put("config", config);
            result.put("build_system", buildSystem);
            result.put("export_timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            return result;

        } catch (Exception e) {
            throw new RuntimeException("Failed to export Jib config: " + e.getMessage(), e);
        }
    }

    public JibConfigResult importJibConfig(String projectPath, Map<String, Object> config) {
        try {
            Path path = Paths.get(projectPath);
            if (!Files.exists(path)) {
                return new JibConfigResult(false, null, "Project path does not exist: " + projectPath);
            }

            String buildSystem = detectBuildSystem(path);
            if ("unknown".equals(buildSystem)) {
                return new JibConfigResult(false, null, "Could not detect build system (Maven or Gradle)");
            }

            boolean success;
            if ("maven".equals(buildSystem)) {
                success = importMavenJibConfig(path, config);
            } else if ("gradle".equals(buildSystem)) {
                success = importGradleJibConfig(path, config);
            } else {
                return new JibConfigResult(false, null, "Unsupported build system: " + buildSystem);
            }

            String message = success ? "Jib configuration imported successfully" : "Failed to import Jib configuration";
            
            // Create a backup of the applied config
            try {
                String configJson = objectMapper.writeValueAsString(config);
                Path backupPath = path.resolve("jib-config-backup.json");
                Files.writeString(backupPath, configJson);
            } catch (JsonProcessingException e) {
                // Ignore backup failure
            }

            return new JibConfigResult(success, path.toString(), message);

        } catch (Exception e) {
            return new JibConfigResult(false, null, "Failed to import Jib config: " + e.getMessage());
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

    private Map<String, Object> exportMavenJibConfig(Path projectPath) throws IOException {
        Path pomPath = projectPath.resolve("pom.xml");
        if (!Files.exists(pomPath)) {
            return new HashMap<>();
        }

        String content = Files.readString(pomPath);
        Map<String, Object> config = new HashMap<>();

        // Extract Jib plugin configuration
        Pattern jibPluginPattern = Pattern.compile(
            "<plugin>\\s*<groupId>com\\.google\\.cloud\\.tools</groupId>\\s*<artifactId>jib-maven-plugin</artifactId>(.*?)</plugin>",
            Pattern.DOTALL
        );
        
        Matcher matcher = jibPluginPattern.matcher(content);
        if (matcher.find()) {
            String pluginContent = matcher.group(1);
            
            // Extract version
            String version = extractXmlValue(pluginContent, "version");
            if (version != null) {
                config.put("version", version);
            }

            // Extract configuration section
            Pattern configPattern = Pattern.compile("<configuration>(.*?)</configuration>", Pattern.DOTALL);
            Matcher configMatcher = configPattern.matcher(pluginContent);
            
            if (configMatcher.find()) {
                String configContent = configMatcher.group(1);
                
                // Extract from image
                Map<String, Object> fromConfig = new HashMap<>();
                String fromImage = extractXmlValue(configContent, "image");
                if (fromImage != null) {
                    fromConfig.put("image", fromImage);
                    config.put("from", fromConfig);
                }
                
                // Extract to image
                Map<String, Object> toConfig = new HashMap<>();
                String toImage = extractNestedXmlValue(configContent, "to", "image");
                if (toImage != null) {
                    toConfig.put("image", toImage);
                    config.put("to", toConfig);
                }
                
                // Extract container configuration
                Map<String, Object> containerConfig = extractContainerConfig(configContent);
                if (!containerConfig.isEmpty()) {
                    config.put("container", containerConfig);
                }
            }
        }

        return config;
    }

    private Map<String, Object> exportGradleJibConfig(Path projectPath) throws IOException {
        Path gradlePath = projectPath.resolve("build.gradle");
        if (!Files.exists(gradlePath)) {
            return new HashMap<>();
        }

        String content = Files.readString(gradlePath);
        Map<String, Object> config = new HashMap<>();

        // Extract Jib configuration block
        Pattern jibPattern = Pattern.compile("jib\\s*\\{([^}]+)\\}", Pattern.DOTALL);
        Matcher matcher = jibPattern.matcher(content);
        
        if (matcher.find()) {
            String jibContent = matcher.group(1);
            
            // Extract from configuration
            String fromImage = extractGradleValue(jibContent, "from", "image");
            if (fromImage != null) {
                Map<String, Object> fromConfig = new HashMap<>();
                fromConfig.put("image", fromImage.replaceAll("['\"]", ""));
                config.put("from", fromConfig);
            }
            
            // Extract to configuration
            String toImage = extractGradleValue(jibContent, "to", "image");
            if (toImage != null) {
                Map<String, Object> toConfig = new HashMap<>();
                toConfig.put("image", toImage.replaceAll("['\"]", ""));
                config.put("to", toConfig);
            }
            
            // Extract container configuration
            Map<String, Object> containerConfig = extractGradleContainerConfig(jibContent);
            if (!containerConfig.isEmpty()) {
                config.put("container", containerConfig);
            }
        }

        return config;
    }

    private boolean importMavenJibConfig(Path projectPath, Map<String, Object> config) {
        try {
            Path pomPath = projectPath.resolve("pom.xml");
            if (!Files.exists(pomPath)) {
                return false;
            }

            String content = Files.readString(pomPath);
            
            // Build new Jib configuration
            StringBuilder jibConfig = new StringBuilder();
            jibConfig.append("            <plugin>\n");
            jibConfig.append("                <groupId>com.google.cloud.tools</groupId>\n");
            jibConfig.append("                <artifactId>jib-maven-plugin</artifactId>\n");
            
            if (config.containsKey("version")) {
                jibConfig.append("                <version>").append(config.get("version")).append("</version>\n");
            } else {
                jibConfig.append("                <version>3.4.0</version>\n");
            }
            
            jibConfig.append("                <configuration>\n");
            
            // Add from configuration
            if (config.containsKey("from")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> fromConfig = (Map<String, Object>) config.get("from");
                jibConfig.append("                    <from>\n");
                if (fromConfig.containsKey("image")) {
                    jibConfig.append("                        <image>").append(fromConfig.get("image")).append("</image>\n");
                }
                jibConfig.append("                    </from>\n");
            }
            
            // Add to configuration
            if (config.containsKey("to")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> toConfig = (Map<String, Object>) config.get("to");
                jibConfig.append("                    <to>\n");
                if (toConfig.containsKey("image")) {
                    jibConfig.append("                        <image>").append(toConfig.get("image")).append("</image>\n");
                }
                jibConfig.append("                    </to>\n");
            }
            
            // Add container configuration
            if (config.containsKey("container")) {
                jibConfig.append(buildMavenContainerConfig(config.get("container")));
            }
            
            jibConfig.append("                </configuration>\n");
            jibConfig.append("            </plugin>");

            // Replace existing Jib plugin or add new one
            if (content.contains("jib-maven-plugin")) {
                // Replace existing configuration
                content = content.replaceAll(
                    "<plugin>\\s*<groupId>com\\.google\\.cloud\\.tools</groupId>\\s*<artifactId>jib-maven-plugin</artifactId>.*?</plugin>",
                    jibConfig.toString()
                );
            } else {
                // Add new plugin before </plugins>
                content = content.replace("</plugins>", jibConfig + "\n        </plugins>");
            }
            
            Files.writeString(pomPath, content);
            return true;

        } catch (Exception e) {
            return false;
        }
    }

    private boolean importGradleJibConfig(Path projectPath, Map<String, Object> config) {
        try {
            Path gradlePath = projectPath.resolve("build.gradle");
            if (!Files.exists(gradlePath)) {
                return false;
            }

            String content = Files.readString(gradlePath);
            
            // Build new Jib configuration
            StringBuilder jibConfig = new StringBuilder();
            jibConfig.append("jib {\n");
            
            // Add from configuration
            if (config.containsKey("from")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> fromConfig = (Map<String, Object>) config.get("from");
                jibConfig.append("    from {\n");
                if (fromConfig.containsKey("image")) {
                    jibConfig.append("        image = '").append(fromConfig.get("image")).append("'\n");
                }
                jibConfig.append("    }\n");
            }
            
            // Add to configuration
            if (config.containsKey("to")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> toConfig = (Map<String, Object>) config.get("to");
                jibConfig.append("    to {\n");
                if (toConfig.containsKey("image")) {
                    jibConfig.append("        image = '").append(toConfig.get("image")).append("'\n");
                }
                jibConfig.append("    }\n");
            }
            
            // Add container configuration
            if (config.containsKey("container")) {
                jibConfig.append(buildGradleContainerConfig(config.get("container")));
            }
            
            jibConfig.append("}");

            // Replace existing Jib configuration or add new one
            if (content.contains("jib {")) {
                content = content.replaceAll("jib\\s*\\{[^}]*\\}", jibConfig.toString());
            } else {
                content += "\n\n" + jibConfig;
            }
            
            Files.writeString(gradlePath, content);
            return true;

        } catch (Exception e) {
            return false;
        }
    }

    // Helper methods for XML parsing
    private String extractXmlValue(String content, String tag) {
        Pattern pattern = Pattern.compile("<" + tag + ">([^<]+)</" + tag + ">");
        Matcher matcher = pattern.matcher(content);
        return matcher.find() ? matcher.group(1) : null;
    }

    private String extractNestedXmlValue(String content, String parentTag, String childTag) {
        Pattern parentPattern = Pattern.compile("<" + parentTag + ">(.*?)</" + parentTag + ">", Pattern.DOTALL);
        Matcher parentMatcher = parentPattern.matcher(content);
        if (parentMatcher.find()) {
            return extractXmlValue(parentMatcher.group(1), childTag);
        }
        return null;
    }

    private Map<String, Object> extractContainerConfig(String content) {
        Map<String, Object> containerConfig = new HashMap<>();
        
        Pattern containerPattern = Pattern.compile("<container>(.*?)</container>", Pattern.DOTALL);
        Matcher matcher = containerPattern.matcher(content);
        
        if (matcher.find()) {
            String containerContent = matcher.group(1);
            
            String mainClass = extractXmlValue(containerContent, "mainClass");
            if (mainClass != null) {
                containerConfig.put("mainClass", mainClass);
            }
        }
        
        return containerConfig;
    }

    // Helper methods for Gradle parsing
    private String extractGradleValue(String content, String section, String key) {
        Pattern sectionPattern = Pattern.compile(section + "\\s*\\{([^}]+)\\}");
        Matcher sectionMatcher = sectionPattern.matcher(content);
        if (sectionMatcher.find()) {
            String sectionContent = sectionMatcher.group(1);
            Pattern keyPattern = Pattern.compile(key + "\\s*=\\s*(['\"][^'\"]*['\"])");
            Matcher keyMatcher = keyPattern.matcher(sectionContent);
            if (keyMatcher.find()) {
                return keyMatcher.group(1);
            }
        }
        return null;
    }

    private Map<String, Object> extractGradleContainerConfig(String content) {
        Map<String, Object> containerConfig = new HashMap<>();
        
        Pattern containerPattern = Pattern.compile("container\\s*\\{([^}]+)\\}");
        Matcher matcher = containerPattern.matcher(content);
        
        if (matcher.find()) {
            String containerContent = matcher.group(1);
            
            Pattern mainClassPattern = Pattern.compile("mainClass\\s*=\\s*['\"]([^'\"]+)['\"]");
            Matcher mainClassMatcher = mainClassPattern.matcher(containerContent);
            if (mainClassMatcher.find()) {
                containerConfig.put("mainClass", mainClassMatcher.group(1));
            }
        }
        
        return containerConfig;
    }

    private String buildMavenContainerConfig(Object containerConfig) {
        StringBuilder config = new StringBuilder();
        config.append("                    <container>\n");
        
        if (containerConfig instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> container = (Map<String, Object>) containerConfig;
            
            if (container.containsKey("mainClass")) {
                config.append("                        <mainClass>").append(container.get("mainClass")).append("</mainClass>\n");
            }
        }
        
        config.append("                    </container>\n");
        return config.toString();
    }

    private String buildGradleContainerConfig(Object containerConfig) {
        StringBuilder config = new StringBuilder();
        config.append("    container {\n");
        
        if (containerConfig instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> container = (Map<String, Object>) containerConfig;
            
            if (container.containsKey("mainClass")) {
                config.append("        mainClass = '").append(container.get("mainClass")).append("'\n");
            }
        }
        
        config.append("    }\n");
        return config.toString();
    }
}