package stark.dataworks.coderaider.genericagent.core.llmspi.adapter;

/**
 * ByteDance Seed (Doubao) adapter using Ark OpenAI-compatible endpoint.
 */
public class SeedLlmClient extends OpenAiCompatibleLlmClient
{
    public SeedLlmClient(String apiKey, String model)
    {
        super(OpenAiCompatibleConfiguration.seed(apiKey, model));
    }
}
