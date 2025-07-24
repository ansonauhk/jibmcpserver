package com.example.jlibmcpserver.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class HelmChartResult {
    private boolean success;
    
    @JsonProperty("chart_path")
    private String chartPath;
    
    @JsonProperty("generated_files")
    private List<String> generatedFiles;
    
    private String message;

    public HelmChartResult() {}

    public HelmChartResult(boolean success, String chartPath, List<String> generatedFiles, String message) {
        this.success = success;
        this.chartPath = chartPath;
        this.generatedFiles = generatedFiles;
        this.message = message;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    
    public String getChartPath() { return chartPath; }
    public void setChartPath(String chartPath) { this.chartPath = chartPath; }
    
    public List<String> getGeneratedFiles() { return generatedFiles; }
    public void setGeneratedFiles(List<String> generatedFiles) { this.generatedFiles = generatedFiles; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}