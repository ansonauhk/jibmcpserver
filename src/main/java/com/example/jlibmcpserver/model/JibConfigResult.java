package com.example.jlibmcpserver.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class JibConfigResult {
    private boolean success;
    
    @JsonProperty("config_file_path")
    private String configFilePath;
    
    private String message;

    public JibConfigResult() {}

    public JibConfigResult(boolean success, String configFilePath, String message) {
        this.success = success;
        this.configFilePath = configFilePath;
        this.message = message;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    
    public String getConfigFilePath() { return configFilePath; }
    public void setConfigFilePath(String configFilePath) { this.configFilePath = configFilePath; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}