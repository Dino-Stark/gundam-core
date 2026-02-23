package stark.dataworks.coderaider.gundam.core.llmspi.adapter;

import stark.dataworks.coderaider.gundam.core.llmspi.ILlmStreamListener;
import stark.dataworks.coderaider.gundam.core.llmspi.LlmOptions;
import stark.dataworks.coderaider.gundam.core.llmspi.LlmRequest;
import stark.dataworks.coderaider.gundam.core.llmspi.LlmResponse;

import java.util.HashMap;
import java.util.Map;

/**
 * ModelScope adapter built on the OpenAI-compatible base client.
 * <p>
 * ModelScope provides OpenAI-compatible API for various models including Qwen.
 * Automatically adds ModelScope-specific options like enable_thinking for reasoning models.
 * Example usage:
 * <pre>
 * ModelScopeLlmClient client = new ModelScopeLlmClient("ms-xxx", "Qwen/Qwen3-4B");
 * </pre>
 */
public class ModelScopeLlmClient extends OpenAiCompatibleLlmClient
{
    private final boolean enableThinking;

    public ModelScopeLlmClient(String apiKey, String model)
    {
        this(apiKey, model, true);
    }

    public ModelScopeLlmClient(String apiKey, String model, boolean enableThinking)
    {
        super(OpenAiCompatibleConfiguration.modelScope(apiKey, model));
        this.enableThinking = enableThinking;
    }

    @Override
    public LlmResponse chat(LlmRequest request)
    {
        // enable_thinking is only supported in stream mode, must be false for non-stream calls
        return super.chat(enhanceRequestForNonStream(request));
    }

    @Override
    public LlmResponse chatStream(LlmRequest request, ILlmStreamListener listener)
    {
        return super.chatStream(enhanceRequestForStream(request), listener);
    }

    private LlmRequest enhanceRequestForNonStream(LlmRequest request)
    {
        Map<String, Object> providerOptions = new HashMap<>(request.getOptions().getProviderOptions());
        // enable_thinking must be false for non-stream calls
        providerOptions.put("enable_thinking", false);
        
        LlmOptions enhancedOptions = new LlmOptions(
            request.getOptions().getTemperature(),
            request.getOptions().getMaxTokens(),
            request.getOptions().getToolChoice(),
            request.getOptions().getResponseFormat(),
            providerOptions
        );
        
        return new LlmRequest(
            request.getModel(),
            request.getMessages(),
            request.getTools(),
            enhancedOptions,
            request.getAttachments()
        );
    }

    private LlmRequest enhanceRequestForStream(LlmRequest request)
    {
        Map<String, Object> providerOptions = new HashMap<>(request.getOptions().getProviderOptions());
        // enable_thinking is only supported in stream mode
        providerOptions.put("enable_thinking", enableThinking);
        
        LlmOptions enhancedOptions = new LlmOptions(
            request.getOptions().getTemperature(),
            request.getOptions().getMaxTokens(),
            request.getOptions().getToolChoice(),
            request.getOptions().getResponseFormat(),
            providerOptions
        );
        
        return new LlmRequest(
            request.getModel(),
            request.getMessages(),
            request.getTools(),
            enhancedOptions,
            request.getAttachments()
        );
    }
}
