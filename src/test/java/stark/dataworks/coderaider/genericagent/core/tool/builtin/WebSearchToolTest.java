package stark.dataworks.coderaider.genericagent.core.tool.builtin;

import org.junit.jupiter.api.Test;
import stark.dataworks.coderaider.genericagent.core.tool.ToolDefinition;
import stark.dataworks.coderaider.genericagent.core.tool.ToolParameterSchema;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class WebSearchToolTest
{
    @Test
    public void shouldReturnErrorWhenMissingSubscriptionKey()
    {
        WebSearchTool tool = new WebSearchTool(new ToolDefinition(
            "web_search",
            "Search the web using Bing Web Search API",
            List.of(new ToolParameterSchema("query", "string", true, "Search query"))), "");

        String output = tool.execute(Map.of("query", "OpenAI"));
        assertTrue(output.contains("missing Bing subscription key"));
    }

    @Test
    public void shouldRequireQuery()
    {
        WebSearchTool tool = new WebSearchTool(new ToolDefinition(
            "web_search",
            "Search the web using Bing Web Search API",
            List.of(new ToolParameterSchema("query", "string", true, "Search query"))), "dummy-key");

        String output = tool.execute(Map.of());
        assertTrue(output.contains("query is required"));
    }
}
