package stark.dataworks.coderaider.genericagent.core.mcp.approval;

import java.util.Map;

/**
 * McpToolApprovalRequest implements MCP server integration and tool bridging.
 */
public record McpToolApprovalRequest(String serverId, String toolName, Map<String, Object> args)
{
}
