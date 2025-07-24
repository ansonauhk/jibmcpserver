package com.example.jlibmcpserver.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class ProjectAnalysisResult {
    @JsonProperty("build_system")
    private String buildSystem;
    
    @JsonProperty("project_type") 
    private String projectType;
    
    @JsonProperty("main_class")
    private String mainClass;
    
    @JsonProperty("existing_jib_config")
    private boolean existingJibConfig;
    
    private List<String> dependencies;
    
    @JsonProperty("java_version")
    private String javaVersion;

    public ProjectAnalysisResult() {}

    public ProjectAnalysisResult(String buildSystem, String projectType, String mainClass, 
                               boolean existingJibConfig, List<String> dependencies, String javaVersion) {
        this.buildSystem = buildSystem;
        this.projectType = projectType;
        this.mainClass = mainClass;
        this.existingJibConfig = existingJibConfig;
        this.dependencies = dependencies;
        this.javaVersion = javaVersion;
    }

    // Getters and setters
    public String getBuildSystem() { return buildSystem; }
    public void setBuildSystem(String buildSystem) { this.buildSystem = buildSystem; }
    
    public String getProjectType() { return projectType; }
    public void setProjectType(String projectType) { this.projectType = projectType; }
    
    public String getMainClass() { return mainClass; }
    public void setMainClass(String mainClass) { this.mainClass = mainClass; }
    
    public boolean isExistingJibConfig() { return existingJibConfig; }
    public void setExistingJibConfig(boolean existingJibConfig) { this.existingJibConfig = existingJibConfig; }
    
    public List<String> getDependencies() { return dependencies; }
    public void setDependencies(List<String> dependencies) { this.dependencies = dependencies; }
    
    public String getJavaVersion() { return javaVersion; }
    public void setJavaVersion(String javaVersion) { this.javaVersion = javaVersion; }
}