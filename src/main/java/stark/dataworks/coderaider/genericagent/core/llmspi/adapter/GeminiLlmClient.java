package stark.dataworks.coderaider.genericagent.core.llmspi.adapter;

/**
 * Gemini adapter using Google's OpenAI-compatible endpoint.
 */
public class GeminiLlmClient extends OpenAiCompatibleLlmClient
{
    public GeminiLlmClient(String apiKey, String model)
    {
        super(OpenAiCompatibleConfiguration.gemini(apiKey, model));
    }
}
