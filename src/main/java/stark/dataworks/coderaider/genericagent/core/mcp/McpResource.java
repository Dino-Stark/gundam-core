package stark.dataworks.coderaider.genericagent.core.mcp;

/**
 * McpResource implements MCP server integration and tool bridging.
 */
public record McpResource(String uri, String mimeType, String content)
{
}
