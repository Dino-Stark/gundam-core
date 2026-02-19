package stark.dataworks.coderaider.gundam.core.tracing;

import java.time.Instant;
import java.util.Map;

/**
 * TraceEvent implements run tracing and span publication.
 * */
public record TraceEvent(String spanName, Instant timestamp, Map<String, String> attributes)
{
}
