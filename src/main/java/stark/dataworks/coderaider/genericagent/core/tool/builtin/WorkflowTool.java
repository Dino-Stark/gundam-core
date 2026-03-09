package stark.dataworks.coderaider.genericagent.core.tool.builtin;

import stark.dataworks.coderaider.genericagent.core.tool.ToolCategory;
import stark.dataworks.coderaider.genericagent.core.tool.ToolDefinition;
import stark.dataworks.coderaider.genericagent.core.workflow.WorkflowExecutionResult;
import stark.dataworks.coderaider.genericagent.core.workflow.WorkflowExecutor;

import java.util.HashMap;
import java.util.Map;

/**
 * WorkflowTool implements tool contracts, schema metadata, and executable tool registration.
 */
public class WorkflowTool extends AbstractBuiltinTool
{
    /**
     * Executor used to run workflow DAGs as a tool.
     */
    private final WorkflowExecutor workflowExecutor;

    /**
     * Initializes WorkflowTool with required runtime dependencies and options.
     *
     * @param definition       definition object.
     * @param workflowExecutor workflow executor.
     */
    public WorkflowTool(ToolDefinition definition, WorkflowExecutor workflowExecutor)
    {
        super(definition, ToolCategory.FUNCTION);
        this.workflowExecutor = workflowExecutor;
    }

    /**
     * Executes this tool operation and returns the produced output.
     *
     * @param input input payload.
     * @return Tool execution output returned by the MCP server.
     */
    @Override
    public String execute(Map<String, Object> input)
    {
        WorkflowExecutionResult result = workflowExecutor.execute(new HashMap<>(input));
        return result.getFinalOutput();
    }
}
