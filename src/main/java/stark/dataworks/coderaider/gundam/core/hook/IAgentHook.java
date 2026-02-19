package stark.dataworks.coderaider.gundam.core.hook;

import stark.dataworks.coderaider.gundam.core.runtime.ExecutionContext;

/**
 * IAgentHook implements runtime lifecycle extension points.
 */
public interface IAgentHook
{

    /**
     * Performs before run as part of IAgentHook runtime responsibilities.
     * @param context The context used by this operation.
     */
    default void beforeRun(ExecutionContext context)
    {
    }

    /**
     * Performs after run as part of IAgentHook runtime responsibilities.
     * @param context The context used by this operation.
     */

    default void afterRun(ExecutionContext context)
    {
    }

    /**
     * Performs on step as part of IAgentHook runtime responsibilities.
     * @param context The context used by this operation.
     */

    default void onStep(ExecutionContext context)
    {
    }

    /**
     * Performs on model response delta as part of IAgentHook runtime responsibilities.
     * @param context The context used by this operation.
     * @param delta The delta used by this operation.
     */
    default void onModelResponseDelta(ExecutionContext context, String delta)
    {
    }
}
