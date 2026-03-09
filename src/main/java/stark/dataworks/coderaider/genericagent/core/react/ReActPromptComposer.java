package stark.dataworks.coderaider.genericagent.core.react;

public final class ReActPromptComposer
{
    private static final String DEFAULT_REACT_INSTRUCTIONS = """
        ## Format
        
        [Thought] <action in 5 words>
        [Action] tool=<name> args=<json>
        [Observation] <result>
        
        ## On Complete
        
        [Answer]
        ## Problem
        <issue>
        ## Solution
        <fix>
        ## Changes
        <code diff>
        ## Verification
        - Command: <cmd>
        - Input: <data>
        - Result: <output>
        
        ## Rules
        - Thought: max 5 words
        - No explanations
        - Act immediately
        """;

    private static final String INTENT_RECOGNITION_PREFIX = """
        [Intent]
        Task: <goal>
        Files: <paths>
        Tools: <names>
        
        """;

    private ReActPromptComposer()
    {
    }

    public static String compose(String baseSystemPrompt, String customInstructions, boolean reactEnabled)
    {
        if (!reactEnabled)
        {
            return baseSystemPrompt;
        }

        String normalizedBase = baseSystemPrompt == null ? "" : baseSystemPrompt;
        String instructions = (customInstructions == null || customInstructions.isBlank())
            ? DEFAULT_REACT_INSTRUCTIONS
            : customInstructions;

        return INTENT_RECOGNITION_PREFIX + normalizedBase + System.lineSeparator() + System.lineSeparator() + instructions;
    }
}
