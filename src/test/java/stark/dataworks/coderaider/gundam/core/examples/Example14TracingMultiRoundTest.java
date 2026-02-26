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
import java.util.concurrent.atomic.AtomicInteger;

public class Example14TracingMultiRoundTest
{
    @Test
    public void run()
    {
        AgentDefinition definition = new AgentDefinition();
        definition.setId("trace-multi");
        definition.setName("trace-multi");
        definition.setModel("Qwen/Qwen3-4B");
        definition.setSystemPrompt("You are concise.");

        AgentRegistry agentRegistry = new AgentRegistry();
        agentRegistry.register(new Agent(definition));

        TracingProcessors processors = new TracingProcessors();
        List<String> spans = new ArrayList<>();
        processors.add(event -> spans.add(event.spanName()));

        AgentRunner runner = AgentRunner.builder()
            .llmClient(new CounterLlmClient())
            .toolRegistry(new ToolRegistry())
            .agentRegistry(agentRegistry)
            .traceProvider(new ProcessorTraceProvider(processors))
            .build();

        RunConfiguration config = new RunConfiguration(12, "trace-session", 0.2, 512, "auto", "text", java.util.Map.of());

        String first = ExampleSupport.chatClient(runner, agentRegistry, "trace-multi")
            .prompt()
            .user("turn-1")
            .runConfiguration(config)
            .runHooks(ExampleSupport.noopHooks())
            .stream(false)
            .call()
            .content();
        String second = ExampleSupport.chatClient(runner, agentRegistry, "trace-multi")
            .prompt()
            .user("turn-2")
            .runConfiguration(config)
            .runHooks(ExampleSupport.noopHooks())
            .stream(false)
            .call()
            .content();

        Assertions.assertEquals("answer-1", first);
        Assertions.assertEquals("answer-2", second);
        Assertions.assertTrue(spans.size() >= 2);
    }

    private static final class CounterLlmClient implements ILlmClient
    {
        private final AtomicInteger turn = new AtomicInteger();

        @Override
        public LlmResponse chat(LlmRequest request)
        {
            int i = turn.incrementAndGet();
            return new LlmResponse("answer-" + i, List.of(), null, new TokenUsage(1, 1));
        }
    }
}
