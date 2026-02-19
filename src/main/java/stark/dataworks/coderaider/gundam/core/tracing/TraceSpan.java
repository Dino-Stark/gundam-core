package stark.dataworks.coderaider.gundam.core.tracing;

/**
 * TraceSpan implements run tracing and span publication.
 */
public interface TraceSpan
{

    /**
     * Performs annotate as part of TraceSpan runtime responsibilities.
     * @param key The key used by this operation.
     * @param value The value used by this operation.
     */
    void annotate(String key, String value);

    /**
     * Performs close as part of TraceSpan runtime responsibilities.
     */

    void close();
}
