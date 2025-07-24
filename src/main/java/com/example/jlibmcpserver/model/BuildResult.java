package com.example.jlibmcpserver.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class BuildResult {
    private boolean success;
    
    @JsonProperty("image_id")
    private String imageId;
    
    @JsonProperty("build_log")
    private String buildLog;
    
    @JsonProperty("execution_time")
    private long executionTime;

    public BuildResult() {}

    public BuildResult(boolean success, String imageId, String buildLog, long executionTime) {
        this.success = success;
        this.imageId = imageId;
        this.buildLog = buildLog;
        this.executionTime = executionTime;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    
    public String getImageId() { return imageId; }
    public void setImageId(String imageId) { this.imageId = imageId; }
    
    public String getBuildLog() { return buildLog; }
    public void setBuildLog(String buildLog) { this.buildLog = buildLog; }
    
    public long getExecutionTime() { return executionTime; }
    public void setExecutionTime(long executionTime) { this.executionTime = executionTime; }
}