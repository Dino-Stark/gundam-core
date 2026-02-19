package stark.dataworks.coderaider.gundam.core.approval;

/**
 * ToolApprovalPolicy implements tool approval workflow.
 * */
public interface ToolApprovalPolicy
{

    /**
     * Performs decide as part of ToolApprovalPolicy runtime responsibilities.
     * @param request The request used by this operation.
     * @return The value produced by this operation.
     */
    ToolApprovalDecision decide(ToolApprovalRequest request);
}
