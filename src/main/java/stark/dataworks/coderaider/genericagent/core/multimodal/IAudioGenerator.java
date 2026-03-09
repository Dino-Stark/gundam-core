package stark.dataworks.coderaider.genericagent.core.multimodal;

/**
 * IAudioGenerator defines provider-agnostic audio generation contract.
 */
public interface IAudioGenerator
{
    GeneratedAsset generate(AudioGenerationRequest request);
}
