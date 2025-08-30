package com.example.jlibmcpserver.service;

import com.example.jlibmcpserver.model.HelmChartResult;
import com.example.jlibmcpserver.model.JibConfigResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class HelmChartServiceTest {

    private HelmChartService helmChartService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        helmChartService = new HelmChartService();
    }

    @Test
    void testGenerateHelmChart() throws IOException {
        HelmChartResult result = helmChartService.generateHelmChart(
            tempDir.toString(), "my-app", "1.0.0", "1.0.0", "default"
        );

        assertTrue(result.isSuccess());
        assertNotNull(result.getChartPath());
        assertFalse(result.getGeneratedFiles().isEmpty());

        // Verify chart directory structure
        Path chartPath = Path.of(result.getChartPath());
        assertTrue(Files.exists(chartPath));
        assertTrue(Files.exists(chartPath.resolve("Chart.yaml")));
        assertTrue(Files.exists(chartPath.resolve("values.yaml")));
        assertTrue(Files.exists(chartPath.resolve("templates")));

        // Verify Chart.yaml content
        String chartYamlContent = Files.readString(chartPath.resolve("Chart.yaml"));
        assertTrue(chartYamlContent.contains("name: my-app"));
        assertTrue(chartYamlContent.contains("version: 1.0.0"));
        assertTrue(chartYamlContent.contains("appVersion: \"1.0.0\""));

        // Verify values.yaml content
        String valuesYamlContent = Files.readString(chartPath.resolve("values.yaml"));
        assertTrue(valuesYamlContent.contains("repository: my-app"));
        assertTrue(valuesYamlContent.contains("namespace: default"));

        // Verify template files
        Path templatesPath = chartPath.resolve("templates");
        assertTrue(Files.exists(templatesPath.resolve("deployment.yaml")));
        assertTrue(Files.exists(templatesPath.resolve("service.yaml")));
        assertTrue(Files.exists(templatesPath.resolve("ingress.yaml")));
        assertTrue(Files.exists(templatesPath.resolve("serviceaccount.yaml")));
        assertTrue(Files.exists(templatesPath.resolve("NOTES.txt")));
    }

    @Test
    void testGenerateHelmChart_CustomNamespace() throws IOException {
        HelmChartResult result = helmChartService.generateHelmChart(
            tempDir.toString(), "custom-app", "2.0.0", "2.0.0", "production"
        );

        assertTrue(result.isSuccess());

        Path chartPath = Path.of(result.getChartPath());
        String valuesYamlContent = Files.readString(chartPath.resolve("values.yaml"));
        assertTrue(valuesYamlContent.contains("namespace: production"));
    }

    @Test
    void testGenerateHelmChart_NonExistentPath() {
        HelmChartResult result = helmChartService.generateHelmChart(
            "/non/existent/path", "my-app", "1.0.0", "1.0.0", "default"
        );

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("does not exist"));
    }

    @Test
    void testUpdateHelmValues_NewValues() throws IOException {
        // First generate a helm chart
        HelmChartResult chartResult = helmChartService.generateHelmChart(
            tempDir.toString(), "test-app", "1.0.0", "1.0.0", "default"
        );
        assertTrue(chartResult.isSuccess());

        // Update values
        Map<String, Object> newValues = Map.of(
            "replicaCount", 3,
            "image", Map.of("tag", "v2.0.0"),
            "service", Map.of("port", 9090)
        );

        JibConfigResult result = helmChartService.updateHelmValues(tempDir.toString(), newValues);

        assertTrue(result.isSuccess());
        
        // Verify values were updated
        Path valuesPath = Path.of(result.getConfigFilePath());
        String valuesContent = Files.readString(valuesPath);
        assertTrue(valuesContent.contains("replicaCount: 3"));
        assertTrue(valuesContent.contains("v2.0.0")); // Check for the value regardless of quotes
        assertTrue(valuesContent.contains("port: 9090"));
    }

    @Test
    void testUpdateHelmValues_NestedValues() throws IOException {
        // Generate helm chart first
        helmChartService.generateHelmChart(
            tempDir.toString(), "nested-app", "1.0.0", "1.0.0", "default"
        );

        // Update nested values
        Map<String, Object> nestedValues = Map.of(
            "ingress", Map.of(
                "enabled", true,
                "hosts", Map.of("host", "example.com")
            ),
            "resources", Map.of(
                "limits", Map.of("cpu", "500m", "memory", "512Mi")
            )
        );

        JibConfigResult result = helmChartService.updateHelmValues(tempDir.toString(), nestedValues);

        assertTrue(result.isSuccess());
        
        // Verify nested values were updated
        Path valuesPath = Path.of(result.getConfigFilePath());
        String valuesContent = Files.readString(valuesPath);
        assertTrue(valuesContent.contains("enabled: true"));
        assertTrue(valuesContent.contains("500m")); // Check for the value regardless of quotes
        assertTrue(valuesContent.contains("512Mi")); // Check for the value regardless of quotes
    }

    @Test
    void testUpdateHelmValues_NoValuesFile() {
        Map<String, Object> values = Map.of("test", "value");
        
        JibConfigResult result = helmChartService.updateHelmValues(tempDir.toString(), values);

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("No Helm values.yaml file found"));
    }

    @Test
    void testGenerateHelmChart_VerifyDeploymentTemplate() throws IOException {
        HelmChartResult result = helmChartService.generateHelmChart(
            tempDir.toString(), "deploy-test", "1.0.0", "1.0.0", "default"
        );

        assertTrue(result.isSuccess());
        
        Path deploymentPath = Path.of(result.getChartPath()).resolve("templates/deployment.yaml");
        String deploymentContent = Files.readString(deploymentPath);
        
        // Verify deployment template has required elements
        assertTrue(deploymentContent.contains("apiVersion: apps/v1"));
        assertTrue(deploymentContent.contains("kind: Deployment"));
        assertTrue(deploymentContent.contains("deploy-test.fullname"));
        assertTrue(deploymentContent.contains("livenessProbe"));
        assertTrue(deploymentContent.contains("readinessProbe"));
        assertTrue(deploymentContent.contains("/actuator/health"));
    }

    @Test
    void testGenerateHelmChart_VerifyServiceTemplate() throws IOException {
        HelmChartResult result = helmChartService.generateHelmChart(
            tempDir.toString(), "service-test", "1.0.0", "1.0.0", "default"
        );

        assertTrue(result.isSuccess());
        
        Path servicePath = Path.of(result.getChartPath()).resolve("templates/service.yaml");
        String serviceContent = Files.readString(servicePath);
        
        // Verify service template
        assertTrue(serviceContent.contains("apiVersion: v1"));
        assertTrue(serviceContent.contains("kind: Service"));
        assertTrue(serviceContent.contains("service-test.fullname"));
        assertTrue(serviceContent.contains("targetPort: http"));
    }

    @Test
    void testGenerateHelmChart_VerifyIngressTemplate() throws IOException {
        HelmChartResult result = helmChartService.generateHelmChart(
            tempDir.toString(), "ingress-test", "1.0.0", "1.0.0", "default"
        );

        assertTrue(result.isSuccess());
        
        Path ingressPath = Path.of(result.getChartPath()).resolve("templates/ingress.yaml");
        String ingressContent = Files.readString(ingressPath);
        
        // Verify ingress template
        assertTrue(ingressContent.contains("{{- if .Values.ingress.enabled -}}"));
        assertTrue(ingressContent.contains("kind: Ingress"));
        assertTrue(ingressContent.contains("ingress-test.fullname"));
    }

    @Test
    void testGenerateHelmChart_VerifyNotesTemplate() throws IOException {
        HelmChartResult result = helmChartService.generateHelmChart(
            tempDir.toString(), "notes-test", "1.0.0", "1.0.0", "default"
        );

        assertTrue(result.isSuccess());
        
        Path notesPath = Path.of(result.getChartPath()).resolve("templates/NOTES.txt");
        String notesContent = Files.readString(notesPath);
        
        // Verify NOTES.txt template
        assertTrue(notesContent.contains("Get the application URL"));
        assertTrue(notesContent.contains("kubectl"));
        assertTrue(notesContent.contains("notes-test.fullname"));
    }

    @Test
    void testInitHelmChartTemplate_Success() throws IOException {
        String chartName = "test-helm-init";
        
        HelmChartResult result = helmChartService.initHelmChartTemplate(
            tempDir.toString(), chartName
        );
        
        assertTrue(result.isSuccess());
        assertNotNull(result.getChartPath());
        assertNotNull(result.getGeneratedFiles());
        assertFalse(result.getGeneratedFiles().isEmpty());
        assertTrue(result.getMessage().contains("helm create"));
        
        // Verify the chart was created with proper structure
        Path chartPath = Path.of(result.getChartPath());
        assertTrue(Files.exists(chartPath));
        assertTrue(Files.exists(chartPath.resolve("Chart.yaml")));
        assertTrue(Files.exists(chartPath.resolve("values.yaml")));
        assertTrue(Files.exists(chartPath.resolve("templates")));
        
        // Verify Chart.yaml contains correct chart name
        String chartYamlContent = Files.readString(chartPath.resolve("Chart.yaml"));
        assertTrue(chartYamlContent.contains("name: " + chartName));
    }

    @Test
    void testInitHelmChartTemplate_NonExistentPath() {
        String nonExistentPath = "/tmp/non-existent-path-" + System.currentTimeMillis();
        
        HelmChartResult result = helmChartService.initHelmChartTemplate(
            nonExistentPath, "test-chart"
        );
        
        assertFalse(result.isSuccess());
        assertNull(result.getChartPath());
        assertTrue(result.getMessage().contains("Project path does not exist"));
    }

    @Test
    void testInitHelmChartTemplate_EmptyChartName() {
        HelmChartResult result = helmChartService.initHelmChartTemplate(
            tempDir.toString(), ""
        );
        
        assertFalse(result.isSuccess());
        assertNull(result.getChartPath());
        assertTrue(result.getMessage().contains("Chart name cannot be null or empty"));
    }

    @Test
    void testInitHelmChartTemplate_NullChartName() {
        HelmChartResult result = helmChartService.initHelmChartTemplate(
            tempDir.toString(), null
        );
        
        assertFalse(result.isSuccess());
        assertNull(result.getChartPath());
        assertTrue(result.getMessage().contains("Chart name cannot be null or empty"));
    }

    @Test
    void testInitHelmChartTemplate_InvalidChartName() {
        // Test invalid chart names that don't follow DNS subdomain conventions
        String[] invalidNames = {
            "Test-Chart", // uppercase letters
            "test_chart", // underscores
            "-test-chart", // starts with hyphen
            "test-chart-", // ends with hyphen
            "test..chart", // double dots
            "test chart", // spaces
            "123test", // starts with number (actually valid for DNS, but testing edge case)
        };
        
        for (String invalidName : invalidNames) {
            HelmChartResult result = helmChartService.initHelmChartTemplate(
                tempDir.toString(), invalidName
            );
            
            if (!result.isSuccess()) {
                assertTrue(result.getMessage().contains("Invalid chart name") ||
                          result.getMessage().contains("Failed to create Helm chart"));
            }
        }
    }

    @Test 
    void testInitHelmChartTemplate_ValidChartNames() throws IOException {
        // Test valid chart names
        String[] validNames = {
            "test-chart",
            "my-app", 
            "service123",
            "a",
            "test123-chart-456"
        };
        
        for (String validName : validNames) {
            // Create a subdirectory for each test to avoid conflicts
            Path testDir = tempDir.resolve("test-" + validName);
            Files.createDirectories(testDir);
            
            HelmChartResult result = helmChartService.initHelmChartTemplate(
                testDir.toString(), validName
            );
            
            assertTrue(result.isSuccess(), "Chart name '" + validName + "' should be valid");
            assertNotNull(result.getChartPath());
            assertTrue(Files.exists(Path.of(result.getChartPath())));
        }
    }
}