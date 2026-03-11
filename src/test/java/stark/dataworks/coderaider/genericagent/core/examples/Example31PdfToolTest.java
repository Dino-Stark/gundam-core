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
import stark.dataworks.coderaider.genericagent.core.tool.builtin.PdfTextExtractTool;

import java.util.List;
import java.util.Map;

/**
 * 31) Single agent using PDF text extraction tool with streaming output.
 */
public class Example31PdfToolTest
{
    @Test
    public void run()
    {
        Dotenv env = Dotenv.configure().filename(".env.local").ignoreIfMalformed().ignoreIfMissing().load();
        String apiKey = env.get("MODEL_SCOPE_API_KEY", System.getenv("MODEL_SCOPE_API_KEY"));
        if (apiKey == null || apiKey.isBlank())
        {
            System.out.println("Skipping test: MODEL_SCOPE_API_KEY not set");
            return;
        }

        AgentDefinition agentDef = new AgentDefinition();
        agentDef.setId("pdf-agent");
        agentDef.setName("PDF Agent");
        agentDef.setModel("Qwen/Qwen3-4B");
        agentDef.setSystemPrompt("You are a concise PDF assistant. Always call pdf_text_extract before summarizing.");
        agentDef.setToolNames(List.of("pdf_text_extract"));

        ToolRegistry toolRegistry = new ToolRegistry();
        toolRegistry.register(new PdfTextExtractTool(new ToolDefinition(
            "pdf_text_extract",
            "Extract text from local PDF files.",
            List.of(
                new ToolParameterSchema("path", "string", true, "Local PDF path"),
                new ToolParameterSchema("max_chars", "number", false, "Maximum returned characters")
            ))));

        AgentRegistry registry = new AgentRegistry();
        registry.register(agentDef);

        AgentRunner runner = AgentRunner.builder()
            .llmClient(new ModelScopeLlmClient(apiKey, "Qwen/Qwen3-4B"))
            .toolRegistry(toolRegistry)
            .agentRegistry(registry)
            .eventPublisher(ExampleStreamingPublishers.textWithToolLifecycle("PDF "))
            .build();

        ContextResult result = runner.chatClient("pdf-agent")
            .prompt()
            .stream(true)
            .user("Read src/test/resources/inputs/3D Convolutional Neural Networks for Human Action Recognition.pdf and summarize main topic in 3 bullets.")
            .runConfiguration(new RunConfiguration(6, null, 0.1, 1200, "auto", "text", Map.of()))
            .runHooks(ExampleSupport.noopHooks())
            .call()
            .contextResult();

        Assertions.assertNotNull(result.getFinalOutput());
        Assertions.assertFalse(result.getFinalOutput().isBlank());
        System.out.println("PDF summary: " + result.getFinalOutput());
    }
}
