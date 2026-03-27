package stark.dataworks.coderaider.genericagent.core.tool.builtin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import stark.dataworks.coderaider.genericagent.core.tool.ToolCategory;
import stark.dataworks.coderaider.genericagent.core.tool.ToolDefinition;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * WebSearchTool implements tool contracts, schema metadata, and executable tool registration.
 */
public class WebSearchTool extends AbstractBuiltinTool
{
    private static final String DEFAULT_ENDPOINT = "https://api.bing.microsoft.com/v7.0/search";
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(20);

    private final String subscriptionKey;
    private final String endpoint;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    /**
     * Initializes WebSearchTool with required runtime dependencies and options.
     *
     * @param definition      definition object.
     * @param subscriptionKey Bing Web Search subscription key.
     */
    public WebSearchTool(ToolDefinition definition, String subscriptionKey)
    {
        this(definition, subscriptionKey, DEFAULT_ENDPOINT, HttpClient.newHttpClient(), new ObjectMapper());
    }

    WebSearchTool(ToolDefinition definition, String subscriptionKey, String endpoint, HttpClient httpClient, ObjectMapper objectMapper)
    {
        super(definition, ToolCategory.WEB_SEARCH);
        this.subscriptionKey = subscriptionKey;
        this.endpoint = endpoint;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Executes this tool operation and returns the produced output.
     *
     * @param input input payload.
     * @return Tool execution output returned by the web search API.
     */
    @Override
    public String execute(Map<String, Object> input)
    {
        if (subscriptionKey == null || subscriptionKey.isBlank())
        {
            return "WebSearch(error): missing Bing subscription key.";
        }
        String query = String.valueOf(input.getOrDefault("query", "")).trim();
        if (query.isEmpty())
        {
            return "WebSearch(error): query is required.";
        }

        int count = parseInt(input.get("count"), 5);
        String market = String.valueOf(input.getOrDefault("market", "en-US"));

        try
        {
            URI uri = URI.create(endpoint
                + "?q=" + URLEncoder.encode(query, StandardCharsets.UTF_8)
                + "&count=" + Math.max(1, Math.min(10, count))
                + "&mkt=" + URLEncoder.encode(market, StandardCharsets.UTF_8)
                + "&textDecorations=false&textFormat=Raw");
            HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(DEFAULT_TIMEOUT)
                .header("Ocp-Apim-Subscription-Key", subscriptionKey)
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() >= 300)
            {
                return "WebSearch(error): HTTP " + response.statusCode() + " -> " + response.body();
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode values = root.path("webPages").path("value");
            if (!values.isArray() || values.isEmpty())
            {
                return "WebSearch(result): no web page results.";
            }

            List<String> lines = new ArrayList<>();
            int idx = 1;
            for (JsonNode item : values)
            {
                String title = item.path("name").asText("");
                String url = item.path("url").asText("");
                String snippet = item.path("snippet").asText("");
                lines.add("[" + idx + "] " + title + "\nURL: " + url + "\nSnippet: " + snippet);
                idx++;
            }
            return String.join("\n\n", lines);
        }
        catch (Exception ex)
        {
            return "WebSearch(error): " + ex.getMessage();
        }
    }

    private static int parseInt(Object raw, int defaultValue)
    {
        if (raw == null)
        {
            return defaultValue;
        }
        if (raw instanceof Number number)
        {
            return number.intValue();
        }
        try
        {
            return Integer.parseInt(raw.toString().trim());
        }
        catch (Exception ex)
        {
            return defaultValue;
        }
    }
}
