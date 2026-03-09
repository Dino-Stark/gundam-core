package stark.dataworks.coderaider.genericagent.core.llmspi;

import stark.dataworks.coderaider.genericagent.core.multimodal.IAudioGenerator;
import stark.dataworks.coderaider.genericagent.core.multimodal.IImageGenerator;
import stark.dataworks.coderaider.genericagent.core.multimodal.IVideoGenerator;

/**
 * IMultimodalLlmClient extends text chat contracts with provider-side media generation capabilities.
 */
public interface IMultimodalLlmClient extends ILlmClient, IImageGenerator, IVideoGenerator, IAudioGenerator
{
}
