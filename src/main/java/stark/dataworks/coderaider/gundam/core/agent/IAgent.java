package stark.dataworks.coderaider.gundam.core.agent;

/**
 * IAgent implements agent definitions and lookup used by runners and handoff resolution.
 */
public interface IAgent
{
    /**
     * Performs definition as part of IAgent runtime responsibilities.
     * @return The value produced by this operation.
     */
    AgentDefinition definition();
}
