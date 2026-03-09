package stark.dataworks.coderaider.genericagent.core.llmspi.adapter;

/**
 * DeepSeek adapter using OpenAI-compatible endpoint.
 */
public class DeepSeekLlmClient extends OpenAiCompatibleLlmClient
{
    public DeepSeekLlmClient(String apiKey, String model)
    {
        super(OpenAiCompatibleConfiguration.deepSeek(apiKey, model));
    }
}
