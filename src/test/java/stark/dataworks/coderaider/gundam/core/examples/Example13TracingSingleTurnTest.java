package stark.dataworks.coderaider.gundam.core.examples;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import stark.dataworks.coderaider.gundam.core.agent.Agent;
import stark.dataworks.coderaider.gundam.core.agent.AgentDefinition;
import stark.dataworks.coderaider.gundam.core.agent.AgentRegistry;
import stark.dataworks.coderaider.gundam.core.llmspi.ILlmClient;
import stark.dataworks.coderaider.gundam.core.llmspi.LlmRequest;
import stark.dataworks.coderaider.gundam.core.llmspi.LlmResponse;
import stark.dataworks.coderaider.gundam.core.metrics.TokenUsage;
import stark.dataworks.coderaider.gundam.core.runner.AgentRunner;
import stark.dataworks.coderaider.gundam.core.runner.RunConfiguration;
import stark.dataworks.coderaider.gundam.core.tool.ToolRegistry;
import stark.dataworks.coderaider.gundam.core.tracing.ProcessorTraceProvider;
import stark.dataworks.coderaider.gundam.core.tracing.processor.TracingProcessors;

import java.util.ArrayList;
import java.util.List;

public class Example13TracingSingleTurnTest
{
    @Test
    public void run()
    {
        AgentDefinition definition = new AgentDefinition();
        definition.setId("trace-single");
        definition.setName("trace-single");
        definition.setModel("Qwen/Qwen3-4B");
        definition.setSystemPrompt("You are concise.");

        AgentRegistry agentRegistry = new AgentRegistry();
        agentRegistry.register(new Agent(definition));

        TracingProcessors processors = new TracingProcessors();
        List<String> spans = new ArrayList<>();
        processors.add(event -> spans.add(event.spanName()));

        AgentRunner runner = AgentRunner.builder()
            .llmClient(new EchoLlmClient("single-turn-ok"))
            .toolRegistry(new ToolRegistry())
            .agentRegistry(agentRegistry)
            .traceProvider(new ProcessorTraceProvider(processors))
            .build();

        String output = ExampleSupport.chatClient(runner, agentRegistry, "trace-single")
            .prompt()
            .user("hello")
            .runConfiguration(RunConfiguration.defaults())
            .runHooks(ExampleSupport.noopHooks())
            .stream(false)
            .call()
            .content();

        Assertions.assertEquals("single-turn-ok", output);
        Assertions.assertTrue(spans.contains("agent.model_call"));
    }

    private record EchoLlmClient(String answer) implements ILlmClient
    {
        @Override
        public LlmResponse chat(LlmRequest request)
        {
            return new LlmResponse(answer, List.of(), null, new TokenUsage(1, 1));
        }
    }
}
