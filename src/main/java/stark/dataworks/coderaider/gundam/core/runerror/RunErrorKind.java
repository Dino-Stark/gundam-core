package stark.dataworks.coderaider.gundam.core.runerror;

/**
 * RunErrorKind implements error classification and handler dispatch.
 */
public enum RunErrorKind
{
    MAX_TURNS,
    INPUT_GUARDRAIL,
    OUTPUT_GUARDRAIL,
    MODEL_INVOCATION,
    HANDOFF,
    TOOL_EXECUTION,
    UNKNOWN
}
