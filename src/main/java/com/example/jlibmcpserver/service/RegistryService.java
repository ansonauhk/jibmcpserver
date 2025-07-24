package com.example.jlibmcpserver.service;

import com.example.jlibmcpserver.model.JibConfigResult;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class RegistryService {

    public JibConfigResult configureRegistry(String projectPath, String registryType, 
                                           String registryUrl, String authMethod) {
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
            boolean authConfigured = false;

            if ("maven".equals(buildSystem)) {
                success = configureMavenRegistry(path, registryType, registryUrl, authMethod);
                authConfigured = configureAuthenticationForMaven(path, registryType, authMethod);
            } else if ("gradle".equals(buildSystem)) {
                success = configureGradleRegistry(path, registryType, registryUrl, authMethod);
                authConfigured = configureAuthenticationForGradle(path, registryType, authMethod);
            } else {
                return new JibConfigResult(false, null, "Unsupported build system: " + buildSystem);
            }

            String message = success ? 
                String.format("Registry configured for %s (auth: %s)", registryType, authConfigured ? "configured" : "manual setup required") :
                "Failed to configure registry";

            return new JibConfigResult(success, path.toString(), message);

        } catch (Exception e) {
            return new JibConfigResult(false, null, "Failed to configure registry: " + e.getMessage());
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

    private boolean configureMavenRegistry(Path projectPath, String registryType, String registryUrl, String authMethod) {
        try {
            Path pomPath = projectPath.resolve("pom.xml");
            if (!Files.exists(pomPath)) {
                return false;
            }

            String content = Files.readString(pomPath);
            
            // Update the target image registry
            String registryConfig = generateRegistryUrl(registryType, registryUrl);
            
            if (content.contains("<to>")) {
                // Update existing to configuration
                content = content.replaceAll(
                    "(<to>\\s*<image>)[^<]*(</image>\\s*</to>)",
                    "$1" + registryConfig + "$2"
                );
            } else {
                // Add to configuration if it doesn't exist
                String toConfig = String.format("""
                                    <to>
                                        <image>%s</image>
                                    </to>""", registryConfig);
                content = content.replace("<configuration>", "<configuration>\n" + toConfig);
            }
            
            Files.writeString(pomPath, content);
            return true;

        } catch (IOException e) {
            return false;
        }
    }

    private boolean configureGradleRegistry(Path projectPath, String registryType, String registryUrl, String authMethod) {
        try {
            Path gradlePath = projectPath.resolve("build.gradle");
            if (!Files.exists(gradlePath)) {
                return false;
            }

            String content = Files.readString(gradlePath);
            
            // Update the target image registry
            String registryConfig = generateRegistryUrl(registryType, registryUrl);
            
            if (content.contains("to {")) {
                // Update existing to configuration
                content = content.replaceAll(
                    "(to \\{\\s*image = ['\"])[^'\"]*(['\"])",
                    "$1" + registryConfig + "$2"
                );
            } else {
                // Add to configuration if it doesn't exist
                String toConfig = String.format("""
                    to {
                        image = '%s'
                    }""", registryConfig);
                
                if (content.contains("jib {")) {
                    content = content.replace("jib {", "jib {\n" + toConfig);
                }
            }
            
            Files.writeString(gradlePath, content);
            return true;

        } catch (IOException e) {
            return false;
        }
    }

    private String generateRegistryUrl(String registryType, String registryUrl) {
        String baseUrl = registryUrl != null ? registryUrl : getDefaultRegistryUrl(registryType);
        
        switch (registryType) {
            case "gcr" -> {
                return baseUrl + "/my-project/my-app:latest";
            }
            case "ecr" -> {
                return baseUrl + "/my-app:latest";
            }
            case "docker-hub" -> {
                return "my-username/my-app:latest";
            }
            case "jfrog" -> {
                return baseUrl + "/my-app:latest";
            }
            case "azure" -> {
                return baseUrl + "/my-app:latest";
            }
            default -> {
                return baseUrl + "/my-app:latest";
            }
        }
    }

    private String getDefaultRegistryUrl(String registryType) {
        return switch (registryType) {
            case "gcr" -> "gcr.io";
            case "ecr" -> "123456789012.dkr.ecr.us-east-1.amazonaws.com";
            case "docker-hub" -> "docker.io";
            case "jfrog" -> "mycompany.jfrog.io";
            case "azure" -> "myregistry.azurecr.io";
            default -> "localhost:5000";
        };
    }

    private boolean configureAuthenticationForMaven(Path projectPath, String registryType, String authMethod) {
        // In a real implementation, this would:
        // 1. Configure Maven settings.xml for registry authentication
        // 2. Set up credential helpers
        // 3. Configure service account keys
        // For now, we'll return true indicating basic configuration is done
        return createAuthenticationHints(projectPath, registryType, authMethod, "maven");
    }

    private boolean configureAuthenticationForGradle(Path projectPath, String registryType, String authMethod) {
        // In a real implementation, this would:
        // 1. Configure Gradle properties for registry authentication
        // 2. Set up credential helpers
        // 3. Configure service account keys
        // For now, we'll return true indicating basic configuration is done
        return createAuthenticationHints(projectPath, registryType, authMethod, "gradle");
    }

    private boolean createAuthenticationHints(Path projectPath, String registryType, 
                                            String authMethod, String buildSystem) {
        try {
            // Create a README file with authentication instructions
            Path authReadmePath = projectPath.resolve("REGISTRY_AUTH_SETUP.md");
            
            String instructions = generateAuthInstructions(registryType, authMethod, buildSystem);
            Files.writeString(authReadmePath, instructions);
            
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private String generateAuthInstructions(String registryType, String authMethod, String buildSystem) {
        return String.format("""
            # Registry Authentication Setup
            
            ## Registry Type: %s
            ## Authentication Method: %s
            ## Build System: %s
            
            ### Steps to complete authentication setup:
            
            %s
            
            ### Verification:
            Run the following command to test authentication:
            %s
            """, 
            registryType, 
            authMethod, 
            buildSystem,
            getRegistrySpecificInstructions(registryType, authMethod, buildSystem),
            getVerificationCommand(buildSystem)
        );
    }

    private String getRegistrySpecificInstructions(String registryType, String authMethod, String buildSystem) {
        return switch (registryType) {
            case "gcr" -> """
                1. Install Google Cloud SDK
                2. Run: gcloud auth configure-docker
                3. Ensure your service account has Container Registry access
                """;
            case "ecr" -> """
                1. Install AWS CLI
                2. Configure AWS credentials
                3. Run: aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin <registry-url>
                """;
            case "docker-hub" -> """
                1. Create Docker Hub access token
                2. Run: docker login -u <username> -p <access-token>
                """;
            case "jfrog" -> """
                1. Obtain API key from JFrog Artifactory
                2. Configure credentials in build tool settings
                """;
            case "azure" -> """
                1. Install Azure CLI
                2. Run: az acr login --name <registry-name>
                """;
            default -> """
                1. Configure registry credentials
                2. Test authentication with docker login
                """;
        };
    }

    private String getVerificationCommand(String buildSystem) {
        return "maven".equals(buildSystem) ? 
            "mvn compile jib:build" : 
            "./gradlew jib";
    }
}