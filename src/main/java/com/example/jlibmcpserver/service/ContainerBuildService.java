package com.example.jlibmcpserver.service;

import com.example.jlibmcpserver.model.BuildResult;
import com.example.jlibmcpserver.model.ValidationResult;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class ContainerBuildService {

    public BuildResult buildContainer(String projectPath, String buildTarget, List<String> tags) {
        long startTime = System.currentTimeMillis();
        
        try {
            Path path = Paths.get(projectPath);
            if (!Files.exists(path)) {
                return new BuildResult(false, null, "Project path does not exist: " + projectPath, 0);
            }

            String buildSystem = detectBuildSystem(path);
            if ("unknown".equals(buildSystem)) {
                return new BuildResult(false, null, "Could not detect build system (Maven or Gradle)", 0);
            }

            // Validate configuration before building
            ValidationResult validation = validateJibConfig(projectPath);
            if (!validation.isValid()) {
                String errorMsg = "Configuration validation failed: " + String.join(", ", validation.getErrors());
                return new BuildResult(false, null, errorMsg, 0);
            }

            // Execute build command
            String command = buildJibCommand(buildSystem, buildTarget, tags);
            ProcessResult result = executeCommand(path, command);
            
            long executionTime = System.currentTimeMillis() - startTime;
            
            if (result.exitCode == 0) {
                String imageId = extractImageId(result.output, buildTarget);
                return new BuildResult(true, imageId, result.output, executionTime);
            } else {
                return new BuildResult(false, null, result.output, executionTime);
            }

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            return new BuildResult(false, null, "Build failed: " + e.getMessage(), executionTime);
        }
    }

    public ValidationResult validateJibConfig(String projectPath) {
        try {
            Path path = Paths.get(projectPath);
            String buildSystem = detectBuildSystem(path);
            
            List<String> errors = new ArrayList<>();
            List<String> warnings = new ArrayList<>();
            List<String> suggestions = new ArrayList<>();

            if ("unknown".equals(buildSystem)) {
                errors.add("Could not detect build system (Maven or Gradle)");
                return new ValidationResult(false, errors, warnings, suggestions);
            }

            // Check if Jib plugin is configured
            boolean hasJibConfig = hasJibConfiguration(path, buildSystem);
            if (!hasJibConfig) {
                errors.add("Jib plugin is not configured. Run init_jib_config first.");
                return new ValidationResult(false, errors, warnings, suggestions);
            }

            // Check for common configuration issues
            validateCommonIssues(path, buildSystem, warnings, suggestions);

            boolean isValid = errors.isEmpty();
            return new ValidationResult(isValid, errors, warnings, suggestions);

        } catch (Exception e) {
            List<String> errors = List.of("Validation failed: " + e.getMessage());
            return new ValidationResult(false, errors, new ArrayList<>(), new ArrayList<>());
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

    private boolean hasJibConfiguration(Path projectPath, String buildSystem) {
        try {
            if ("maven".equals(buildSystem)) {
                Path pomPath = projectPath.resolve("pom.xml");
                if (Files.exists(pomPath)) {
                    String content = Files.readString(pomPath);
                    return content.contains("jib-maven-plugin");
                }
            } else if ("gradle".equals(buildSystem)) {
                Path gradlePath = projectPath.resolve("build.gradle");
                if (Files.exists(gradlePath)) {
                    String content = Files.readString(gradlePath);
                    return content.contains("jib") || content.contains("com.google.cloud.tools.jib");
                }
            }
        } catch (IOException e) {
            // Ignore and return false
        }
        return false;
    }

    private void validateCommonIssues(Path projectPath, String buildSystem, 
                                     List<String> warnings, List<String> suggestions) {
        try {
            if ("maven".equals(buildSystem)) {
                Path pomPath = projectPath.resolve("pom.xml");
                if (Files.exists(pomPath)) {
                    String content = Files.readString(pomPath);
                    
                    if (!content.contains("<mainClass>")) {
                        warnings.add("No main class specified in Jib configuration");
                        suggestions.add("Set main class using set_main_class tool");
                    }
                    
                    if (!content.contains("<image>") || content.contains("<image></image>")) {
                        warnings.add("Target image not specified");
                        suggestions.add("Set target image in Jib configuration");
                    }
                }
            } else if ("gradle".equals(buildSystem)) {
                Path gradlePath = projectPath.resolve("build.gradle");
                if (Files.exists(gradlePath)) {
                    String content = Files.readString(gradlePath);
                    
                    if (!content.contains("mainClass")) {
                        warnings.add("No main class specified in Jib configuration");
                        suggestions.add("Set main class using set_main_class tool");
                    }
                    
                    if (!content.contains("to {") || !content.contains("image =")) {
                        warnings.add("Target image not specified");
                        suggestions.add("Set target image in Jib configuration");
                    }
                }
            }
        } catch (IOException e) {
            warnings.add("Could not read build configuration file");
        }
    }

    private String buildJibCommand(String buildSystem, String buildTarget, List<String> tags) {
        StringBuilder command = new StringBuilder();
        
        if ("maven".equals(buildSystem)) {
            command.append("mvn compile jib:");
            switch (buildTarget) {
                case "registry" -> command.append("build");
                case "docker" -> command.append("dockerBuild");
                case "tar" -> command.append("tarBuild");
                default -> command.append("dockerBuild");
            }
        } else if ("gradle".equals(buildSystem)) {
            command.append("./gradlew ");
            switch (buildTarget) {
                case "registry" -> command.append("jib");
                case "docker" -> command.append("jibDockerBuild");
                case "tar" -> command.append("jibBuildTar");
                default -> command.append("jibDockerBuild");
            }
        }

        // Add tags if specified
        if (tags != null && !tags.isEmpty()) {
            for (String tag : tags) {
                if ("maven".equals(buildSystem)) {
                    command.append(" -Djib.to.tags=").append(tag);
                } else {
                    command.append(" -Djib.to.tags=").append(tag);
                }
            }
        }

        return command.toString();
    }

    private ProcessResult executeCommand(Path workingDirectory, String command) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder();
        
        // Split command for process builder
        String[] commandParts = command.split("\\s+");
        processBuilder.command(commandParts);
        processBuilder.directory(workingDirectory.toFile());
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();
        
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }

        boolean finished = process.waitFor(10, TimeUnit.MINUTES);
        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException("Build process timed out after 10 minutes");
        }

        return new ProcessResult(process.exitValue(), output.toString());
    }

    private String extractImageId(String buildOutput, String buildTarget) {
        // Extract image ID from build output
        String[] lines = buildOutput.split("\n");
        for (String line : lines) {
            if (line.contains("Built and pushed image as") || line.contains("Built image to Docker daemon as")) {
                // Extract image name/ID from the line
                String[] parts = line.split(" ");
                for (String part : parts) {
                    if (part.contains(":") && !part.startsWith("http")) {
                        return part;
                    }
                }
            }
        }
        return null; // Could not extract image ID
    }

    private static class ProcessResult {
        final int exitCode;
        final String output;

        ProcessResult(int exitCode, String output) {
            this.exitCode = exitCode;
            this.output = output;
        }
    }
}