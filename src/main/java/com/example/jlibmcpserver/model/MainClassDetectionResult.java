package com.example.jlibmcpserver.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class MainClassDetectionResult {
    @JsonProperty("main_classes")
    private List<String> mainClasses;
    
    @JsonProperty("recommended_main_class")
    private String recommendedMainClass;

    public MainClassDetectionResult() {}

    public MainClassDetectionResult(List<String> mainClasses, String recommendedMainClass) {
        this.mainClasses = mainClasses;
        this.recommendedMainClass = recommendedMainClass;
    }

    public List<String> getMainClasses() { return mainClasses; }
    public void setMainClasses(List<String> mainClasses) { this.mainClasses = mainClasses; }
    
    public String getRecommendedMainClass() { return recommendedMainClass; }
    public void setRecommendedMainClass(String recommendedMainClass) { this.recommendedMainClass = recommendedMainClass; }
}