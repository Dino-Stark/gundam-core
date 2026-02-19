package stark.dataworks.coderaider.gundam.core.tracing.data;

import java.util.Map;

/**
 * GenerationSpanData implements run tracing and span publication.
 */
public class GenerationSpanData extends SpanData
{

    /**
     * Performs generation span data as part of GenerationSpanData runtime responsibilities.
     * @param attributes The attributes used by this operation.
     */
    public GenerationSpanData(Map<String, String> attributes)
    {
        super("generation", attributes);
    }
}
