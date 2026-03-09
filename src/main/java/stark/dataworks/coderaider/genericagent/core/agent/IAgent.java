package stark.dataworks.coderaider.genericagent.core.agent;

/**
 * IAgent implements agent definitions and lookup used by runners and handoff resolution.
 */
public interface IAgent
{
    /**
     * Returns definition metadata for this component.
     *
     * @return Agent definition associated with this instance.
     */
    AgentDefinition definition();
}
