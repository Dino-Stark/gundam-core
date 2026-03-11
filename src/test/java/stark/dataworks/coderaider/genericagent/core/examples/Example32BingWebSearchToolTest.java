package stark.dataworks.coderaider.genericagent.core.examples;

import io.github.cdimascio.dotenv.Dotenv;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import stark.dataworks.coderaider.genericagent.core.agent.AgentDefinition;
import stark.dataworks.coderaider.genericagent.core.agent.AgentRegistry;
import stark.dataworks.coderaider.genericagent.core.context.ContextResult;
import stark.dataworks.coderaider.genericagent.core.llmspi.adapter.ModelScopeLlmClient;
import stark.dataworks.coderaider.genericagent.core.runner.AgentRunner;
import stark.dataworks.coderaider.genericagent.core.runner.RunConfiguration;
import stark.dataworks.coderaider.genericagent.core.tool.ToolDefinition;
import stark.dataworks.coderaider.genericagent.core.tool.ToolParameterSchema;
import stark.dataworks.coderaider.genericagent.core.tool.ToolRegistry;
import stark.dataworks.coderaider.genericagent.core.tool.builtin.WebSearchTool;

import java.util.List;
import java.util.Map;

/**
 * 32) Single agent using real Bing Web Search tool with streaming output.
 */
public class Example32BingWebSearchToolTest
{
    @Test
    public void run()
    {
        Dotenv env = Dotenv.configure().filename(".env.local").ignoreIfMalformed().ignoreIfMissing().load();
        String apiKey = env.get("MODEL_SCOPE_API_KEY", System.getenv("MODEL_SCOPE_API_KEY"));
        String bingKey = env.get("BING_SEARCH_V7_SUBSCRIPTION_KEY", System.getenv("BING_SEARCH_V7_SUBSCRIPTION_KEY"));
        if (apiKey == null || apiKey.isBlank())
        {
            System.out.println("Skipping test: MODEL_SCOPE_API_KEY not set");
            return;
        }
        if (bingKey == null || bingKey.isBlank())
        {
            System.out.println("Skipping test: BING_SEARCH_V7_SUBSCRIPTION_KEY not set");
            return;
        }

        AgentDefinition agentDef = new AgentDefinition();
        agentDef.setId("web-search-agent");
        agentDef.setName("Web Search Agent");
        agentDef.setModel("Qwen/Qwen3-4B");
        agentDef.setSystemPrompt("You are a web research assistant. Always use web_search to answer time-sensitive questions.");
        agentDef.setToolNames(List.of("web_search"));

        ToolRegistry toolRegistry = new ToolRegistry();
        toolRegistry.register(new WebSearchTool(new ToolDefinition(
            "web_search",
            "Search the web through Bing Web Search API.",
            List.of(
                new ToolParameterSchema("query", "string", true, "Search query"),
                new ToolParameterSchema("count", "number", false, "Max number of results"),
                new ToolParameterSchema("market", "string", false, "Market, e.g. en-US")
            )), bingKey));

        AgentRegistry registry = new AgentRegistry();
        registry.register(agentDef);

        AgentRunner runner = AgentRunner.builder()
            .llmClient(new ModelScopeLlmClient(apiKey, "Qwen/Qwen3-4B"))
            .toolRegistry(toolRegistry)
            .agentRegistry(registry)
            .eventPublisher(ExampleStreamingPublishers.textWithToolLifecycle("BING "))
            .build();

        ContextResult result = runner.chatClient("web-search-agent")
            .prompt()
            .stream(true)
            .user("Find two recent resources that explain Java 21 virtual threads and summarize each in one sentence.")
            .runConfiguration(new RunConfiguration(6, null, 0.1, 1200, "auto", "text", Map.of()))
            .runHooks(ExampleSupport.noopHooks())
            .call()
            .contextResult();

        Assertions.assertNotNull(result.getFinalOutput());
        Assertions.assertFalse(result.getFinalOutput().isBlank());
        System.out.println("Web summary: " + result.getFinalOutput());
    }
}
