package stark.dataworks.coderaider.genericagent.core.multimodal;

/**
 * IImageGenerator defines provider-agnostic image generation contract.
 */
public interface IImageGenerator
{
    GeneratedAsset generate(ImageGenerationRequest request);
}
