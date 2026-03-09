package stark.dataworks.coderaider.genericagent.core.voice;

/**
 * Contract for voice-capable agent processing pipelines.
 */
public interface IVoicePipeline
{
    VoiceResult process(VoiceInput input, VoicePipelineConfig config);
}
