package com.example.jlibmcpserver.service;

import com.example.jlibmcpserver.model.HelmChartResult;
import com.example.jlibmcpserver.model.JibConfigResult;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

@Service
public class HelmChartService {

    public HelmChartResult generateHelmChart(String projectPath, String chartName, String chartVersion,
                                           String appVersion, String namespace) {
        try {
            Path path = Paths.get(projectPath);
            if (!Files.exists(path)) {
                return new HelmChartResult(false, null, null, "Project path does not exist: " + projectPath);
            }

            // Create helm chart directory structure
            Path chartPath = path.resolve("helm").resolve(chartName);
            Files.createDirectories(chartPath);
            Files.createDirectories(chartPath.resolve("templates"));

            List<String> generatedFiles = new ArrayList<>();

            // Generate Chart.yaml
            String chartYamlContent = generateChartYaml(chartName, chartVersion, appVersion);
            Path chartYamlPath = chartPath.resolve("Chart.yaml");
            Files.writeString(chartYamlPath, chartYamlContent);
            generatedFiles.add(chartYamlPath.toString());

            // Generate values.yaml
            String valuesYamlContent = generateValuesYaml(chartName, namespace);
            Path valuesYamlPath = chartPath.resolve("values.yaml");
            Files.writeString(valuesYamlPath, valuesYamlContent);
            generatedFiles.add(valuesYamlPath.toString());

            // Generate templates
            generateTemplates(chartPath.resolve("templates"), chartName, generatedFiles);

            return new HelmChartResult(true, chartPath.toString(), generatedFiles, 
                                     "Helm chart generated successfully");

        } catch (Exception e) {
            return new HelmChartResult(false, null, null, "Failed to generate Helm chart: " + e.getMessage());
        }
    }

    public JibConfigResult updateHelmValues(String projectPath, Map<String, Object> values) {
        try {
            Path path = Paths.get(projectPath);
            
            // Find values.yaml file
            Path valuesPath = findValuesYaml(path);
            if (valuesPath == null) {
                return new JibConfigResult(false, null, "No Helm values.yaml file found");
            }

            // Load existing values
            Yaml yaml = new Yaml();
            Map<String, Object> existingValues = new HashMap<>();
            
            if (Files.exists(valuesPath)) {
                String content = Files.readString(valuesPath);
                if (!content.trim().isEmpty()) {
                    existingValues = yaml.load(content);
                    if (existingValues == null) {
                        existingValues = new HashMap<>();
                    }
                }
            }

            // Merge with new values
            mergeValues(existingValues, values);

            // Write updated values
            DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            options.setPrettyFlow(true);
            
            Yaml yamlWriter = new Yaml(options);
            String updatedContent = yamlWriter.dump(existingValues);
            Files.writeString(valuesPath, updatedContent);

            return new JibConfigResult(true, valuesPath.toString(), "Helm values updated successfully");

        } catch (Exception e) {
            return new JibConfigResult(false, null, "Failed to update Helm values: " + e.getMessage());
        }
    }

    private String generateChartYaml(String chartName, String chartVersion, String appVersion) {
        return String.format("""
                apiVersion: v2
                name: %s
                description: A Helm chart for %s
                type: application
                version: %s
                appVersion: "%s"
                """, chartName, chartName, chartVersion, appVersion);
    }

    private String generateValuesYaml(String chartName, String namespace) {
        return String.format("""
                # Default values for %s
                replicaCount: 1
                
                image:
                  repository: %s
                  pullPolicy: IfNotPresent
                  tag: "latest"
                
                imagePullSecrets: []
                nameOverride: ""
                fullnameOverride: ""
                
                serviceAccount:
                  create: true
                  annotations: {}
                  name: ""
                
                podAnnotations: {}
                
                podSecurityContext: {}
                
                securityContext: {}
                
                service:
                  type: ClusterIP
                  port: 8080
                
                ingress:
                  enabled: false
                  className: ""
                  annotations: {}
                  hosts:
                    - host: chart-example.local
                      paths:
                        - path: /
                          pathType: Prefix
                  tls: []
                
                resources: {}
                
                autoscaling:
                  enabled: false
                  minReplicas: 1
                  maxReplicas: 100
                  targetCPUUtilizationPercentage: 80
                
                nodeSelector: {}
                
                tolerations: []
                
                affinity: {}
                
                namespace: %s
                """, chartName, chartName, namespace);
    }

    private void generateTemplates(Path templatesPath, String chartName, List<String> generatedFiles) throws IOException {
        // Generate deployment.yaml
        String deploymentContent = generateDeploymentTemplate(chartName);
        Path deploymentPath = templatesPath.resolve("deployment.yaml");
        Files.writeString(deploymentPath, deploymentContent);
        generatedFiles.add(deploymentPath.toString());

        // Generate service.yaml
        String serviceContent = generateServiceTemplate(chartName);
        Path servicePath = templatesPath.resolve("service.yaml");
        Files.writeString(servicePath, serviceContent);
        generatedFiles.add(servicePath.toString());

        // Generate serviceaccount.yaml
        String serviceAccountContent = generateServiceAccountTemplate(chartName);
        Path serviceAccountPath = templatesPath.resolve("serviceaccount.yaml");
        Files.writeString(serviceAccountPath, serviceAccountContent);
        generatedFiles.add(serviceAccountPath.toString());

        // Generate ingress.yaml
        String ingressContent = generateIngressTemplate(chartName);
        Path ingressPath = templatesPath.resolve("ingress.yaml");
        Files.writeString(ingressPath, ingressContent);
        generatedFiles.add(ingressPath.toString());

        // Generate NOTES.txt
        String notesContent = generateNotesTemplate(chartName);
        Path notesPath = templatesPath.resolve("NOTES.txt");
        Files.writeString(notesPath, notesContent);
        generatedFiles.add(notesPath.toString());
    }

    private String generateDeploymentTemplate(String chartName) {
        return String.format("""
                apiVersion: apps/v1
                kind: Deployment
                metadata:
                  name: {{ include "%s.fullname" . }}
                  labels:
                    {{- include "%s.labels" . | nindent 4 }}
                spec:
                  {{- if not .Values.autoscaling.enabled }}
                  replicas: {{ .Values.replicaCount }}
                  {{- end }}
                  selector:
                    matchLabels:
                      {{- include "%s.selectorLabels" . | nindent 6 }}
                  template:
                    metadata:
                      {{- with .Values.podAnnotations }}
                      annotations:
                        {{- toYaml . | nindent 8 }}
                      {{- end }}
                      labels:
                        {{- include "%s.selectorLabels" . | nindent 8 }}
                    spec:
                      {{- with .Values.imagePullSecrets }}
                      imagePullSecrets:
                        {{- toYaml . | nindent 8 }}
                      {{- end }}
                      serviceAccountName: {{ include "%s.serviceAccountName" . }}
                      securityContext:
                        {{- toYaml .Values.podSecurityContext | nindent 8 }}
                      containers:
                        - name: {{ .Chart.Name }}
                          securityContext:
                            {{- toYaml .Values.securityContext | nindent 12 }}
                          image: "{{ .Values.image.repository }}:{{ .Values.image.tag | default .Chart.AppVersion }}"
                          imagePullPolicy: {{ .Values.image.pullPolicy }}
                          ports:
                            - name: http
                              containerPort: {{ .Values.service.port }}
                              protocol: TCP
                          livenessProbe:
                            httpGet:
                              path: /actuator/health
                              port: http
                            initialDelaySeconds: 30
                            periodSeconds: 10
                          readinessProbe:
                            httpGet:
                              path: /actuator/health
                              port: http
                            initialDelaySeconds: 5
                            periodSeconds: 5
                          resources:
                            {{- toYaml .Values.resources | nindent 12 }}
                      {{- with .Values.nodeSelector }}
                      nodeSelector:
                        {{- toYaml . | nindent 8 }}
                      {{- end }}
                      {{- with .Values.affinity }}
                      affinity:
                        {{- toYaml . | nindent 8 }}
                      {{- end }}
                      {{- with .Values.tolerations }}
                      tolerations:
                        {{- toYaml . | nindent 8 }}
                      {{- end }}
                """, chartName, chartName, chartName, chartName, chartName);
    }

    private String generateServiceTemplate(String chartName) {
        return String.format("""
                apiVersion: v1
                kind: Service
                metadata:
                  name: {{ include "%s.fullname" . }}
                  labels:
                    {{- include "%s.labels" . | nindent 4 }}
                spec:
                  type: {{ .Values.service.type }}
                  ports:
                    - port: {{ .Values.service.port }}
                      targetPort: http
                      protocol: TCP
                      name: http
                  selector:
                    {{- include "%s.selectorLabels" . | nindent 4 }}
                """, chartName, chartName, chartName);
    }

    private String generateServiceAccountTemplate(String chartName) {
        return String.format("""
                {{- if .Values.serviceAccount.create -}}
                apiVersion: v1
                kind: ServiceAccount
                metadata:
                  name: {{ include "%s.serviceAccountName" . }}
                  labels:
                    {{- include "%s.labels" . | nindent 4 }}
                  {{- with .Values.serviceAccount.annotations }}
                  annotations:
                    {{- toYaml . | nindent 4 }}
                  {{- end }}
                {{- end }}
                """, chartName, chartName);
    }

    private String generateIngressTemplate(String chartName) {
        return String.format("""
                {{- if .Values.ingress.enabled -}}
                {{- $fullName := include "%s.fullname" . -}}
                {{- $svcPort := .Values.service.port -}}
                {{- if and .Values.ingress.className (not (hasKey .Values.ingress.annotations "kubernetes.io/ingress.class")) }}
                  {{- $_ := set .Values.ingress.annotations "kubernetes.io/ingress.class" .Values.ingress.className}}
                {{- end }}
                {{- if semverCompare ">=1.19-0" .Capabilities.KubeVersion.GitVersion -}}
                apiVersion: networking.k8s.io/v1
                {{- else if semverCompare ">=1.14-0" .Capabilities.KubeVersion.GitVersion -}}
                apiVersion: networking.k8s.io/v1beta1
                {{- else -}}
                apiVersion: extensions/v1beta1
                {{- end }}
                kind: Ingress
                metadata:
                  name: {{ $fullName }}
                  labels:
                    {{- include "%s.labels" . | nindent 4 }}
                  {{- with .Values.ingress.annotations }}
                  annotations:
                    {{- toYaml . | nindent 4 }}
                  {{- end }}
                spec:
                  {{- if and .Values.ingress.className (semverCompare ">=1.18-0" .Capabilities.KubeVersion.GitVersion) }}
                  ingressClassName: {{ .Values.ingress.className }}
                  {{- end }}
                  {{- if .Values.ingress.tls }}
                  tls:
                    {{- range .Values.ingress.tls }}
                    - hosts:
                        {{- range .hosts }}
                        - {{ . | quote }}
                        {{- end }}
                      secretName: {{ .secretName }}
                    {{- end }}
                  {{- end }}
                  rules:
                    {{- range .Values.ingress.hosts }}
                    - host: {{ .host | quote }}
                      http:
                        paths:
                          {{- range .paths }}
                          - path: {{ .path }}
                            {{- if and .pathType (semverCompare ">=1.18-0" $.Capabilities.KubeVersion.GitVersion) }}
                            pathType: {{ .pathType }}
                            {{- end }}
                            backend:
                              {{- if semverCompare ">=1.19-0" $.Capabilities.KubeVersion.GitVersion }}
                              service:
                                name: {{ $fullName }}
                                port:
                                  number: {{ $svcPort }}
                              {{- else }}
                              serviceName: {{ $fullName }}
                              servicePort: {{ $svcPort }}
                              {{- end }}
                          {{- end }}
                    {{- end }}
                {{- end }}
                """, chartName, chartName);
    }

    private String generateNotesTemplate(String chartName) {
        return String.format("""
                1. Get the application URL by running these commands:
                {{- if .Values.ingress.enabled }}
                {{- range $host := .Values.ingress.hosts }}
                  {{- range .paths }}
                  http{{ if $.Values.ingress.tls }}s{{ end }}://{{ $host.host }}{{ .path }}
                  {{- end }}
                {{- end }}
                {{- else if contains "NodePort" .Values.service.type }}
                  export NODE_PORT=$(kubectl get --namespace {{ .Release.Namespace }} -o jsonpath="{.spec.ports[0].nodePort}" services {{ include "%s.fullname" . }})
                  export NODE_IP=$(kubectl get nodes --namespace {{ .Release.Namespace }} -o jsonpath="{.items[0].status.addresses[0].address}")
                  echo http://$NODE_IP:$NODE_PORT
                {{- else if contains "LoadBalancer" .Values.service.type }}
                     NOTE: It may take a few minutes for the LoadBalancer IP to be available.
                           You can watch the status of by running 'kubectl get --namespace {{ .Release.Namespace }} svc -w {{ include "%s.fullname" . }}'
                  export SERVICE_IP=$(kubectl get svc --namespace {{ .Release.Namespace }} {{ include "%s.fullname" . }} --template "{{"{{ range (index .status.loadBalancer.ingress 0) }}{{.}}{{ end }}"}}")
                  echo http://$SERVICE_IP:{{ .Values.service.port }}
                {{- else if contains "ClusterIP" .Values.service.type }}
                  export POD_NAME=$(kubectl get pods --namespace {{ .Release.Namespace }} -l "app.kubernetes.io/name={{ include "%s.name" . }},app.kubernetes.io/instance={{ .Release.Name }}" -o jsonpath="{.items[0].metadata.name}")
                  export CONTAINER_PORT=$(kubectl get pod --namespace {{ .Release.Namespace }} $POD_NAME -o jsonpath="{.spec.containers[0].ports[0].containerPort}")
                  echo "Visit http://127.0.0.1:8080 to use your application"
                  kubectl --namespace {{ .Release.Namespace }} port-forward $POD_NAME 8080:$CONTAINER_PORT
                {{- end }}
                """, chartName, chartName, chartName, chartName);
    }

    private Path findValuesYaml(Path projectPath) throws IOException {
        // Look for values.yaml in helm directories (including subdirectories for chart names)
        Path[] possibleDirectories = {
            projectPath.resolve("helm"),
            projectPath.resolve("charts"),
            projectPath.resolve("chart")
        };
        
        for (Path dir : possibleDirectories) {
            if (Files.exists(dir) && Files.isDirectory(dir)) {
                // First check direct values.yaml in the directory
                Path directValuesPath = dir.resolve("values.yaml");
                if (Files.exists(directValuesPath)) {
                    return directValuesPath;
                }
                
                // Then check subdirectories (chart names) for values.yaml
                try (Stream<Path> subDirs = Files.list(dir)) {
                    Optional<Path> valuesPath = subDirs
                        .filter(Files::isDirectory)
                        .map(subDir -> subDir.resolve("values.yaml"))
                        .filter(Files::exists)
                        .findFirst();
                    
                    if (valuesPath.isPresent()) {
                        return valuesPath.get();
                    }
                }
            }
        }
        
        // Check root directory
        Path rootValuesPath = projectPath.resolve("values.yaml");
        if (Files.exists(rootValuesPath)) {
            return rootValuesPath;
        }
        
        // Final fallback: search recursively for any values.yaml file
        try (Stream<Path> walkStream = Files.walk(projectPath, 3)) {
            return walkStream
                    .filter(path -> path.getFileName().toString().equals("values.yaml"))
                    .findFirst()
                    .orElse(null);
        }
    }

    @SuppressWarnings("unchecked")
    private void mergeValues(Map<String, Object> existing, Map<String, Object> updates) {
        for (Map.Entry<String, Object> entry : updates.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            if (value instanceof Map && existing.get(key) instanceof Map) {
                // Recursively merge nested maps
                mergeValues((Map<String, Object>) existing.get(key), (Map<String, Object>) value);
            } else {
                // Overwrite or add new value
                existing.put(key, value);
            }
        }
    }

    /**
     * Initialize a Helm chart template using the native helm create command
     */
    public HelmChartResult initHelmChartTemplate(String projectPath, String chartName) {
        try {
            Path path = Paths.get(projectPath);
            if (!Files.exists(path)) {
                return new HelmChartResult(false, null, null, "Project path does not exist: " + projectPath);
            }

            if (chartName == null || chartName.trim().isEmpty()) {
                return new HelmChartResult(false, null, null, "Chart name cannot be null or empty");
            }

            // Validate chart name (Helm has specific naming requirements)
            if (!isValidHelmChartName(chartName)) {
                return new HelmChartResult(false, null, null, 
                    "Invalid chart name. Chart names must follow DNS subdomain naming conventions");
            }

            // Execute helm create command in the project directory
            ProcessResult result = executeCommand(path, "helm", "create", chartName);
            
            if (result.exitCode != 0) {
                return new HelmChartResult(false, null, null, 
                    "Failed to create Helm chart: " + result.output);
            }

            // Verify the chart was created successfully
            Path chartPath = path.resolve(chartName);
            if (!Files.exists(chartPath) || !Files.exists(chartPath.resolve("Chart.yaml"))) {
                return new HelmChartResult(false, null, null, 
                    "Helm chart creation failed - chart directory or Chart.yaml not found");
            }

            // Collect generated files
            List<String> generatedFiles = new ArrayList<>();
            collectGeneratedFiles(chartPath, generatedFiles);

            return new HelmChartResult(true, chartPath.toString(), generatedFiles, 
                "Helm chart template initialized successfully using 'helm create'");

        } catch (Exception e) {
            return new HelmChartResult(false, null, null, 
                "Failed to initialize Helm chart template: " + e.getMessage());
        }
    }

    private boolean isValidHelmChartName(String chartName) {
        // Helm chart names must follow DNS subdomain naming conventions
        // - contain only lowercase alphanumeric characters or '-'
        // - start and end with an alphanumeric character
        // - be no more than 253 characters long
        if (chartName == null || chartName.length() == 0 || chartName.length() > 253) {
            return false;
        }
        
        return chartName.matches("^[a-z0-9]([a-z0-9-]*[a-z0-9])?$");
    }

    private void collectGeneratedFiles(Path chartPath, List<String> generatedFiles) throws IOException {
        try (Stream<Path> walk = Files.walk(chartPath)) {
            walk.filter(Files::isRegularFile)
                .forEach(file -> generatedFiles.add(file.toString()));
        }
    }

    private ProcessResult executeCommand(Path workingDirectory, String... command) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(workingDirectory.toFile());
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();
        
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }

        boolean finished = process.waitFor(30, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException("Command timed out: " + String.join(" ", command));
        }

        return new ProcessResult(process.exitValue(), output.toString());
    }

    private static class ProcessResult {
        final int exitCode;
        final String output;

        ProcessResult(int exitCode, String output) {
            this.exitCode = exitCode;
            this.output = output;
        }
    }
}