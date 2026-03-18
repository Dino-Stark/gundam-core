package stark.dataworks.coderaider.genericagent.core.examples;

import io.github.cdimascio.dotenv.Dotenv;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import stark.dataworks.coderaider.genericagent.core.agent.AgentDefinition;
import stark.dataworks.coderaider.genericagent.core.agent.AgentRegistry;
import stark.dataworks.coderaider.genericagent.core.context.ContextResult;
import stark.dataworks.coderaider.genericagent.core.editor.ApplyPatchOperation;
import stark.dataworks.coderaider.genericagent.core.editor.ApplyPatchResult;
import stark.dataworks.coderaider.genericagent.core.editor.IApplyPatchEditor;
import stark.dataworks.coderaider.genericagent.core.llmspi.adapter.ModelScopeLlmClient;
import stark.dataworks.coderaider.genericagent.core.llmspi.adapter.SeedLlmClient;
import stark.dataworks.coderaider.genericagent.core.runner.AgentRunner;
import stark.dataworks.coderaider.genericagent.core.runner.RunConfiguration;
import stark.dataworks.coderaider.genericagent.core.tool.ToolDefinition;
import stark.dataworks.coderaider.genericagent.core.tool.ToolParameterSchema;
import stark.dataworks.coderaider.genericagent.core.tool.ToolRegistry;
import stark.dataworks.coderaider.genericagent.core.tool.builtin.ApplyPatchTool;
import stark.dataworks.coderaider.genericagent.core.tool.builtin.LocalShellTool;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;


/**
 * 25) Harder ReAct debug/fix workflow: logical bug fixing with runtime verification.
 * <p>
 * Pattern:
 * - Coordinator: decides delegation order.
 * - Investigator: inspects source + verification evidence.
 * - Fixer: patches iteratively and runs verification.
 * - Reviewer: validates verification result and summarizes.
 */
public class Example25ComplexReActDebugFixTest
{
    // Switch model.
    public static final String API_KEY_NAME = "MODEL_SCOPE_API_KEY";
//    public static final String API_KEY_NAME = "VOLCENGINE_API_KEY";
    private static final String MODEL = "Qwen/Qwen3-4B";
//    private static final String MODEL = "doubao-seed-code-preview-251028";
    private static final Path INPUT_FILE = Path.of("src", "test", "resources", "inputs", "InvoiceSummaryEngine.java");
    private static final Path INPUT_VERIFIER_FILE = Path.of("src", "test", "resources", "inputs", "InvoiceSummaryEngineVerifier.java");
    private static final RunConfiguration EXAMPLE_RUN_CONFIGURATION =
        new RunConfiguration(4, null, 0.0, 1024, "auto", "text", Map.of());

    @Test
    public void run() throws IOException
    {
        long startedAt = System.nanoTime();
        Dotenv env = Dotenv.configure().filename(".env.local").ignoreIfMalformed().ignoreIfMissing().load();
        String apiKey = env.get(API_KEY_NAME, System.getenv(API_KEY_NAME));
        if (apiKey == null || apiKey.isBlank())
        {
            System.out.println("Skipping test: " + API_KEY_NAME + " not set");
            return;
        }

        RuntimeOs runtimeOs = detectRuntimeOs();
        Path workspace = Path.of("src", "test", "resources", "outputs", "react-agent", "example25");
        Path targetFile = resetWorkspace(workspace);

        ToolRegistry toolRegistry = new ToolRegistry();
        toolRegistry.register(createShellTool());
        toolRegistry.register(new ApplyPatchTool(new FileSystemEditor(workspace), false));

        AgentRegistry agentRegistry = createAgentRegistry(runtimeOs, workspace);

        AgentRunner runner = AgentRunner.builder()
            // Switch model.
            .llmClient(new ModelScopeLlmClient(apiKey, MODEL))
//            .llmClient(new SeedLlmClient(apiKey, MODEL))
            .toolRegistry(toolRegistry)
            .agentRegistry(agentRegistry)
            .eventPublisher(ExampleStreamingPublishers.reactThoughtActionObservation())
            .build();

        String behaviorOutput = runBehaviorVerification(runtimeOs, workspace);
        System.out.println("INITIAL_VERIFICATION: " + behaviorOutput.trim());
        logSourceSnapshot(targetFile, "INITIAL_SOURCE");

        ContextResult coordinatorPlan = runner.chatClient("react25-coordinator")
            .prompt()
            .stream(true)
            .user(buildCoordinatorPrompt(runtimeOs, workspace, behaviorOutput))
            .runConfiguration(EXAMPLE_RUN_CONFIGURATION)
            .runHooks(ExampleSupport.noopHooks())
            .call()
            .contextResult();

        ContextResult investigatorResult = runner.chatClient("react25-investigator")
            .prompt()
            .stream(true)
            .user(buildInvestigatorPrompt(runtimeOs, workspace, behaviorOutput))
            .runConfiguration(EXAMPLE_RUN_CONFIGURATION)
            .runHooks(ExampleSupport.noopHooks())
            .call()
            .contextResult();

        System.out.println("COORDINATOR_OUTPUT: " + coordinatorPlan.getFinalOutput());
        System.out.println("INVESTIGATOR_OUTPUT: " + investigatorResult.getFinalOutput());

        ContextResult fixerResult = null;
        ContextResult reviewerResult = null;
        for (int attempt = 1; attempt <= 5; attempt++)
        {
            String sourceSnapshot = Files.readString(targetFile);
            fixerResult = runner.chatClient("react25-fixer")
                .prompt()
                .stream(true)
                .user(buildFixerPrompt(runtimeOs, workspace, attempt, behaviorOutput, investigatorResult.getFinalOutput(), sourceSnapshot))
                .runConfiguration(EXAMPLE_RUN_CONFIGURATION)
                .runHooks(ExampleSupport.noopHooks())
                .call()
                .contextResult();

            reviewerResult = runner.chatClient("react25-reviewer")
                .prompt()
                .stream(true)
                .user(buildReviewerPrompt(runtimeOs, workspace, fixerResult.getFinalOutput(), behaviorOutput))
                .runConfiguration(EXAMPLE_RUN_CONFIGURATION)
                .runHooks(ExampleSupport.noopHooks())
                .call()
                .contextResult();

            System.out.println("FIXER_ATTEMPT_" + attempt + "_OUTPUT: " + fixerResult.getFinalOutput());
            System.out.println("REVIEWER_ATTEMPT_" + attempt + "_OUTPUT: " + reviewerResult.getFinalOutput());

            behaviorOutput = runBehaviorVerification(runtimeOs, workspace);
            System.out.println("ATTEMPT_" + attempt + "_VERIFICATION: " + behaviorOutput.trim());
            logSourceSnapshot(targetFile, "ATTEMPT_" + attempt + "_SOURCE");
            if (behaviorOutput.contains("BEHAVIOR_OK"))
            {
                break;
            }
        }

        Assertions.assertNotNull(coordinatorPlan, "Expected coordinator output");
        Assertions.assertNotNull(investigatorResult, "Expected investigator output");
        Assertions.assertNotNull(fixerResult, "Expected fixer output");
        Assertions.assertNotNull(reviewerResult, "Expected reviewer output");

        Assertions.assertTrue(coordinatorPlan.getFinalOutput() != null && !coordinatorPlan.getFinalOutput().isBlank(),
            "Expected coordinator summary output");
        Assertions.assertTrue(investigatorResult.getFinalOutput() != null && !investigatorResult.getFinalOutput().isBlank(),
            "Expected investigator summary output");
        Assertions.assertTrue(fixerResult.getFinalOutput() != null && !fixerResult.getFinalOutput().isBlank(),
            "Expected fixer summary output");
        Assertions.assertTrue(reviewerResult.getFinalOutput() != null && !reviewerResult.getFinalOutput().isBlank(),
            "Expected reviewer summary output");
        
        Assertions.assertTrue(behaviorOutput.contains("BEHAVIOR_OK"), 
            "Agent must fix all bugs successfully. Verification output: " + behaviorOutput);
        
        System.out.println("FINAL_VERIFICATION: " + behaviorOutput.trim());
        long elapsedSeconds = (System.nanoTime() - startedAt) / 1_000_000_000L;
        Assertions.assertTrue(elapsedSeconds <= 120, "Expected runtime (<=120s) but took " + elapsedSeconds + "s");
    }

    private static Path resetWorkspace(Path workspace) throws IOException
    {
        if (Files.exists(workspace))
        {
            Files.walk(workspace)
                .sorted((a, b) -> b.getNameCount() - a.getNameCount())
                .forEach(path ->
                {
                    try
                    {
                        Files.delete(path);
                    }
                    catch (IOException ex)
                    {
                        throw new RuntimeException(ex);
                    }
                });
        }
        Files.createDirectories(workspace);
        Path targetFile = workspace.resolve("InvoiceSummaryEngine.java");
        Files.writeString(targetFile, Files.readString(INPUT_FILE));
        stageVerifierSource(workspace.resolve("InvoiceSummaryEngineVerifier.java"));
        return targetFile;
    }

    private static AgentRegistry createAgentRegistry(RuntimeOs runtimeOs, Path workspace)
    {
        AgentRegistry registry = new AgentRegistry();
        registry.register(createCoordinatorAgent(runtimeOs, workspace));
        registry.register(createInvestigatorAgent(runtimeOs, workspace));
        registry.register(createFixerAgent(runtimeOs, workspace));
        registry.register(createReviewerAgent(runtimeOs, workspace));
        return registry;
    }

    private static AgentDefinition createCoordinatorAgent(RuntimeOs runtimeOs, Path workspace)
    {
        AgentDefinition def = new AgentDefinition();
        def.setId("react25-coordinator");
        def.setName("Complex ReAct Coordinator");
        def.setModel(MODEL);
        def.setReactEnabled(true);
        def.setSystemPrompt("""
            You are a workflow coordinator for Java debugging.

            Build a concise execution plan and enforce this order:
            1) Investigator finds concrete bug evidence
            2) Fixer patches and verifies
            3) Reviewer validates verification output

            OS: %s
            Workspace: %s
            """.formatted(runtimeOs.displayName, workspace));
        def.setReactInstructions("Plan the delegation in 3-5 short bullets with concrete commands.");
        def.setToolNames(List.of("local_shell"));
        def.setModelProviderOptions(Map.of("working_directory", workspace.toString()));
        def.setModelReasoning(Map.of("effort", "low"));
        return def;
    }

    private static AgentDefinition createInvestigatorAgent(RuntimeOs runtimeOs, Path workspace)
    {
        AgentDefinition def = new AgentDefinition();
        def.setId("react25-investigator");
        def.setName("Complex ReAct Investigator");
        def.setModel(MODEL);
        def.setReactEnabled(true);
        def.setSystemPrompt("""
            You are a Java bug investigator.

            Inspect source and verifier output to identify root causes.
            Report exact bug locations and expected behavior.

            OS: %s
            Workspace: %s
            Verify command: %s
            """.formatted(runtimeOs.displayName, workspace, runtimeOs.verifyCommand(workspace)));
        def.setReactInstructions("Read file + verifier output, then return a concrete root-cause list for each failing behavior.");
        def.setToolNames(List.of("local_shell"));
        def.setModelProviderOptions(Map.of("working_directory", workspace.toString()));
        def.setModelReasoning(Map.of("effort", "low"));
        return def;
    }

    private static AgentDefinition createFixerAgent(RuntimeOs runtimeOs, Path workspace)
    {
        AgentDefinition def = new AgentDefinition();
        def.setId("react25-fixer");
        def.setName("Complex ReAct Fixer");
        def.setModel(MODEL);
        def.setReactEnabled(true);
        def.setSystemPrompt("""
            You are a Java code fixer.

            Rules:
            - Patch only InvoiceSummaryEngine.java
            - Fix the root causes from investigator evidence
            - Run verification after patching
            - Stop only when verification output contains BEHAVIOR_OK
            - Follow the coding style

            OS: %s
            Workspace: %s
            Verify command: %s
            """.formatted(runtimeOs.displayName, workspace, runtimeOs.verifyCommand(workspace)));
        def.setReactInstructions("Read → patch -> verify -> if fail, patch again. Keep final response concise with exact fixes.");
        def.setToolNames(List.of("apply_patch", "local_shell"));
        def.setModelProviderOptions(Map.of("working_directory", workspace.toString()));
        def.setModelReasoning(Map.of("effort", "low"));
        return def;
    }

    private static AgentDefinition createReviewerAgent(RuntimeOs runtimeOs, Path workspace)
    {
        AgentDefinition def = new AgentDefinition();
        def.setId("react25-reviewer");
        def.setName("Complex ReAct Reviewer");
        def.setModel(MODEL);
        def.setReactEnabled(true);
        def.setSystemPrompt("""
            You are a strict verifier for Java bug-fix tasks.

            Validate with runtime evidence. PASS only when output contains BEHAVIOR_OK.

            OS: %s
            Workspace: %s
            Verify command: %s
            """.formatted(runtimeOs.displayName, workspace, runtimeOs.verifyCommand(workspace)));
        def.setReactInstructions("Run verifier and return PASS/FAIL with command output evidence.");
        def.setToolNames(List.of("local_shell"));
        def.setModelProviderOptions(Map.of("working_directory", workspace.toString()));
        def.setModelReasoning(Map.of("effort", "low"));
        return def;
    }

    private static String buildCoordinatorPrompt(RuntimeOs runtimeOs, Path workspace, String behaviorOutput)
    {
        return """
            Build a concise execution plan for fixing InvoiceSummaryEngine.java.

            Current verification output:
            %s

            Required behavior contract:
            %s

            Require this sequence:
            - Investigator gathers evidence against the behavior contract
            - Fixer patches and verifies until BEHAVIOR_OK
            - Reviewer validates with runtime evidence

            Runtime OS: %s
            Workspace: %s
            """.formatted(behaviorOutput.trim(), expectedBehaviorContract(), runtimeOs.displayName, workspace);
    }

    private static String buildInvestigatorPrompt(RuntimeOs runtimeOs, Path workspace, String behaviorOutput)
    {
        return """
            Investigate root causes in InvoiceSummaryEngine.java.

            Current verification output:
            %s

            Required behavior contract:
            %s

            Known likely bug categories to check explicitly:
            - Subtotal loop skips first item by starting from index 1.
            - Food tax rate may be wrong (expected 0.08 for this verifier).
            - round2 may be rounding to 1 decimal instead of 2 decimals.

            Steps:
            1) Print InvoiceSummaryEngine.java
            2) Compile and run verifier
            3) Confirm or reject each likely bug category with evidence
            4) Produce a concrete fixer checklist

            Runtime OS: %s
            Workspace: %s
            File print command: %s
            Verify command: %s
            """.formatted(behaviorOutput.trim(), expectedBehaviorContract(), runtimeOs.displayName, workspace,
            runtimeOs.printFileCommand(workspace, "InvoiceSummaryEngine.java"), runtimeOs.verifyCommand(workspace));
    }

    private static String buildFixerPrompt(RuntimeOs runtimeOs, Path workspace, int attempt,
                                           String behaviorOutput, String investigationOutput, String sourceSnapshot)
    {
        return """
            Attempt %d to fix InvoiceSummaryEngine.java.

            Investigator findings:
            %s

            Required behavior contract:
            %s

            Current verification status:
            %s

            Current code:
            %s

            Fix all bugs and verify with: %s
            Target: output should contain BEHAVIOR_OK
            Do not stop at partial fixes. Keep patching until both caseA and caseB pass.

            OS: %s
            Workspace: %s
            """.formatted(attempt, investigationOutput, expectedBehaviorContract(), behaviorOutput.trim(), sourceSnapshot,
            runtimeOs.verifyCommand(workspace), runtimeOs.displayName, workspace);
    }


    private static String expectedBehaviorContract()
    {
        return """
            - caseA: calculateTotal([20.0, 30.0, 50.0], "food", true) must equal 102.6
            - caseB: calculateTotal([10.0, 40.0], "book", false) must equal 52.0
            - Verifier must print BEHAVIOR_OK
            """;
    }

    private static String buildReviewerPrompt(RuntimeOs runtimeOs, Path workspace, String fixerOutput, String behaviorOutput)
    {
        return """
            Review the fix result for InvoiceSummaryEngine.java.

            Fixer output:
            %s

            Latest host-side verification output:
            %s

            Execute verifier again:
            %s

            Return PASS only when output contains BEHAVIOR_OK, else FAIL with reasons.

            OS: %s
            Workspace: %s
            """.formatted(fixerOutput, behaviorOutput.trim(), runtimeOs.verifyCommand(workspace), runtimeOs.displayName, workspace);
    }

    private static void logSourceSnapshot(Path targetFile, String prefix) throws IOException
    {
        String source = Files.readString(targetFile);
        System.out.println(prefix + ":\n" + source);
    }

    private static void stageVerifierSource(Path targetVerifierFile) throws IOException
    {
        if (!Files.exists(INPUT_VERIFIER_FILE))
        {
            throw new IOException("Missing verifier input source: " + INPUT_VERIFIER_FILE);
        }
        Files.writeString(targetVerifierFile, Files.readString(INPUT_VERIFIER_FILE));
    }

    private static String runBehaviorVerification(RuntimeOs runtimeOs, Path workspace)
    {
        ProcessBuilder builder = createBehaviorVerificationProcessBuilder(runtimeOs, workspace);
        builder.redirectErrorStream(true);
        try
        {
            Process process = builder.start();
            String output = new String(process.getInputStream().readAllBytes(), runtimeConsoleCharset());
            int exitCode = process.waitFor();
            if (exitCode != 0)
            {
                return output + "\nEXIT=" + exitCode;
            }
            return output;
        }
        catch (InterruptedException ex)
        {
            Thread.currentThread().interrupt();
            return "Behavior verification failed: " + ex.getMessage();
        }
        catch (IOException ex)
        {
            return "Behavior verification failed: " + ex.getMessage();
        }
    }

    private static Charset runtimeConsoleCharset()
    {
        String nativeEncoding = System.getProperty("native.encoding");
        if (nativeEncoding != null && !nativeEncoding.isBlank())
        {
            try
            {
                return Charset.forName(nativeEncoding);
            }
            catch (Exception ignored)
            {
            }
        }
        return Charset.defaultCharset();
    }

    private static ProcessBuilder createBehaviorVerificationProcessBuilder(RuntimeOs runtimeOs, Path workspace)
    {
        return switch (runtimeOs)
        {
            case WINDOWS -> new ProcessBuilder("cmd", "/c",
                "cd /d \"" + workspace + "\" && javac InvoiceSummaryEngine.java InvoiceSummaryEngineVerifier.java && java InvoiceSummaryEngineVerifier");
            case MACOS, LINUX -> new ProcessBuilder("bash", "-lc",
                "cd '" + workspace + "' && javac InvoiceSummaryEngine.java InvoiceSummaryEngineVerifier.java && java InvoiceSummaryEngineVerifier");
        };
    }

    private static RuntimeOs detectRuntimeOs()
    {
        String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (osName.contains("win"))
        {
            return RuntimeOs.WINDOWS;
        }
        if (osName.contains("mac"))
        {
            return RuntimeOs.MACOS;
        }
        return RuntimeOs.LINUX;
    }

    public enum RuntimeOs
    {
        WINDOWS("windows"),
        MACOS("macos"),
        LINUX("linux");

        private final String displayName;

        RuntimeOs(String displayName)
        {
            this.displayName = displayName;
        }

        private String verifyCommand(Path workspace)
        {
            return "javac InvoiceSummaryEngine.java InvoiceSummaryEngineVerifier.java && java InvoiceSummaryEngineVerifier";
        }

        private String printFileCommand(Path workspace, String fileName)
        {
            return switch (this)
            {
                case WINDOWS -> "type " + fileName;
                case MACOS, LINUX -> "cat " + fileName;
            };
        }
    }

    private static LocalShellTool createShellTool()
    {
        ToolDefinition definition = new ToolDefinition(
            "local_shell",
            "Execute a local shell command and return stdout/stderr.",
            List.of(new ToolParameterSchema("command", "string", true, "Shell command to execute")));
        return new LocalShellTool(definition);
    }

    public static final class FileSystemEditor implements IApplyPatchEditor
    {
        private final Path root;

        public FileSystemEditor(Path root)
        {
            this.root = root;
        }

        @Override
        public ApplyPatchResult createFile(ApplyPatchOperation operation)
        {
            try
            {
                Path path = safeResolve(operation.getPath());
                if (path.getParent() != null)
                {
                    Files.createDirectories(path.getParent());
                }
                Files.writeString(path, operation.getDiff());
                return ApplyPatchResult.completed("Created " + operation.getPath());
            }
            catch (IOException ex)
            {
                return ApplyPatchResult.failed("Create failed: " + ex.getMessage());
            }
        }

        @Override
        public ApplyPatchResult updateFile(ApplyPatchOperation operation)
        {
            try
            {
                Path path = safeResolve(operation.getPath());
                if (!Files.exists(path))
                {
                    return ApplyPatchResult.failed("File not found: " + operation.getPath());
                }
                String diff = operation.getDiff();
                if (containsUnsupportedDiffMarkers(diff))
                {
                    return ApplyPatchResult.failed("Unsupported diff format. Use simple diff only with context/'-'/'+' lines.");
                }
                if (!hasExplicitChangeLines(diff))
                {
                    return ApplyPatchResult.failed("Invalid diff: must include '-' and '+' change lines.");
                }
                String original = Files.readString(path);
                String updated = applySmartDiff(original, diff);
                
                // Critical: Check if any actual changes were applied
                if (updated.equals(original))
                {
                    return ApplyPatchResult.failed(buildNoChangeAppliedError(diff));
                }
                
                Files.writeString(path, updated);
                return ApplyPatchResult.completed("Updated " + operation.getPath());
            }
            catch (Exception ex)
            {
                return ApplyPatchResult.failed("Update failed: " + ex.getMessage());
            }
        }
        
        private static String buildNoChangeAppliedError(String diff)
        {
            StringBuilder errorMsg = new StringBuilder();
            errorMsg.append("Diff failed: NO CHANGES were applied to the file.\n\n");
            errorMsg.append("Your diff content:\n");
            if (diff != null)
            {
                String[] lines = diff.split("\\R", -1);
                int shown = 0;
                for (String line : lines)
                {
                    if (shown >= 6) break;
                    if (line.startsWith("-") || line.startsWith("+"))
                    {
                        String display = line.length() > 80 ? line.substring(0, 80) + "..." : line;
                        errorMsg.append("  ").append(display).append("\n");
                        shown++;
                    }
                }
            }
            errorMsg.append("\nPossible reasons:\n");
            errorMsg.append("1. The '-' content does NOT exist in the file (check exact spacing/indentation).\n");
            errorMsg.append("2. The '-' and '+' lines are IDENTICAL (no actual change).\n");
            errorMsg.append("3. The file was already modified and content has changed.\n");
            errorMsg.append("\nTo fix: Read the file again, then provide a correct diff.\n");
            return errorMsg.toString();
        }

        @Override
        public ApplyPatchResult deleteFile(ApplyPatchOperation operation)
        {
            try
            {
                Path path = safeResolve(operation.getPath());
                Files.deleteIfExists(path);
                return ApplyPatchResult.completed("Deleted " + operation.getPath());
            }
            catch (IOException ex)
            {
                return ApplyPatchResult.failed("Delete failed: " + ex.getMessage());
            }
        }

        private Path safeResolve(String relativePath)
        {
            Path raw = Path.of(relativePath == null ? "" : relativePath).normalize();
            Path candidate;
            if (raw.isAbsolute())
            {
                candidate = raw;
            }
            else
            {
                candidate = root.resolve(raw).normalize();
            }
            if (candidate.startsWith(root) && Files.exists(candidate))
            {
                return candidate;
            }

            // Graceful fallback for model-generated long paths: keep file name in workspace.
            Path fileName = raw.getFileName();
            if (fileName != null)
            {
                Path byName = root.resolve(fileName).normalize();
                if (byName.startsWith(root))
                {
                    return byName;
                }
            }
            throw new IllegalArgumentException("Path escapes workspace: " + relativePath);
        }

        private static boolean hasExplicitChangeLines(String diff)
        {
            if (diff == null || diff.isBlank())
            {
                return false;
            }
            boolean hasMinus = false;
            boolean hasPlus = false;
            String[] lines = diff.split("\\R", -1);
            for (String line : lines)
            {
                if (line.startsWith("-"))
                {
                    hasMinus = true;
                }
                else if (line.startsWith("+"))
                {
                    hasPlus = true;
                }
            }
            return hasMinus && hasPlus;
        }

        private static boolean containsUnsupportedDiffMarkers(String diff)
        {
            if (diff == null)
            {
                return false;
            }
            return diff.contains("diff --git")
                || diff.contains("\n--- ")
                || diff.contains("\n+++ ")
                || diff.contains("\n@@");
        }

        private static String applySmartDiff(String original, String diff)
        {
            // Validate diff format first
            validateDiffFormat(diff);
            
            if (looksLikeReplacementOnlyDiff(diff))
            {
                return applySimpleReplacementDiff(original, diff);
            }
            try
            {
                String result = ApplyPatchTool.applyDiff(original, diff);
                // Verify the result is valid (not corrupted)
                if (result != null && !result.equals(original))
                {
                    validateResultNotCorrupted(original, result);
                }
                return result;
            }
            catch (Exception ignored)
            {
                return applySimpleReplacementDiff(original, diff);
            }
        }
        
        /**
         * Validate diff format to catch common mistakes.
         */
        private static void validateDiffFormat(String diff)
        {
            if (diff == null || diff.isBlank())
            {
                throw new IllegalArgumentException("Empty diff.");
            }
            
            String[] lines = diff.split("\\R", -1);
            for (String line : lines)
            {
                if (line.isBlank()) continue;
                
                // Check for lines that look like code but don't start with - or +
                if (!line.startsWith("-") && !line.startsWith("+"))
                {
                    if (line.startsWith(" ") || line.startsWith("\t"))
                    {
                        throw new IllegalArgumentException(
                            "Invalid diff format: line '" + line.substring(0, Math.min(40, line.length())) + 
                            "...' starts with whitespace but is not a valid context line.\n" +
                            "For simple replacement diff, ALL lines must start with '-' or '+'.\n" +
                            "Please check your diff format and ensure proper '-old' and '+new' pairs."
                        );
                    }
                }
                
                // Check for - and + lines with identical content (no actual change)
                if (line.startsWith("-") && lines.length > 0)
                {
                    String content = line.substring(1);
                    for (String otherLine : lines)
                    {
                        if (otherLine.startsWith("+") && otherLine.substring(1).equals(content))
                        {
                            throw new IllegalArgumentException(
                                "Invalid diff: '-" + content.substring(0, Math.min(50, content.length())) + 
                                "...' and '+" + content.substring(0, Math.min(50, content.length())) + 
                                "...' are IDENTICAL. There is no actual change.\n" +
                                "Please ensure '-' line contains the OLD content and '+' line contains the NEW content."
                            );
                        }
                    }
                }
            }
        }
        
        /**
         * Check if the result file is corrupted (e.g., structure destroyed).
         */
        private static void validateResultNotCorrupted(String original, String result)
        {
            long originalFuncCount = countOccurrences(original, "def ");
            long resultFuncCount = countOccurrences(result, "def ");
            
            if (resultFuncCount < originalFuncCount)
            {
                throw new IllegalArgumentException(
                    "Diff would corrupt the file: function definitions would be lost.\n" +
                    "Original had " + originalFuncCount + " function(s), result would have " + resultFuncCount + ".\n" +
                    "Please verify your diff is correct and targets the right lines."
                );
            }
            
            String[] resultLines = result.split("\\R", -1);
            if (resultLines.length > 0)
            {
                String firstLine = resultLines[0];
                if (firstLine.startsWith("    ") || firstLine.startsWith("\t"))
                {
                    if (original.split("\\R", -1)[0].startsWith("import ") || 
                        original.split("\\R", -1)[0].startsWith("def ") ||
                        original.split("\\R", -1)[0].startsWith("class ") ||
                        original.split("\\R", -1)[0].startsWith("#") ||
                        original.split("\\R", -1)[0].isBlank())
                    {
                        throw new IllegalArgumentException(
                            "Diff would corrupt the file: first line would be incorrectly indented.\n" +
                            "First line of result: '" + firstLine.substring(0, Math.min(50, firstLine.length())) + "...'\n" +
                            "Please verify your diff targets the correct location in the file."
                        );
                    }
                }
            }
        }
        
        private static long countOccurrences(String text, String substring)
        {
            long count = 0;
            int index = 0;
            while ((index = text.indexOf(substring, index)) != -1)
            {
                count++;
                index += substring.length();
            }
            return count;
        }

        private static boolean looksLikeReplacementOnlyDiff(String diff)
        {
            if (diff == null || diff.isBlank())
            {
                return false;
            }
            boolean hasMinus = false;
            boolean hasPlus = false;
            String[] lines = diff.split("\\R", -1);
            for (String line : lines)
            {
                if (line.isBlank())
                {
                    continue;
                }
                if (line.startsWith("-"))
                {
                    hasMinus = true;
                    continue;
                }
                if (line.startsWith("+"))
                {
                    hasPlus = true;
                    continue;
                }
                return false;
            }
            return hasMinus && hasPlus;
        }

        private static String applySimpleReplacementDiff(String original, String diff)
        {
            if (diff == null || diff.isBlank())
            {
                throw new IllegalArgumentException("Empty diff.");
            }

            String[] lines = diff.split("\\R", -1);
            String updated = original;
            String pendingRemoved = null;
            int replacedCount = 0;
            List<String> unmatchedOldLines = new ArrayList<>();
            for (String line : lines)
            {
                if (line.startsWith("-"))
                {
                    pendingRemoved = line.substring(1);
                    continue;
                }
                if (line.startsWith("+") && pendingRemoved != null)
                {
                    String added = line.substring(1);
                    int index = updated.indexOf(pendingRemoved);
                    if (index >= 0)
                    {
                        updated = updated.substring(0, index) + added + updated.substring(index + pendingRemoved.length());
                        replacedCount++;
                    }
                    else
                    {
                        unmatchedOldLines.add(pendingRemoved);
                    }
                    pendingRemoved = null;
                    continue;
                }
                if (!line.isBlank())
                {
                    pendingRemoved = null;
                }
            }
            if (replacedCount == 0)
            {
                StringBuilder errorMsg = new StringBuilder();
                errorMsg.append("Diff failed: content not found in file.\n\n");
                errorMsg.append("The following old content was NOT found:\n");
                for (int i = 0; i < Math.min(3, unmatchedOldLines.size()); i++)
                {
                    String unmatched = unmatchedOldLines.get(i);
                    String display = unmatched.length() > 100 ? unmatched.substring(0, 100) + "..." : unmatched;
                    errorMsg.append("  \"").append(display).append("\"\n");
                }
                errorMsg.append("\nTo fix this:\n");
                errorMsg.append("1. Read the file again to get its CURRENT content.\n");
                errorMsg.append("2. Compare your diff with the actual file content.\n");
                errorMsg.append("3. Provide a new diff that matches EXACTLY what's in the file (including whitespace).\n");
                throw new IllegalArgumentException(errorMsg.toString());
            }
            return updated;
        }
    }
}
