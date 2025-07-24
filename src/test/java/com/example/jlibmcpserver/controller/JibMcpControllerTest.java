package com.example.jlibmcpserver.controller;

import com.example.jlibmcpserver.model.*;
import com.example.jlibmcpserver.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JibMcpControllerTest {

    @Mock
    private ProjectAnalysisService projectAnalysisService;

    @Mock
    private JibConfigurationService jibConfigurationService;

    @Mock
    private ContainerBuildService containerBuildService;

    @Mock
    private RegistryService registryService;

    @Mock
    private HelmChartService helmChartService;

    @Mock
    private DeploymentService deploymentService;

    @Mock
    private ConfigurationManagementService configurationManagementService;

    @InjectMocks
    private JibMcpController jibMcpController;

    @BeforeEach
    void setUp() {
        // Mock setup is handled by @Mock annotations
    }

    @Test
    void testAnalyzeProject() {
        // Arrange
        String projectPath = "/test/project";
        ProjectAnalysisResult expectedResult = new ProjectAnalysisResult(
            "maven", "single-module", "com.example.Application", 
            false, List.of("spring-boot-starter"), "17"
        );
        when(projectAnalysisService.analyzeProject(projectPath)).thenReturn(expectedResult);

        // Act
        ProjectAnalysisResult result = jibMcpController.analyzeProject(projectPath);

        // Assert
        assertNotNull(result);
        assertEquals("maven", result.getBuildSystem());
        assertEquals("com.example.Application", result.getMainClass());
        verify(projectAnalysisService).analyzeProject(projectPath);
    }

    @Test
    void testDetectMainClass() {
        // Arrange
        String projectPath = "/test/project";
        MainClassDetectionResult expectedResult = new MainClassDetectionResult(
            List.of("com.example.App1", "com.example.App2"), "com.example.App1"
        );
        when(projectAnalysisService.detectMainClasses(projectPath)).thenReturn(expectedResult);

        // Act
        MainClassDetectionResult result = jibMcpController.detectMainClass(projectPath);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getMainClasses().size());
        assertEquals("com.example.App1", result.getRecommendedMainClass());
        verify(projectAnalysisService).detectMainClasses(projectPath);
    }

    @Test
    void testInitJibConfig() {
        // Arrange
        String projectPath = "/test/project";
        String buildSystem = "maven";
        String baseImage = "eclipse-temurin:21-jre";
        String targetImage = "my-app:latest";
        
        JibConfigResult expectedResult = new JibConfigResult(true, "/test/project/pom.xml", "Success");
        when(jibConfigurationService.initJibConfig(projectPath, buildSystem, baseImage, targetImage))
            .thenReturn(expectedResult);

        // Act
        JibConfigResult result = jibMcpController.initJibConfig(projectPath, buildSystem, baseImage, targetImage);

        // Assert
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals("Success", result.getMessage());
        verify(jibConfigurationService).initJibConfig(projectPath, buildSystem, baseImage, targetImage);
    }

    @Test
    void testInitJibConfig_WithDefaults() {
        // Arrange
        String projectPath = "/test/project";
        String buildSystem = "gradle";
        
        JibConfigResult expectedResult = new JibConfigResult(true, "/test/project/build.gradle", "Success");
        when(jibConfigurationService.initJibConfig(eq(projectPath), eq(buildSystem), 
                                                  eq("eclipse-temurin:21-jre"), eq("my-app:latest")))
            .thenReturn(expectedResult);

        // Act
        JibConfigResult result = jibMcpController.initJibConfig(projectPath, buildSystem, null, null);

        // Assert
        assertNotNull(result);
        assertTrue(result.isSuccess());
        verify(jibConfigurationService).initJibConfig(projectPath, buildSystem, 
                                                     "eclipse-temurin:21-jre", "my-app:latest");
    }

    @Test
    void testSetMainClass() {
        // Arrange
        String projectPath = "/test/project";
        String mainClass = "com.example.NewApplication";
        
        JibConfigResult expectedResult = new JibConfigResult(true, null, "Main class set successfully");
        when(jibConfigurationService.setMainClass(projectPath, mainClass)).thenReturn(expectedResult);

        // Act
        JibConfigResult result = jibMcpController.setMainClass(projectPath, mainClass);

        // Assert
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals("Main class set successfully", result.getMessage());
        verify(jibConfigurationService).setMainClass(projectPath, mainClass);
    }

    @Test
    void testBuildContainer() {
        // Arrange
        String projectPath = "/test/project";
        String buildTarget = "docker";
        List<String> tags = List.of("v1.0", "latest");
        
        BuildResult expectedResult = new BuildResult(true, "my-app:latest", "Build completed", 30000);
        when(containerBuildService.buildContainer(projectPath, buildTarget, tags)).thenReturn(expectedResult);

        // Act
        BuildResult result = jibMcpController.buildContainer(projectPath, buildTarget, tags);

        // Assert
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals("my-app:latest", result.getImageId());
        assertEquals(30000, result.getExecutionTime());
        verify(containerBuildService).buildContainer(projectPath, buildTarget, tags);
    }

    @Test
    void testBuildContainer_WithDefaults() {
        // Arrange
        String projectPath = "/test/project";
        
        BuildResult expectedResult = new BuildResult(true, "my-app:latest", "Build completed", 25000);
        when(containerBuildService.buildContainer(eq(projectPath), eq("docker"), isNull())).thenReturn(expectedResult);

        // Act
        BuildResult result = jibMcpController.buildContainer(projectPath, null, null);

        // Assert
        assertNotNull(result);
        assertTrue(result.isSuccess());
        verify(containerBuildService).buildContainer(projectPath, "docker", null);
    }

    @Test
    void testValidateJibConfig() {
        // Arrange
        String projectPath = "/test/project";
        ValidationResult expectedResult = new ValidationResult(
            true, List.of(), List.of("Minor warning"), List.of("Consider optimization")
        );
        when(containerBuildService.validateJibConfig(projectPath)).thenReturn(expectedResult);

        // Act
        ValidationResult result = jibMcpController.validateJibConfig(projectPath);

        // Assert
        assertNotNull(result);
        assertTrue(result.isValid());
        assertEquals(1, result.getWarnings().size());
        assertEquals(1, result.getSuggestions().size());
        verify(containerBuildService).validateJibConfig(projectPath);
    }

    @Test
    void testConfigureRegistry() {
        // Arrange
        String projectPath = "/test/project";
        String registryType = "gcr";
        String registryUrl = "gcr.io";
        String authMethod = "service-account";
        
        JibConfigResult expectedResult = new JibConfigResult(true, projectPath, "Registry configured");
        when(registryService.configureRegistry(projectPath, registryType, registryUrl, authMethod))
            .thenReturn(expectedResult);

        // Act
        JibConfigResult result = jibMcpController.configureRegistry(projectPath, registryType, registryUrl, authMethod);

        // Assert
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals("Registry configured", result.getMessage());
        verify(registryService).configureRegistry(projectPath, registryType, registryUrl, authMethod);
    }

    @Test
    void testGenerateHelmChart() {
        // Arrange
        String projectPath = "/test/project";
        String chartName = "test-chart";
        String chartVersion = "1.0.0";
        String appVersion = "1.0.0";
        String namespace = "production";
        
        HelmChartResult expectedResult = new HelmChartResult(
            true, "/test/project/helm/test-chart", 
            List.of("Chart.yaml", "values.yaml"), "Chart generated successfully"
        );
        when(helmChartService.generateHelmChart(projectPath, chartName, chartVersion, appVersion, namespace))
            .thenReturn(expectedResult);

        // Act
        HelmChartResult result = jibMcpController.generateHelmChart(
            projectPath, chartName, chartVersion, appVersion, namespace
        );

        // Assert
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals(2, result.getGeneratedFiles().size());
        verify(helmChartService).generateHelmChart(projectPath, chartName, chartVersion, appVersion, namespace);
    }

    @Test
    void testGenerateHelmChart_WithDefaults() {
        // Arrange
        String projectPath = "/test/project";
        String chartName = "my-chart";
        
        HelmChartResult expectedResult = new HelmChartResult(true, "/test/project/helm/my-chart", 
                                                           List.of("Chart.yaml"), "Success");
        when(helmChartService.generateHelmChart(eq(projectPath), eq(chartName), 
                                              eq("0.1.0"), eq("1.0.0"), eq("default")))
            .thenReturn(expectedResult);

        // Act
        HelmChartResult result = jibMcpController.generateHelmChart(projectPath, chartName, null, null, null);

        // Assert
        assertNotNull(result);
        assertTrue(result.isSuccess());
        verify(helmChartService).generateHelmChart(projectPath, chartName, "0.1.0", "1.0.0", "default");
    }

    @Test
    void testDeployToK3s() {
        // Arrange
        String projectPath = "/test/project";
        String namespace = "test";
        Boolean waitForReady = true;
        
        DeploymentResult expectedResult = new DeploymentResult(
            true, "test-deployment", "http://10.0.0.1:8080", "ready", "Deployment successful"
        );
        when(deploymentService.deployToK3s(projectPath, namespace, waitForReady)).thenReturn(expectedResult);

        // Act
        DeploymentResult result = jibMcpController.deployToK3s(projectPath, namespace, waitForReady);

        // Assert
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals("test-deployment", result.getDeploymentName());
        assertEquals("ready", result.getStatus());
        verify(deploymentService).deployToK3s(projectPath, namespace, waitForReady);
    }

    @Test
    void testDeployToK3s_WithDefaults() {
        // Arrange
        String projectPath = "/test/project";
        
        DeploymentResult expectedResult = new DeploymentResult(true, "deployment", null, "ready", "Success");
        when(deploymentService.deployToK3s(eq(projectPath), eq("default"), eq(true))).thenReturn(expectedResult);

        // Act
        DeploymentResult result = jibMcpController.deployToK3s(projectPath, null, null);

        // Assert
        assertNotNull(result);
        assertTrue(result.isSuccess());
        verify(deploymentService).deployToK3s(projectPath, "default", true);
    }

    @Test
    void testGetBuildStatus() {
        // Arrange
        String projectPath = "/test/project";
        String statusMessage = "Last build completed successfully";
        when(deploymentService.getBuildStatus(projectPath)).thenReturn(statusMessage);

        // Act
        Map<String, Object> result = jibMcpController.getBuildStatus(projectPath);

        // Assert
        assertNotNull(result);
        assertEquals("unknown", result.get("last_build_time"));
        assertEquals("unknown", result.get("last_build_status"));
        assertEquals(statusMessage, result.get("status_message"));
        verify(deploymentService).getBuildStatus(projectPath);
    }

    @Test
    void testExportJibConfig() {
        // Arrange
        String projectPath = "/test/project";
        Map<String, Object> expectedConfig = Map.of(
            "config", Map.of("from", Map.of("image", "openjdk:11")),
            "build_system", "maven",
            "export_timestamp", "2023-01-01T10:00:00"
        );
        when(configurationManagementService.exportJibConfig(projectPath)).thenReturn(expectedConfig);

        // Act
        Map<String, Object> result = jibMcpController.exportJibConfig(projectPath);

        // Assert
        assertNotNull(result);
        assertEquals("maven", result.get("build_system"));
        assertTrue(result.containsKey("config"));
        verify(configurationManagementService).exportJibConfig(projectPath);
    }

    @Test
    void testImportJibConfig() {
        // Arrange
        String projectPath = "/test/project";
        Map<String, Object> config = Map.of("from", Map.of("image", "openjdk:17"));
        
        JibConfigResult expectedResult = new JibConfigResult(true, projectPath, "Config imported successfully");
        when(configurationManagementService.importJibConfig(projectPath, config)).thenReturn(expectedResult);

        // Act
        JibConfigResult result = jibMcpController.importJibConfig(projectPath, config);

        // Assert
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals("Config imported successfully", result.getMessage());
        verify(configurationManagementService).importJibConfig(projectPath, config);
    }
}