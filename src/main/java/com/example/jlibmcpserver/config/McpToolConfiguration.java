package com.example.jlibmcpserver.config;

import com.example.jlibmcpserver.controller.JibMcpController;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for registering MCP tools with the Spring AI MCP server.
 * This class ensures that all @Tool annotated methods in JibMcpController 
 * are properly registered as MCP tools.
 */
@Configuration
public class McpToolConfiguration {

    /**
     * Register all Jib MCP tools as a ToolCallbackProvider.
     * This allows the Spring AI MCP server to automatically discover and
     * provide all tools defined in the JibMcpController.
     * 
     * @param jibMcpController the controller containing all MCP tool methods
     * @return ToolCallbackProvider that exposes all Jib tools to the MCP server
     */
    @Bean
    public ToolCallbackProvider jibMcpTools(JibMcpController jibMcpController) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(jibMcpController)
                .build();
    }
}