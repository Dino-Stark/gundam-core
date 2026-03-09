package stark.dataworks.coderaider.genericagent.core.runerror;

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
