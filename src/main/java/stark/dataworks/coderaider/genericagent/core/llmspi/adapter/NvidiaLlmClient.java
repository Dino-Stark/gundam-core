package stark.dataworks.coderaider.genericagent.core.llmspi.adapter;

import java.util.Map;

/**
 * NVIDIA NIM adapter using OpenAI-compatible endpoint.
 * <p>
 * Supports reasoning models with reasoning_content field.
 * Use {@code LlmOptions.providerOptions} to pass NVIDIA-specific parameters like:
 * <ul>
 *     <li>{@code chat_template_kwargs} - for reasoning models (e.g., {"enable_thinking": true, "clear_thinking": false})</li>
 * </ul>
 */
public class NvidiaLlmClient extends OpenAiCompatibleLlmClient
{
    /**
     * Creates an NVIDIA NIM client.
     *
     * @param apiKey NVIDIA API key.
     * @param model  Model name (e.g., "z-ai/glm5", "meta/llama-3.1-8b-instruct").
     */
    public NvidiaLlmClient(String apiKey, String model)
    {
        super(OpenAiCompatibleConfiguration.nvidia(apiKey, model));
    }

    /**
     * Creates an NVIDIA NIM client with reasoning model defaults.
     * <p>
     * Pre-configures chat_template_kwargs for reasoning models.
     *
     * @param apiKey NVIDIA API key.
     * @param model  Model name.
     * @return Client instance with reasoning support enabled.
     */
    public static NvidiaLlmClient withReasoning(String apiKey, String model)
    {
        // Note: Provider options should be set in LlmOptions when creating requests
        // Example: new LlmOptions(1.0, 16384, "auto", "text", 
        //     Map.of("chat_template_kwargs", Map.of("enable_thinking", true, "clear_thinking", false)))
        return new NvidiaLlmClient(apiKey, model);
    }
}
