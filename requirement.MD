## Context: ## 
This is a software development team in a bank , corp environment with firewall, control, and in house default configuration.

For Java project, developer is using Google Jib based Maven plugin / Google Jib Gradle Plugin for any container build operated by the plugin.

Developer is using vscode GitHub Copilot agent mode with MCP server support.

Developer is working on containerlization of legacy Java project which is running on virtual machine currently.

Developer is using an in house developed maven/gradle plugin , wrapped on top of Google Jib , to generate helm chart, and deploy the container build and helm chart locally to k3s for fast dev testing.

It remains difficult to get started with Google Jib as requiring developer to learn Jib and config their maven/gradle project to use the plugin.


## Project goal: ##

Build a MCP Server for Google Jib maven plugin/gradle plugin in Java. The MCP server should provide transparent container operations - users should not need to explicitly mention "jib" or "use jib" in their requests. All container-related operations supported by Google Jib should be automatically handled through the MCP server.

For example:
- "Build a container" should automatically trigger the MCP server to build using Jib
- "Deploy my application" should use Jib to containerize and deploy
- "Set mainClass to com.abc.Main" should configure Jib's main class setting
- "Push to registry" should use Jib's registry push functionality

The in house developed plugin support generation of helm chart template, this should be done with MCP server too.
The MCP server should help user to config their maven/gradle project accordingly to use google jib plugin , with default jib settings or help to update the plugin settings that's supported by Google Jib.
The MCP server should either have logic to modify the jib configuration directly, or provide guidance to agent mode so it knows what code to change.



Reference: 

https://github.com/GoogleContainerTools/jib/blob/master/jib-gradle-plugin/README.md
https://github.com/GoogleContainerTools/jib/blob/master/jib-maven-plugin/README.md