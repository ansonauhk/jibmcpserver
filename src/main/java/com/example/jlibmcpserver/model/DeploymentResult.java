package com.example.jlibmcpserver.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DeploymentResult {
    private boolean success;
    
    @JsonProperty("deployment_name")
    private String deploymentName;
    
    @JsonProperty("service_url")
    private String serviceUrl;
    
    private String status;
    private String message;

    public DeploymentResult() {}

    public DeploymentResult(boolean success, String deploymentName, String serviceUrl, String status, String message) {
        this.success = success;
        this.deploymentName = deploymentName;
        this.serviceUrl = serviceUrl;
        this.status = status;
        this.message = message;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    
    public String getDeploymentName() { return deploymentName; }
    public void setDeploymentName(String deploymentName) { this.deploymentName = deploymentName; }
    
    public String getServiceUrl() { return serviceUrl; }
    public void setServiceUrl(String serviceUrl) { this.serviceUrl = serviceUrl; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}