package stark.dataworks.coderaider.genericagent.core.handoff;

/**
 * HandoffFilter implements agent transfer rules between specialized agents.
 */
public interface IHandoffFilter
{

    /**
     * Returns an allow decision.
     *
     * @param handoff handoff.
     * @return {@code true} when the condition is satisfied; otherwise {@code false}.
     */
    boolean allow(Handoff handoff);
}
