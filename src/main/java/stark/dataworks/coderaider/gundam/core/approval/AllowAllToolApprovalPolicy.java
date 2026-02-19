package stark.dataworks.coderaider.gundam.core.approval;

/**
 * AllowAllToolApprovalPolicy implements tool approval workflow.
 */
public class AllowAllToolApprovalPolicy implements IToolApprovalPolicy
{

    /**
     * Performs decide as part of AllowAllToolApprovalPolicy runtime responsibilities.
     * @param request The request used by this operation.
     * @return The value produced by this operation.
     */
    @Override
    public ToolApprovalDecision decide(ToolApprovalRequest request)
    {
        return ToolApprovalDecision.approve();
    }
}
