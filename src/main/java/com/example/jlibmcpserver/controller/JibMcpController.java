package com.example.jlibmcpserver.controller;

import com.example.jlibmcpserver.model.*;
import com.example.jlibmcpserver.service.*;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class JibMcpController {

    @Autowired
    private ProjectAnalysisService projectAnalysisService;
    
    @Autowired
    private JibConfigurationService jibConfigurationService;
    
    @Autowired
    private ContainerBuildService containerBuildService;
    
    @Autowired
    private RegistryService registryService;
    
    @Autowired
    private HelmChartService helmChartService;
    
    @Autowired
    private DeploymentService deploymentService;
    
    @Autowired
    private ConfigurationManagementService configurationManagementService;

    // Project Analysis Tools
    
    @Tool(name = "analyze_project", 
          description = "Analyze the current Java project structure and detect build system (Maven/Gradle)")
    public ProjectAnalysisResult analyzeProject(String project_path) {
        return projectAnalysisService.analyzeProject(project_path);
    }

    @Tool(name = "detect_main_class", 
          description = "Automatically detect the main class in the Java project")
    public MainClassDetectionResult detectMainClass(String project_path) {
        return projectAnalysisService.detectMainClasses(project_path);
    }

    // Jib Configuration Tools
    
    @Tool(name = "init_jib_config", 
          description = "Initialize Jib plugin configuration transparently in Maven pom.xml or Gradle build.gradle (triggered by containerization requests without explicit 'jib' mention)")
    public JibConfigResult initJibConfig(String project_path, String build_system, 
                                        String base_image, String target_image) {
        String baseImageDefault = base_image != null ? base_image : "eclipse-temurin:21-jre";
        String targetImageDefault = target_image != null ? target_image : "my-app:latest";
        
        return jibConfigurationService.initJibConfig(project_path, build_system, 
                                                   baseImageDefault, targetImageDefault);
    }

    @Tool(name = "update_jib_config", 
          description = "Update specific Jib configuration parameters")
    public JibConfigResult updateJibConfig(String project_path, String config_section, 
                                          Map<String, Object> parameters) {
        return jibConfigurationService.updateJibConfig(project_path, config_section, parameters);
    }

    @Tool(name = "set_main_class", 
          description = "Set the main class for the containerized application")
    public JibConfigResult setMainClass(String project_path, String main_class) {
        return jibConfigurationService.setMainClass(project_path, main_class);
    }

    @Tool(name = "configure_container_settings", 
          description = "Configure container runtime settings (ports, environment variables, JVM flags)")
    public JibConfigResult configureContainerSettings(String project_path, List<String> ports,
                                                      Map<String, String> environment_variables,
                                                      List<String> jvm_flags, String working_directory) {
        return jibConfigurationService.configureContainerSettings(project_path, ports, 
                                                                 environment_variables, jvm_flags, working_directory);
    }

    // Container Build Tools
    
    @Tool(name = "build_container", 
          description = "Build container image transparently using Jib (triggered by user requests like 'build a container', 'build my application', etc.)")
    public BuildResult buildContainer(String project_path, String build_target, List<String> tags) {
        String buildTargetDefault = build_target != null ? build_target : "docker";
        return containerBuildService.buildContainer(project_path, buildTargetDefault, tags);
    }

    @Tool(name = "validate_jib_config", 
          description = "Validate current Jib configuration before building")
    public ValidationResult validateJibConfig(String project_path) {
        return containerBuildService.validateJibConfig(project_path);
    }

    // Registry and Authentication Tools
    
    @Tool(name = "configure_registry", 
          description = "Configure container registry settings and authentication")
    public JibConfigResult configureRegistry(String project_path, String registry_type, 
                                            String registry_url, String auth_method) {
        return registryService.configureRegistry(project_path, registry_type, registry_url, auth_method);
    }

    // Helm Chart Generation Tools
    
    @Tool(name = "generate_helm_chart", 
          description = "Generate Helm chart templates for the containerized application")
    public HelmChartResult generateHelmChart(String project_path, String chart_name, 
                                           String chart_version, String app_version, String namespace) {
        String chartVersionDefault = chart_version != null ? chart_version : "0.1.0";
        String appVersionDefault = app_version != null ? app_version : "1.0.0";
        String namespaceDefault = namespace != null ? namespace : "default";
        
        return helmChartService.generateHelmChart(project_path, chart_name, chartVersionDefault, 
                                                appVersionDefault, namespaceDefault);
    }

    @Tool(name = "update_helm_values", 
          description = "Update Helm chart values for deployment configuration")
    public JibConfigResult updateHelmValues(String project_path, Map<String, Object> values) {
        return helmChartService.updateHelmValues(project_path, values);
    }

    // Development and Testing Tools
    
    @Tool(name = "deploy_to_k3s", 
          description = "Deploy the containerized application to local k3s for testing (automatically containerizes using Jib if needed)")
    public DeploymentResult deployToK3s(String project_path, String namespace, Boolean wait_for_ready) {
        String namespaceDefault = namespace != null ? namespace : "default";
        boolean waitForReadyDefault = wait_for_ready != null ? wait_for_ready : true;
        
        return deploymentService.deployToK3s(project_path, namespaceDefault, waitForReadyDefault);
    }

    @Tool(name = "get_build_status", 
          description = "Get the status of the last container build")
    public Map<String, Object> getBuildStatus(String project_path) {
        String status = deploymentService.getBuildStatus(project_path);
        Map<String, Object> result = new HashMap<>();
        result.put("last_build_time", "unknown");
        result.put("last_build_status", "unknown");
        result.put("image_id", null);
        result.put("error_message", null);
        result.put("status_message", status);
        return result;
    }

    // Configuration Management Tools
    
    @Tool(name = "export_jib_config", 
          description = "Export current Jib configuration as JSON for backup or sharing")
    public Map<String, Object> exportJibConfig(String project_path) {
        return configurationManagementService.exportJibConfig(project_path);
    }

    @Tool(name = "import_jib_config", 
          description = "Import Jib configuration from JSON")
    public JibConfigResult importJibConfig(String project_path, Map<String, Object> config) {
        return configurationManagementService.importJibConfig(project_path, config);
    }
}