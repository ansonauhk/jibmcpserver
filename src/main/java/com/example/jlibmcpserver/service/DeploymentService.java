package com.example.jlibmcpserver.service;

import com.example.jlibmcpserver.model.DeploymentResult;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

@Service
public class DeploymentService {

    public DeploymentResult deployToK3s(String projectPath, String namespace, boolean waitForReady) {
        try {
            Path path = Paths.get(projectPath);
            if (!Files.exists(path)) {
                return new DeploymentResult(false, null, null, "failed", 
                                          "Project path does not exist: " + projectPath);
            }

            // Find Helm chart
            Path chartPath = findHelmChart(path);
            if (chartPath == null) {
                return new DeploymentResult(false, null, null, "failed", 
                                          "No Helm chart found. Generate one first using generate_helm_chart");
            }

            // Check if k3s is running
            if (!isK3sRunning()) {
                return new DeploymentResult(false, null, null, "failed", 
                                          "k3s is not running. Please start k3s first");
            }

            // Create namespace if it doesn't exist
            createNamespaceIfNotExists(namespace);

            // Deploy using Helm
            String chartName = chartPath.getFileName().toString();
            String deploymentName = chartName + "-" + System.currentTimeMillis();
            
            boolean deploySuccess = deployHelmChart(chartPath, deploymentName, namespace);
            if (!deploySuccess) {
                return new DeploymentResult(false, deploymentName, null, "failed", 
                                          "Helm deployment failed");
            }

            // Wait for deployment to be ready if requested
            if (waitForReady) {
                boolean ready = waitForDeploymentReady(deploymentName, namespace, 300); // 5 minutes timeout
                if (!ready) {
                    return new DeploymentResult(false, deploymentName, null, "timeout", 
                                              "Deployment timed out waiting for ready status");
                }
            }

            // Get service URL
            String serviceUrl = getServiceUrl(deploymentName, namespace);

            return new DeploymentResult(true, deploymentName, serviceUrl, "ready", 
                                      "Deployment successful");

        } catch (Exception e) {
            return new DeploymentResult(false, null, null, "failed", 
                                      "Deployment failed: " + e.getMessage());
        }
    }

    public String getBuildStatus(String projectPath) {
        try {
            Path path = Paths.get(projectPath);
            
            // Look for build logs or status files
            Path buildLogPath = path.resolve("build.log");
            if (Files.exists(buildLogPath)) {
                // In a real implementation, this would parse the build log
                // and return structured status information
                return "Last build completed successfully";
            }
            
            return "No build history found";
            
        } catch (Exception e) {
            return "Failed to get build status: " + e.getMessage();
        }
    }

    private Path findHelmChart(Path projectPath) throws IOException {
        // Look for Helm charts in common directories
        Path[] possiblePaths = {
            projectPath.resolve("helm"),
            projectPath.resolve("charts"),
            projectPath.resolve("chart")
        };
        
        for (Path path : possiblePaths) {
            if (Files.exists(path) && Files.isDirectory(path)) {
                // Look for Chart.yaml to confirm it's a Helm chart
                try {
                    return Files.walk(path, 2)
                            .filter(Files::isDirectory)
                            .filter(dir -> Files.exists(dir.resolve("Chart.yaml")))
                            .findFirst()
                            .orElse(null);
                } catch (IOException e) {
                    continue;
                }
            }
        }
        
        return null;
    }

    private boolean isK3sRunning() {
        try {
            ProcessResult result = executeCommand("kubectl", "cluster-info");
            return result.exitCode == 0 && result.output.toLowerCase().contains("running");
        } catch (Exception e) {
            return false;
        }
    }

    private void createNamespaceIfNotExists(String namespace) throws IOException, InterruptedException {
        if (!"default".equals(namespace)) {
            // Check if namespace exists
            ProcessResult checkResult = executeCommand("kubectl", "get", "namespace", namespace);
            if (checkResult.exitCode != 0) {
                // Create namespace
                executeCommand("kubectl", "create", "namespace", namespace);
            }
        }
    }

    private boolean deployHelmChart(Path chartPath, String releaseName, String namespace) {
        try {
            // Upgrade or install Helm chart
            ProcessResult result = executeCommand("helm", "upgrade", "--install", 
                                                 releaseName, chartPath.toString(), 
                                                 "--namespace", namespace,
                                                 "--create-namespace");
            return result.exitCode == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean waitForDeploymentReady(String deploymentName, String namespace, int timeoutSeconds) {
        try {
            ProcessResult result = executeCommand("kubectl", "wait", 
                                                 "--for=condition=available", 
                                                 "deployment/" + deploymentName,
                                                 "--namespace", namespace,
                                                 "--timeout=" + timeoutSeconds + "s");
            return result.exitCode == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private String getServiceUrl(String deploymentName, String namespace) {
        try {
            // Get service information
            ProcessResult result = executeCommand("kubectl", "get", "service", 
                                                 "--namespace", namespace,
                                                 "-l", "app.kubernetes.io/instance=" + deploymentName,
                                                 "-o", "jsonpath={.items[0].spec.clusterIP}");
            
            if (result.exitCode == 0 && !result.output.trim().isEmpty()) {
                String clusterIP = result.output.trim();
                
                // Get service port
                ProcessResult portResult = executeCommand("kubectl", "get", "service", 
                                                        "--namespace", namespace,
                                                        "-l", "app.kubernetes.io/instance=" + deploymentName,
                                                        "-o", "jsonpath={.items[0].spec.ports[0].port}");
                
                if (portResult.exitCode == 0 && !portResult.output.trim().isEmpty()) {
                    String port = portResult.output.trim();
                    return "http://" + clusterIP + ":" + port;
                }
            }
            
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private ProcessResult executeCommand(String... command) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();
        
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }

        boolean finished = process.waitFor(30, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException("Command timed out: " + String.join(" ", command));
        }

        return new ProcessResult(process.exitValue(), output.toString());
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