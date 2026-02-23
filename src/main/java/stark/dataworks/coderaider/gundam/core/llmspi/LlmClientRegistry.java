package stark.dataworks.coderaider.gundam.core.llmspi;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * LlmClientRegistry resolves a concrete {@link ILlmClient} for a run.
 * <p>
 * Resolution order is intentionally simple (tiny LiteLLM style):
 * <ol>
 *     <li>provider option {@code llmClient} alias</li>
 *     <li>exact model name match</li>
 *     <li>model provider prefix before {@code /} (for example {@code Qwen/Qwen3-4B -> Qwen})</li>
 *     <li>configured default client</li>
 * </ol>
 */
public class LlmClientRegistry
{
    private final Map<String, ILlmClient> clients;
    private final String defaultAlias;

    public LlmClientRegistry(Map<String, ILlmClient> clients, String defaultAlias)
    {
        if (clients == null || clients.isEmpty())
        {
            throw new IllegalArgumentException("clients must not be empty");
        }
        LinkedHashMap<String, ILlmClient> copy = new LinkedHashMap<>();
        clients.forEach((k, v) -> copy.put(Objects.requireNonNull(k, "client alias"), Objects.requireNonNull(v, "client")));
        this.clients = Map.copyOf(copy);
        this.defaultAlias = defaultAlias;
    }

    public static LlmClientRegistry single(ILlmClient client)
    {
        return new LlmClientRegistry(Map.of("default", Objects.requireNonNull(client, "client")), "default");
    }

    public ILlmClient resolve(String model, Map<String, Object> providerOptions)
    {
        String explicitAlias = extractAlias(providerOptions);
        if (explicitAlias != null)
        {
            ILlmClient byAlias = clients.get(explicitAlias);
            if (byAlias != null)
            {
                return byAlias;
            }
        }

        if (model != null)
        {
            ILlmClient byExactModel = clients.get(model);
            if (byExactModel != null)
            {
                return byExactModel;
            }

            int index = model.indexOf('/');
            if (index > 0)
            {
                String providerPrefix = model.substring(0, index);
                ILlmClient byProvider = clients.get(providerPrefix);
                if (byProvider != null)
                {
                    return byProvider;
                }
            }
        }

        if (defaultAlias != null && clients.containsKey(defaultAlias))
        {
            return clients.get(defaultAlias);
        }

        throw new IllegalStateException("No ILlmClient matched model=" + model + ", alias=" + explicitAlias + ", configured aliases=" + clients.keySet());
    }

    private static String extractAlias(Map<String, Object> providerOptions)
    {
        if (providerOptions == null)
        {
            return null;
        }
        Object value = providerOptions.get("llmClient");
        return value instanceof String str && !str.isBlank() ? str : null;
    }
}
