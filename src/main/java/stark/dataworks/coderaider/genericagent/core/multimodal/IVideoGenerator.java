package stark.dataworks.coderaider.genericagent.core.multimodal;

/**
 * IVideoGenerator defines provider-agnostic video generation contract.
 */
public interface IVideoGenerator
{
    GeneratedAsset generate(VideoGenerationRequest request);
}
