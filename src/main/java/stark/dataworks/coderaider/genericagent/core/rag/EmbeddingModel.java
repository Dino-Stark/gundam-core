package stark.dataworks.coderaider.genericagent.core.rag;

import java.util.List;

public interface EmbeddingModel
{
    List<Double> embed(String text);
}
