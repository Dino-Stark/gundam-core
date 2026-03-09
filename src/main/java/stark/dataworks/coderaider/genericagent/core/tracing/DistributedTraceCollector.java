package stark.dataworks.coderaider.genericagent.core.tracing;

/**
 * Sink for distributed trace spans.
 */
@FunctionalInterface
public interface DistributedTraceCollector
{
    void collect(DistributedTraceEvent event);
}
