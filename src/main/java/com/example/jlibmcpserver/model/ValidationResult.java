package com.example.jlibmcpserver.model;

import java.util.List;

public class ValidationResult {
    private boolean valid;
    private List<String> errors;
    private List<String> warnings;
    private List<String> suggestions;

    public ValidationResult() {}

    public ValidationResult(boolean valid, List<String> errors, List<String> warnings, List<String> suggestions) {
        this.valid = valid;
        this.errors = errors;
        this.warnings = warnings;
        this.suggestions = suggestions;
    }

    public boolean isValid() { return valid; }
    public void setValid(boolean valid) { this.valid = valid; }
    
    public List<String> getErrors() { return errors; }
    public void setErrors(List<String> errors) { this.errors = errors; }
    
    public List<String> getWarnings() { return warnings; }
    public void setWarnings(List<String> warnings) { this.warnings = warnings; }
    
    public List<String> getSuggestions() { return suggestions; }
    public void setSuggestions(List<String> suggestions) { this.suggestions = suggestions; }
}